package io.github.yutoutcourt.itfollows.tracking;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.entity.ModEntities;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Pilote la traque côté serveur (Phase 2a) : délai de grâce, sélection de la cible
 * (le plus fatigué via {@link TargetSelector}), puis spawn/despawn dynamique de
 * {@link StalkerEntity} près de la cible. Échantillonné toutes les
 * {@code hauntSampleIntervalTicks} pour ne pas tout calculer chaque tick.
 *
 * <p>État de session (compteur, début de grâce, cible forcée) gardé en mémoire ; la vérité
 * persistante (cible, entité, phase) vit dans {@link StalkTrackerState}.
 */
public final class HauntController {

    private static int tickCounter;
    /** Tick (server tick count) auquel la grâce a démarré, ou -1 tant qu'aucun joueur n'est venu. */
    private static long graceStartTick = -1;
    /** {@code true} si une cible a été forcée via commande debug (ignore la grâce). */
    private static boolean forced;

    private HauntController() {
    }

    public static void tick(MinecraftServer server) {
        ItFollowsConfig config = ItFollowsConfig.get();

        if (graceStartTick < 0 && !server.getPlayerList().getPlayers().isEmpty()) {
            graceStartTick = server.getTickCount();
        }

        // Laisse vérifiée chaque tick (et non par échantillon) : aux vitesses élevées (élytres,
        // cheval, /tp) la cible peut sortir de la zone de simulation entre deux échantillons. On
        // dématérialise dès le dépassement, en sauvant la position, pour ne jamais geler l'entité.
        leashCheck(server, config);

        if (++tickCounter < config.hauntSampleIntervalTicks) {
            return;
        }
        tickCounter = 0;
        runSample(server, config);
    }

    private static void runSample(MinecraftServer server, ItFollowsConfig config) {
        StalkTrackerState state = StalkTrackerState.get(server);

        if (!forced && !isGraceOver(server, config)) {
            // Pas encore de traque : on retire l'entité et on repart d'une poursuite vierge.
            despawn(server);
            state.clearVirtual();
            return;
        }

        // Cible courante encore valable ? sinon on (re)sélectionne le plus fatigué,
        // en excluant la dernière victime pour ne pas la re-maudire dès son réveil.
        ServerPlayer target = resolveTargetPlayer(server, state);
        if (target == null) {
            ServerPlayer chosen = TargetSelector.chooseTarget(server, state.getLastVictim());
            if (chosen == null) {
                // Aucun joueur traquable (tout le monde hors ligne/exempté) : on gèle la poursuite.
                despawn(server);
                return;
            }
            // Une nouvelle cible (différente de la victime) lève l'exclusion.
            if (state.getLastVictim() != null && !chosen.getUUID().equals(state.getLastVictim())) {
                state.setLastVictim(null);
            }
            state.setTargetPlayer(chosen.getUUID());
            target = chosen;
        }

        ensureVirtualSeeded(server, state, target, config);

        StalkerEntity entity = resolveEntity(server, state);
        if (entity != null && !entity.isRemoved()) {
            // Déjà matérialisée : la dématérialisation est gérée chaque tick par leashCheck ; ici on
            // se contente d'entretenir le suivi (cible courante + position logique tenue à jour).
            entity.setTargetUuid(target.getUUID());
            state.setVirtual(entity.position(), entity.level().dimension());
            return;
        }

        // Pas matérialisée : la poursuite virtuelle avance, et se matérialise si elle est assez proche.
        simulateAndMaybeMaterialize(server, state, target, config);
    }

    /**
     * Fait avancer la position logique vers la cible (3D), puis matérialise l'entité réelle si la
     * distance pondérée passe sous {@link #materializeDistance} (dérivée de la distance de simulation).
     * L'écart vertical est pénalisé (cf. {@code stalkerVerticalPenalty}) pour récompenser la fuite en hauteur/profondeur.
     */
    private static void simulateAndMaybeMaterialize(MinecraftServer server, StalkTrackerState state,
                                                    ServerPlayer target, ItFollowsConfig config) {
        double step = config.stalkerVirtualSpeed * (config.hauntSampleIntervalTicks / 20.0);
        Vec3 from = state.getVirtualPos();
        Vec3 to = target.position();
        Vec3 delta = to.subtract(from);
        double len = delta.length();
        Vec3 next = len <= step ? to : from.add(delta.scale(step / len));
        state.setVirtual(next, target.level().dimension());

        double horizontal = Math.hypot(to.x - next.x, to.z - next.z);
        double vertical = Math.abs(to.y - next.y);
        double effective = horizontal + config.stalkerVerticalPenalty * vertical;
        if (effective <= materializeDistance(server, config)) {
            materialize(server, state, target, next, config);
        }
    }

    // --- API debug (commande) ---

    /** Force la cible (ignore la grâce) et fait apparaître l'entité immédiatement près d'elle. */
    public static void forceTarget(MinecraftServer server, ServerPlayer target) {
        forced = true;
        ItFollowsConfig config = ItFollowsConfig.get();
        StalkTrackerState state = StalkTrackerState.get(server);
        state.setTargetPlayer(target.getUUID());
        // Amorce la position logique derrière la cible puis matérialise tout de suite (debug).
        Vec3 behind = behindTarget(target, materializeDistance(server, config));
        state.setVirtual(behind, target.level().dimension());
        materialize(server, state, target, behind, config);
    }

    /**
     * À appeler quand un joueur meurt (event {@code AFTER_DEATH}). Si c'était la cible traquée,
     * on la mémorise comme dernière victime, on efface la cible et on retire l'entité : le prochain
     * échantillonnage choisira quelqu'un d'autre (cf. {@link TargetSelector#chooseTarget}).
     */
    public static void onPlayerDeath(ServerPlayer dead) {
        MinecraftServer server = dead.getServer();
        if (server == null) {
            return;
        }
        StalkTrackerState state = StalkTrackerState.get(server);
        UUID targetId = state.getTargetPlayer();
        if (targetId == null || !targetId.equals(dead.getUUID())) {
            return;
        }
        state.setLastVictim(dead.getUUID());
        state.setTargetPlayer(null);
        // La poursuite repart de zéro pour la prochaine victime (réamorcée derrière elle).
        state.clearVirtual();
        // La traque forcée (debug) s'arrête à la mort de la cible imposée.
        forced = false;
        despawn(server);
    }

    /** Retire l'entité courante et met la traque en pause (cible effacée, grâce non forcée). */
    public static void despawn(MinecraftServer server) {
        StalkTrackerState state = StalkTrackerState.get(server);
        discardAllStalkers(server);
        state.setStalkerEntityId(null);
    }

    /** Arrête complètement la traque (debug) : retire l'entité et oublie la cible. */
    public static void stop(MinecraftServer server) {
        forced = false;
        StalkTrackerState state = StalkTrackerState.get(server);
        discardAllStalkers(server);
        state.setStalkerEntityId(null);
        state.setTargetPlayer(null);
        state.clearVirtual();
    }

    public static boolean isForced() {
        return forced;
    }

    /** Ticks restants avant la fin de la grâce (0 si écoulée ou forcée). */
    public static long graceRemainingTicks(MinecraftServer server, ItFollowsConfig config) {
        if (forced || graceStartTick < 0) {
            return forced ? 0 : config.graceTicks;
        }
        long elapsed = server.getTickCount() - graceStartTick;
        return Math.max(0, config.graceTicks - elapsed);
    }

    // --- Helpers ---

    private static boolean isGraceOver(MinecraftServer server, ItFollowsConfig config) {
        return graceStartTick >= 0 && (server.getTickCount() - graceStartTick) >= config.graceTicks;
    }

    /**
     * Dématérialise l'entité dès qu'elle dépasse {@link #despawnDistance} de la cible (ou change de
     * dimension / perd sa cible), en sauvant sa position dans la poursuite virtuelle. Appelée chaque
     * tick : c'est la sécurité qui empêche l'entité de geler quand la cible file plus vite que le pas
     * d'échantillonnage (élytres, cheval, /tp). Ne fait rien tant qu'aucune entité n'est matérialisée.
     */
    private static void leashCheck(MinecraftServer server, ItFollowsConfig config) {
        StalkTrackerState state = StalkTrackerState.get(server);
        StalkerEntity entity = resolveEntity(server, state);
        if (entity == null || entity.isRemoved()) {
            return;
        }
        ServerPlayer target = resolveTargetPlayer(server, state);
        int range = despawnDistance(server, config);
        boolean tooFar = target == null
                || entity.level() != target.level()
                || entity.distanceToSqr(target) > (double) range * range;
        if (tooFar) {
            state.setVirtual(entity.position(), entity.level().dimension());
            despawn(server);
        }
    }

    /** Distance de rendu (blocs) configurée serveur : portée d'envoi des chunks au client. */
    private static int renderBlocks(MinecraftServer server) {
        return server.getPlayerList().getViewDistance() * 16;
    }

    /** Frontière (blocs) où l'entité cesse de « tourner » : distance de simulation du serveur × 16. */
    private static int simulationBlocks(MinecraftServer server) {
        return server.getPlayerList().getSimulationDistance() * 16;
    }

    /**
     * Distance (blocs) à la cible au-delà de laquelle l'entité matérialisée est dématérialisée.
     * Calée juste en deçà de la frontière de simulation pour qu'elle ne gèle jamais, quelle que soit
     * la config du serveur (en solo : la distance de simulation des options vidéo du joueur).
     */
    private static int despawnDistance(MinecraftServer server, ItFollowsConfig config) {
        return Math.max(32, simulationBlocks(server) - config.stalkerDespawnMargin);
    }

    /**
     * Distance (pondérée) d'apparition : une fraction de la distance de RENDU (le joueur voit donc la
     * traqueuse arriver de loin), bornée pour rester à l'intérieur de la zone de simulation — au-delà
     * l'entité ne pourrait pas bouger. Si le rendu dépasse largement la simulation, elle apparaît donc
     * au bord de la zone de simulation plutôt qu'à la moitié exacte du rendu.
     */
    private static double materializeDistance(MinecraftServer server, ItFollowsConfig config) {
        double fromRender = renderBlocks(server) * config.stalkerSpawnViewFraction;
        return Mth.clamp(fromRender, 16.0, despawnDistance(server, config) - 16.0);
    }

    /** Pour le debug : résume les seuils courants dérivés des distances de rendu / simulation. */
    public static String describeRange(MinecraftServer server, ItFollowsConfig config) {
        return String.format("app.<%.0f / disp.>%d (rendu %d ch., sim %d ch.)",
                materializeDistance(server, config),
                despawnDistance(server, config),
                server.getPlayerList().getViewDistance(),
                server.getPlayerList().getSimulationDistance());
    }

    private static ServerPlayer resolveTargetPlayer(MinecraftServer server, StalkTrackerState state) {
        UUID id = state.getTargetPlayer();
        if (id == null) {
            return null;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        // Exclure les exemptés (passés en créatif/spectateur entre-temps).
        if (player == null || player.isCreative() || player.isSpectator()) {
            return null;
        }
        return player;
    }

    public static StalkerEntity resolveEntity(MinecraftServer server, StalkTrackerState state) {
        UUID id = state.getStalkerEntityId();
        if (id == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof StalkerEntity stalker) {
                return stalker;
            }
        }
        return null;
    }

    /** Point d'amorçage de la poursuite : derrière la cible, à la distance d'apparition (≈ moitié du rendu). */
    private static Vec3 behindTarget(ServerPlayer target, double distance) {
        Vec3 look = target.getLookAngle();
        return new Vec3(target.getX() - look.x * distance, target.getY(), target.getZ() - look.z * distance);
    }

    /**
     * Amorce la position logique derrière la cible si elle est absente ou dans une autre dimension
     * (la traqueuse « réapparaît » en poursuite dans le nouveau monde plutôt que de rester coincée).
     */
    private static void ensureVirtualSeeded(MinecraftServer server, StalkTrackerState state,
                                            ServerPlayer target, ItFollowsConfig config) {
        ResourceKey<Level> dim = target.level().dimension();
        if (state.getVirtualPos() == null || !dim.equals(state.getVirtualDim())) {
            state.setVirtual(behindTarget(target, materializeDistance(server, config)), dim);
        }
    }

    /**
     * Matérialise l'entité réelle à la position logique {@code at}, en résolvant un Y de sol valable
     * autour de l'altitude de la cible (le Y logique n'est jamais utilisé tel quel — cf. discussion axe Y).
     */
    private static void materialize(MinecraftServer server, StalkTrackerState state, ServerPlayer target,
                                    Vec3 at, ItFollowsConfig config) {
        // Garantit l'unicité : on retire toute entité résiduelle avant d'en créer une.
        discardAllStalkers(server);

        ServerLevel level = (ServerLevel) target.level();
        StalkerEntity stalker = ModEntities.STALKER.create(level);
        if (stalker == null) {
            return;
        }

        int bx = Mth.floor(at.x);
        int bz = Mth.floor(at.z);
        double y = resolveSpawnY(level, bx, bz, target.blockPosition().getY(), config);

        stalker.moveTo(at.x, y, at.z, target.getYRot(), 0.0f);
        stalker.setTargetUuid(target.getUUID());
        stalker.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(config.stalkerSpeed);
        stalker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(config.stalkerAttackDamage);

        level.addFreshEntity(stalker);
        state.setStalkerEntityId(stalker.getUUID());
        state.setVirtual(stalker.position(), level.dimension());
    }

    /**
     * Cherche un sol où poser l'entité au X/Z donné, dans une fenêtre verticale autour de
     * {@code anchorY} (l'altitude de la cible) : « sol solide + 2 blocs d'air ». À défaut, retombe
     * sur la surface (heightmap). Ainsi un joueur en grotte fait apparaître l'entité à sa hauteur,
     * pas en surface.
     */
    private static double resolveSpawnY(ServerLevel level, int x, int z, int anchorY, ItFollowsConfig config) {
        int search = config.stalkerPlacementSearch;
        int from = Mth.clamp(anchorY + search, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1);
        int to = Mth.clamp(anchorY - search, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1);
        for (int y = from; y >= to; y--) {
            if (isStandable(level, x, y, z)) {
                return y;
            }
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** Emplacement debout : bloc dessous avec collision, et deux blocs d'air (sans collision) au-dessus. */
    private static boolean isStandable(ServerLevel level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos below = feet.below();
        BlockPos head = feet.above();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    private static void discardAllStalkers(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (StalkerEntity stalker : level.getEntities(ModEntities.STALKER, stalker -> true)) {
                stalker.discard();
            }
        }
    }
}

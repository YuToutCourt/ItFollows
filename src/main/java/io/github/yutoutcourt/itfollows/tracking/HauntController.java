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

        StalkTrackerState state = StalkTrackerState.get(server);
        HauntPhase phase = state.getHauntPhase();

        // Traque active : le leash est vérifié chaque tick (et non par échantillon). Aux vitesses élevées
        // (élytres, cheval, /tp) la cible peut sortir de la zone de simulation entre deux échantillons ; on
        // dématérialise dès le dépassement, en sauvant la position, pour ne jamais geler l'entité.
        if (phase == HauntPhase.HUNTING) {
            leashCheck(server, config);
        }

        // Escalade & révélation : pilotées chaque tick (pour que la détection du regard sur le leurre, à la
        // révélation, soit réactive). HauntPhaseController planifie ses propres événements par l'heure-monde.
        if (phase.isEscalation()) {
            ServerPlayer target = resolveTargetPlayer(server, state);
            if (target == null) {
                // Cible déconnectée / passée en créatif pendant l'escalade : on annule proprement (leurre
                // retiré) et on repart de zéro ; l'échantillon suivant resélectionnera une cible.
                despawn(server);
                state.setTargetPlayer(null);
                beginPhase(server, state, HauntPhase.NONE);
                return;
            }
            HauntPhaseController.tick(server, state, target, config);
            return;
        }

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
            // Cible fraîchement choisie : en mode normal on démarre l'escalade des avertissements (étape 1),
            // qui se terminera par la révélation puis la traque. Forcé (debug) : on traque directement.
            beginPhase(server, state, forced ? HauntPhase.HUNTING : HauntPhase.WARNING_DISTANT);
            if (!forced) {
                // L'escalade prend la main dès le prochain tick (branche phase.isEscalation() de tick()).
                return;
            }
        }

        // Sécurité : une cible existe mais on n'est ni en escalade ni en traque (état incohérent après un
        // chargement legacy) → on (re)lance l'escalade plutôt que de traquer sans avertissement.
        if (state.getHauntPhase() != HauntPhase.HUNTING) {
            beginPhase(server, state, HauntPhase.WARNING_DISTANT);
            return;
        }

        ensureVirtualSeeded(server, state, target, config);

        StalkerEntity entity = resolveEntity(server, state);
        if (entity != null && !entity.isRemoved() && !entity.isDecoy()) {
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
     * Bascule la phase de traque et horodate son début (heure-monde, robuste aux déco/reco/redémarrages).
     * Réinitialise les minuteurs de session de {@link HauntPhaseController} pour repartir proprement.
     */
    static void beginPhase(MinecraftServer server, StalkTrackerState state, HauntPhase phase) {
        state.setHauntPhase(phase);
        state.setPhaseStartTime(server.overworld().getGameTime());
        HauntPhaseController.resetTimers();
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
            spawn(server, state, target, next, false, config);
        }
    }

    // --- API debug (commande) ---

    /** Force la cible (ignore la grâce, saute l'escalade) et fait apparaître l'entité traqueuse tout de suite. */
    public static void forceTarget(MinecraftServer server, ServerPlayer target) {
        forced = true;
        ItFollowsConfig config = ItFollowsConfig.get();
        StalkTrackerState state = StalkTrackerState.get(server);
        state.setTargetPlayer(target.getUUID());
        beginPhase(server, state, HauntPhase.HUNTING);
        // Amorce la position logique derrière la cible puis matérialise tout de suite (debug).
        Vec3 behind = behindTarget(target, materializeDistance(server, config));
        state.setVirtual(behind, target.level().dimension());
        spawn(server, state, target, behind, false, config);
    }

    /**
     * Démarre l'escalade des avertissements sur {@code target} dès maintenant (debug : ignore la grâce et
     * la sélection par fatigue). Enchaîne ensuite étapes → révélation → traque comme en jeu normal.
     */
    public static void forceEscalation(MinecraftServer server, ServerPlayer target) {
        forced = true;
        StalkTrackerState state = StalkTrackerState.get(server);
        despawn(server);
        state.clearVirtual();
        state.setTargetPlayer(target.getUUID());
        beginPhase(server, state, HauntPhase.WARNING_DISTANT);
    }

    /**
     * Saute directement à une phase donnée sur la cible courante (ou {@code target} si fournie) — outil
     * de test pour ne pas attendre 20 min. {@code NONE} remet la traque à zéro ; {@code HUNTING} matérialise
     * la traque ; les phases d'escalade laissent {@link HauntPhaseController} reprendre au prochain tick.
     */
    public static boolean forcePhase(MinecraftServer server, ServerPlayer target, HauntPhase phase) {
        forced = true;
        ItFollowsConfig config = ItFollowsConfig.get();
        StalkTrackerState state = StalkTrackerState.get(server);
        if (target != null) {
            state.setTargetPlayer(target.getUUID());
        }
        ServerPlayer resolved = resolveTargetPlayer(server, state);
        if (resolved == null) {
            return false;
        }
        despawn(server);
        if (phase == HauntPhase.NONE) {
            state.setTargetPlayer(null);
            state.clearVirtual();
            forced = false;
        }
        beginPhase(server, state, phase);
        if (phase == HauntPhase.HUNTING) {
            Vec3 behind = behindTarget(resolved, materializeDistance(server, config));
            state.setVirtual(behind, resolved.level().dimension());
            spawn(server, state, resolved, behind, false, config);
        }
        return true;
    }

    /**
     * Transfert de malédiction (Phase 3) : bascule la traque sur {@code victim} sans rejouer toute
     * l'escalade (la victime est déjà « prise »). Réamorce la poursuite derrière elle, retire
     * l'entité courante (re-spawn près de la victime), et réhorodate HUNTING — ce qui relance le
     * compte des 10 min avant la livraison de SON objectif (cf. {@code CurseManager}).
     */
    public static void transferTarget(MinecraftServer server, ServerPlayer victim) {
        ItFollowsConfig config = ItFollowsConfig.get();
        StalkTrackerState state = StalkTrackerState.get(server);
        forced = false;
        state.setLastVictim(null);
        state.setTargetPlayer(victim.getUUID());
        despawn(server);
        Vec3 behind = behindTarget(victim, materializeDistance(server, config));
        state.setVirtual(behind, victim.level().dimension());
        beginPhase(server, state, HauntPhase.HUNTING);
        spawn(server, state, victim, behind, false, config);
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
        // Prochaine cible : on repasse par l'escalade complète (phase remise à zéro).
        beginPhase(server, state, HauntPhase.NONE);
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
        beginPhase(server, state, HauntPhase.NONE);
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

    /**
     * Cible actuellement traquée (en ligne, hors créatif/spectateur), ou {@code null}. Exposée pour les
     * systèmes transverses (effets de présence) afin de réutiliser l'exemption créatif/spectateur.
     */
    public static ServerPlayer currentTarget(MinecraftServer server) {
        return resolveTargetPlayer(server, StalkTrackerState.get(server));
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

    /** Point derrière la cible (opposé de son regard, horizontal) à {@code distance} blocs. */
    public static Vec3 behindTarget(ServerPlayer target, double distance) {
        Vec3 look = target.getLookAngle();
        double horiz = Math.hypot(look.x, look.z);
        // Direction de regard projetée à l'horizontale (évite que regarder le ciel/sol écrase le décalage).
        double lx = horiz > 1.0e-4 ? look.x / horiz : -Math.sin(Math.toRadians(target.getYRot()));
        double lz = horiz > 1.0e-4 ? look.z / horiz : Math.cos(Math.toRadians(target.getYRot()));
        return new Vec3(target.getX() - lx * distance, target.getY(), target.getZ() - lz * distance);
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
     * (Ré)apparaît l'entité (unique) à la position logique {@code at}, en résolvant un Y de sol valable
     * autour de l'altitude de la cible (le Y logique n'est jamais utilisé tel quel — cf. discussion axe Y).
     * {@code decoy} = {@code true} : leurre immobile (silhouette / révélation), tourné vers la cible ;
     * {@code false} : entité traqueuse réelle. Renvoie l'entité créée, ou {@code null} si l'échec.
     */
    static StalkerEntity spawn(MinecraftServer server, StalkTrackerState state, ServerPlayer target,
                               Vec3 at, boolean decoy, ItFollowsConfig config) {
        // Garantit l'unicité : on retire toute entité résiduelle avant d'en créer une.
        discardAllStalkers(server);

        ServerLevel level = (ServerLevel) target.level();
        StalkerEntity stalker = ModEntities.STALKER.create(level);
        if (stalker == null) {
            return null;
        }

        int bx = Mth.floor(at.x);
        int bz = Mth.floor(at.z);
        double y = resolveSpawnY(level, bx, bz, target.blockPosition().getY(), config);

        // Un leurre fixe regarde la cible (l'effet « il est planté là à te fixer ») ; la traqueuse, elle,
        // s'oriente comme la cible (le rendu se cale ensuite via la nav).
        float yaw = decoy ? yawToward(at.x, at.z, target.getX(), target.getZ()) : target.getYRot();
        stalker.moveTo(at.x, y, at.z, yaw, 0.0f);
        stalker.setTargetUuid(target.getUUID());
        stalker.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(config.stalkerSpeed);
        stalker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(config.stalkerAttackDamage);
        stalker.setDecoy(decoy);

        level.addFreshEntity(stalker);
        state.setStalkerEntityId(stalker.getUUID());
        state.setVirtual(stalker.position(), level.dimension());
        return stalker;
    }

    /**
     * Révélation accomplie : (re)fait apparaître la <b>vraie</b> traqueuse le plus loin possible
     * derrière la cible — juste en deçà de la frontière de dématérialisation, pour qu'elle ne soit
     * pas immédiatement larguée par {@link #leashCheck} mais reparte d'une vraie poursuite, et non
     * « à côté » du leurre. Amorce aussi la position logique. Renvoie l'entité, ou {@code null} si l'échec.
     */
    static StalkerEntity spawnFarHunter(MinecraftServer server, StalkTrackerState state,
                                        ServerPlayer target, ItFollowsConfig config) {
        double distance = despawnDistance(server, config) - 16.0;
        Vec3 far = behindTarget(target, distance);
        state.setVirtual(far, target.level().dimension());
        return spawn(server, state, target, far, false, config);
    }

    /** Yaw (degrés, convention Minecraft) pour qu'une entité en {@code (fx,fz)} regarde {@code (tx,tz)}. */
    private static float yawToward(double fx, double fz, double tx, double tz) {
        return (float) (Mth.atan2(tz - fz, tx - fx) * (180.0 / Math.PI)) - 90.0f;
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

    static void discardAllStalkers(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (StalkerEntity stalker : level.getEntities(ModEntities.STALKER, stalker -> true)) {
                stalker.discard();
            }
        }
    }
}

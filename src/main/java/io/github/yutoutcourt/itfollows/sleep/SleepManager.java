package io.github.yutoutcourt.itfollows.sleep;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import io.github.yutoutcourt.itfollows.fatigue.FatigueManager;
import io.github.yutoutcourt.itfollows.fatigue.FatigueRules;
import io.github.yutoutcourt.itfollows.mixin.ServerLevelSleepInvoker;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import io.github.yutoutcourt.itfollows.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sommeil profond &amp; nuit globale (Phase 5).
 *
 * <p>Distinct de la sieste (Phase 1.5, {@code SiesteManager}) : la sieste se fait <b>le jour</b>
 * sans toucher au temps ; le sommeil profond se fait <b>la nuit</b> et, si assez de joueurs sont
 * couchés, fait <b>défiler la nuit en douceur</b> (interpolation de temps gérée par
 * {@code ServerLevelNightTransitionMixin} + {@link NightTransition}).
 *
 * <p>Les dormeurs profonds ne sont volontairement <b>pas</b> enregistrés dans le set « sieste » :
 * le mixin {@code PlayerSleepTimerMixin} ne bloque que les siesteurs, donc les dormeurs profonds
 * comptent normalement pour le saut de nuit vanilla (le « tous couchés 5 s » = le {@code sleepTimer}
 * vanilla de 100 ticks).
 *
 * <p>La récup de fatigue (rapide, modulée par la qualité du sommeil) est appliquée de façon
 * centralisée dans {@link FatigueManager} pour rester la seule source de vérité de la fatigue.
 */
public final class SleepManager {

    /** Tick serveur auquel chaque dormeur profond s'est couché. */
    private static final Map<UUID, Integer> deepSleepStartTick = new HashMap<>();
    /** Dernier tick auquel un son d'ambiance a été joué à chaque dormeur. */
    private static final Map<UUID, Integer> lastAudioTick = new HashMap<>();

    /** État (en mémoire) de la transition de nuit globale — overworld uniquement. */
    private static final NightTransition NIGHT = new NightTransition();

    private SleepManager() {
    }

    public static void register() {
        // Rien à enregistrer : le déclenchement vit dans SiesteManager.onUseBlock (un seul UseBlockCallback),
        // la transition dans ServerLevelNightTransitionMixin, et la récup dans FatigueManager.
    }

    /** L'instance unique de transition de nuit (lue/écrite par le mixin serveur). */
    public static NightTransition nightTransition() {
        return NIGHT;
    }

    /** Couche le joueur en sommeil profond (le {@code forceNap} est fait par l'appelant). */
    public static void startDeepSleep(ServerPlayer player, ItFollowsConfig config) {
        deepSleepStartTick.put(player.getUUID(), player.server.getTickCount());
        // forceNap utilise la version basse de startSleeping qui ne met pas à jour le sleepStatus
        // du monde → on le recalcule pour que vanilla compte ce dormeur et déclenche le saut de nuit.
        ((ServerLevelSleepInvoker) (Object) player.serverLevel()).itfollows$updateSleepingPlayerList();
        float quality = computeSleepQuality(player, config);
        player.displayClientMessage(quality < 0.5f
                ? Component.literal("Votre sommeil est agité…")
                : Component.literal("Vous sombrez dans un sommeil profond."), true);
    }

    /** Vrai si le joueur est en sommeil profond (couché la nuit). */
    public static boolean isDeepSleeping(UUID playerId) {
        return deepSleepStartTick.containsKey(playerId);
    }

    /** Vrai si le sommeil profond a dépassé le délai initial et récupère donc de la fatigue. */
    public static boolean isRecovering(UUID playerId, MinecraftServer server, ItFollowsConfig config) {
        Integer start = deepSleepStartTick.get(playerId);
        return start != null && (server.getTickCount() - start) >= config.deepSleepDelayTicks;
    }

    /**
     * Facteur de qualité du sommeil dans {@code [sleepQualityMin, 1]} : module la récup de fatigue
     * et l'intensité des sons d'ambiance. Mauvais : pluie/orage, monstres proches, entité traqueuse
     * proche. Bon : feu de camp à proximité.
     */
    public static float computeSleepQuality(ServerPlayer player, ItFollowsConfig config) {
        if (!config.sleepQualityEnabled) {
            return 1.0f;
        }
        ServerLevel level = player.serverLevel();
        float q = 1.0f;

        if (level.isThundering()) {
            q -= 0.4f;
        } else if (level.isRaining()) {
            q += 0.2f;
        }

        AABB near = player.getBoundingBox().inflate(8.0);
        int mobs = level.getEntitiesOfClass(Monster.class, near).size();
        q -= Math.min(0.4f, mobs * 0.1f);

        if (!level.getEntitiesOfClass(StalkerEntity.class, player.getBoundingBox().inflate(24.0)).isEmpty()) {
            q -= 0.5f; // l'entité rôde : sommeil pourri (lien horreur)
        }

        if (campfireNear(level, player.blockPosition())) {
            q += 0.15f;
        }

        return Mth.clamp(q, config.sleepQualityMin, 1.0f);
    }

    /** Valide les sommeils profonds en cours (réveil/déco/repos complet) et joue les sons d'ambiance. À appeler chaque tick serveur. */
    public static void tick(MinecraftServer server) {
        if (deepSleepStartTick.isEmpty()) {
            return;
        }
        ItFollowsConfig config = ItFollowsConfig.get();
        int now = server.getTickCount();
        deepSleepStartTick.keySet().removeIf(id -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                lastAudioTick.remove(id);
                return true; // déconnecté
            }
            if (!player.isSleeping()) {
                lastAudioTick.remove(id);
                return true; // a quitté le lit (ou réveillé par le saut de nuit)
            }
            // Réveil auto si complètement reposé — mais PAS pendant la transition de nuit : sinon le
            // dormeur qui atteint le max quitterait le lit, ferait chuter le compte et annulerait la
            // transition pour tout le monde. On le laisse être réveillé par la fin de l'animation.
            if (!NIGHT.isActive() && FatigueManager.getFatigue(player) >= FatigueRules.MAX) {
                player.stopSleeping(); // complètement reposé → réveil automatique
                lastAudioTick.remove(id);
                return true;
            }
            maybePlaySleepAudio(player, now, config);
            return false;
        });
    }

    // --- Helpers ---

    private static void maybePlaySleepAudio(ServerPlayer player, int now, ItFollowsConfig config) {
        if (!config.sleepAudioEnabled) {
            return;
        }
        UUID id = player.getUUID();
        Integer last = lastAudioTick.get(id);
        if (last != null && (now - last) < config.sleepAudioIntervalTicks) {
            return;
        }
        lastAudioTick.put(id, now);

        // Plus le sommeil est mauvais, plus les bruits angoissants sont probables/forts.
        float badness = 1.0f - computeSleepQuality(player, config);
        if (player.getRandom().nextFloat() > 0.35f + 0.5f * badness) {
            return; // un peu d'aléatoire pour ne pas être métronomique
        }

        var sound = ModSounds.SLEEP_BREATH;
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
        double dist = 1.5 + player.getRandom().nextDouble() * 2.5; // tout près du lit
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;
        float volume = 0.35f + 0.45f * badness;
        float pitch = 0.85f + player.getRandom().nextFloat() * 0.2f;
        ItFollowsNetworking.playSoundTo(player, sound, x, player.getY(), z, volume, pitch);
    }

    private static boolean campfireNear(ServerLevel level, BlockPos center) {
        int r = 3;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(pos).getBlock() instanceof CampfireBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

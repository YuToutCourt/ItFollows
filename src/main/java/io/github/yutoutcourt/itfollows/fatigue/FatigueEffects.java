package io.github.yutoutcourt.itfollows.fatigue;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Applique les effets serveur liés à la fatigue (les effets sensoriels — overlay, FOV, sons —
 * sont gérés côté client).
 *
 * <p>Appelé à chaque échantillonnage par le {@link FatigueManager}. La lenteur et la faiblesse
 * suivent les paliers ; la fatigue de minage, la nausée et la cécité ont leurs propres seuils
 * de valeur (config) pour un dosage précis :
 * <ul>
 *   <li>lenteur dès MOYENNE, amplifiée + sprint coupé en CRITIQUE ;</li>
 *   <li>faiblesse dès ELEVEE, amplifiée en CRITIQUE ;</li>
 *   <li>fatigue de minage sous {@code miningFatigueThreshold} (~30 %) ;</li>
 *   <li>nausée <em>par vagues</em> sous {@code nauseaThreshold} (~10 %) : une vague courte
 *       (~7 s) puis un répit aléatoire (20–50 s), pour donner la tête qui tourne par intermittence
 *       plutôt qu'une nausée permanente ;</li>
 *   <li>cécité sous {@code blindnessThreshold} (~5 %), au bord de l'évanouissement.</li>
 * </ul>
 */
public final class FatigueEffects {

    /** Durée des effets : un peu plus longue que l'intervalle d'échantillonnage pour éviter les trous. */
    private static final int EFFECT_DURATION_TICKS = 40;

    /** Planificateur de vagues de nausée par joueur (transitoire, pas besoin de persistance). */
    private static final Map<UUID, NauseaWave> nauseaWaves = new HashMap<>();
    private static final Random RANDOM = new Random();

    private FatigueEffects() {
    }

    /** Petit état par joueur : la vague est active jusqu'à {@code nextChangeTick}, puis on bascule. */
    private static final class NauseaWave {
        boolean active;
        long nextChangeTick;
    }

    /** Oublie l'état de nausée d'un joueur (à appeler à la déconnexion). */
    public static void clear(UUID playerId) {
        nauseaWaves.remove(playerId);
    }

    public static void apply(ServerPlayer player, float value, ItFollowsConfig config) {
        // Lenteur + faiblesse : pilotées par palier.
        switch (FatigueRules.tierFor(value, config)) {
            case MOYENNE -> applyEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 0);
            case ELEVEE -> {
                applyEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 0);
                applyEffect(player, MobEffects.WEAKNESS, 0);
            }
            case CRITIQUE -> {
                applyEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 1);
                applyEffect(player, MobEffects.WEAKNESS, 1);
                // Sprint impossible en état critique.
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
            }
            default -> {
                // NONE / LEGERE : aucun effet de mouvement serveur.
            }
        }

        // Fatigue de minage : seuil dédié (~30 %), amplifiée tout en bas.
        if (value < config.miningFatigueThreshold) {
            applyEffect(player, MobEffects.DIG_SLOWDOWN, value < config.thresholdCritique ? 1 : 0);
        }

        // Nausée par vagues : une vague courte puis un long répit aléatoire.
        if (value < config.nauseaThreshold) {
            if (isNauseaWaveActive(player, config)) {
                // Durée courte (juste de quoi tenir entre deux échantillons) : ainsi, dès la fin
                // de la vague, l'effet s'estompe rapidement au lieu de s'éterniser pendant le répit.
                applyEffect(player, MobEffects.CONFUSION, 0, config.nauseaWaveOnTicks);
            }
        } else {
            // Au-dessus du seuil : on oublie la planification pour repartir d'une vague à la prochaine chute.
            nauseaWaves.remove(player.getUUID());
        }

        // Cécité : tout en bas.
        if (value < config.blindnessThreshold) {
            applyEffect(player, MobEffects.BLINDNESS, 0);
        }
    }

    /**
     * Fenêtre « active » du cycle de nausée, planifiée par joueur. Une vague dure
     * {@code nauseaWaveOnTicks}, suivie d'un répit tiré au hasard dans
     * [{@code nauseaWaveOffMinTicks}, {@code nauseaWaveOffMaxTicks}]. En s'arrêtant de réappliquer
     * l'effet durant le répit, la nausée en cours s'estompe naturellement → effet de vagues.
     */
    private static boolean isNauseaWaveActive(ServerPlayer player, ItFollowsConfig config) {
        long now = player.server.getTickCount();
        NauseaWave wave = nauseaWaves.get(player.getUUID());
        if (wave == null) {
            // Première chute sous le seuil : on démarre directement par une vague.
            wave = new NauseaWave();
            wave.active = true;
            wave.nextChangeTick = now + Math.max(1, config.nauseaWaveOnTicks);
            nauseaWaves.put(player.getUUID(), wave);
            return true;
        }
        if (now >= wave.nextChangeTick) {
            // Bascule de phase.
            wave.active = !wave.active;
            wave.nextChangeTick = now + (wave.active
                    ? Math.max(1, config.nauseaWaveOnTicks)
                    : randomOffDuration(config));
        }
        return wave.active;
    }

    /** Durée d'un répit tirée au hasard dans la plage configurée (bornes incluses). */
    private static int randomOffDuration(ItFollowsConfig config) {
        int min = Math.max(1, config.nauseaWaveOffMinTicks);
        int max = Math.max(min, config.nauseaWaveOffMaxTicks);
        return min + RANDOM.nextInt(max - min + 1);
    }

    private static void applyEffect(ServerPlayer player, MobEffect effect, int amplifier) {
        player.addEffect(new MobEffectInstance(
                effect,
                EFFECT_DURATION_TICKS,
                amplifier,
                true,   // ambient
                false,  // pas de particules
                false   // pas d'icône HUD
        ));
    }

    private static void applyEffect(ServerPlayer player, MobEffect effect, int amplifier, int duration) {
        player.addEffect(new MobEffectInstance(
                effect,
                duration,
                amplifier,
                true,   // ambient
                false,  // pas de particules
                false   // pas d'icône HUD
        ));
    }
}

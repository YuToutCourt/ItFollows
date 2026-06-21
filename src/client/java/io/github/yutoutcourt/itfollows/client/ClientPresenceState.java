package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import net.minecraft.util.Mth;

/**
 * Présence de l'entité côté client (piste 2), alimentée par {@code presence_sync} : la <b>distance</b>
 * (blocs) entre l'entité et le joueur traqué, ou {@code -1} si aucune entité applicable.
 *
 * <p>La piste fatigue (piste 1, tous les joueurs) n'a pas besoin de ce canal : elle est dérivée
 * directement de {@link ClientFatigueState} dans {@code PresenceHudOverlay}.
 *
 * <p>Une « proximité » lissée ∈ [0,1] (0 au-delà de {@code presenceEntityMaxDistance}, 1 au contact)
 * rejoint progressivement sa cible chaque frame pour éviter les à-coups (l'entité se matérialise →
 * saut brutal de distance).
 */
public final class ClientPresenceState {

    private static final float NO_ENTITY = -1.0f;

    private static float distance = NO_ENTITY;
    private static float smoothedCloseness = 0.0f;

    private ClientPresenceState() {
    }

    /** Distance reçue du serveur (blocs), ou {@code -1} si pas d'entité. */
    public static void set(float value) {
        distance = value;
    }

    /** Distance brute courante (blocs), ou {@code -1}. */
    public static float distance() {
        return distance;
    }

    /** Vrai si l'entité est dans la portée d'effet (distance ≥ 0). */
    public static boolean hasEntity() {
        return distance >= 0.0f;
    }

    /**
     * Proximité lissée ∈ [0,1] (à appeler une fois par frame) : 0 à la distance max d'effet, 1 au contact.
     */
    public static float advanceCloseness() {
        float max = ItFollowsConfig.get().presenceEntityMaxDistance;
        float targetCloseness = distance < 0.0f ? 0.0f : Mth.clamp((max - distance) / max, 0.0f, 1.0f);
        smoothedCloseness += (targetCloseness - smoothedCloseness) * 0.08f;
        if (Math.abs(targetCloseness - smoothedCloseness) < 0.001f) {
            smoothedCloseness = targetCloseness;
        }
        return smoothedCloseness;
    }
}

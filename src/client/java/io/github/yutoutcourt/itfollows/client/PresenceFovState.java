package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;

/**
 * Facteur de FOV de présence (Phase 4b), lu par le mixin sur {@code GameRenderer#getFov} :
 * <ul>
 *   <li><b>vision tunnel</b> : réduit le FOV (jusqu'à {@code presenceFovTunnelFactor}) à courte distance ;</li>
 *   <li><b>respiration visuelle</b> : légère oscillation de zoom (in/out) superposée.</li>
 * </ul>
 * Mis à jour par {@link PresenceVisuals#tick(float)} ; le mixle peut lire avec 1 frame de retard (sans impact).
 */
public final class PresenceFovState {

    /** Rampe 0→1 de la bande « très proche » (0 = aucun tunnel, 1 = tunnel max). */
    private static float tunnelRamp;

    private PresenceFovState() {
    }

    public static void update(float closeRamp) {
        tunnelRamp = closeRamp;
    }

    /** Facteur multiplicatif à appliquer au FOV (1.0 = neutre). */
    public static float factor() {
        ItFollowsConfig cfg = ItFollowsConfig.get();
        if (!cfg.presenceFovEnabled || tunnelRamp <= 0.0f) {
            return 1.0f;
        }
        float base = lerp(tunnelRamp, 1.0f, cfg.presenceFovTunnelFactor);
        double t = System.currentTimeMillis() / 700.0;
        float breath = 1.0f + (float) Math.sin(t) * (cfg.presenceFovBreathAmplitude * tunnelRamp);
        return base * breath;
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }
}

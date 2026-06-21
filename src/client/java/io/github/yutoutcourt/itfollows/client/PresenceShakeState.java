package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;

/**
 * Amplitude (degrés) du screen shake de présence (Phase 4b), lue par le mixin sur {@code Camera#setup}.
 * Monte avec la proximité à partir de la bande « danger » (≤ 10 b). Mise à jour par
 * {@link PresenceVisuals#tick(float)}.
 */
public final class PresenceShakeState {

    private static float amplitude;

    private PresenceShakeState() {
    }

    public static void update(float dangerRamp) {
        ItFollowsConfig cfg = ItFollowsConfig.get();
        amplitude = cfg.presenceShakeEnabled ? dangerRamp * cfg.presenceShakeMaxDegrees : 0.0f;
    }

    /** Amplitude courante en degrés (0 = aucun tremblement). */
    public static float amplitude() {
        return amplitude;
    }
}

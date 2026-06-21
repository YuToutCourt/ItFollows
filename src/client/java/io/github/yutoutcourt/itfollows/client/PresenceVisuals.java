package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;

/**
 * Source unique des <b>niveaux d'effets de rendu</b> (Phase 4b) dérivés de la présence de l'entité
 * (distance, via {@link ClientPresenceState}) et, à terme, de la fatigue. Mis à jour une fois par frame
 * par {@code PresencePostProcessor#renderAfterWorld} (avant le HUD), pour que tout le monde lise la même
 * valeur lissée — y compris {@code PresenceHudOverlay} pour la proximité.
 *
 * <p><b>Étape 1 (Fondation)</b> : seule la <b>désaturation</b> est branchée ; wave/blur/noise/vignette
 * restent à 0 (passthrough) et seront activés aux étapes suivantes.
 */
public final class PresenceVisuals {

    private static float closeness;
    private static float desat;
    private static float wave;
    private static float blur;
    private static float ghost;
    private static float noise;
    private static float vignette;

    private PresenceVisuals() {
    }

    /** Recalcule les niveaux à partir de la proximité lissée. À appeler une fois par frame. */
    public static void tick(float tickDelta) {
        ItFollowsConfig cfg = ItFollowsConfig.get();
        closeness = ClientPresenceState.advanceCloseness(); // 0 (loin/aucune) → 1 (contact)
        float d = ClientPresenceState.distance();
        boolean has = ClientPresenceState.hasEntity();

        // Effets shader, par bande de distance (cf. cahier).
        desat = closeness * cfg.presenceDesatMax;                         // dès 30 b
        wave = ramp(d, cfg.presenceBandConfirmed) * cfg.presenceWaveMax;  // dès 15 b
        blur = ramp(d, cfg.presenceBandConfirmed) * cfg.presenceBlurMax;  // dès 15 b
        ghost = ramp(d, cfg.presenceBandDanger) * cfg.presenceGhostMax;   // dès 10 b (rémanence)
        noise = has ? closeness * cfg.presenceNoiseMax : 0.0f;            // dès 30 b, dense au contact

        // Vignette : gradient radial lisse côté shader, pilotée par la proximité ET la fatigue
        // (remplace l'ancienne vignette HUD en anneaux qui créait des « bandes » à l'écran).
        float fatigueVignette = fatigueVignetteLevel(ClientFatigueState.get(), cfg);
        vignette = Math.max(closeness, fatigueVignette);

        // Effets caméra/FOV (lus par les mixins) : vision tunnel ≤ 5 b, screen shake ≤ 10 b.
        PresenceFovState.update(ramp(d, cfg.presenceBandClose));
        PresenceShakeState.update(ramp(d, cfg.presenceBandDanger));
    }

    /** Niveau de vignette par paliers de fatigue (20/10/5 %) ∈ [0,1]. */
    private static float fatigueVignetteLevel(float fatigue, ItFollowsConfig cfg) {
        if (fatigue <= cfg.presenceFatigueWhisper) return 0.9f;
        if (fatigue <= cfg.presenceFatigueStrong) return 0.7f;
        if (fatigue <= cfg.presenceFatigueLight) return 0.35f;
        return 0.0f;
    }

    /**
     * Rampe 0→1 quand la distance {@code d} passe sous {@code start} (0 à {@code start}, 1 au contact).
     * Renvoie 0 si pas d'entité ({@code d < 0}) ou {@code start <= 0}.
     */
    private static float ramp(float d, float start) {
        if (d < 0.0f || start <= 0.0f) {
            return 0.0f;
        }
        float r = (start - d) / start;
        return r < 0.0f ? 0.0f : (r > 1.0f ? 1.0f : r);
    }

    /** Proximité lissée ∈ [0,1] (réutilisée par l'overlay HUD). */
    public static float closeness() {
        return closeness;
    }

    public static float desat() {
        return desat;
    }

    public static float wave() {
        return wave;
    }

    public static float blur() {
        return blur;
    }

    public static float ghost() {
        return ghost;
    }

    public static float noise() {
        return noise;
    }

    public static float vignette() {
        return vignette;
    }

    /** Vrai si au moins un effet shader est non négligeable (sinon on n'allume pas le pipeline). */
    public static boolean anyActive() {
        return desat > 0.004f || wave > 0.004f || blur > 0.004f || ghost > 0.004f || noise > 0.004f || vignette > 0.004f;
    }
}

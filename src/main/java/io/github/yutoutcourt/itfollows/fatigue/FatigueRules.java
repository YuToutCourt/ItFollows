package io.github.yutoutcourt.itfollows.fatigue;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;

/**
 * Cœur de calcul de la fatigue, sans aucune dépendance Minecraft.
 * Toutes les fonctions sont pures → faciles à raisonner et à vérifier.
 *
 * <p>Échelle : 0 (épuisé) à 100 (en forme).
 */
public final class FatigueRules {

    public static final float MIN = 0.0f;
    public static final float MAX = 100.0f;

    private FatigueRules() {
    }

    /** Borne une valeur dans [0, 100]. */
    public static float clamp(float value) {
        if (value < MIN) return MIN;
        if (value > MAX) return MAX;
        return value;
    }

    /** Applique un coût (réduit la fatigue), borné. */
    public static float applyCost(float current, float cost) {
        return clamp(current - cost);
    }

    /** Applique une régénération (augmente la fatigue), bornée. */
    public static float applyRegen(float current, float regen) {
        return clamp(current + regen);
    }

    /**
     * Détermine le palier pour une valeur donnée, selon les seuils de la config.
     * Seuils par défaut : LEGERE &lt; 75, MOYENNE &lt; 50, ELEVEE &lt; 25, CRITIQUE &lt; 10.
     */
    public static FatigueTier tierFor(float value, ItFollowsConfig config) {
        if (value < config.thresholdCritique) return FatigueTier.CRITIQUE;
        if (value < config.thresholdElevee) return FatigueTier.ELEVEE;
        if (value < config.thresholdMoyenne) return FatigueTier.MOYENNE;
        if (value < config.thresholdLegere) return FatigueTier.LEGERE;
        return FatigueTier.NONE;
    }
}

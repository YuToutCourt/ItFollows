package io.github.yutoutcourt.itfollows.fatigue;

/**
 * Paliers de fatigue. La barre va de 100 (en forme) à 0 (épuisé).
 * Les seuils sont configurables ; voir {@link FatigueRules#tierFor}.
 *
 * <p>{@code level} sert d'index croissant de sévérité (0 = aucune, 4 = critique)
 * pour piloter les effets serveur et l'overlay client sans dépendre des noms.
 */
public enum FatigueTier {
    NONE(0),
    LEGERE(1),
    MOYENNE(2),
    ELEVEE(3),
    CRITIQUE(4);

    private final int level;

    FatigueTier(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }
}

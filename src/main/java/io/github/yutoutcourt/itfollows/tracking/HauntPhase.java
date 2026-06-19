package io.github.yutoutcourt.itfollows.tracking;

/**
 * Phases de la traque (Phase 2). Stockées sous forme d'ordinal dans {@link StalkTrackerState#getPhase()}
 * et pilotées par {@link HauntController} (sélection + traque active) et {@link HauntPhaseController}
 * (escalade des avertissements + révélation « LOOK BEHIND YOU »).
 *
 * <p>Cycle nominal après le délai de grâce :
 * {@link #NONE} → {@link #WARNING_DISTANT} → {@link #WARNING_PHYSICAL} → {@link #WARNING_SILHOUETTE}
 * → {@link #REVEAL} → {@link #HUNTING}.
 */
public enum HauntPhase {
    /** Aucune cible / délai de grâce en cours. */
    NONE,
    /** Étape 1 : bruits lointains ciblés (le seul traqué les entend). */
    WARNING_DISTANT,
    /** Étape 2 : portes qui s'ouvrent / blocs cassés <i>réels</i> à proximité (perçus par tous). */
    WARNING_PHYSICAL,
    /** Étape 3 : silhouette aperçue brièvement (flash de l'entité immobile, visible de la seule cible). */
    WARNING_SILHOUETTE,
    /** Révélation : leurre immobile à quelques blocs derrière la cible + voix « LOOK BEHIND YOU ». */
    REVEAL,
    /** Traque active : l'entité réelle poursuit la cible (comportement historique). */
    HUNTING;

    public static HauntPhase fromOrdinal(int ordinal) {
        HauntPhase[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }

    /** Vrai pendant l'escalade des avertissements (étapes 1-3) ou la révélation, géré par {@link HauntPhaseController}. */
    public boolean isEscalation() {
        return this == WARNING_DISTANT || this == WARNING_PHYSICAL
                || this == WARNING_SILHOUETTE || this == REVEAL;
    }
}

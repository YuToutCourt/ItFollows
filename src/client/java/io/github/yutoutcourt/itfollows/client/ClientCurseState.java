package io.github.yutoutcourt.itfollows.client;

import java.util.UUID;

/**
 * État de malédiction du joueur local côté client, alimenté par les packets {@code curse_*}.
 * Source unique pour l'overlay d'objectif (HUD) et l'indicateur de progression au-dessus de la victime.
 */
public final class ClientCurseState {

    private static boolean cursed;
    private static String objective = "";
    private static UUID victim;
    private static int progressPercent = -1;
    /** Heure client (ms) de la dernière mise à jour d'objectif, pour le « flash » d'arrivée. */
    private static long objectiveSetAt;
    /** Heure client (ms) de la dernière réussite d'action, pour la bannière verte. */
    private static long successAt;
    private static String successVictim = "";

    private ClientCurseState() {
    }

    public static void setCursed(boolean value) {
        cursed = value;
        if (!value) {
            objective = "";
            victim = null;
            progressPercent = -1;
        }
    }

    public static boolean isCursed() {
        return cursed;
    }

    public static void setObjective(String text) {
        objective = text == null ? "" : text;
        objectiveSetAt = System.currentTimeMillis();
        if (objective.isEmpty()) {
            victim = null;
            progressPercent = -1;
        }
    }

    public static String objective() {
        return objective;
    }

    public static long objectiveSetAt() {
        return objectiveSetAt;
    }

    public static boolean hasObjective() {
        return cursed && !objective.isEmpty();
    }

    public static void setProgress(UUID victimId, int percent) {
        victim = victimId;
        progressPercent = percent;
    }

    public static UUID victim() {
        return victim;
    }

    public static int progressPercent() {
        return progressPercent;
    }

    /** Déclenche la bannière de réussite (action accomplie, malédiction transmise). */
    public static void flashSuccess(String victimName) {
        successAt = System.currentTimeMillis();
        successVictim = victimName == null ? "" : victimName;
    }

    public static long successAt() {
        return successAt;
    }

    public static String successVictim() {
        return successVictim;
    }
}

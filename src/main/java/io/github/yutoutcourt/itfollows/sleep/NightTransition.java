package io.github.yutoutcourt.itfollows.sleep;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import net.minecraft.server.level.ServerLevel;

/**
 * État (en mémoire) de la transition de nuit globale côté serveur (Phase 5).
 *
 * <p>Au lieu d'un saut de temps vanilla brut, on interpole l'heure du monde en douceur sur
 * quelques secondes (courbe S {@link #integralEase}), basée sur l'horloge réelle. Modelé sur
 * {@code SleepAnimationState} du mod de référence {@code seamless-sleep}. Un seul instance
 * suffit : seule l'overworld déclenche la transition (cf. {@code ServerLevelNightTransitionMixin}).
 *
 * <p>L'état est volontairement transitoire (quelques secondes) : pas besoin de {@code SavedData}.
 * Si le serveur redémarre en pleine transition, le temps reste simplement là où il en était.
 */
public final class NightTransition {

    private static final long FULL_NIGHT_TICKS = 12000L;

    private boolean active;
    private long startTimeOfDay;
    private long endTimeOfDay;
    private int durationTicks;
    private long startMillis;

    public boolean isActive() {
        return active;
    }

    /** Démarre l'interpolation de {@code currentTime} vers {@code targetTime} (no-op si pas un saut en avant). */
    public void start(long currentTime, long targetTime, ItFollowsConfig config) {
        if (targetTime <= currentTime) {
            active = false;
            return;
        }
        durationTicks = computeDurationTicks(targetTime - currentTime, config);
        active = true;
        startTimeOfDay = currentTime;
        endTimeOfDay = targetTime;
        startMillis = System.currentTimeMillis();
    }

    public void cancel() {
        active = false;
    }

    /** Avance le temps du monde d'un cran interpolé. À appeler chaque tick serveur tant que {@link #isActive()}. */
    public void tick(ServerLevel world) {
        if (!active) {
            return;
        }
        double elapsedMs = System.currentTimeMillis() - startMillis;
        if (elapsedMs <= 0.0) {
            world.setDayTime(startTimeOfDay);
            return;
        }
        double totalMs = durationTicks * 50.0;
        double x = elapsedMs / totalMs;
        if (x >= 1.0) {
            active = false;
            world.setDayTime(endTimeOfDay);
            return;
        }
        double eased = integralEase(x);
        long delta = endTimeOfDay - startTimeOfDay;
        world.setDayTime(startTimeOfDay + (long) (delta * eased));
    }

    public long getStartTimeOfDay() {
        return startTimeOfDay;
    }

    public long getEndTimeOfDay() {
        return endTimeOfDay;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public long getStartMillis() {
        return startMillis;
    }

    /** Durée de l'animation proportionnelle à la portion de nuit restante, bornée [min, max] de la config. */
    private static int computeDurationTicks(long delta, ItFollowsConfig config) {
        double fraction = delta / (double) FULL_NIGHT_TICKS;
        if (fraction < 0.0) {
            fraction = 0.0;
        } else if (fraction > 1.0) {
            fraction = 1.0;
        }
        int min = config.nightTransitionMinTicks;
        int max = Math.max(min, config.nightTransitionMaxTicks);
        return min + (int) Math.round((max - min) * fraction);
    }

    /** Courbe S « ease-in-out » (reprise telle quelle du mod de référence pour un rendu identique). */
    public static double integralEase(double x) {
        if (x <= 0.0) {
            return 0.0;
        }
        if (x >= 1.0) {
            return 1.0;
        }
        double x2 = x * x;
        double base = (x2 - 1.0) * Math.sqrt(1.0 - x2) + 1.0;
        double oneMinus = 1.0 - base;
        return 1.0 - Math.pow(oneMinus, 3.0);
    }
}

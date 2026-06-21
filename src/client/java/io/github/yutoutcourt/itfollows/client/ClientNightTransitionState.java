package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.sleep.NightTransition;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Miroir client de la transition de nuit globale (Phase 5).
 *
 * <p>Sur réception du packet {@code SLEEP_NIGHT_START}, on interpole localement l'heure du monde
 * avec la <b>même</b> courbe ({@link NightTransition#integralEase}) que le serveur, en compensant
 * la latence (on rattrape le temps écoulé depuis le démarrage serveur). Le ciel défile donc en
 * douceur et de façon synchrone sur tous les clients. Modelé sur {@code ClientSleepAnimationState}
 * du mod de référence {@code seamless-sleep}. Tout statique, comme les autres états client du mod.
 */
public final class ClientNightTransitionState {

    private static boolean active;
    private static long startTimeOfDay;
    private static long endTimeOfDay;
    private static int durationTicks;
    private static long startMillis;

    private ClientNightTransitionState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void reset() {
        active = false;
    }

    public static void start(long startTime, long endTime, int serverDurationTicks, long serverStartMillis) {
        long now = System.currentTimeMillis();
        long elapsedSinceServerStart = Math.max(0L, now - serverStartMillis);
        durationTicks = (int) Math.max(1L, serverDurationTicks - elapsedSinceServerStart / 50L);
        startTimeOfDay = startTime;
        endTimeOfDay = endTime;
        startMillis = now;
        active = true;
    }

    /** Avance l'heure locale du monde d'un cran interpolé. À appeler chaque frame tant que {@link #isActive()}. */
    public static void tick(ClientLevel world) {
        if (!active) {
            return;
        }
        long elapsedMs = System.currentTimeMillis() - startMillis;
        double totalMs = durationTicks * 50.0;
        double x = totalMs <= 0.0 ? 1.0 : Math.min(1.0, elapsedMs / totalMs);
        double eased = NightTransition.integralEase(x);
        long delta = endTimeOfDay - startTimeOfDay;
        long newTimeOfDay = startTimeOfDay + (long) (delta * eased);

        world.getLevelData().setDayTime(newTimeOfDay);

        if (x >= 1.0) {
            active = false;
        }
    }
}

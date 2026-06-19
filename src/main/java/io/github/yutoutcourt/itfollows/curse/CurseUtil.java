package io.github.yutoutcourt.itfollows.curse;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Helpers géométriques partagés par les actions : proximité, regard, champ de vision.
 */
public final class CurseUtil {

    private CurseUtil() {
    }

    /** Même dimension et distance horizontale+verticale ≤ range. */
    public static boolean within(ServerPlayer a, ServerPlayer b, double range) {
        return a.level() == b.level() && a.distanceToSqr(b) <= range * range;
    }

    /** {@code a} regarde-t-il {@code b} dans un cône de demi-angle {@code coneDeg} ? */
    public static boolean looksAt(ServerPlayer a, ServerPlayer b, double coneDeg) {
        Vec3 look = a.getLookAngle().normalize();
        Vec3 toB = b.getEyePosition().subtract(a.getEyePosition());
        if (toB.lengthSqr() < 1.0e-4) {
            return true;
        }
        double dot = look.dot(toB.normalize());
        double cos = Math.cos(Math.toRadians(coneDeg));
        return dot >= cos;
    }

    /** La cible regarde-t-elle approximativement vers le maudit (pour « dans son champ de vision ») ? */
    public static boolean inViewOf(ServerPlayer viewer, ServerPlayer subject, double coneDeg) {
        return looksAt(viewer, subject, coneDeg);
    }
}

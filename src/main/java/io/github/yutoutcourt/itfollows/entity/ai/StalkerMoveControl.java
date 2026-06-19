package io.github.yutoutcourt.itfollows.entity.ai;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Contrôle de déplacement de la traqueuse (refonte unifiée). Le mode physique découle du milieu
 * réel et du flag de vol — il n'est plus arbitré par capteurs concurrents :
 *
 * <ul>
 *   <li><b>Fluide</b> (eau ou lave) : pilotage 3D maison (orientation + avance/plongée) vers la
 *       position voulue posée par {@link StalkerChaseGoal}. La poussée verticale {@code yya} fait
 *       aussi <b>sortir du fluide</b> en visant un nœud sur la berge.</li>
 *   <li><b>Vol</b> ({@link StalkerEntity#isFlying()}) : même pilotage 3D, mais dans l'air (la
 *       physique sans gravité est appliquée par {@code StalkerEntity#travel}).</li>
 *   <li><b>Sol</b> : délègue à {@link MoveControl#tick()} → marche/saut <b>strictement vanilla</b>.</li>
 * </ul>
 */
public final class StalkerMoveControl extends MoveControl {

    public StalkerMoveControl(Mob mob) {
        super(mob);
    }

    /** Met le contrôle en attente (utilisé par le goal pendant la casse : rester sur place). */
    public void setWaiting() {
        this.operation = Operation.WAIT;
    }

    @Override
    public void tick() {
        boolean inFluid = this.mob.isInWater() || this.mob.isInLava();
        boolean flying = this.mob instanceof StalkerEntity stalker && stalker.isFlying();

        if (this.operation == Operation.MOVE_TO && (inFluid || flying)) {
            steer3D(inFluid ? ItFollowsConfig.get().stalkerSwimSpeedFactor
                    : ItFollowsConfig.get().stalkerFlySpeedFactor);
        } else {
            super.tick();
            // Coupe l'auto-saut vanilla : maxUpStep (1.1) gère les marches d'1 bloc, et les vrais
            // obstacles verticaux passent par un pas JUMP/DIG/FLY du plan. Sans ça, l'entité
            // « sautillait » sans cesse en butant contre un mur (retour de test).
            this.mob.setJumping(false);
        }
    }

    /**
     * Oriente l'entité vers la position voulue et applique avance ({@code zza}) + plongée/montée
     * ({@code yya}) proportionnelles à la direction 3D. Commun à la nage et au vol.
     */
    private void steer3D(float speedFactor) {
        double dx = this.getWantedX() - this.mob.getX();
        double dy = this.getWantedY() - this.mob.getY();
        double dz = this.getWantedZ() - this.mob.getZ();
        double distSqr = dx * dx + dy * dy + dz * dz;
        if (distSqr < 2.5e-7) {
            this.mob.setZza(0.0f);
            this.mob.setYya(0.0f);
            return;
        }
        double dist = Math.sqrt(distSqr);

        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 12.0f));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.yHeadRot = this.mob.getYRot();

        float speed = (float) (this.getSpeedModifier()
                * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * speedFactor);
        this.mob.setSpeed(speed);
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        this.mob.setZza((float) (horizontal / dist)); // avance (le yaw vise déjà la cible)
        this.mob.setYya((float) (dy / dist));          // plonge / remonte
        this.mob.setXxa(0.0f);
    }
}

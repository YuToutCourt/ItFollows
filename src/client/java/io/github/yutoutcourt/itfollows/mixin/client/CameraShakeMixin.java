package io.github.yutoutcourt.itfollows.mixin.client;

import io.github.yutoutcourt.itfollows.client.PresenceShakeState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Phase 4b : screen shake de présence. Après le placement de la caméra, on ajoute un micro-décalage
 * aléatoire de la rotation (yaw/pitch) dont l'amplitude vient de {@link PresenceShakeState}. Neutre
 * quand l'amplitude est nulle (joueur non concerné / entité au-delà de la bande danger).
 */
@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    private static final Random ITFOLLOWS$RANDOM = new Random();

    @Inject(method = "setup", at = @At("TAIL"))
    private void itfollows$shake(BlockGetter level, Entity entity, boolean detached,
                                boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        float amp = PresenceShakeState.amplitude();
        if (amp <= 0.0f) {
            return;
        }
        float dYaw = (ITFOLLOWS$RANDOM.nextFloat() * 2.0f - 1.0f) * amp;
        float dPitch = (ITFOLLOWS$RANDOM.nextFloat() * 2.0f - 1.0f) * amp;
        setRotation(getYRot() + dYaw, getXRot() + dPitch);
    }
}

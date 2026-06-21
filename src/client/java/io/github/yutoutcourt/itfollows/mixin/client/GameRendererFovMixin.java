package io.github.yutoutcourt.itfollows.mixin.client;

import io.github.yutoutcourt.itfollows.client.PresenceFovState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase 4b : applique le facteur de FOV de présence (vision tunnel + respiration) au FOV calculé par
 * le moteur. À RETURN pour multiplier la valeur finale ; neutre tant que {@link PresenceFovState#factor()}
 * vaut 1.0 (joueur non concerné / entité loin).
 */
@Mixin(GameRenderer.class)
public class GameRendererFovMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void itfollows$presenceFov(Camera camera, float tickDelta, boolean useFovSetting,
                                       CallbackInfoReturnable<Double> cir) {
        float factor = PresenceFovState.factor();
        if (factor != 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * factor);
        }
    }
}

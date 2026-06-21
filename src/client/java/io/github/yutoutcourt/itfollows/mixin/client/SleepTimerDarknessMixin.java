package io.github.yutoutcourt.itfollows.mixin.client;

import io.github.yutoutcourt.itfollows.client.ClientNightTransitionState;
import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Atténue le voile noir de sommeil <b>pendant la transition de nuit</b> (Phase 5) pour laisser
 * voir le ciel défiler.
 *
 * <p>Le HUD calcule l'opacité du fondu au noir à partir de {@code Player.getSleepTimer()}. En le
 * réduisant d'un facteur ({@code sleepDarknessFactor}) uniquement quand
 * {@link ClientNightTransitionState#isActive()}, on garde le comportement vanilla pour la sieste
 * et le sommeil normal, et on n'allège l'écran que le temps de l'animation de nuit.
 */
@Mixin(Player.class)
public class SleepTimerDarknessMixin {

    @Inject(method = "getSleepTimer", at = @At("RETURN"), cancellable = true)
    private void itfollows$dimNightTransition(CallbackInfoReturnable<Integer> cir) {
        if (!ClientNightTransitionState.isActive()) {
            return;
        }
        int original = cir.getReturnValueI();
        if (original <= 0) {
            return;
        }
        int scaled = (int) (original * ItFollowsConfig.get().sleepDarknessFactor);
        cir.setReturnValue(Math.max(0, scaled));
    }
}

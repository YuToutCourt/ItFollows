package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.sieste.SiesteManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Empêche une sieste (Phase 1.5) de faire passer la nuit.
 *
 * <p>Le saut de temps vanilla n'a lieu que si assez de joueurs « dorment depuis assez
 * longtemps » ({@code isSleepingLongEnough}). En forçant ce retour à {@code false} pour les
 * joueurs en sieste, on garde le visuel « couché dans le lit » sans jamais avancer l'heure —
 * la nuit globale reste réservée à la Phase 5.
 */
@Mixin(Player.class)
public class PlayerSleepTimerMixin {

    @Inject(method = "isSleepingLongEnough", at = @At("HEAD"), cancellable = true)
    private void itfollows$blockNapNightSkip(CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this) instanceof ServerPlayer serverPlayer
                && SiesteManager.isNapping(serverPlayer.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}

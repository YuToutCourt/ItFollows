package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.curse.CurseManager;
import io.github.yutoutcourt.itfollows.fatigue.FatigueManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Capte les sauts pour appliquer le coût de fatigue. {@code jumpFromGround} est appelé
 * lorsqu'une entité saute depuis le sol ; on filtre sur les joueurs serveur.
 */
@Mixin(LivingEntity.class)
public class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void itfollows$onJump(CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayer serverPlayer) {
            FatigueManager.onJump(serverPlayer);
            CurseManager.onCurserJump(serverPlayer);
        }
    }
}

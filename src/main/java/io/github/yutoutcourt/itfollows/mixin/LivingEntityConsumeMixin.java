package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.curse.CurseManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Capte la fin de consommation d'un aliment (Phase 3 : sommelier). {@code completeUsingItem} est
 * appelé quand l'entité termine d'utiliser un item ; on filtre les joueurs serveur et les aliments.
 */
@Mixin(LivingEntity.class)
public class LivingEntityConsumeMixin {

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void itfollows$onConsume(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player) {
            ItemStack stack = player.getUseItem();
            if (!stack.isEmpty() && stack.isEdible()) {
                CurseManager.onVictimConsume(player, stack);
            }
        }
    }
}

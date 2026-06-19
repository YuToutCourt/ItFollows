package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.curse.CurseManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Capte le ramassage d'un item par un joueur (Phase 3 : quêteur, dénuement, diamantaire).
 * On notifie {@link CurseManager} avec l'item touché <b>et l'UUID de celui qui l'a jeté</b>
 * ({@code thrower}), côté serveur uniquement. Le thrower permet d'exiger que l'objet provienne
 * bien du maudit (un item miné par la victime a un thrower {@code null}).
 */
@Mixin(ItemEntity.class)
public class ItemEntityPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void itfollows$onPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = self.getItem();
            if (!stack.isEmpty()) {
                UUID thrower = ((ItemEntityThrowerAccessor) (Object) self).itfollows$getThrower();
                CurseManager.onVictimPickup(serverPlayer, stack, thrower);
            }
        }
    }
}

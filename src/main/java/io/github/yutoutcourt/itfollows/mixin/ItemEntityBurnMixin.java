package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.curse.CurseManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Capte la destruction d'un item par le feu/la lave (Phase 3 : sacrifice). Un item brûle en quelques
 * ticks — trop vite pour l'échantillonnage périodique des actions ; on notifie donc dès le premier
 * dégât de feu, en ne retenant que les objets <b>jetés par le maudit</b> ({@code thrower}). La
 * dé-duplication par id d'entité est gérée côté action.
 */
@Mixin(ItemEntity.class)
public class ItemEntityBurnMixin {

    @Inject(method = "hurt", at = @At("HEAD"))
    private void itfollows$onFireHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide() || !source.is(DamageTypeTags.IS_FIRE)) {
            return;
        }
        UUID throwerId = ((ItemEntityThrowerAccessor) (Object) self).itfollows$getThrower();
        if (throwerId == null || !(self.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer thrower = level.getServer().getPlayerList().getPlayer(throwerId);
        if (thrower != null) {
            CurseManager.onCurserItemBurned(thrower, self);
        }
    }
}

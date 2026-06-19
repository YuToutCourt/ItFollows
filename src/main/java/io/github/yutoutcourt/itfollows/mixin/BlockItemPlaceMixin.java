package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.curse.CurseManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Capte la pose d'un bloc par un joueur (Phase 3 : maçon, geôlier, bâtisseur, pyromane,
 * encerclement, facteur). Fabric 1.20.1 n'a pas d'event « bloc posé » ; on injecte donc à la fin
 * de {@link BlockItem#place} et on ne notifie qu'en cas de succès, côté serveur.
 */
@Mixin(BlockItem.class)
public class BlockItemPlaceMixin {

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void itfollows$onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) {
            return;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return;
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            CurseManager.onCurserPlaceBlock(player, context.getClickedPos(),
                    level.getBlockState(context.getClickedPos()));
        }
    }
}

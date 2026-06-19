package io.github.yutoutcourt.itfollows.mixin;

import com.mojang.authlib.GameProfile;
import io.github.yutoutcourt.itfollows.sieste.NapStarter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Donne à {@code ServerPlayer} le moyen de se coucher sans les vérifications vanilla.
 *
 * <p>{@code ServerPlayer.startSleeping} refuse de dormir en journée ({@code NOT_POSSIBLE_NOW})
 * ou près d'un monstre ({@code NOT_SAFE}). Pour la sieste (Phase 1.5), on veut contourner ça :
 * en étendant {@code Player} dans ce mixin, {@code super.startSleeping(pos)} compile en
 * {@code invokespecial Player.startSleeping}, soit la version basse qui couche le joueur sans
 * aucun check.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerNapMixin extends Player implements NapStarter {

    private ServerPlayerNapMixin(Level level, BlockPos pos, float yRot, GameProfile profile) {
        super(level, pos, yRot, profile);
    }

    @Override
    public void itfollows$forceNap(BlockPos pos) {
        super.startSleeping(pos);
    }
}

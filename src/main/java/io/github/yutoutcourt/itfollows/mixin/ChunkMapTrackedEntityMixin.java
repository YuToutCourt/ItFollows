package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import io.github.yutoutcourt.itfollows.tracking.StalkTrackerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

/**
 * Rendu hybride « je suis le seul à la voir » (Phase 2a).
 *
 * <p>{@code ChunkMap.TrackedEntity#updatePlayer} calcule un booléen « ce joueur doit-il voir
 * l'entité ? » : si vrai et pas encore vu → spawn packet ({@code addPairing}) ; si faux et déjà
 * vu → remove packet ({@code removePairing}). On force ce booléen à {@code false} pour tout
 * observateur du {@link StalkerEntity} qui n'est pas la cible courante.
 *
 * <p>Pourquoi modifier le booléen plutôt qu'annuler la méthode : annuler en {@code HEAD}
 * empêchait aussi le <i>retrait</i>. Quand la cible changeait (ex. passage en créatif), l'ancien
 * observateur restait dans {@code seenBy} et gardait une entité fantôme jusqu'à une déco/reco.
 * En passant par la branche vanilla {@code removePairing}, l'ancienne cible reçoit proprement le
 * paquet de retrait dès le tick suivant — sans relog.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class ChunkMapTrackedEntityMixin {

    @Shadow
    @Final
    private Entity entity;

    @ModifyVariable(method = "updatePlayer", at = @At("STORE"), ordinal = 0)
    private boolean itfollows$hideStalkerFromNonTargets(boolean shouldTrack, ServerPlayer player) {
        if (!shouldTrack || !(entity instanceof StalkerEntity)) {
            return shouldTrack;
        }
        UUID target = StalkTrackerState.get(player.server).getTargetPlayer();
        return target != null && player.getUUID().equals(target);
    }
}

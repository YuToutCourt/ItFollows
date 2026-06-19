package io.github.yutoutcourt.itfollows.sieste;

import net.minecraft.core.BlockPos;

/**
 * Duck interface implémentée par {@code ServerPlayer} (via {@code mixin/ServerPlayerNapMixin}).
 *
 * <p>Permet de coucher le joueur en appelant la version <strong>basse</strong>
 * {@code Player.startSleeping(BlockPos)} — celle qui ne fait aucun check vanilla
 * (jour/monstres/portée). L'override {@code ServerPlayer.startSleeping}, lui, refuserait
 * une sieste en journée ou près d'un monstre.
 */
public interface NapStarter {

    /** Couche le joueur dans le lit, sans aucune vérification vanilla. */
    void itfollows$forceNap(BlockPos pos);
}

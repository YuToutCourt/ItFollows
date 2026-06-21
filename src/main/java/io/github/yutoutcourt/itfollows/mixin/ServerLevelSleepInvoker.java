package io.github.yutoutcourt.itfollows.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Expose {@code ServerLevel.updateSleepingPlayerList()} (Phase 5).
 *
 * <p>Le sommeil profond couche le joueur via la version basse {@code Player.startSleeping}
 * (cf. {@code ServerPlayerNapMixin}) pour contourner les checks vanilla (monstres/portée). Mais
 * cette version ne met pas à jour le {@code sleepStatus} du monde — du coup vanilla ne compte
 * jamais le dormeur et le saut de nuit ne se déclenche pas. On rejoue donc ce recalcul à la main
 * juste après avoir couché un dormeur profond.
 */
@Mixin(ServerLevel.class)
public interface ServerLevelSleepInvoker {

    @Invoker("updateSleepingPlayerList")
    void itfollows$updateSleepingPlayerList();
}

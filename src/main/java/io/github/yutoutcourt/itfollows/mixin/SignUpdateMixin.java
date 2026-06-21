package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.curse.CurseManager;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Capte la (ré)écriture du texte d'un panneau par un joueur (Phase 3 : facteur). Fabric 1.20.1 n'a
 * pas d'event « panneau édité » ; on injecte donc à la réception du {@link ServerboundSignUpdatePacket}
 * côté serveur, où le maudit confirme son texte.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SignUpdateMixin {

    @Inject(method = "updateSignText", at = @At("HEAD"))
    private void itfollows$onSignText(ServerboundSignUpdatePacket packet, List<FilteredText> lines, CallbackInfo ci) {
        ServerGamePacketListenerImpl self = (ServerGamePacketListenerImpl) (Object) this;
        String text = String.join(" ", packet.getLines()).trim();
        CurseManager.onCurserSign(self.player, packet.getPos(), text);
    }
}

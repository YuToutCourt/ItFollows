package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.client.hud.FatigueHudOverlay;
import io.github.yutoutcourt.itfollows.client.render.StalkerRenderer;
import io.github.yutoutcourt.itfollows.entity.ModEntities;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ItfollowsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Réception de la fatigue du joueur local (S2C).
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.FATIGUE_SYNC,
                (client, handler, buf, responseSender) -> {
                    float fatigue = buf.readFloat();
                    client.execute(() -> ClientFatigueState.set(fatigue));
                });

        // Réception de l'état d'évanouissement (écran noir).
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.FAINT_SYNC,
                (client, handler, buf, responseSender) -> {
                    boolean fainted = buf.readBoolean();
                    client.execute(() -> ClientFatigueState.setFainted(fainted));
                });

        // Overlay HUD.
        HudRenderCallback.EVENT.register(new FatigueHudOverlay());

        // Phase 2 : rendu GeckoLib de l'entité traqueuse (reçue par le seul client traqué).
        EntityRendererRegistry.register(ModEntities.STALKER, StalkerRenderer::new);
    }
}

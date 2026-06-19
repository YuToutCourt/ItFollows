package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.client.hud.CurseHudOverlay;
import io.github.yutoutcourt.itfollows.client.hud.FatigueHudOverlay;
import io.github.yutoutcourt.itfollows.client.render.CurseIndicatorRenderer;
import io.github.yutoutcourt.itfollows.client.render.StalkerRenderer;
import io.github.yutoutcourt.itfollows.entity.ModEntities;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.UUID;

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

        // Phase 3 : statut de malédiction du joueur local.
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.CURSE_SYNC,
                (client, handler, buf, responseSender) -> {
                    boolean cursed = buf.readBoolean();
                    client.execute(() -> ClientCurseState.setCursed(cursed));
                });

        // Phase 3 : texte de l'objectif secret.
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.CURSE_OBJECTIVE,
                (client, handler, buf, responseSender) -> {
                    String text = buf.readUtf();
                    client.execute(() -> ClientCurseState.setObjective(text));
                });

        // Phase 3 : avancement de l'action (indicateur au-dessus de la victime).
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.CURSE_PROGRESS,
                (client, handler, buf, responseSender) -> {
                    UUID victim = buf.readUUID();
                    int percent = buf.readVarInt();
                    client.execute(() -> ClientCurseState.setProgress(victim, percent));
                });

        // Phase 3 : action accomplie → bannière verte + son de réussite.
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.CURSE_RESULT,
                (client, handler, buf, responseSender) -> {
                    String victimName = buf.readUtf();
                    client.execute(() -> {
                        ClientCurseState.flashSuccess(victimName);
                        client.getSoundManager().play(SimpleSoundInstance.forUI(
                                SoundEvents.PLAYER_LEVELUP, 1.0f));
                    });
                });

        // Overlays HUD.
        HudRenderCallback.EVENT.register(new FatigueHudOverlay());
        HudRenderCallback.EVENT.register(new CurseHudOverlay());

        // Phase 2 : rendu GeckoLib de l'entité traqueuse (reçue par le seul client traqué).
        EntityRendererRegistry.register(ModEntities.STALKER, StalkerRenderer::new);

        // Phase 3 : indicateur de progression flottant au-dessus de la victime désignée.
        WorldRenderEvents.AFTER_ENTITIES.register(CurseIndicatorRenderer::render);
    }
}

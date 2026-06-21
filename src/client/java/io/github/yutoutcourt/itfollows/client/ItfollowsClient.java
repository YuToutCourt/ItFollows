package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.Itfollows;
import io.github.yutoutcourt.itfollows.client.hud.CurseHudOverlay;
import io.github.yutoutcourt.itfollows.client.hud.FatigueHudOverlay;
import io.github.yutoutcourt.itfollows.client.hud.PresenceHudOverlay;
import io.github.yutoutcourt.itfollows.client.render.CurseIndicatorRenderer;
import io.github.yutoutcourt.itfollows.client.render.PresencePostProcessor;
import io.github.yutoutcourt.itfollows.client.render.StalkerRenderer;
import io.github.yutoutcourt.itfollows.entity.ModEntities;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
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
                        // Son « rare achievement » de Minecraft (toast de défi accompli).
                        client.getSoundManager().play(SimpleSoundInstance.forUI(
                                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
                    });
                });

        // Phase 4a : distance entité→cible (piste 2, pilote les overlays de présence). -1 = aucune.
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.PRESENCE_SYNC,
                (client, handler, buf, responseSender) -> {
                    float distance = buf.readFloat();
                    client.execute(() -> ClientPresenceState.set(distance));
                });

        // Phase 5 : démarrage de la transition de nuit globale (interpolation lissée + compensation latence).
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.SLEEP_NIGHT_START,
                (client, handler, buf, responseSender) -> {
                    long startTime = buf.readLong();
                    long endTime = buf.readLong();
                    int durationTicks = buf.readVarInt();
                    long startMillis = buf.readLong();
                    client.execute(() ->
                            ClientNightTransitionState.start(startTime, endTime, durationTicks, startMillis));
                });

        // Phase 5 : annulation de la transition de nuit (un dormeur s'est levé).
        ClientPlayNetworking.registerGlobalReceiver(ItFollowsNetworking.SLEEP_NIGHT_STOP,
                (client, handler, buf, responseSender) -> client.execute(ClientNightTransitionState::reset));

        // Overlays HUD.
        HudRenderCallback.EVENT.register(new FatigueHudOverlay());
        HudRenderCallback.EVENT.register(new CurseHudOverlay());
        HudRenderCallback.EVENT.register(new PresenceHudOverlay());

        // Phase 2 : rendu GeckoLib de l'entité traqueuse (reçue par le seul client traqué).
        EntityRendererRegistry.register(ModEntities.STALKER, StalkerRenderer::new);

        // Phase 3 : indicateur de progression flottant au-dessus de la victime désignée.
        WorldRenderEvents.AFTER_ENTITIES.register(CurseIndicatorRenderer::render);

        // Phase 4b : boucles ambiantes constantes (murmures + parasite radio) à ≤ 5 b, pilotées par la distance.
        ClientTickEvents.END_CLIENT_TICK.register(ClientPresenceAmbience::clientTick);

        // Phase 5 : avancée de la transition de nuit côté client (overworld uniquement), une fois par frame.
        WorldRenderEvents.START.register(context -> {
            ClientLevel world = context.world();
            if (world != null && ClientNightTransitionState.isActive()
                    && world.dimension().equals(Level.OVERWORLD)) {
                ClientNightTransitionState.tick(world);
            }
        });

        // Phase 4b : pipeline post-process GLSL (désaturation/wave/blur…) après le rendu du monde.
        PresencePostProcessor presencePostProcessor = new PresencePostProcessor();
        WorldRenderEvents.LAST.register(context ->
                presencePostProcessor.renderAfterWorld(context.tickDelta()));
        // Recréer la chaîne au rechargement des resources (F3+T, changement de pack).
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return new ResourceLocation(Itfollows.MOD_ID, "presence_post_processor");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager resourceManager) {
                        presencePostProcessor.reload();
                    }
                });
    }
}

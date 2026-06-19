package io.github.yutoutcourt.itfollows;

import io.github.yutoutcourt.itfollows.command.ItFollowsCommand;
import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.fatigue.FaintManager;
import io.github.yutoutcourt.itfollows.fatigue.FatigueManager;
import io.github.yutoutcourt.itfollows.entity.ModEntities;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import io.github.yutoutcourt.itfollows.sieste.SiesteManager;
import io.github.yutoutcourt.itfollows.sound.ModSounds;
import io.github.yutoutcourt.itfollows.tracking.HauntController;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Itfollows implements ModInitializer {

    public static final String MOD_ID = "itfollows";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Phase 0 : config + réseau + sons custom.
        ItFollowsConfig.get();
        ModSounds.register();
        ItFollowsNetworking.registerServer();

        // Phase 1 : fatigue.
        FatigueManager.register();
        ServerTickEvents.END_SERVER_TICK.register(FatigueManager::onServerTick);

        // Évanouissement : verrouillage/réveil quand la fatigue tombe à 0.
        ServerTickEvents.END_SERVER_TICK.register(FaintManager::tick);

        // Phase 1.5 : sieste (récup rapide via un lit, sans changer l'heure du monde).
        SiesteManager.register();
        ServerTickEvents.END_SERVER_TICK.register(SiesteManager::tick);

        // Phase 2 : entité traqueuse (enregistrement + boucle de traque serveur).
        ModEntities.register();
        ServerTickEvents.END_SERVER_TICK.register(HauntController::tick);
        // À la mort de la cible : on re-cible un autre joueur (pas la victime qui vient de mourir).
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                HauntController.onPlayerDeath(player);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ItFollowsCommand.register(dispatcher));

        LOGGER.info("[ItFollows] Initialisé (Phase 0 + Phase 1 : fatigue, Phase 2 : traque).");
    }
}

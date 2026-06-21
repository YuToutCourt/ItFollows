package io.github.yutoutcourt.itfollows.mixin;

import io.github.yutoutcourt.itfollows.Itfollows;
import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.net.ItFollowsNetworking;
import io.github.yutoutcourt.itfollows.sleep.NightTransition;
import io.github.yutoutcourt.itfollows.sleep.SleepManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Lisse le saut de nuit vanilla en une transition de temps interpolée (Phase 5).
 *
 * <p>Modelé sur {@code ServerWorldSleepAnimationMixin} du mod de référence {@code seamless-sleep},
 * re-mappé en noms officiels (Mojang) 1.20.1. Au lieu d'avancer le temps d'un coup, on démarre une
 * {@link NightTransition} (interpolation douce ~2-9 s), on la diffuse aux clients, et on
 * <b>suspend</b> le réveil des joueurs / la remise à zéro de la météo jusqu'à la fin de l'animation.
 * Si trop de dormeurs se lèvent en cours de route, on annule et on prévient les clients.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelNightTransitionMixin {

    @Unique
    private boolean itfollows$wakePlayersPending;
    @Unique
    private boolean itfollows$resetWeatherPending;
    /** Nombre de dormeurs au démarrage de l'animation : on annule dès qu'il en manque un (cf. cahier §7). */
    @Unique
    private int itfollows$sleepersAtStart;

    @Invoker("wakeUpAllPlayers")
    abstract void itfollows$invokeWakeUpAllPlayers();

    @Invoker("resetWeatherCycle")
    abstract void itfollows$invokeResetWeatherCycle();

    /** Le saut de nuit vanilla : on le remplace par le démarrage de l'animation. */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private void itfollows$redirectSetDayTime(ServerLevel world, long newTime) {
        if (!ItFollowsConfig.get().deepSleepEnabled || !world.dimension().equals(Level.OVERWORLD)) {
            world.setDayTime(newTime);
            return;
        }
        NightTransition state = SleepManager.nightTransition();
        if (state.isActive()) {
            return; // animation déjà en cours : ignore les saut(s) re-déclenchés par vanilla
        }
        long currentTime = world.getDayTime();
        if (newTime <= currentTime) {
            world.setDayTime(newTime);
            return;
        }
        state.start(currentTime, newTime, ItFollowsConfig.get());
        itfollows$wakePlayersPending = true;
        itfollows$resetWeatherPending = world.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE);
        itfollows$sleepersAtStart = itfollows$countSleeping(world);
        ItFollowsNetworking.sendNightStart(world, state.getStartTimeOfDay(), state.getEndTimeOfDay(),
                state.getDurationTicks(), state.getStartMillis());
        Itfollows.LOGGER.info("[ItFollows] Transition de nuit : {} → {} sur {} ticks ({} dormeurs).",
                currentTime, newTime, state.getDurationTicks(), itfollows$sleepersAtStart);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;wakeUpAllPlayers()V"))
    private void itfollows$redirectWakeUpAllPlayers(ServerLevel world) {
        if (world.dimension().equals(Level.OVERWORLD) && itfollows$wakePlayersPending) {
            return; // rejoué à la fin de l'animation
        }
        itfollows$invokeWakeUpAllPlayers();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;resetWeatherCycle()V"))
    private void itfollows$redirectResetWeatherCycle(ServerLevel world) {
        if (world.dimension().equals(Level.OVERWORLD) && itfollows$wakePlayersPending && itfollows$resetWeatherPending) {
            return; // rejoué à la fin de l'animation
        }
        itfollows$invokeResetWeatherCycle();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void itfollows$tickNightTransition(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (!self.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        NightTransition state = SleepManager.nightTransition();
        if (!state.isActive()) {
            return;
        }

        state.tick(self);

        // Annule si un dormeur s'est levé (vanilla avait validé le saut au départ ; on ne re-juge donc
        // PAS le pourcentage — un écart de formule avec SleepStatus annulerait à tort et réveillerait tout
        // le monde). « Si un seul se lève → arrêt immédiat » (cahier §7).
        if (itfollows$countSleeping(self) < itfollows$sleepersAtStart) {
            state.cancel();
            itfollows$wakePlayersPending = false;
            itfollows$resetWeatherPending = false;
            ItFollowsNetworking.sendNightStop(self);
            Itfollows.LOGGER.info("[ItFollows] Transition de nuit annulée : un dormeur s'est levé.");
            return;
        }

        if (!state.isActive() && itfollows$wakePlayersPending) {
            itfollows$invokeWakeUpAllPlayers();
            if (itfollows$resetWeatherPending) {
                itfollows$invokeResetWeatherCycle();
            }
            itfollows$wakePlayersPending = false;
            itfollows$resetWeatherPending = false;
            Itfollows.LOGGER.info("[ItFollows] Transition de nuit terminée, dormeurs réveillés.");
        }
    }

    @Unique
    private int itfollows$countSleeping(ServerLevel world) {
        int sleeping = 0;
        for (ServerPlayer player : world.players()) {
            if (!player.isSpectator() && player.isSleeping()) {
                sleeping++;
            }
        }
        return sleeping;
    }
}

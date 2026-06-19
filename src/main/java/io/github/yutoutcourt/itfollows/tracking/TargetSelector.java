package io.github.yutoutcourt.itfollows.tracking;

import io.github.yutoutcourt.itfollows.fatigue.FatigueState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Choix de la cible de l'entité : le joueur le <b>plus fatigué</b>.
 *
 * <p>Rappel de l'échelle : 0 = épuisé, 100 = en forme → « le plus fatigué » = la valeur la
 * plus <b>basse</b>. Les modes créatif/spectateur sont exclus (comme pour la fatigue elle-même).
 */
public final class TargetSelector {

    private TargetSelector() {
    }

    public static ServerPlayer chooseTarget(MinecraftServer server) {
        return chooseTarget(server, null);
    }

    /**
     * Choisit le joueur le plus fatigué en excluant {@code excluded} (la dernière victime),
     * pour qu'un joueur tué par l'entité ne soit pas re-maudit immédiatement à son réveil.
     *
     * <p>Si l'exclu est le <i>seul</i> candidat éligible (ex. partie solo), on le retient
     * quand même en repli : sinon la traque s'arrêterait définitivement après sa première mort.
     */
    public static ServerPlayer chooseTarget(MinecraftServer server, UUID excluded) {
        FatigueState fatigue = FatigueState.get(server);
        ServerPlayer best = null;
        float bestValue = Float.MAX_VALUE;
        ServerPlayer fallback = null;
        float fallbackValue = Float.MAX_VALUE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isExempt(player)) {
                continue;
            }
            float value = fatigue.getFatigue(player.getUUID());
            if (excluded != null && player.getUUID().equals(excluded)) {
                if (value < fallbackValue) {
                    fallbackValue = value;
                    fallback = player;
                }
                continue;
            }
            if (value < bestValue) {
                bestValue = value;
                best = player;
            }
        }
        return best != null ? best : fallback;
    }

    /** Aligné sur l'exemption de {@code FatigueManager} : créatif et spectateur ne sont pas traqués. */
    private static boolean isExempt(ServerPlayer player) {
        return player.isCreative() || player.isSpectator();
    }
}

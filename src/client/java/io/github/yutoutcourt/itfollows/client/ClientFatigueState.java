package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.fatigue.FatigueRules;
import io.github.yutoutcourt.itfollows.fatigue.FatigueTier;

/**
 * Fatigue du joueur local côté client, alimentée par les packets {@code fatigue_sync}.
 * Sert d'unique source pour l'overlay HUD et les futurs effets sensoriels client.
 */
public final class ClientFatigueState {

    private static float fatigue = FatigueRules.MAX;
    private static boolean fainted = false;

    private ClientFatigueState() {
    }

    public static void set(float value) {
        fatigue = FatigueRules.clamp(value);
    }

    public static float get() {
        return fatigue;
    }

    public static void setFainted(boolean value) {
        fainted = value;
    }

    public static boolean isFainted() {
        return fainted;
    }

    public static FatigueTier tier() {
        return FatigueRules.tierFor(fatigue, ItFollowsConfig.get());
    }
}

package io.github.yutoutcourt.itfollows.net;

import io.github.yutoutcourt.itfollows.Itfollows;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Couche réseau du mod. En 1.20.1 on utilise l'API basée buffer
 * ({@link ServerPlayNetworking#send}), pas le {@code CustomPayload} à codec (1.20.5+).
 */
public final class ItFollowsNetworking {

    /** Canal S2C : synchronise la fatigue du joueur local. Payload = float. */
    public static final ResourceLocation FATIGUE_SYNC =
            new ResourceLocation(Itfollows.MOD_ID, "fatigue_sync");

    /** Canal S2C : signale l'état d'évanouissement (écran noir client). Payload = boolean. */
    public static final ResourceLocation FAINT_SYNC =
            new ResourceLocation(Itfollows.MOD_ID, "faint_sync");

    private ItFollowsNetworking() {
    }

    /** Réservé aux futurs receivers C2S (aucun pour le MVP fatigue). */
    public static void registerServer() {
    }

    /** Envoie la valeur de fatigue courante au joueur concerné. */
    public static void sendFatigue(ServerPlayer player, float fatigue) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(fatigue);
        ServerPlayNetworking.send(player, FATIGUE_SYNC, buf);
    }

    /** Active ({@code true}) ou lève ({@code false}) l'écran noir d'évanouissement chez le joueur. */
    public static void sendFaint(ServerPlayer player, boolean fainted) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(fainted);
        ServerPlayNetworking.send(player, FAINT_SYNC, buf);
    }
}

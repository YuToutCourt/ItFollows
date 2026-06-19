package io.github.yutoutcourt.itfollows.net;

import io.github.yutoutcourt.itfollows.Itfollows;
import io.github.yutoutcourt.itfollows.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

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

    /**
     * Joue un son <b>uniquement</b> chez {@code player}, à la position monde donnée (sons spatialisés :
     * directionnels et atténués par la distance). Sert aux avertissements ciblés de la traque
     * (bruits lointains de l'escalade, voix « LOOK BEHIND YOU ») : les autres joueurs n'entendent rien,
     * cohérent avec « je suis le seul à la voir ». On envoie le {@link ClientboundSoundPacket} vanilla
     * directement sur la connexion du joueur plutôt que via {@code Level#playSound} (qui diffuse à tous).
     */
    public static void playSoundTo(ServerPlayer player, SoundEvent sound,
                                   double x, double y, double z, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                ModSounds.holder(sound), SoundSource.HOSTILE,
                x, y, z, volume, pitch, player.getRandom().nextLong()));
    }
}

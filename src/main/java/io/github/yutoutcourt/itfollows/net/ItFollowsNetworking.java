package io.github.yutoutcourt.itfollows.net;

import io.github.yutoutcourt.itfollows.Itfollows;
import io.github.yutoutcourt.itfollows.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

    /** Canal S2C : statut de malédiction du joueur local. Payload = boolean. */
    public static final ResourceLocation CURSE_SYNC =
            new ResourceLocation(Itfollows.MOD_ID, "curse_sync");

    /** Canal S2C : texte de l'objectif de malédiction (livraison + rappel). Payload = String. */
    public static final ResourceLocation CURSE_OBJECTIVE =
            new ResourceLocation(Itfollows.MOD_ID, "curse_objective");

    /** Canal S2C : avancement de l'action (UUID victime + pourcentage), pour l'indicateur. */
    public static final ResourceLocation CURSE_PROGRESS =
            new ResourceLocation(Itfollows.MOD_ID, "curse_progress");

    /** Canal S2C : action accomplie ! (retour visuel/sonore de réussite chez le maudit). Payload = String (nom victime). */
    public static final ResourceLocation CURSE_RESULT =
            new ResourceLocation(Itfollows.MOD_ID, "curse_result");

    /**
     * Canal S2C : distance (blocs) entre l'entité et la cible locale, pilote les effets de présence
     * « piste 2 ». Payload = float ; {@code -1} = pas d'entité applicable (la piste fatigue reste gérée
     * côté client via {@code fatigue_sync}).
     */
    public static final ResourceLocation PRESENCE_SYNC =
            new ResourceLocation(Itfollows.MOD_ID, "presence_sync");

    /**
     * Canal S2C : démarre la transition de nuit globale (Phase 5) sur tous les clients de l'overworld.
     * Payload = {@code long startTime, long endTime, int durationTicks, long startMillis} (interpolation
     * lissée + compensation de latence côté client).
     */
    public static final ResourceLocation SLEEP_NIGHT_START =
            new ResourceLocation(Itfollows.MOD_ID, "sleep_night_start");

    /** Canal S2C : annule la transition de nuit en cours (un dormeur s'est levé). Payload vide. */
    public static final ResourceLocation SLEEP_NIGHT_STOP =
            new ResourceLocation(Itfollows.MOD_ID, "sleep_night_stop");

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

    /** Signale au joueur s'il est maudit (overlay client on/off). */
    public static void sendCurse(ServerPlayer player, boolean cursed) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(cursed);
        ServerPlayNetworking.send(player, CURSE_SYNC, buf);
    }

    /** Livre/rappelle le texte de l'objectif de malédiction au maudit. */
    public static void sendCurseObjective(ServerPlayer player, String text) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(text);
        ServerPlayNetworking.send(player, CURSE_OBJECTIVE, buf);
    }

    /** Envoie l'avancement de l'action courante (UUID de la victime + pourcentage 0-100). */
    public static void sendCurseProgress(ServerPlayer player, java.util.UUID victim, int percent) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(victim);
        buf.writeVarInt(percent);
        ServerPlayNetworking.send(player, CURSE_PROGRESS, buf);
    }

    /** Signale au maudit que son action est accomplie (déclenche la bannière + le son de réussite). */
    public static void sendCurseResult(ServerPlayer player, String victimName) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(victimName);
        ServerPlayNetworking.send(player, CURSE_RESULT, buf);
    }

    /** Envoie la distance (blocs) entre l'entité et la cible ({@code -1} = aucune) pour la piste 2. */
    public static void sendPresence(ServerPlayer player, float entityDistance) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(entityDistance);
        ServerPlayNetworking.send(player, PRESENCE_SYNC, buf);
    }

    /** Diffuse le démarrage de la transition de nuit à tous les joueurs du monde concerné. */
    public static void sendNightStart(ServerLevel world, long startTime, long endTime,
                                      int durationTicks, long startMillis) {
        for (ServerPlayer player : world.players()) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeLong(startTime);
            buf.writeLong(endTime);
            buf.writeVarInt(durationTicks);
            buf.writeLong(startMillis);
            ServerPlayNetworking.send(player, SLEEP_NIGHT_START, buf);
        }
    }

    /** Diffuse l'annulation de la transition de nuit à tous les joueurs du monde concerné. */
    public static void sendNightStop(ServerLevel world) {
        for (ServerPlayer player : world.players()) {
            ServerPlayNetworking.send(player, SLEEP_NIGHT_STOP, PacketByteBufs.create());
        }
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

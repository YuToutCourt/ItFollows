package io.github.yutoutcourt.itfollows.client;

import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Boucles ambiantes « constantes » de présence (Phase 4b), pilotées par la distance reçue
 * ({@link ClientPresenceState}) — donc uniquement chez la cible. Deux couches :
 * <ul>
 *   <li><b>Parasite radio</b> ({@code radio_static}) : boucle dès que l'entité est à ≤
 *       {@code presenceEntityMaxDistance} (30 b), de plus en plus <b>fort</b> en approchant
 *       (quasi-silence à 30 b → plein volume au contact).</li>
 *   <li><b>Murmures</b> ({@code whisper}) : boucle à très courte distance, ≤ {@code presenceBandClose}
 *       (5 b), montant jusqu'au contact.</li>
 * </ul>
 * Le volume suit la proximité ; chaque couche s'arrête dès qu'on sort de sa portée.
 */
public final class ClientPresenceAmbience {

    private static PresenceLoopSound whisper;
    private static PresenceLoopSound radio;

    private ClientPresenceAmbience() {
    }

    public static void clientTick(Minecraft client) {
        ItFollowsConfig cfg = ItFollowsConfig.get();
        float d = ClientPresenceState.distance();

        if (client.player == null || d < 0.0f) {
            stopAll();
            return;
        }

        // Parasite radio : boucle sur TOUTE la portée (≤ 30 b). Part de quasi-silence à 30 b et monte
        // « de plus en plus fort » en approchant. Courbe quadratique (closeness²) → discret au loin,
        // franchement présent de près.
        float closeRadio = Mth.clamp((cfg.presenceEntityMaxDistance - d) / cfg.presenceEntityMaxDistance, 0.0f, 1.0f);
        radio = ensure(client, radio, ModSounds.RADIO_STATIC,
                cfg.presenceRadioVolume * (0.05f + 0.95f * closeRadio * closeRadio));

        // Murmures : boucle uniquement à très courte distance (≤ 5 b), montant jusqu'au contact.
        if (d <= cfg.presenceBandClose) {
            float near = Mth.clamp((cfg.presenceBandClose - d) / cfg.presenceBandClose, 0.0f, 1.0f);
            whisper = ensure(client, whisper, ModSounds.WHISPER,
                    cfg.presenceWhisperVolume * Mth.lerp(near, 0.35f, 1.0f));
        } else {
            whisper = stop(whisper);
        }
    }

    private static PresenceLoopSound ensure(Minecraft client, PresenceLoopSound instance,
                                            net.minecraft.sounds.SoundEvent event, float volume) {
        if (instance == null || instance.isStopped()) {
            instance = new PresenceLoopSound(event);
            instance.setVolume(volume); // volume réglé AVANT play (sinon démarrage à 0 → son sauté)
            client.getSoundManager().play(instance);
        }
        instance.setVolume(volume);
        return instance;
    }

    /** Arrête une boucle si elle tourne et renvoie {@code null} (pour réassigner le champ). */
    private static PresenceLoopSound stop(PresenceLoopSound instance) {
        if (instance != null) {
            instance.requestStop();
        }
        return null;
    }

    private static void stopAll() {
        whisper = stop(whisper);
        radio = stop(radio);
    }
}

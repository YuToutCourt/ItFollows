package io.github.yutoutcourt.itfollows.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Son de présence <b>en boucle</b> côté client (Phase 4b), pour les ambiances « constantes » à très
 * courte distance (murmures, parasite radio). Joué <i>relatif</i> au joueur (sans atténuation/panning)
 * pour rester présent en continu ; le volume est piloté en direct par {@link ClientPresenceAmbience}.
 */
public class PresenceLoopSound extends AbstractTickableSoundInstance {

    public PresenceLoopSound(SoundEvent event) {
        super(event, SoundSource.HOSTILE, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.relative = true; // relatif au joueur → audible en permanence (pas d'atténuation par distance)
    }

    public void setVolume(float value) {
        this.volume = value;
    }

    /**
     * Autorise le démarrage de la boucle même à volume 0 : sinon le {@code SoundEngine} la « saute »
     * (Skipping silent sound) et elle ne tourne jamais, restant muette même quand le volume remonte
     * ensuite avec la proximité.
     */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    /** Demande l'arrêt de la boucle (le SoundManager la retirera). */
    public void requestStop() {
        this.stop();
    }

    @Override
    public void tick() {
        // Rien : la boucle se maintient ; le volume est mis à jour de l'extérieur.
    }
}

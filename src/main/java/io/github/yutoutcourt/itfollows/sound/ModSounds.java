package io.github.yutoutcourt.itfollows.sound;

import io.github.yutoutcourt.itfollows.Itfollows;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Sons custom du mod (Phase 2 : escalade de la traque). Enregistrés dans le registre commun
 * (donc côté client comme serveur) par {@link #register()}, appelé depuis {@code Itfollows.onInitialize()}.
 *
 * <p>Les fichiers audio attendus (à fournir) vivent dans
 * {@code src/main/resources/assets/itfollows/sounds/} et sont déclarés dans
 * {@code assets/itfollows/sounds.json} :
 * <ul>
 *   <li>{@code haunt_distant.ogg} → bruit lointain de l'étape 1 ;</li>
 *   <li>{@code look_behind_you.ogg} → voix « LOOK BEHIND YOU » de la révélation.</li>
 * </ul>
 */
public final class ModSounds {

    /** Bruit lointain joué périodiquement à la cible pendant l'étape 1 (avertissement discret). */
    public static SoundEvent HAUNT_DISTANT;
    /** Voix « LOOK BEHIND YOU » jouée au déclenchement de la révélation, derrière la cible. */
    public static SoundEvent LOOK_BEHIND_YOU;

    // --- Phase 4a : effets de présence (fichiers .ogg à fournir, sinon silencieux sans crash) ---
    /** Murmures / chuchotements (fatigue ≤ 5 % et bande entité ~10 blocs, en stéréo L/R). */
    public static SoundEvent WHISPER;
    /** Cri de « hunting » joué rarement quand l'entité est dans la bande lointaine (~30 blocs). */
    public static SoundEvent HUNTING_CRY;
    /** Parasite radio / grésillement électrique de la bande intermédiaire (~15 blocs). */
    public static SoundEvent RADIO_STATIC;

    // --- Phase 5 : audio en dormant (joué uniquement au dormeur, près du lit) ---
    /** Respiration lente/oppressante perçue près du lit pendant le sommeil profond. */
    public static SoundEvent SLEEP_BREATH;

    // --- Voix de l'entité (lignes Fiddlesticks fournies par .ogg ; plusieurs variantes possibles). ---
    /** Fausse réassurance chuchotée pendant les flashs de silhouette (étape 3) : « ce n'est qu'un épouvantail… ». */
    public static SoundEvent SILHOUETTE_WHISPER;
    /** Appât chuchoté pendant l'attente de la révélation, tant que la cible n'a pas regardé le leurre : « approche… ». */
    public static SoundEvent REVEAL_LURE;
    /** Éclat de panique de l'entité au moment où elle frappe sa cible (rare). */
    public static SoundEvent ENTITY_PANIC;
    /** Joué à l'ancien maudit, soulagé, quand sa malédiction part vers une autre victime. */
    public static SoundEvent CURSE_PASSED;
    /** Joué à la nouvelle victime au moment où la malédiction lui est transmise (présage menaçant). */
    public static SoundEvent CURSE_RECEIVED;

    private ModSounds() {
    }

    public static void register() {
        HAUNT_DISTANT = create("haunt_distant");
        LOOK_BEHIND_YOU = create("look_behind_you");
        WHISPER = create("whisper");
        HUNTING_CRY = create("hunting_cry");
        RADIO_STATIC = create("radio_static");
        SLEEP_BREATH = create("sleep_breath");
        SILHOUETTE_WHISPER = create("silhouette_whisper");
        REVEAL_LURE = create("reveal_lure");
        ENTITY_PANIC = create("entity_panic");
        CURSE_PASSED = create("curse_passed");
        CURSE_RECEIVED = create("curse_received");
    }

    private static SoundEvent create(String name) {
        ResourceLocation id = new ResourceLocation(Itfollows.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Holder direct (sérialisé inline dans le paquet) pour {@code ClientboundSoundPacket}. */
    public static Holder<SoundEvent> holder(SoundEvent event) {
        return Holder.direct(event);
    }
}

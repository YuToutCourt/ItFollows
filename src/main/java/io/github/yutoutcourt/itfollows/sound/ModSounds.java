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

    private ModSounds() {
    }

    public static void register() {
        HAUNT_DISTANT = create("haunt_distant");
        LOOK_BEHIND_YOU = create("look_behind_you");
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

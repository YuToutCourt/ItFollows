package io.github.yutoutcourt.itfollows.mixin.client;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Accès au champ privé {@code passes} de {@link PostChain} : permet d'itérer les passes pour écrire
 * nos uniforms custom ({@code DesatAmount}, {@code WaveAmount}…) chaque frame avant {@code process()}.
 */
@Mixin(PostChain.class)
public interface PostChainAccessor {

    @Accessor("passes")
    List<PostPass> itfollows$getPasses();
}

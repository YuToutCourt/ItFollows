package io.github.yutoutcourt.itfollows.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

/**
 * Accès en lecture au champ privé {@code thrower} d'un {@link ItemEntity} (1.20.1 ne fournit pas de
 * getter public). Utilisé en Phase 3 pour vérifier qu'un objet ramassé par la victime a bien été
 * <b>jeté par le maudit</b> (un objet miné par la victime a un thrower {@code null}).
 */
@Mixin(ItemEntity.class)
public interface ItemEntityThrowerAccessor {

    @Accessor("thrower")
    UUID itfollows$getThrower();
}

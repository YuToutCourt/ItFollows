package io.github.yutoutcourt.itfollows.curse;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * Grandes catégories d'objets utilisées par « Le dénuement » : on cherche une catégorie
 * <b>entièrement absente</b> de l'inventaire de la cible pour forcer un don ciblé.
 */
public enum ItemCategory {
    ARME("une arme", stack -> stack.getItem() instanceof SwordItem),
    NOURRITURE("de la nourriture", stack -> stack.getItem().isEdible()),
    OUTIL("un outil", stack -> stack.getItem() instanceof DiggerItem),
    ARMURE("une armure", stack -> stack.getItem() instanceof ArmorItem);

    private final String label;
    private final java.util.function.Predicate<ItemStack> test;

    ItemCategory(String label, java.util.function.Predicate<ItemStack> test) {
        this.label = label;
        this.test = test;
    }

    public String label() {
        return label;
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && test.test(stack);
    }
}

package io.github.yutoutcourt.itfollows.entity;

import io.github.yutoutcourt.itfollows.Itfollows;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Enregistrement des entités du mod (Phase 2). {@link #register()} doit être appelé
 * depuis {@code Itfollows.onInitialize()}.
 */
public final class ModEntities {

    public static final EntityType<StalkerEntity> STALKER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            new ResourceLocation(Itfollows.MOD_ID, "stalker"),
            FabricEntityTypeBuilder.create(MobCategory.MISC, StalkerEntity::new)
                    .dimensions(EntityDimensions.scalable(0.6f, 1.95f))
                    .trackRangeBlocks(1000)
                    .build());

    private ModEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.createAttributes());
    }
}

package io.github.yutoutcourt.itfollows.client.render;

import io.github.yutoutcourt.itfollows.Itfollows;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Lie l'entité Stalker à ses ressources GeckoLib exportées depuis Blockbench :
 * géométrie ({@code geo/stalker.geo.json}), animations
 * ({@code animations/stalker.animation.json}) et texture.
 */
public class StalkerGeoModel extends GeoModel<StalkerEntity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(Itfollows.MOD_ID, "geo/stalker.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Itfollows.MOD_ID, "textures/entity/stalker.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(Itfollows.MOD_ID, "animations/stalker.animation.json");

    @Override
    public ResourceLocation getModelResource(StalkerEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StalkerEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(StalkerEntity entity) {
        return ANIMATION;
    }
}

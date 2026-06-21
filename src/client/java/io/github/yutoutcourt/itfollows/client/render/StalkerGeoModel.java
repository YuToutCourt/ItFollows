package io.github.yutoutcourt.itfollows.client.render;

import io.github.yutoutcourt.itfollows.Itfollows;
import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Lie l'entité Stalker à ses ressources GeckoLib exportées depuis Blockbench :
 * géométrie ({@code geo/toww_geckolib.geo.json}), animations
 * ({@code animations/toww_geckolib.animation.json}) et texture ({@code toww_reborn.png}).
 */
public class StalkerGeoModel extends GeoModel<StalkerEntity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(Itfollows.MOD_ID, "geo/toww_geckolib.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Itfollows.MOD_ID, "textures/entity/toww_reborn.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(Itfollows.MOD_ID, "animations/toww_geckolib.animation.json");

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

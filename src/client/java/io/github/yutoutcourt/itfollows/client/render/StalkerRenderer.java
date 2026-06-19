package io.github.yutoutcourt.itfollows.client.render;

import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Rendu GeckoLib de l'entité traqueuse à partir du modèle Blockbench.
 *
 * <p>Le {@link AutoGlowingGeoLayer} rend la couche émissive
 * ({@code textures/entity/stalker_glowmask.png}) en fullbright : yeux et bouche
 * « brillent » dans le noir (et captés par les shaders émissifs type Iris).
 *
 * <p>Comme seul le client de la cible reçoit l'entité (cf. {@code ChunkMapTrackedEntityMixin}),
 * aucune logique conditionnelle n'est nécessaire ici : on dessine toujours.
 */
public class StalkerRenderer extends GeoEntityRenderer<StalkerEntity> {

    public StalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new StalkerGeoModel());
        this.shadowRadius = 0.5f;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}

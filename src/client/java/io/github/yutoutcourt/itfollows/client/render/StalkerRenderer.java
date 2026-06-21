package io.github.yutoutcourt.itfollows.client.render;

import io.github.yutoutcourt.itfollows.entity.StalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Rendu GeckoLib de l'entité traqueuse à partir du modèle Blockbench
 * ({@code toww_geckolib.geo.json} + {@code toww_reborn.png}).
 *
 * <p>Comme seul le client de la cible reçoit l'entité (cf. {@code ChunkMapTrackedEntityMixin}),
 * aucune logique conditionnelle n'est nécessaire ici : on dessine toujours.
 */
public class StalkerRenderer extends GeoEntityRenderer<StalkerEntity> {

    public StalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new StalkerGeoModel());
        this.shadowRadius = 0.5f;
    }
}

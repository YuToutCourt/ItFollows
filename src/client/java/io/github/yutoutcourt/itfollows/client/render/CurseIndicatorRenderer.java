package io.github.yutoutcourt.itfollows.client.render;

import io.github.yutoutcourt.itfollows.client.ClientCurseState;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * Dessine, <b>uniquement pour le maudit</b>, le pourcentage d'avancement de l'action en cours
 * flottant au-dessus de la victime désignée (« il la regarde, 10 %… 15 %… »). Visible à travers
 * les blocs pour rester lisible où qu'elle soit.
 */
public final class CurseIndicatorRenderer {

    private CurseIndicatorRenderer() {
    }

    public static void render(WorldRenderContext context) {
        if (!ClientCurseState.isCursed()) {
            return;
        }
        int percent = ClientCurseState.progressPercent();
        UUID victimId = ClientCurseState.victim();
        if (percent < 0 || victimId == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        Entity victim = findByUuid(client, victimId);
        if (victim == null || victim == client.player) {
            return;
        }

        Camera camera = context.camera();
        Vec3 cam = camera.getPosition();
        float tickDelta = context.tickDelta();
        double x = victim.xOld + (victim.getX() - victim.xOld) * tickDelta;
        double y = victim.yOld + (victim.getY() - victim.yOld) * tickDelta;
        double z = victim.zOld + (victim.getZ() - victim.zOld) * tickDelta;

        var pose = context.matrixStack();
        pose.pushPose();
        pose.translate(x - cam.x, y + victim.getBbHeight() + 0.6 - cam.y, z - cam.z);
        pose.mulPose(camera.rotation());
        pose.scale(-0.025f, -0.025f, 0.025f);

        Font font = client.font;
        String text = percent + "%";
        float dx = -font.width(text) / 2.0f;
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        int color = colorFor(percent);
        font.drawInBatch(text, dx, 0.0f, color, false, matrix, buffers,
                Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        buffers.endBatch();
        pose.popPose();
    }

    private static Entity findByUuid(Minecraft client, UUID id) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(id)) {
                return entity;
            }
        }
        return null;
    }

    private static int colorFor(int percent) {
        if (percent >= 100) {
            return 0xFF55FF55;
        }
        if (percent >= 50) {
            return 0xFFFFFF55;
        }
        return 0xFFFF8080;
    }
}

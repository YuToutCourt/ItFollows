package io.github.yutoutcourt.itfollows.client.hud;

import io.github.yutoutcourt.itfollows.client.ClientFatigueState;
import io.github.yutoutcourt.itfollows.client.ClientPresenceState;
import io.github.yutoutcourt.itfollows.client.PresenceVisuals;
import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * Overlay HUD des effets de présence — ce qui ne passe pas par le shader/les mixins :
 * <ul>
 *   <li><b>Assombrissement</b> plein écran (voile noir uniforme, fatigue uniquement) ;</li>
 *   <li><b>Pulsation cardiaque</b> : voile rouge plein écran lissé, synchronisé au battement ;</li>
 *   <li><b>Silhouette</b> 1 frame (bande danger ≤10 b) ;</li>
 *   <li><b>Micro-freeze</b> : flash sombre 1 frame (bande ≤5 b).</li>
 * </ul>
 *
 * <p>La <b>vignette</b> et la <b>désaturation/wave/blur/noise</b> sont gérées par le shader GLSL
 * ({@code PresencePostProcessor} + {@code PresenceVisuals}) — gradient radial lisse, sans les
 * « bandes » des anciens anneaux HUD. Tous les voiles ici sont des {@code fill} pleins (aucun anneau).
 */
public class PresenceHudOverlay implements HudRenderCallback {

    private static final Random RANDOM = new Random();

    @Override
    public void onHudRender(GuiGraphics guiGraphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }
        // L'écran noir d'évanouissement prime sur tout.
        if (ClientFatigueState.isFainted()) {
            return;
        }

        ItFollowsConfig config = ItFollowsConfig.get();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        float fatigue = ClientFatigueState.get();
        float closeness = PresenceVisuals.closeness(); // 0 (loin/aucune) → 1 (contact)
        float distance = ClientPresenceState.distance();
        boolean hasEntity = ClientPresenceState.hasEntity();

        // Assombrissement plein écran lié à la fatigue (voile uniforme — pas d'anneaux).
        // L'assombrissement de proximité a migré vers la vignette radiale du shader (Phase 4b).
        float darken = fatigueDarken(fatigue, config) * config.presenceDarkenMaxAlpha;
        if (darken > 0.004f) {
            int a = (int) (darken * 255.0f) << 24;
            guiGraphics.fill(0, 0, width, height, a);
        }

        // Pulsation cardiaque : voile rouge plein écran lissé, synchronisé au battement.
        drawHeartbeatPulse(guiGraphics, width, height, fatigue, distance, hasEntity, closeness, config);

        // Silhouette « 1 frame » — bande danger (≤10 b).
        if (hasEntity && distance <= config.presenceBandDanger && distance > config.presenceBandCut) {
            if (RANDOM.nextFloat() < 0.02f + closeness * 0.05f) {
                drawSilhouette(guiGraphics, width, height);
            }
        }

        // Micro-freeze (approx du gel 0,1 s) — bande très proche (≤5 b) : flash sombre 1 frame.
        if (hasEntity && distance <= config.presenceBandClose) {
            if (RANDOM.nextFloat() < 0.01f + closeness * 0.03f) {
                guiGraphics.fill(0, 0, width, height, 0xDD000000);
            }
        }
    }

    private static float fatigueDarken(float f, ItFollowsConfig c) {
        if (f <= c.presenceFatigueWhisper) return 1.0f;
        if (f <= c.presenceFatigueStrong) return 0.6f;
        return 0.0f;
    }

    /** Voile rouge plein écran (lissé, sans anneaux) pulsant au rythme du cœur. */
    private static void drawHeartbeatPulse(GuiGraphics g, int width, int height, float fatigue,
                                           float distance, boolean hasEntity, float closeness, ItFollowsConfig c) {
        float intensity;
        long period;
        if (hasEntity && distance <= c.presenceBandClose) {
            intensity = Mth.clamp(closeness, 0.0f, 1.0f);
            period = (long) Mth.lerp(intensity, 900.0f, 420.0f);
        } else if (fatigue <= c.presenceFatigueLight) {
            float t = Mth.clamp(1.0f - fatigue / Math.max(1.0f, c.presenceFatigueLight), 0.0f, 1.0f);
            intensity = Mth.lerp(t, 0.25f, 0.8f);
            period = (long) Mth.lerp(t, 1200.0f, 520.0f);
        } else {
            return;
        }
        float phase = (System.currentTimeMillis() % period) / (float) period;
        float pulse = (float) Math.max(0.0, Math.sin(phase * Math.PI));
        float amp = intensity * pulse;
        if (amp <= 0.02f) {
            return;
        }
        int alpha = (int) (amp * 0.14f * 255.0f) << 24; // voile rouge léger au pic
        g.fill(0, 0, width, height, alpha | 0x550000);
    }

    /** Silhouette humanoïde sombre dessinée pour une seule frame (effet « quelque chose passe »). */
    private static void drawSilhouette(GuiGraphics g, int width, int height) {
        int figH = height / 3;
        int figW = figH / 3;
        int fx = RANDOM.nextInt(Math.max(1, width - figW));
        int fy = (height - figH) / 2 + RANDOM.nextInt(Math.max(1, height / 6)) - height / 12;
        int color = 0xB0000000;
        int head = figW / 2;
        g.fill(fx + (figW - head) / 2, fy, fx + (figW + head) / 2, fy + head, color); // tête
        g.fill(fx, fy + head, fx + figW, fy + figH, color);                            // corps
    }
}

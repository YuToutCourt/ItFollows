package io.github.yutoutcourt.itfollows.client.hud;

import io.github.yutoutcourt.itfollows.client.ClientCurseState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Overlay HUD de la malédiction : affiche discrètement l'objectif secret du maudit en haut de
 * l'écran. À l'arrivée d'un nouvel objectif, un bref « flash » plus visible attire l'attention.
 */
public class CurseHudOverlay implements HudRenderCallback {

    private static final int FLASH_MS = 6000;
    private static final int SUCCESS_MS = 5000;

    @Override
    public void onHudRender(GuiGraphics guiGraphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }
        int width = client.getWindow().getGuiScaledWidth();

        // Bannière de réussite : prioritaire, s'affiche même après la levée de la malédiction.
        long sinceSuccess = System.currentTimeMillis() - ClientCurseState.successAt();
        if (sinceSuccess < SUCCESS_MS) {
            String victim = ClientCurseState.successVictim();
            String done = victim.isEmpty()
                    ? "✔ Malédiction transmise !"
                    : "✔ Malédiction transmise à " + victim + " !";
            guiGraphics.drawCenteredString(client.font, done, width / 2, 8, 0xFF55FF55);
            return;
        }

        if (!ClientCurseState.hasObjective()) {
            return;
        }

        long since = System.currentTimeMillis() - ClientCurseState.objectiveSetAt();
        boolean flash = since < FLASH_MS;

        int headerColor = flash ? 0xFFFF5555 : 0xFFAA3333;
        String header = "✦ Transmettre la malédiction ✦";
        guiGraphics.drawCenteredString(client.font, header, width / 2, 6, headerColor);

        String objective = ClientCurseState.objective();
        int objColor = flash ? 0xFFFFFFFF : 0xFFD0C0C0;
        guiGraphics.drawCenteredString(client.font, objective, width / 2, 18, objColor);
    }
}

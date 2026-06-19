package io.github.yutoutcourt.itfollows.client.hud;

import io.github.yutoutcourt.itfollows.client.ClientFatigueState;
import io.github.yutoutcourt.itfollows.fatigue.FatigueRules;
import io.github.yutoutcourt.itfollows.fatigue.FatigueTier;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Overlay HUD client de la fatigue : barre en haut à gauche + assombrissement
 * progressif de l'écran aux paliers ELEVEE / CRITIQUE.
 *
 * <p>Les effets plus poussés (FOV, respiration, nausée) viendront s'ajouter ici,
 * pilotés par le même {@link FatigueTier}.
 */
public class FatigueHudOverlay implements HudRenderCallback {

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 12;

    @Override
    public void onHudRender(GuiGraphics guiGraphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        // Évanouissement : écran totalement noir, prioritaire et indépendant de hideGui.
        if (ClientFatigueState.isFainted()) {
            drawFaint(client, guiGraphics);
            return;
        }

        if (client.options.hideGui) {
            return;
        }

        drawBar(client, guiGraphics);
        drawDarkening(client, guiGraphics);
    }

    private void drawFaint(Minecraft client, GuiGraphics guiGraphics) {
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        guiGraphics.fill(0, 0, width, height, 0xFF000000);
        guiGraphics.drawCenteredString(client.font, "Évanoui...", width / 2, height / 2, 0xFF555555);
    }

    private void drawBar(Minecraft client, GuiGraphics guiGraphics) {
        float fatigue = ClientFatigueState.get();
        FatigueTier tier = ClientFatigueState.tier();

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        
        // Alignée avec le début de la barre de faim (à droite de la barre d'action)
        int x = screenWidth / 2 + 10;
        // Juste au-dessus de la barre de faim (qui est à height - 39)
        int y = screenHeight - 55;

        // Corps de la batterie (bordure + fond)
        guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF000000);
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF3A3A3A);

        // Embout de la batterie (à droite)
        guiGraphics.fill(x + BAR_WIDTH + 1, y + 3, x + BAR_WIDTH + 3, y + BAR_HEIGHT - 3, 0xFF000000);
        guiGraphics.fill(x + BAR_WIDTH + 1, y + 4, x + BAR_WIDTH + 2, y + BAR_HEIGHT - 4, 0xFF3A3A3A);

        // Remplissage proportionnel.
        int fillWidth = Math.round(BAR_WIDTH * (fatigue / FatigueRules.MAX));
        if (fillWidth > 0) {
            guiGraphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, colorFor(tier));
        }

        // Texte du pourcentage centré
        int percentage = Math.round((fatigue / FatigueRules.MAX) * 100);
        String text = percentage + "%";
        // Le texte fait 8 pixels de haut, on le dessine à y + 2 pour le centrer dans la barre de 12
        guiGraphics.drawCenteredString(client.font, text, x + BAR_WIDTH / 2, y + 2, 0xFFFFFF);
    }

    private void drawDarkening(Minecraft client, GuiGraphics guiGraphics) {
        float darkness = switch (ClientFatigueState.tier()) {
            case ELEVEE -> 0.25f;
            case CRITIQUE -> 0.5f;
            default -> 0.0f;
        };
        if (darkness <= 0.0f) {
            return;
        }
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int alpha = (int) (darkness * 255.0f) << 24;
        guiGraphics.fill(0, 0, width, height, alpha);
    }

    private static int colorFor(FatigueTier tier) {
        return switch (tier) {
            case NONE -> 0xFF4CAF50;     // vert
            case LEGERE -> 0xFF9CCC65;   // vert-jaune
            case MOYENNE -> 0xFFFFEB3B;  // jaune
            case ELEVEE -> 0xFFFF9800;   // orange
            case CRITIQUE -> 0xFFF44336; // rouge
        };
    }
}

package io.github.yutoutcourt.itfollows.client.hud;

import io.github.yutoutcourt.itfollows.client.ClientFatigueState;
import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
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

        ItFollowsConfig config = ItFollowsConfig.get();
        String style = config.fatigueHudStyle != null ? config.fatigueHudStyle : "BATTERY_VERTICAL";
        String alignment = config.fatigueHudXAlignment != null ? config.fatigueHudXAlignment : "CENTER";
        boolean showPercent = config.fatigueHudShowPercent;

        int x = 0;
        int y = 0;

        switch (style) {
            case "BATTERY_HORIZONTAL" -> {
                x = switch (alignment) {
                    case "CENTER" -> screenWidth / 2 - BAR_WIDTH / 2;
                    case "LEFT_OF_HUNGER" -> screenWidth / 2 + 10;
                    case "RIGHT_OF_HEARTS" -> screenWidth / 2 - 10 - BAR_WIDTH;
                    default -> screenWidth / 2 + 10; // ABOVE_HUNGER / DEFAULT
                };
                y = screenHeight - 55;
            }
            case "BUBBLE" -> {
                int bubbleWidth = 9;
                x = switch (alignment) {
                    case "CENTER" -> screenWidth / 2 - 4; // screenWidth / 2 - bubbleWidth / 2
                    case "LEFT_OF_HUNGER" -> screenWidth / 2 + 5;
                    case "RIGHT_OF_HEARTS" -> screenWidth / 2 - 14;
                    default -> screenWidth / 2 + 10; // ABOVE_HUNGER
                };
                y = alignment.equals("CENTER") || alignment.equals("LEFT_OF_HUNGER") || alignment.equals("RIGHT_OF_HEARTS")
                        ? screenHeight - 42
                        : screenHeight - 55;
            }
            default -> { // "BATTERY_VERTICAL"
                int batteryWidth = 8;
                x = switch (alignment) {
                    case "CENTER" -> screenWidth / 2 - 4;
                    case "LEFT_OF_HUNGER" -> screenWidth / 2 + 5;
                    case "RIGHT_OF_HEARTS" -> screenWidth / 2 - 13;
                    default -> screenWidth / 2 + 10; // ABOVE_HUNGER
                };
                y = alignment.equals("CENTER") || alignment.equals("LEFT_OF_HUNGER") || alignment.equals("RIGHT_OF_HEARTS")
                        ? screenHeight - 48
                        : screenHeight - 55;
            }
        }

        x += config.fatigueHudOffsetX;
        y += config.fatigueHudOffsetY;

        switch (style) {
            case "BATTERY_HORIZONTAL" -> drawHorizontalBattery(guiGraphics, client, x, y, fatigue, tier, showPercent);
            case "BUBBLE" -> drawBubble(guiGraphics, client, x, y, fatigue, tier, showPercent);
            default -> drawVerticalBattery(guiGraphics, client, x, y, fatigue, tier, showPercent);
        }
    }

    private void drawHorizontalBattery(GuiGraphics guiGraphics, Minecraft client, int x, int y, float fatigue, FatigueTier tier, boolean showPercent) {
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
        if (showPercent) {
            int percentage = Math.round((fatigue / FatigueRules.MAX) * 100);
            String text = percentage + "%";
            // Le texte fait 8 pixels de haut, on le dessine à y + 2 pour le centrer dans la barre de 12
            guiGraphics.drawCenteredString(client.font, text, x + BAR_WIDTH / 2, y + 2, 0xFFFFFF);
        }
    }

    private void drawVerticalBattery(GuiGraphics guiGraphics, Minecraft client, int x, int y, float fatigue, FatigueTier tier, boolean showPercent) {
        int width = 8;
        int height = 14;

        // Embout du haut (centré, largeur 4, hauteur 2)
        guiGraphics.fill(x + 2, y - 2, x + 6, y, 0xFF000000);
        guiGraphics.fill(x + 3, y - 1, x + 5, y, 0xFF3A3A3A);

        // Corps de la batterie (bordure + fond)
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
        guiGraphics.fill(x, y, x + width, y + height, 0xFF3A3A3A);

        // Remplissage proportionnel du bas vers le haut
        int fillHeight = Math.round(height * (fatigue / FatigueRules.MAX));
        if (fillHeight > 0) {
            guiGraphics.fill(x, y + height - fillHeight, x + width, y + height, colorFor(tier));
        }

        // Texte du pourcentage au-dessus
        if (showPercent) {
            int percentage = Math.round((fatigue / FatigueRules.MAX) * 100);
            String text = percentage + "%";
            guiGraphics.drawCenteredString(client.font, text, x + width / 2, y - 12, 0xFFFFFF);
        }
    }

    private void drawBubble(GuiGraphics guiGraphics, Minecraft client, int x, int y, float fatigue, FatigueTier tier, boolean showPercent) {
        // Dessin d'une bulle pixelisée de 9x9 pixels
        // Bordure noire (outline)
        guiGraphics.fill(x + 2, y,     x + 7, y + 1, 0xFF000000); // Haut
        guiGraphics.fill(x + 1, y + 1, x + 2, y + 2, 0xFF000000);
        guiGraphics.fill(x + 7, y + 1, x + 8, y + 2, 0xFF000000);
        guiGraphics.fill(x,     y + 2, x + 1, y + 7, 0xFF000000); // Gauche
        guiGraphics.fill(x + 8, y + 2, x + 9, y + 7, 0xFF000000); // Droite
        guiGraphics.fill(x + 1, y + 7, x + 2, y + 8, 0xFF000000);
        guiGraphics.fill(x + 7, y + 7, x + 8, y + 8, 0xFF000000);
        guiGraphics.fill(x + 2, y + 8, x + 7, y + 9, 0xFF000000); // Bas

        // Fond gris à l'intérieur
        guiGraphics.fill(x + 2, y + 1, x + 7, y + 2, 0xFF3A3A3A);
        guiGraphics.fill(x + 1, y + 2, x + 8, y + 7, 0xFF3A3A3A);
        guiGraphics.fill(x + 2, y + 7, x + 7, y + 8, 0xFF3A3A3A);

        // Remplissage de la couleur correspondant au tier (du bas vers le haut)
        // La hauteur intérieure est de 7 pixels (y+1 à y+7 inclus)
        int fillHeight = Math.round(7 * (fatigue / FatigueRules.MAX));
        for (int i = 0; i < fillHeight; i++) {
            int rowY = y + 7 - i;
            if (rowY == y + 7 || rowY == y + 1) {
                // Les rangées 1 et 7 ont une largeur intérieure de 5 pixels (de x+2 à x+6 inclus)
                guiGraphics.fill(x + 2, rowY, x + 7, rowY + 1, colorFor(tier));
            } else {
                // Les rangées 2 à 6 ont une largeur de 7 pixels (de x+1 à x+7 inclus)
                guiGraphics.fill(x + 1, rowY, x + 8, rowY + 1, colorFor(tier));
            }
        }

        // Texte du pourcentage au-dessus
        if (showPercent) {
            int percentage = Math.round((fatigue / FatigueRules.MAX) * 100);
            String text = percentage + "%";
            guiGraphics.drawCenteredString(client.font, text, x + 4, y - 10, 0xFFFFFF);
        }
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

package com.skyhelper.features.hud;

import com.skyhelper.config.SkyHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD overlay showing the Pest Repellent countdown timer, icon, and progress bar.
 */
public class PestRepellentOverlay {

    private static final int BG_COLOR = 0xAA000000;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int BAR_BG = 0xFF333333;

    // Color thresholds
    private static final int COLOR_GREEN = 0xFF44CC44;
    private static final int COLOR_YELLOW = 0xFFDDDD22;
    private static final int COLOR_RED = 0xFFDD3333;

    private static final long FIVE_MIN_MS = 5 * 60 * 1000;
    private static final long TEN_MIN_MS = 10 * 60 * 1000;

    public static void render(GuiGraphics g) {
        if (!PestRepellentHandler.isActive()) return;

        SkyHelperConfig.HudPanelLayout layout = SkyHelperConfig.getPanel("pest_repellent");
        if (layout == null || !layout.visible) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = (int) (layout.x * screenW);
        int y = (int) (layout.y * screenH);
        int panelW = layout.width;
        int panelH = layout.height;

        // Clamp on screen
        x = Math.max(0, Math.min(x, screenW - panelW));
        y = Math.max(0, Math.min(y, screenH - panelH));

        long remainingMs = PestRepellentHandler.getRemainingMs();
        int timerColor = getTimerColor(remainingMs);

        // Background
        g.fill(x, y, x + panelW, y + panelH, BG_COLOR);

        // Icon: simple potion bottle shape (drawn with basic geometry)
        int iconX = x + 3;
        int iconY = y + 4;
        drawPotionIcon(g, iconX, iconY, timerColor);

        // Label: "Repellent" or "Repellent MAX"
        String label = PestRepellentHandler.isMax() ? "Repellent MAX" : "Repellent";
        int textX = iconX + 12;
        g.drawString(font, label, textX, y + 3, timerColor);

        // Timer: MM:SS
        int totalSecs = (int) (remainingMs / 1000);
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;
        String timerText = String.format("%d:%02d", mins, secs);
        g.drawString(font, timerText, textX, y + 14, TEXT_WHITE);

        // Progress bar at the bottom
        int barX = x + 3;
        int barY = y + panelH - 6;
        int barW = panelW - 6;
        int barH = 3;

        g.fill(barX, barY, barX + barW, barY + barH, BAR_BG);
        float progress = PestRepellentHandler.getProgress();
        int fillW = (int) (barW * progress);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, timerColor);
        }
    }

    private static int getTimerColor(long remainingMs) {
        if (remainingMs <= FIVE_MIN_MS) return COLOR_RED;
        if (remainingMs <= TEN_MIN_MS) return COLOR_YELLOW;
        return COLOR_GREEN;
    }

    /**
     * Draws a simple potion/spray bottle icon using basic filled rectangles.
     * About 8x12 pixels.
     */
    private static void drawPotionIcon(GuiGraphics g, int x, int y, int color) {
        // Bottle neck (narrow top)
        g.fill(x + 3, y, x + 5, y + 3, color);
        // Cap
        g.fill(x + 2, y, x + 6, y + 1, color);
        // Bottle body (wider)
        g.fill(x + 1, y + 3, x + 7, y + 10, color);
        // Bottom of bottle
        g.fill(x + 2, y + 10, x + 6, y + 11, color);
        // Liquid fill inside (darker shade as inner area)
        int inner = 0xFF000000 | ((color & 0xFF) / 2) | (((color >> 8) & 0xFF) / 2 << 8) | (((color >> 16) & 0xFF) / 2 << 16);
        g.fill(x + 2, y + 5, x + 6, y + 9, inner);
    }
}

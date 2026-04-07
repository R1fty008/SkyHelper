package com.skyhelper.features.hud;

import com.skyhelper.config.SkyHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Displays current skill XP progress on the HUD.
 * Reads position and size from the hud_layout config.
 */
public class SkillOverlay {

    private static final int BG_COLOR = 0xAA000000;
    private static final int BAR_BG = 0xFF333333;
    private static final int BAR_FILL = 0xFF44AAFF;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_SKILL = 0xFF55FFFF;

    private static final Pattern SKILL_PATTERN = Pattern.compile(
            "\\+(\\d[\\d,]*\\.?\\d*)\\s+(\\w+)\\s+\\(([\\d,]+\\.?\\d*)/([\\d,]+\\.?\\d*)\\)"
    );

    private static String currentSkill = "";
    private static double currentXp = 0;
    private static double maxXp = 0;
    private static long lastUpdateTime = 0;
    private static final long DISPLAY_DURATION = 10_000;

    public static void parseActionBar(String message) {
        Matcher matcher = SKILL_PATTERN.matcher(message);
        if (matcher.find()) {
            currentSkill = matcher.group(2);
            currentXp = parseNumber(matcher.group(3));
            maxXp = parseNumber(matcher.group(4));
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    private static double parseNumber(String s) {
        return Double.parseDouble(s.replace(",", ""));
    }

    public static void render(GuiGraphics g) {
        if (currentSkill.isEmpty()) return;
        if (System.currentTimeMillis() - lastUpdateTime > DISPLAY_DURATION) return;

        SkyHelperConfig.HudPanelLayout layout = SkyHelperConfig.getPanel("skill_xp");
        if (layout == null || !layout.visible) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = (int) (layout.x * screenW);
        int y = (int) (layout.y * screenH);
        int panelW = layout.width;
        int panelH = layout.height;

        // Safeguard: clamp on screen
        x = Math.max(0, Math.min(x, screenW - panelW));
        y = Math.max(0, Math.min(y, screenH - panelH));

        // Background panel
        g.fill(x, y, x + panelW, y + panelH, BG_COLOR);

        // Skill name and XP text
        String xpText = String.format("%s: %,.0f / %,.0f", currentSkill, currentXp, maxXp);
        g.drawString(font, xpText, x + 4, y + 4, TEXT_SKILL);

        // Progress bar
        int barX = x + 4;
        int barY = y + 18;
        int barW = panelW - 8;
        int barH = Math.min(8, panelH - 22);
        if (barH <= 0) return;

        g.fill(barX, barY, barX + barW, barY + barH, BAR_BG);

        double progress = maxXp > 0 ? Math.min(currentXp / maxXp, 1.0) : 0;
        int fillW = (int) (barW * progress);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, BAR_FILL);
        }

        String pctText = String.format("%.1f%%", progress * 100);
        int pctW = font.width(pctText);
        g.drawString(font, pctText, barX + (barW - pctW) / 2, barY, TEXT_WHITE);
    }
}

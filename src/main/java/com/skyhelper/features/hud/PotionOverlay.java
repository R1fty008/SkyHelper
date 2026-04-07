package com.skyhelper.features.hud;

import com.skyhelper.config.SkyHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;

/**
 * Displays active potion effects with remaining durations on the HUD.
 * Reads position and size from the hud_layout config.
 */
public class PotionOverlay {

    private static final int BG_COLOR = 0xAA000000;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_EFFECT = 0xFF88FF88;
    private static final int TEXT_TIME = 0xFFFFFF55;
    private static final int ROW_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 14;
    private static final int PAD = 4;

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) return;

        SkyHelperConfig.HudPanelLayout layout = SkyHelperConfig.getPanel("potion_effects");
        if (layout == null || !layout.visible) return;

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = (int) (layout.x * screenW);
        int y = (int) (layout.y * screenH);
        int panelW = layout.width;

        // Safeguard: clamp on screen
        int totalHeight = HEADER_HEIGHT + effects.size() * ROW_HEIGHT + PAD;
        x = Math.max(0, Math.min(x, screenW - panelW));
        y = Math.max(0, Math.min(y, screenH - totalHeight));

        // Background
        g.fill(x, y, x + panelW, y + totalHeight, BG_COLOR);

        // Header
        g.drawString(font, "Active Effects", x + PAD, y + 3, TEXT_WHITE);

        int rowY = y + HEADER_HEIGHT;
        for (MobEffectInstance effect : effects) {
            String name = effect.getEffect().value().getDescriptionId();
            String displayName = formatEffectName(name);
            int amplifier = effect.getAmplifier();
            if (amplifier > 0) {
                displayName += " " + toRoman(amplifier + 1);
            }

            String duration = formatDuration(effect.getDuration());

            g.drawString(font, displayName, x + PAD, rowY + 2, TEXT_EFFECT);

            int durW = font.width(duration);
            g.drawString(font, duration, x + panelW - durW - PAD, rowY + 2, TEXT_TIME);

            rowY += ROW_HEIGHT;
        }
    }

    private static String formatEffectName(String translationKey) {
        String name = translationKey;
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            name = name.substring(lastDot + 1);
        }
        name = name.replace('_', ' ');
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    private static String formatDuration(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }
}

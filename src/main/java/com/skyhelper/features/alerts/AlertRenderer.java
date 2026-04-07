package com.skyhelper.features.alerts;

import com.skyhelper.config.SkyHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders on-screen visual alerts for reboot warnings, transfers, and admin checks.
 * Called from HudRenderHandler each frame.
 */
public class AlertRenderer {

    // ── Reboot warning state ──
    private static boolean rebootWarningActive = false;
    private static long rebootWarningStart = 0;

    // ── Transfer toast state ──
    private static boolean transferToastActive = false;
    private static long transferToastStart = 0;
    private static final long TRANSFER_TOAST_DURATION = 10_000; // 10 seconds

    // ── Admin check alert state ──
    private static boolean adminCheckActive = false;
    private static long adminCheckStart = 0;
    private static final long ADMIN_CHECK_DURATION = 30_000; // 30 seconds

    // ── Colors ──
    private static final int RED_BANNER = 0xCC990000;
    private static final int RED_BANNER_FLASH = 0xCCFF2200;
    private static final int YELLOW_BANNER = 0xCC996600;
    private static final int YELLOW_BANNER_FLASH = 0xCCFFAA00;
    private static final int TOAST_BG = 0xDD222244;
    private static final int TOAST_BORDER = 0xFF5555FF;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_YELLOW = 0xFFFFFF55;
    private static final int TEXT_RED = 0xFFFF5555;
    private static final int SCREEN_FLASH = 0x30FF0000;
    private static final int DISMISS_BG = 0xCC333333;
    private static final int DISMISS_BG_HOVER = 0xCC555555;
    private static final int TEXT_LIGHT_GRAY = 0xFFCCCCCC;

    // ── Public triggers (called from AlertHandler) ──

    public static void showRebootWarning() {
        rebootWarningActive = true;
        rebootWarningStart = System.currentTimeMillis();
    }

    public static void clearRebootWarning() {
        rebootWarningActive = false;
    }

    public static void showTransferToast() {
        transferToastActive = true;
        transferToastStart = System.currentTimeMillis();
    }

    public static void showAdminCheckAlert() {
        adminCheckActive = true;
        adminCheckStart = System.currentTimeMillis();
    }

    public static void clearAdminCheckAlert() {
        adminCheckActive = false;
    }

    public static void clearTransferToast() {
        transferToastActive = false;
    }

    // ── Pest repellent toast state ──
    private static boolean pestToastActive = false;
    private static long pestToastStart = 0;
    private static String pestToastMessage = "";
    private static final long PEST_TOAST_DURATION = 8_000; // 8 seconds

    /** Returns true if any alert is currently showing. */
    public static boolean hasActiveAlert() {
        return rebootWarningActive || transferToastActive || adminCheckActive;
    }

    public static void showPestRepellentToast(String message) {
        pestToastActive = true;
        pestToastStart = System.currentTimeMillis();
        pestToastMessage = message;
    }

    // ── Main render (called every frame from HudRenderHandler) ──

    public static void render(GuiGraphics g) {
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Pest repellent toast auto-expires (not gated by alertsEnabled)
        if (pestToastActive && now - pestToastStart > PEST_TOAST_DURATION) {
            pestToastActive = false;
        }
        if (pestToastActive) {
            renderPestToast(g, font, screenW, now);
        }

        if (!SkyHelperConfig.get().alertsEnabled) return;

        // Render each active alert
        if (rebootWarningActive && SkyHelperConfig.get().rebootAlertEnabled) {
            renderRebootWarning(g, font, screenW, screenH, now);
        }
        if (transferToastActive && SkyHelperConfig.get().transferAlertEnabled) {
            renderTransferToast(g, font, screenW, screenH, now);
        }
        if (adminCheckActive && SkyHelperConfig.get().adminCheckAlertEnabled) {
            renderAdminCheckAlert(g, font, screenW, screenH, now);
        }
    }

    // ── Reboot warning: flashing red banner at top of screen ──

    private static void renderRebootWarning(GuiGraphics g, Font font, int screenW, int screenH, long now) {
        // Flash between two red tones every 500ms
        boolean flash = ((now - rebootWarningStart) / 500) % 2 == 0;
        int bannerColor = flash ? RED_BANNER_FLASH : RED_BANNER;
        int bannerH = 24;

        // Top banner
        g.fill(0, 0, screenW, bannerH, bannerColor);

        String text = "\u26A0 SERVER REBOOTING - WARP OUT NOW \u26A0";
        int textW = font.width(text);
        g.drawString(font, text, (screenW - textW) / 2, (bannerH - 8) / 2, TEXT_WHITE);

        // Dismiss hint on the right side of the banner
        String dismiss = "[DEL] Dismiss";
        int dismissW = font.width(dismiss);
        g.drawString(font, dismiss, screenW - dismissW - 6, (bannerH - 8) / 2, TEXT_LIGHT_GRAY);

        // Optional full-screen flash overlay
        if (SkyHelperConfig.get().rebootFlashScreen && flash) {
            g.fill(0, 0, screenW, screenH, SCREEN_FLASH);
        }
    }

    // ── Transfer toast: top-right notification with fade-out ──

    private static void renderTransferToast(GuiGraphics g, Font font, int screenW, int screenH, long now) {
        long elapsed = now - transferToastStart;

        // No fade — stays visible until dismissed
        float alpha = 1.0f;

        // Slide in from right in first 300ms
        float slide = Math.min(1.0f, elapsed / 300f);

        String line1 = "\u26A0 Server Transfer";
        String line2 = "You have been transferred to a new server!";
        int toastW = Math.max(font.width(line1), font.width(line2)) + 20;
        int toastH = 32;

        int baseX = screenW - toastW - 10;
        int offscreenX = screenW + 10;
        int toastX = (int) (offscreenX + (baseX - offscreenX) * slide);
        int toastY = 35; // below potential reboot banner

        int bgAlpha = (int) (0xDD * alpha) << 24;
        int borderAlpha = (int) (0xFF * alpha) << 24;

        // Background
        g.fill(toastX, toastY, toastX + toastW, toastY + toastH, (bgAlpha & 0xFF000000) | (TOAST_BG & 0x00FFFFFF));
        // Border
        g.fill(toastX, toastY, toastX + 2, toastY + toastH, (borderAlpha & 0xFF000000) | (TOAST_BORDER & 0x00FFFFFF));

        int textAlpha = (int) (255 * alpha);
        int yellowWithAlpha = (textAlpha << 24) | (TEXT_YELLOW & 0x00FFFFFF);
        int whiteWithAlpha = (textAlpha << 24) | (TEXT_WHITE & 0x00FFFFFF);

        g.drawString(font, line1, toastX + 10, toastY + 4, yellowWithAlpha);
        g.drawString(font, line2, toastX + 10, toastY + 18, whiteWithAlpha);

        // Dismiss hint below the toast
        String dismiss = "[DEL] Dismiss";
        int dismissAlpha = (textAlpha << 24) | (TEXT_LIGHT_GRAY & 0x00FFFFFF);
        g.drawString(font, dismiss, toastX + 10, toastY + toastH + 3, dismissAlpha);
    }

    // ── Admin check alert: flashing yellow message center-screen ──

    private static void renderAdminCheckAlert(GuiGraphics g, Font font, int screenW, int screenH, long now) {
        long elapsed = now - adminCheckStart;

        // Flash between two yellow tones every 400ms
        boolean flash = (elapsed / 400) % 2 == 0;
        int bannerColor = flash ? YELLOW_BANNER_FLASH : YELLOW_BANNER;

        String text = "\u26A0 POSSIBLE ADMIN CHECK - REACT NOW \u26A0";
        int textW = font.width(text);
        int bannerW = textW + 30;
        int bannerH = 22;
        int bannerX = (screenW - bannerW) / 2;
        int bannerY = screenH / 3; // upper-center area

        // Banner background
        g.fill(bannerX, bannerY, bannerX + bannerW, bannerY + bannerH, bannerColor);

        // Border
        g.fill(bannerX, bannerY, bannerX + bannerW, bannerY + 1, TEXT_YELLOW);
        g.fill(bannerX, bannerY + bannerH - 1, bannerX + bannerW, bannerY + bannerH, TEXT_YELLOW);
        g.fill(bannerX, bannerY, bannerX + 1, bannerY + bannerH, TEXT_YELLOW);
        g.fill(bannerX + bannerW - 1, bannerY, bannerX + bannerW, bannerY + bannerH, TEXT_YELLOW);

        g.drawString(font, text, (screenW - textW) / 2, bannerY + (bannerH - 8) / 2, TEXT_WHITE);

        // Full-screen red flash overlay (same as reboot)
        if (flash) {
            g.fill(0, 0, screenW, screenH, SCREEN_FLASH);
        }

        // Dismiss hint
        String dismiss = "Press [DELETE] to dismiss";
        int dismissW = font.width(dismiss);
        g.drawString(font, dismiss, (screenW - dismissW) / 2, bannerY + bannerH + 4, TEXT_YELLOW);
    }

    // ── Pest repellent toast: top-center notification ──

    private static void renderPestToast(GuiGraphics g, Font font, int screenW, long now) {
        long elapsed = now - pestToastStart;

        // Fade out in the last 2 seconds
        float alpha = 1.0f;
        if (elapsed > PEST_TOAST_DURATION - 2000) {
            alpha = (float) (PEST_TOAST_DURATION - elapsed) / 2000f;
            alpha = Math.max(0, Math.min(1, alpha));
        }

        int toastW = font.width(pestToastMessage) + 20;
        int toastH = 20;
        int toastX = (screenW - toastW) / 2;
        int toastY = 30;

        int bgAlpha = (int) (0xDD * alpha) << 24;
        int borderAlpha = (int) (0xFF * alpha) << 24;
        int textAlpha = (int) (255 * alpha);

        int greenBorder = 0x0055AA55;
        g.fill(toastX, toastY, toastX + toastW, toastY + toastH,
                (bgAlpha & 0xFF000000) | (TOAST_BG & 0x00FFFFFF));
        g.fill(toastX, toastY, toastX + toastW, toastY + 1,
                (borderAlpha & 0xFF000000) | greenBorder);
        g.fill(toastX, toastY + toastH - 1, toastX + toastW, toastY + toastH,
                (borderAlpha & 0xFF000000) | greenBorder);

        int textColor = (textAlpha << 24) | (TEXT_WHITE & 0x00FFFFFF);
        g.drawCenteredString(font, pestToastMessage, screenW / 2, toastY + 6, textColor);
    }
}

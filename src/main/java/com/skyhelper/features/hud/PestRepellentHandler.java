package com.skyhelper.features.hud;

import com.skyhelper.config.SkyHelperConfig;
import com.skyhelper.features.alerts.AlertRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Detects Pest Repellent activation via chat messages and manages the 60-minute countdown.
 * Persists the start time to config so it survives relogs.
 */
public class PestRepellentHandler {

    private static final long DURATION_MS = 60 * 60 * 1000; // 60 minutes
    private static final long FIVE_MIN_MS = 5 * 60 * 1000;

    private static boolean isActive = false;
    private static boolean isMax = false;
    private static long startTime = 0;

    // Warning flags to avoid repeating
    private static boolean warned5Min = false;
    private static boolean warnedExpired = false;

    public static void init() {
        // Restore persisted timer on startup
        SkyHelperConfig cfg = SkyHelperConfig.get();
        if (cfg.pestRepellentStartTime > 0) {
            long elapsed = System.currentTimeMillis() - cfg.pestRepellentStartTime;
            if (elapsed < DURATION_MS) {
                startTime = cfg.pestRepellentStartTime;
                isActive = true;
                isMax = false; // can't know which type after relog, default to regular
                warned5Min = elapsed > (DURATION_MS - FIVE_MIN_MS);
                warnedExpired = false;
            } else {
                // Expired while offline — clear it
                cfg.pestRepellentStartTime = 0;
                SkyHelperConfig.save();
            }
        }

        // Chat message listener for activation
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!SkyHelperConfig.get().pestRepellentTimerEnabled) return true;

            String text = message.getString();
            if (text.contains("Pest Repellent") && text.contains("active")) {
                boolean max = text.contains("MAX") || text.contains("Repellent II");
                activate(max);
            }
            return true;
        });

        // Tick listener for warnings
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isActive) return;
            if (!SkyHelperConfig.get().pestRepellentTimerEnabled) return;

            long remaining = getRemainingMs();

            // 5 minute warning
            if (!warned5Min && remaining <= FIVE_MIN_MS && remaining > 0) {
                warned5Min = true;
                if (SkyHelperConfig.get().pestRepellentSoundAt5Min) {
                    playAlertSound();
                }
                if (SkyHelperConfig.get().pestRepellentShowToast) {
                    AlertRenderer.showPestRepellentToast(
                            "\u26A0 Pest Repellent expiring in 5 minutes!");
                }
            }

            // Expired
            if (!warnedExpired && remaining <= 0) {
                warnedExpired = true;
                isActive = false;
                SkyHelperConfig.get().pestRepellentStartTime = 0;
                SkyHelperConfig.save();

                if (SkyHelperConfig.get().pestRepellentSoundOnExpire) {
                    playAlertSound();
                }
                if (SkyHelperConfig.get().pestRepellentShowToast) {
                    AlertRenderer.showPestRepellentToast(
                            "Pest Repellent has expired! Reapply now.");
                }
            }
        });
    }

    private static void activate(boolean max) {
        isActive = true;
        isMax = max;
        startTime = System.currentTimeMillis();
        warned5Min = false;
        warnedExpired = false;

        // Persist
        SkyHelperConfig.get().pestRepellentStartTime = startTime;
        SkyHelperConfig.save();
    }

    private static void playAlertSound() {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.0f, 1.0f));
    }

    /** Test method: starts a 60-minute timer as if the player just used Pest Repellent. */
    public static void testActivate() {
        activate(false);
    }

    /** Test method: starts a 60-minute timer as MAX variant. */
    public static void testActivateMax() {
        activate(true);
    }

    /** Clears the active timer (for testing). */
    public static void testClear() {
        isActive = false;
        isMax = false;
        startTime = 0;
        warned5Min = false;
        warnedExpired = false;
        SkyHelperConfig.get().pestRepellentStartTime = 0;
        SkyHelperConfig.save();
    }

    // ── Public accessors for the overlay ──

    public static boolean isActive() {
        if (!isActive) return false;
        if (getRemainingMs() <= 0) {
            isActive = false;
            return false;
        }
        return true;
    }

    public static boolean isMax() { return isMax; }

    public static long getRemainingMs() {
        if (startTime == 0) return 0;
        return Math.max(0, DURATION_MS - (System.currentTimeMillis() - startTime));
    }

    public static float getProgress() {
        return (float) getRemainingMs() / DURATION_MS;
    }
}

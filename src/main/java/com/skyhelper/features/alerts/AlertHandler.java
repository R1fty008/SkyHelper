package com.skyhelper.features.alerts;

import com.skyhelper.config.SkyHelperConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Detects server reboot warnings, unexpected transfers, and potential admin checks.
 * Plays distinct sounds and delegates visual alerts to AlertRenderer.
 */
public class AlertHandler {

    // ── Position / rotation tracking for admin check ──
    private static double lastX, lastY, lastZ;
    private static float lastYaw, lastPitch;
    private static boolean hasPreviousPos = false;

    // ── Alarm loop states ──
    private static boolean rebootAlarmActive = false;
    private static long lastRebootSoundTime = 0;
    private static boolean transferAlarmActive = false;
    private static long lastTransferSoundTime = 0;
    private static boolean adminCheckAlarmActive = false;
    private static long lastAdminCheckSoundTime = 0;

    // ── Admin check cooldown ──
    private static long lastAdminAlertTime = 0;
    private static final long ADMIN_ALERT_COOLDOWN = 10_000; // 10 seconds

    // ── Suppress admin check right after a known warp ──
    private static long lastKnownWarpTime = 0;
    private static final long WARP_SUPPRESS_WINDOW = 4_000; // 4 seconds (increased from 2s)

    // ── Suppress admin check after right-click (AOTE/AOTV, ender pearls) ──
    private static long lastUseKeyTime = 0;
    private static final long USE_KEY_SUPPRESS_WINDOW = 2_000; // 2 seconds

    // ── Suppress transfer alert after intentional warp command ──
    private static long lastIntentionalWarpTime = 0;
    private static final long INTENTIONAL_WARP_WINDOW = 5_000; // 5 seconds

    // ── Dismiss key tracking ──
    private static boolean dismissKeyWasDown = false;

    // ── Farming detection ──
    private static long lastFarmingActivityTime = 0;
    private static final long FARMING_TIMEOUT = 15_000; // 15 seconds grace period

    public static void init() {
        // ── Chat message listener ──
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!SkyHelperConfig.get().alertsEnabled) return true;

            String text = message.getString();
            checkRebootMessages(text);
            checkTransferMessages(text);
            return true; // never cancel chat messages from this handler
        });

        // ── Outgoing command listener (detect intentional warps) ──
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            String cmd = command.toLowerCase().split(" ")[0];
            if (isWarpCommand(cmd)) {
                lastIntentionalWarpTime = System.currentTimeMillis();
                lastKnownWarpTime = System.currentTimeMillis();
                hasPreviousPos = false;
            }
            return true; // never block commands
        });

        // ── Tick listener for admin check detection + reboot alarm repeat + dismiss key ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Dismiss key (DELETE) — works regardless of alertsEnabled
            long windowHandle = client.getWindow().handle();
            boolean dismissDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_DELETE) == GLFW.GLFW_PRESS;
            if (dismissDown && !dismissKeyWasDown) {
                dismissAllAlerts();
            }
            dismissKeyWasDown = dismissDown;

            if (!SkyHelperConfig.get().alertsEnabled) return;
            LocalPlayer player = client.player;
            if (player == null) {
                hasPreviousPos = false;
                return;
            }

            long now = System.currentTimeMillis();

            // Track right-click (use key) for AOTE/AOTV/pearl suppression
            if (client.options.keyUse.isDown()) {
                lastUseKeyTime = now;
            }

            // Farming detection: holding hoe + attack key down + looking at a block
            boolean holdingHoe = player.getMainHandItem().getItem() instanceof HoeItem;
            boolean attackHeld = client.options.keyAttack.isDown();
            boolean lookingAtBlock = client.hitResult instanceof BlockHitResult;
            if (holdingHoe && attackHeld && lookingAtBlock) {
                lastFarmingActivityTime = now;
            }

            // Repeating reboot alarm (every 5 seconds)
            if (rebootAlarmActive && SkyHelperConfig.get().rebootAlertEnabled) {
                if (now - lastRebootSoundTime >= 5000) {
                    playRebootSound();
                    lastRebootSoundTime = now;
                }
            }

            // Repeating transfer alarm (every 0.5 seconds)
            if (transferAlarmActive && SkyHelperConfig.get().transferAlertEnabled) {
                if (now - lastTransferSoundTime >= 500) {
                    playTransferSound();
                    lastTransferSoundTime = now;
                }
            }

            // Repeating admin check alarm (every 1 second)
            if (adminCheckAlarmActive && SkyHelperConfig.get().adminCheckAlertEnabled) {
                if (now - lastAdminCheckSoundTime >= 1000) {
                    playAdminCheckSound();
                    lastAdminCheckSoundTime = now;
                }
            }

            // Admin check detection
            if (SkyHelperConfig.get().adminCheckAlertEnabled) {
                tickAdminCheck(player);
            }
        });
    }

    /** Dismiss all active alerts (called from DELETE key). */
    public static void dismissAllAlerts() {
        rebootAlarmActive = false;
        transferAlarmActive = false;
        adminCheckAlarmActive = false;
        AlertRenderer.clearRebootWarning();
        AlertRenderer.clearTransferToast();
        AlertRenderer.clearAdminCheckAlert();
    }

    // ── Reboot detection ───────────────────────────────────────

    private static void checkRebootMessages(String text) {
        if (!SkyHelperConfig.get().rebootAlertEnabled) return;
        if (!isOnHypixel()) return;

        boolean isReboot = text.contains("[Important] This server will restart soon")
                || text.equals("You have 60 seconds to warp out!")
                || text.equals("You have 30 seconds to warp out!")
                || text.equals("You have 15 seconds to warp out!");

        if (isReboot) {
            rebootAlarmActive = true;
            playRebootSound();
            lastRebootSoundTime = System.currentTimeMillis();
            AlertRenderer.showRebootWarning();
        }
    }

    // ── Transfer detection ─────────────────────────────────────

    private static void checkTransferMessages(String text) {
        if (!SkyHelperConfig.get().transferAlertEnabled) return;
        if (!isOnHypixel()) return;

        boolean isTransfer = text.contains("Sending you to the lobby")
                || text.contains("Warping you to the Skyblock hub")
                || text.contains("You are being transferred")
                || text.contains("Sending to server")
                || text.contains("You were kicked while joining");

        if (isTransfer) {
            // Stop reboot alarm if active (player has been moved)
            rebootAlarmActive = false;
            AlertRenderer.clearRebootWarning();

            // Mark as known warp to suppress false admin-check positives
            lastKnownWarpTime = System.currentTimeMillis();
            hasPreviousPos = false;

            // Only alert if this wasn't an intentional warp by the player
            long now = System.currentTimeMillis();
            if (now - lastIntentionalWarpTime < INTENTIONAL_WARP_WINDOW) {
                return; // player initiated this warp, skip alert
            }

            // Skip if farming-only mode is on and player isn't farming
            if (SkyHelperConfig.get().alertsOnlyWhileFarming && !isFarming()) {
                return;
            }

            transferAlarmActive = true;
            playTransferSound();
            lastTransferSoundTime = now;
            AlertRenderer.showTransferToast();
        }
    }

    // ── Admin check detection (tick-based) ─────────────────────

    private static void tickAdminCheck(LocalPlayer player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        float yaw = player.getYRot(1.0f);
        float pitch = player.getXRot(1.0f);

        if (!hasPreviousPos) {
            lastX = px; lastY = py; lastZ = pz;
            lastYaw = yaw; lastPitch = pitch;
            hasPreviousPos = true;
            return;
        }

        long now = System.currentTimeMillis();

        // Only run on Hypixel
        if (!isOnHypixel()) {
            lastX = px; lastY = py; lastZ = pz;
            lastYaw = yaw; lastPitch = pitch;
            return;
        }

        // Skip if within the known-warp suppression window
        if (now - lastKnownWarpTime < WARP_SUPPRESS_WINDOW) {
            lastX = px; lastY = py; lastZ = pz;
            lastYaw = yaw; lastPitch = pitch;
            return;
        }

        // Skip if use key (right-click) was recently pressed — covers AOTE/AOTV, ender pearls
        if (now - lastUseKeyTime < USE_KEY_SUPPRESS_WINDOW) {
            lastX = px; lastY = py; lastZ = pz;
            lastYaw = yaw; lastPitch = pitch;
            return;
        }

        // Check sudden teleport (>10 blocks in 1 tick, no movement keys held)
        double dx = px - lastX;
        double dy = py - lastY;
        double dz = pz - lastZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        boolean playerMoving = isPlayerMoving(player);

        boolean teleportDetected = dist > 10.0 && !playerMoving;

        // Check sudden camera rotation (>90 degrees in 1 tick — high threshold to avoid mouse flick false positives)
        float dYaw = Math.abs(angleDiff(yaw, lastYaw));
        float dPitch = Math.abs(pitch - lastPitch);
        boolean rotationDetected = (dYaw > 90.0f || dPitch > 90.0f);

        // Skip if farming-only mode is on and player isn't farming
        if (SkyHelperConfig.get().alertsOnlyWhileFarming && !isFarming()) {
            lastX = px; lastY = py; lastZ = pz;
            lastYaw = yaw; lastPitch = pitch;
            return;
        }

        if ((teleportDetected || rotationDetected) && now - lastAdminAlertTime > ADMIN_ALERT_COOLDOWN) {
            lastAdminAlertTime = now;
            adminCheckAlarmActive = true;
            playAdminCheckSound();
            lastAdminCheckSoundTime = now;
            AlertRenderer.showAdminCheckAlert();

            if (SkyHelperConfig.get().adminCheckChatAlert) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00A7e[SkyHelper] \u00A7cPossible admin macro check detected! React naturally."
                            ), false
                    );
                }
            }
        }

        lastX = px; lastY = py; lastZ = pz;
        lastYaw = yaw; lastPitch = pitch;
    }

    private static boolean isPlayerMoving(LocalPlayer player) {
        var input = player.input;
        if (input == null) return false;
        var keys = input.keyPresses;
        if (keys == null) return false;
        return keys.forward() || keys.backward() || keys.left() || keys.right() || keys.jump();
    }

    /** Returns true if the player has been farming recently (hoe + breaking blocks). */
    public static boolean isFarming() {
        return System.currentTimeMillis() - lastFarmingActivityTime < FARMING_TIMEOUT;
    }

    private static float angleDiff(float a, float b) {
        float diff = a - b;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return diff;
    }

    /** Returns true if the command is a known Hypixel/Skyblock warp command. */
    private static boolean isWarpCommand(String cmd) {
        return switch (cmd) {
            case "hub", "lobby", "l", "spawn", "warp", "is", "island", "home",
                 "visit", "play", "server",
                 // Skyblock-specific
                 "et", "etherwarps", "aote", "aotv",
                 "sb", "skyblock",
                 "ah", "auctionhouse",
                 "bz", "bazaar",
                 "bank",
                 "museum",
                 "garden",
                 "desk", "sbs",
                 "plottp", "tptoplot", "plots",
                 "pets",
                 "wardrobe",
                 "craftedminions", "minions",
                 "recipe", "recipes",
                 "trades",
                 "storage",
                 "rift",
                 "dungeons", "dungeon",
                 "kuudra",
                 "crimson",
                 "dwarven", "forge",
                 "jerry",
                 "tp", "tps",
                 "party", "p" -> true;
            default -> false;
        };
    }

    /** Returns true if the player is currently on Hypixel. */
    private static boolean isOnHypixel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() == null) return false;
        String ip = mc.getCurrentServer().ip.toLowerCase();
        return ip.contains("hypixel.net") || ip.contains("hypixel.io");
    }

    // ── Sound playback ─────────────────────────────────────────

    private static float getVolume() {
        return switch (SkyHelperConfig.get().alertVolume) {
            case 0 -> 0.4f;
            case 1 -> 0.7f;
            default -> 1.0f;
        };
    }

    private static void playRebootSound() {
        Minecraft mc = Minecraft.getInstance();
        float vol = getVolume();
        // Note block bass — deep alarm tone
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8f, vol));
    }

    private static void playTransferSound() {
        Minecraft mc = Minecraft.getInstance();
        float vol = getVolume();
        // Note block flute
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.0f, vol));
    }

    private static void playAdminCheckSound() {
        Minecraft mc = Minecraft.getInstance();
        float vol = getVolume();
        // Note block ender dragon imitation
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_IMITATE_ENDER_DRAGON.value(), 1.0f, vol));
    }

    // ── Test methods (called from config GUI) ──────────────────

    public static void testRebootAlert() {
        rebootAlarmActive = true;
        playRebootSound();
        lastRebootSoundTime = System.currentTimeMillis();
        AlertRenderer.showRebootWarning();
    }

    /** Stop only the reboot test alarm (STOP button in config GUI). */
    public static void stopRebootTest() {
        rebootAlarmActive = false;
        AlertRenderer.clearRebootWarning();
    }

    public static void testTransferAlert() {
        transferAlarmActive = true;
        playTransferSound();
        lastTransferSoundTime = System.currentTimeMillis();
        AlertRenderer.showTransferToast();
    }

    public static void testAdminCheckAlert() {
        adminCheckAlarmActive = true;
        playAdminCheckSound();
        lastAdminCheckSoundTime = System.currentTimeMillis();
        AlertRenderer.showAdminCheckAlert();
    }

    /** Called when player is transferred — stops reboot alarm. */
    public static void stopRebootAlarm() {
        rebootAlarmActive = false;
    }
}

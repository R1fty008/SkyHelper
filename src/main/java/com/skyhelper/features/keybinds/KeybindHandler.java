package com.skyhelper.features.keybinds;

import com.skyhelper.config.SkyHelperConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * Listens for bound key presses each tick and sends the mapped command.
 * Only fires when: feature enabled, on Hypixel, no screen open, key just pressed.
 */
public class KeybindHandler {

    // Track which keys are currently held to detect fresh presses
    private static final Set<Integer> heldKeys = new HashSet<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!SkyHelperConfig.get().keybindsEnabled) return;
            if (client.player == null) return;
            if (client.screen != null) return; // no GUI open
            if (!isOnHypixel()) return;

            long window = client.getWindow().handle();

            for (SkyHelperConfig.KeybindEntry entry : SkyHelperConfig.get().keybinds) {
                if (!entry.enabled) continue;
                if (entry.key == null || entry.command == null) continue;

                int glfwKey = keyNameToGlfw(entry.key);
                if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) continue;

                boolean pressed = GLFW.glfwGetKey(window, glfwKey) == GLFW.GLFW_PRESS;

                if (pressed && !heldKeys.contains(glfwKey)) {
                    // Fresh press — send the command
                    heldKeys.add(glfwKey);
                    String cmd = entry.command.trim();
                    if (!cmd.startsWith("/")) cmd = "/" + cmd;
                    client.player.connection.sendCommand(cmd.substring(1)); // sendCommand strips the /
                } else if (!pressed) {
                    heldKeys.remove(glfwKey);
                }
            }
        });
    }

    private static boolean isOnHypixel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() == null) return false;
        ServerData server = mc.getCurrentServer();
        String ip = server.ip.toLowerCase();
        return ip.contains("hypixel.net");
    }

    // ── Key name ↔ GLFW mapping ──────────────────────────────────────

    /**
     * Convert a human-readable key name (e.g. "B", "F5", "HOME") to a GLFW key constant.
     */
    public static int keyNameToGlfw(String name) {
        if (name == null || name.isEmpty()) return GLFW.GLFW_KEY_UNKNOWN;
        String upper = name.toUpperCase().trim();

        // Single letter A-Z
        if (upper.length() == 1 && upper.charAt(0) >= 'A' && upper.charAt(0) <= 'Z') {
            return GLFW.GLFW_KEY_A + (upper.charAt(0) - 'A');
        }
        // Single digit 0-9
        if (upper.length() == 1 && upper.charAt(0) >= '0' && upper.charAt(0) <= '9') {
            return GLFW.GLFW_KEY_0 + (upper.charAt(0) - '0');
        }
        // Function keys F1-F12
        if (upper.startsWith("F") && upper.length() >= 2 && upper.length() <= 3) {
            try {
                int num = Integer.parseInt(upper.substring(1));
                if (num >= 1 && num <= 12) return GLFW.GLFW_KEY_F1 + (num - 1);
            } catch (NumberFormatException ignored) {}
        }
        // Numpad
        if (upper.startsWith("NUMPAD") || upper.startsWith("KP_")) {
            String numPart = upper.replace("NUMPAD", "").replace("KP_", "").trim();
            try {
                int num = Integer.parseInt(numPart);
                if (num >= 0 && num <= 9) return GLFW.GLFW_KEY_KP_0 + num;
            } catch (NumberFormatException ignored) {}
        }

        return switch (upper) {
            case "SPACE"        -> GLFW.GLFW_KEY_SPACE;
            case "TAB"          -> GLFW.GLFW_KEY_TAB;
            case "CAPS LOCK", "CAPSLOCK", "CAPS" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
            case "BACKSPACE"    -> GLFW.GLFW_KEY_BACKSPACE;
            case "ESCAPE", "ESC" -> GLFW.GLFW_KEY_ESCAPE;
            case "INSERT"       -> GLFW.GLFW_KEY_INSERT;
            case "DELETE", "DEL" -> GLFW.GLFW_KEY_DELETE;
            case "HOME"         -> GLFW.GLFW_KEY_HOME;
            case "END"          -> GLFW.GLFW_KEY_END;
            case "PAGE UP", "PAGEUP", "PG UP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE DOWN", "PAGEDOWN", "PG DN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "UP"           -> GLFW.GLFW_KEY_UP;
            case "DOWN"         -> GLFW.GLFW_KEY_DOWN;
            case "LEFT"         -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT"        -> GLFW.GLFW_KEY_RIGHT;
            case "MINUS", "-"   -> GLFW.GLFW_KEY_MINUS;
            case "EQUALS", "="  -> GLFW.GLFW_KEY_EQUAL;
            case "BACKSLASH", "\\" -> GLFW.GLFW_KEY_BACKSLASH;
            case "SEMICOLON", ";" -> GLFW.GLFW_KEY_SEMICOLON;
            case "APOSTROPHE", "'" -> GLFW.GLFW_KEY_APOSTROPHE;
            case "COMMA", ","   -> GLFW.GLFW_KEY_COMMA;
            case "PERIOD", "."  -> GLFW.GLFW_KEY_PERIOD;
            case "SLASH", "/"   -> GLFW.GLFW_KEY_SLASH;
            case "GRAVE", "GRAVE ACCENT", "`" -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            case "LEFT BRACKET", "[" -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case "RIGHT BRACKET", "]" -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case "NUM LOCK"     -> GLFW.GLFW_KEY_NUM_LOCK;
            case "SCROLL LOCK"  -> GLFW.GLFW_KEY_SCROLL_LOCK;
            case "PAUSE"        -> GLFW.GLFW_KEY_PAUSE;
            case "PRINT SCREEN" -> GLFW.GLFW_KEY_PRINT_SCREEN;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }

    /**
     * Convert a GLFW key constant to a human-readable name.
     */
    public static String glfwToKeyName(int key) {
        // A-Z
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        }
        // 0-9
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        }
        // F1-F12
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) {
            return "F" + (1 + (key - GLFW.GLFW_KEY_F1));
        }
        // Numpad 0-9
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            return "Numpad" + (key - GLFW.GLFW_KEY_KP_0);
        }

        return switch (key) {
            case GLFW.GLFW_KEY_SPACE        -> "Space";
            case GLFW.GLFW_KEY_TAB          -> "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK    -> "Caps Lock";
            case GLFW.GLFW_KEY_ENTER        -> "Enter";
            case GLFW.GLFW_KEY_BACKSPACE    -> "Backspace";
            case GLFW.GLFW_KEY_ESCAPE       -> "Escape";
            case GLFW.GLFW_KEY_INSERT       -> "Insert";
            case GLFW.GLFW_KEY_DELETE       -> "Delete";
            case GLFW.GLFW_KEY_HOME         -> "Home";
            case GLFW.GLFW_KEY_END          -> "End";
            case GLFW.GLFW_KEY_PAGE_UP      -> "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN    -> "Page Down";
            case GLFW.GLFW_KEY_UP           -> "Up";
            case GLFW.GLFW_KEY_DOWN         -> "Down";
            case GLFW.GLFW_KEY_LEFT         -> "Left";
            case GLFW.GLFW_KEY_RIGHT        -> "Right";
            case GLFW.GLFW_KEY_MINUS        -> "Minus";
            case GLFW.GLFW_KEY_EQUAL        -> "Equals";
            case GLFW.GLFW_KEY_BACKSLASH    -> "Backslash";
            case GLFW.GLFW_KEY_SEMICOLON    -> "Semicolon";
            case GLFW.GLFW_KEY_APOSTROPHE   -> "Apostrophe";
            case GLFW.GLFW_KEY_COMMA        -> "Comma";
            case GLFW.GLFW_KEY_PERIOD       -> "Period";
            case GLFW.GLFW_KEY_SLASH        -> "Slash";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "Grave";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "]";
            case GLFW.GLFW_KEY_NUM_LOCK     -> "Num Lock";
            case GLFW.GLFW_KEY_SCROLL_LOCK  -> "Scroll Lock";
            case GLFW.GLFW_KEY_PAUSE        -> "Pause";
            case GLFW.GLFW_KEY_PRINT_SCREEN -> "Print Screen";
            default -> "Unknown";
        };
    }

    /**
     * Returns true if the given GLFW key is a modifier (Shift, Ctrl, Alt, Super).
     */
    public static boolean isModifierKey(int key) {
        return key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT
                || key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL
                || key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT
                || key == GLFW.GLFW_KEY_LEFT_SUPER || key == GLFW.GLFW_KEY_RIGHT_SUPER;
    }

    /**
     * Check if a GLFW key conflicts with a vanilla Minecraft keybind.
     */
    public static boolean conflictsWithVanilla(int key) {
        // Common vanilla defaults
        return key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_A
                || key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_D
                || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_E
                || key == GLFW.GLFW_KEY_Q || key == GLFW.GLFW_KEY_F
                || key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_ESCAPE
                || key == GLFW.GLFW_KEY_T || key == GLFW.GLFW_KEY_SLASH
                || key == GLFW.GLFW_KEY_F1 || key == GLFW.GLFW_KEY_F2
                || key == GLFW.GLFW_KEY_F3 || key == GLFW.GLFW_KEY_F5
                || key == GLFW.GLFW_KEY_F11;
    }
}

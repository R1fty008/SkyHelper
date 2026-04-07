package com.skyhelper.features.calculator;

import com.skyhelper.config.SkyHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CalculatorOverlay {

    // Layout constants
    private static final int WIDTH = 220;
    private static final int INPUT_HEIGHT = 18;
    private static final int LINE_HEIGHT = 12;
    private static final int PAD = 4;
    private static final int BOTTOM_MARGIN = 8;
    private static final int CLEAR_W = 30;
    private static final int CLEAR_H = 12;
    private static final int MAX_HISTORY = 5;

    // Colors
    private static final int BG = 0xCC000000;
    private static final int INPUT_BG = 0xFF333333;
    private static final int INPUT_FOCUSED_BG = 0xFF444444;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int RESULT_COLOR = 0xFF55FF55;
    private static final int SEARCH_COLOR = 0xFF55CCFF;
    private static final int HINT_COLOR = 0xFF888888;
    private static final int HISTORY_COLOR = 0xFFAAAAAA;
    private static final int CLEAR_BG = 0xFF663333;
    private static final int CLEAR_HOVER_BG = 0xFF884444;
    private static final int ITEM_DIM_OVERLAY = 0xAA000000;

    // State
    private String expression = "";
    private boolean focused = false;
    private boolean active = false;
    private final List<String> history = new ArrayList<>();

    // Persistent search state across storage screens
    private String persistentSearchQuery = "";
    private boolean wasInStorageScreen = false;

    // Debounce
    private long lastEditTime = 0;
    private boolean dirty = false;
    private String lastAddedEntry = "";

    public void reset() {
        expression = "";
        focused = false;
        lastEditTime = 0;
        dirty = false;
        lastAddedEntry = "";
    }

    public void toggleFromKeybind() {
        if (!SkyHelperConfig.get().calculatorEnabled) return;
        active = !active;
        if (active) {
            focused = true;
            // Restore persistent search if we're in a storage screen
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
                if (isStorageScreen(containerScreen)) {
                    expression = persistentSearchQuery;
                }
            }
        } else {
            focused = false;
        }
    }

    public void onScreenOpen(AbstractContainerScreen<?> screen) {
        if (!SkyHelperConfig.get().calculatorEnabled) return;
        // If returning to a storage screen with a persistent search, restore it
        if (isStorageScreen(screen) && !persistentSearchQuery.isEmpty()) {
            active = true;
            focused = false;
            expression = persistentSearchQuery;
            wasInStorageScreen = true;
        } else if (isStorageScreen(screen)) {
            wasInStorageScreen = true;
        }
    }

    public void onScreenClose() {
        // When closing a storage screen, preserve search query
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            if (isStorageScreen(containerScreen) && !expression.isEmpty() && !ExpressionParser.isMathExpression(expression)) {
                persistentSearchQuery = expression;
                wasInStorageScreen = true;
                return;
            }
        }
        // If we were in a storage screen but now are back in the game world, clear
        if (wasInStorageScreen && mc.screen == null) {
            persistentSearchQuery = "";
            wasInStorageScreen = false;
            active = false;
        }
    }

    public static boolean isStorageScreen(AbstractContainerScreen<?> screen) {
        String title = screen.getTitle().getString();
        // Ender Chest pages: "Ender Chest (1/9)" through "Ender Chest (9/9)"
        if (title.matches("Ender Chest \\(\\d/9\\)")) return true;
        // Main storage menu
        if (title.equals("Storage")) return true;
        // Backpack screens typically have backpack names or "Backpack" in the title
        if (title.contains("Backpack")) return true;
        return false;
    }

    // ── Input handling ────────────────────────────────────────────────

    public boolean onKeyPress(int key, int scancode, int modifiers) {
        // Toggle overlay with ] key, even when not active/focused
        if (key == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            toggleFromKeybind();
            return false;
        }

        if (!active || !focused) return true;

        if (key == GLFW.GLFW_KEY_BACKSPACE && !expression.isEmpty()) {
            expression = expression.substring(0, expression.length() - 1);
            updatePersistentSearch();
            markDirty();
            return false;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            focused = false;
            return true;
        }

        if (key == GLFW.GLFW_KEY_SPACE) {
            expression += ' ';
            updatePersistentSearch();
            return false;
        }

        // Letters A-Z for search mode
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            char letter = (char) ('a' + (key - GLFW.GLFW_KEY_A));
            if (shift) letter = Character.toUpperCase(letter);
            expression += letter;
            updatePersistentSearch();
            return false;
        }

        char c = keyToChar(key, modifiers);
        if (c != 0) {
            expression += c;
            updatePersistentSearch();
            markDirty();
        }

        return false;
    }

    private void updatePersistentSearch() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            if (isStorageScreen(containerScreen) && !ExpressionParser.isMathExpression(expression)) {
                persistentSearchQuery = expression;
            }
        }
    }

    public boolean onMouseClick(double mx, double my, int screenW, int screenH) {
        if (!active) return true;

        Layout layout = layout(screenW, screenH);
        int x0 = (screenW - WIDTH) / 2;

        // Clear button
        if (!history.isEmpty()) {
            int cbx = x0 + WIDTH - CLEAR_W - PAD;
            int cby = layout.topY + PAD;
            if (mx >= cbx && mx <= cbx + CLEAR_W && my >= cby && my <= cby + CLEAR_H) {
                history.clear();
                return false;
            }
        }

        // Input bar
        if (mx >= layout.inputX && mx <= layout.inputX + layout.inputW
                && my >= layout.inputY && my <= layout.inputY + INPUT_HEIGHT) {
            focused = true;
            return false;
        }

        // Inside overlay
        if (mx >= x0 && mx <= x0 + WIDTH
                && my >= layout.topY && my <= layout.topY + layout.totalH) {
            return false;
        }

        focused = false;
        return true;
    }

    // ── Rendering ─────────────────────────────────────────────────────

    public void render(GuiGraphics g, int screenW, int screenH, int mouseX, int mouseY) {
        if (!active || !SkyHelperConfig.get().calculatorEnabled) return;

        Font font = Minecraft.getInstance().font;
        boolean isMath = ExpressionParser.isMathExpression(expression);

        // Evaluate if math mode
        Double result = isMath ? ExpressionParser.evaluate(expression) : null;
        String resultStr = (result != null) ? formatNumber(result) : null;

        // Debounce for math history
        if (isMath) {
            processDebounce(resultStr);
        }

        // Apply search highlighting if in search mode and in a storage screen
        if (!isMath && !expression.isEmpty()) {
            applySearchHighlighting(g);
        }

        Layout layout = layout(screenW, screenH);
        int x0 = (screenW - WIDTH) / 2;

        // Background
        g.fill(x0, layout.topY, x0 + WIDTH, layout.topY + layout.totalH, BG);

        int cy = layout.topY + PAD;

        // History section (math mode only)
        if (!history.isEmpty() && isMath) {
            g.drawString(font, "History", x0 + PAD, cy + 2, HISTORY_COLOR);

            int cbx = x0 + WIDTH - CLEAR_W - PAD;
            boolean hover = mouseX >= cbx && mouseX <= cbx + CLEAR_W
                    && mouseY >= cy && mouseY <= cy + CLEAR_H;
            g.fill(cbx, cy, cbx + CLEAR_W, cy + CLEAR_H, hover ? CLEAR_HOVER_BG : CLEAR_BG);
            int clearTextX = cbx + (CLEAR_W - font.width("Clear")) / 2;
            g.drawString(font, "Clear", clearTextX, cy + 2, TEXT_COLOR);

            cy += LINE_HEIGHT + PAD;
            for (String entry : history) {
                g.drawString(font, entry, x0 + PAD, cy + 1, HISTORY_COLOR);
                cy += LINE_HEIGHT;
            }
        }

        // Input bar
        g.fill(layout.inputX, layout.inputY,
                layout.inputX + layout.inputW, layout.inputY + INPUT_HEIGHT,
                focused ? INPUT_FOCUSED_BG : INPUT_BG);

        int tx = layout.inputX + 4;
        int ty = layout.inputY + (INPUT_HEIGHT - 8) / 2;

        // Mode indicator
        String modeLabel;
        int modeColor;
        if (expression.isEmpty()) {
            modeLabel = "";
            modeColor = HINT_COLOR;
        } else if (isMath) {
            modeLabel = "[CALC] ";
            modeColor = RESULT_COLOR;
        } else {
            modeLabel = "[SEARCH] ";
            modeColor = SEARCH_COLOR;
        }

        if (!modeLabel.isEmpty()) {
            g.drawString(font, modeLabel, tx, ty, modeColor);
            tx += font.width(modeLabel);
        }

        if (expression.isEmpty() && !focused) {
            g.drawString(font, "Type to calc or search...", tx, ty, HINT_COLOR);
        } else if (resultStr != null && !expression.isEmpty()) {
            String exprPart = expression + " = ";
            int ew = font.width(exprPart);
            g.drawString(font, exprPart, tx, ty, TEXT_COLOR);
            g.drawString(font, resultStr, tx + ew, ty, RESULT_COLOR);
        } else {
            g.drawString(font, expression, tx, ty, TEXT_COLOR);
        }

        // Blinking cursor
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int curX = tx + font.width(expression);
            g.fill(curX, layout.inputY + 3, curX + 1, layout.inputY + INPUT_HEIGHT - 3, TEXT_COLOR);
        }
    }

    private void applySearchHighlighting(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        String query = expression.toLowerCase();
        var handler = containerScreen.getMenu();

        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            String itemName = stack.getHoverName().getString().toLowerCase();
            if (!itemName.contains(query)) {
                // Dim non-matching items
                int slotX = containerScreen.leftPos + slot.x;
                int slotY = containerScreen.topPos + slot.y;
                g.fill(slotX, slotY, slotX + 16, slotY + 16, ITEM_DIM_OVERLAY);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void markDirty() {
        lastEditTime = System.currentTimeMillis();
        dirty = true;
    }

    private void processDebounce(String resultStr) {
        if (!dirty) return;
        if (System.currentTimeMillis() - lastEditTime < 500) return;

        dirty = false;
        if (resultStr == null || expression.isEmpty()) return;

        String entry = expression + " = " + resultStr;
        if (entry.equals(lastAddedEntry)) return;

        history.add(entry);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
        lastAddedEntry = entry;
    }

    private static String formatNumber(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        return String.format("%.2f", v);
    }

    private static char keyToChar(int key, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            if (shift) {
                return switch (key) {
                    case GLFW.GLFW_KEY_8 -> '*';
                    case GLFW.GLFW_KEY_9 -> '(';
                    case GLFW.GLFW_KEY_0 -> ')';
                    default -> 0;
                };
            }
            return (char) key;
        }

        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            return (char) ('0' + (key - GLFW.GLFW_KEY_KP_0));
        }

        return switch (key) {
            case GLFW.GLFW_KEY_PERIOD      -> '.';
            case GLFW.GLFW_KEY_KP_DECIMAL  -> '.';
            case GLFW.GLFW_KEY_MINUS       -> '-';
            case GLFW.GLFW_KEY_KP_SUBTRACT -> '-';
            case GLFW.GLFW_KEY_SLASH       -> '/';
            case GLFW.GLFW_KEY_KP_DIVIDE   -> '/';
            case GLFW.GLFW_KEY_KP_ADD      -> '+';
            case GLFW.GLFW_KEY_KP_MULTIPLY -> '*';
            case GLFW.GLFW_KEY_EQUAL       -> shift ? '+' : 0;
            default -> 0;
        };
    }

    private Layout layout(int screenW, int screenH) {
        int x0 = (screenW - WIDTH) / 2;
        int rows = history.size();
        int headerH = rows > 0 ? LINE_HEIGHT + PAD : 0;
        int historyH = rows * LINE_HEIGHT;
        int gap = rows > 0 ? PAD : 0;
        int totalH = PAD + headerH + historyH + gap + INPUT_HEIGHT + PAD;
        int topY = screenH - BOTTOM_MARGIN - totalH;
        int inputY = topY + totalH - PAD - INPUT_HEIGHT;
        int inputX = x0 + PAD;
        int inputW = WIDTH - PAD * 2;
        return new Layout(topY, totalH, inputX, inputY, inputW);
    }

    private record Layout(int topY, int totalH, int inputX, int inputY, int inputW) {}
}

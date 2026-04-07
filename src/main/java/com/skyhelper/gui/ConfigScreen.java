package com.skyhelper.gui;

import com.skyhelper.config.SkyHelperConfig;
import com.skyhelper.features.alerts.AlertHandler;
import com.skyhelper.features.hud.PestRepellentHandler;
import com.skyhelper.features.keybinds.KeybindHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 120;
    private static final int HEADER_HEIGHT = 30;
    private static final int CATEGORY_HEIGHT = 24;
    private static final int TOGGLE_ROW_HEIGHT = 28;
    private static final int TOGGLE_W = 36;
    private static final int TOGGLE_H = 16;

    // Keybind list row
    private static final int KEYBIND_ROW_H = 22;
    private static final int SMALL_BTN_W = 36;
    private static final int SMALL_BTN_H = 14;

    // Colors
    private static final int BG_DARK = 0xE0101020;
    private static final int SIDEBAR_BG = 0xE0181830;
    private static final int HEADER_BG = 0xFF202040;
    private static final int CAT_HOVER = 0x40FFFFFF;
    private static final int CAT_SELECTED = 0x60AAAAFF;
    private static final int TOGGLE_ON = 0xFF44BB44;
    private static final int TOGGLE_OFF = 0xFF883333;
    private static final int TOGGLE_KNOB = 0xFFEEEEEE;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_TITLE = 0xFF8888FF;
    private static final int DIVIDER = 0xFF333355;
    private static final int BTN_BG = 0xFF334488;
    private static final int BTN_HOVER = 0xFF4466AA;
    private static final int BTN_RED = 0xFF884433;
    private static final int BTN_RED_H = 0xFFAA6644;
    private static final int BTN_GREEN = 0xFF337733;
    private static final int BTN_GREEN_H = 0xFF44AA44;
    private static final int ROW_ALT = 0x18FFFFFF;
    private static final int CHECKBOX_BG = 0xFF333355;
    private static final int CHECKBOX_CHECK = 0xFF55FF55;
    private static final int TEXT_KEY = 0xFFFFFF88;

    // Test button
    private static final int TEST_BTN_W = 50;
    private static final int TEST_BTN_H = 14;
    private static final int BTN_ORANGE = 0xFF886633;
    private static final int BTN_ORANGE_H = 0xFFAA8844;

    // Volume selector
    private static final int VOL_BTN_W = 60;
    private static final int VOL_BTN_H = 16;

    // Category IDs (indexes)
    private static final int CAT_STORAGE = 0;
    private static final int CAT_CHAT = 1;
    private static final int CAT_HUD = 2;
    private static final int CAT_KEYBINDS = 3;
    private static final int CAT_ALERTS = 4;
    private static final int CAT_DEPLOYABLES = 5;

    @Nullable
    private final Screen parent;
    private int selectedCategory = 0;
    private int scrollOffset = 0;

    // Toggle entry for simple boolean categories
    private record ToggleEntry(String label, String description, BoolGetter getter, BoolSetter setter) {}

    @FunctionalInterface
    private interface BoolGetter { boolean get(SkyHelperConfig c); }
    @FunctionalInterface
    private interface BoolSetter { void set(SkyHelperConfig c, boolean v); }

    // Category data
    private record SimpleCategory(String name, int color, List<ToggleEntry> entries) {}

    private final List<SimpleCategory> simpleCategories = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private final List<Integer> categoryColors = new ArrayList<>();

    public ConfigScreen(@Nullable Screen parent) {
        super(Component.literal("SkyHelper Config"));
        this.parent = parent;
        buildCategories();
    }

    private void buildCategories() {
        simpleCategories.clear();
        categoryNames.clear();
        categoryColors.clear();

        // 0: Storage
        List<ToggleEntry> storage = new ArrayList<>();
        storage.add(new ToggleEntry("Calculator / Search Bar",
                "Smart input bar: math calculator + storage item search (keybind: ])",
                c -> c.calculatorEnabled, (c, v) -> c.calculatorEnabled = v));
        simpleCategories.add(new SimpleCategory("Storage", 0xFFFFAA00, storage));
        categoryNames.add("Storage");
        categoryColors.add(0xFFFFAA00);

        // 1: Chat
        List<ToggleEntry> chat = new ArrayList<>();
        chat.add(new ToggleEntry("Copy Chat Messages",
                "Ctrl+Click a chat message to copy its text to clipboard",
                c -> c.copyChatEnabled, (c, v) -> c.copyChatEnabled = v));
        chat.add(new ToggleEntry("Hyperion Chat Filter",
                "Suppress \"There are blocks in the way!\" spam in chat",
                c -> c.hyperionFilterEnabled, (c, v) -> c.hyperionFilterEnabled = v));
        simpleCategories.add(new SimpleCategory("Chat", 0xFF55FFFF, chat));
        categoryNames.add("Chat");
        categoryColors.add(0xFF55FFFF);

        // 2: HUD
        List<ToggleEntry> hud = new ArrayList<>();
        hud.add(new ToggleEntry("Skill XP Overlay",
                "Show skill XP progress on the HUD with a progress bar",
                c -> c.skillOverlayEnabled, (c, v) -> c.skillOverlayEnabled = v));
        hud.add(new ToggleEntry("Potion Effects Overlay",
                "Show active potion effects with remaining durations",
                c -> c.potionOverlayEnabled, (c, v) -> c.potionOverlayEnabled = v));
        hud.add(new ToggleEntry("Pest Repellent Timer",
                "Show countdown timer when Pest Repellent is active",
                c -> c.pestRepellentTimerEnabled, (c, v) -> c.pestRepellentTimerEnabled = v));
        hud.add(new ToggleEntry("Repellent 5-Min Warning Sound",
                "Play a sound when Pest Repellent has 5 minutes left",
                c -> c.pestRepellentSoundAt5Min, (c, v) -> c.pestRepellentSoundAt5Min = v));
        hud.add(new ToggleEntry("Repellent Expired Sound",
                "Play a sound when Pest Repellent expires",
                c -> c.pestRepellentSoundOnExpire, (c, v) -> c.pestRepellentSoundOnExpire = v));
        hud.add(new ToggleEntry("Repellent Toast Notifications",
                "Show toast popups for 5-min warning and expiry",
                c -> c.pestRepellentShowToast, (c, v) -> c.pestRepellentShowToast = v));
        simpleCategories.add(new SimpleCategory("HUD", 0xFF55FF55, hud));
        categoryNames.add("HUD");
        categoryColors.add(0xFF55FF55);

        // 3: Keybinds (custom rendering, not in simpleCategories)
        simpleCategories.add(null); // placeholder
        categoryNames.add("Keybinds");
        categoryColors.add(0xFFFF88FF);

        // 4: Alerts (custom rendering, not in simpleCategories)
        simpleCategories.add(null); // placeholder
        categoryNames.add("Alerts");
        categoryColors.add(0xFFFF5555);

        // 5: Deployables (custom rendering, not in simpleCategories)
        simpleCategories.add(null); // placeholder
        categoryNames.add("Deployables");
        categoryColors.add(0xFF8855FF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        Font font = Minecraft.getInstance().font;
        int w = this.width;
        int h = this.height;

        // Full background
        g.fill(0, 0, w, h, BG_DARK);

        // Header bar
        g.fill(0, 0, w, HEADER_HEIGHT, HEADER_BG);
        g.drawCenteredString(font, "SkyHelper Config", w / 2, (HEADER_HEIGHT - 8) / 2, TEXT_TITLE);
        String credit = "- Created by R1fty008";
        int creditX = w / 2 + font.width("SkyHelper Config") / 2 + 6;
        g.drawString(font, credit, creditX, (HEADER_HEIGHT - 8) / 2, TEXT_GRAY);

        // Sidebar background
        int sidebarTop = HEADER_HEIGHT;
        g.fill(0, sidebarTop, SIDEBAR_WIDTH, h, SIDEBAR_BG);

        // Draw categories in sidebar
        for (int i = 0; i < categoryNames.size(); i++) {
            int cy = sidebarTop + i * CATEGORY_HEIGHT;
            boolean hovered = mouseX >= 0 && mouseX < SIDEBAR_WIDTH && mouseY >= cy && mouseY < cy + CATEGORY_HEIGHT;

            if (i == selectedCategory) {
                g.fill(0, cy, SIDEBAR_WIDTH, cy + CATEGORY_HEIGHT, CAT_SELECTED);
            } else if (hovered) {
                g.fill(0, cy, SIDEBAR_WIDTH, cy + CATEGORY_HEIGHT, CAT_HOVER);
            }

            int catColor = categoryColors.get(i);
            g.fill(0, cy, 3, cy + CATEGORY_HEIGHT, catColor);
            g.drawString(font, categoryNames.get(i), 10, cy + (CATEGORY_HEIGHT - 8) / 2, catColor);
        }

        // Divider line
        g.fill(SIDEBAR_WIDTH, sidebarTop, SIDEBAR_WIDTH + 1, h, DIVIDER);

        // Right panel
        int panelX = SIDEBAR_WIDTH + 15;
        int panelRightEdge = w - 15;

        if (selectedCategory == CAT_KEYBINDS) {
            renderKeybindsPanel(g, font, panelX, panelRightEdge, sidebarTop, mouseX, mouseY);
        } else if (selectedCategory == CAT_HUD) {
            renderHudPanel(g, font, panelX, panelRightEdge, sidebarTop, mouseX, mouseY);
        } else if (selectedCategory == CAT_ALERTS) {
            renderAlertsPanel(g, font, panelX, panelRightEdge, sidebarTop, mouseX, mouseY);
        } else if (selectedCategory == CAT_DEPLOYABLES) {
            renderDeployablesPanel(g, font, panelX, panelRightEdge, sidebarTop, mouseX, mouseY);
        } else if (selectedCategory >= 0 && selectedCategory < simpleCategories.size()) {
            renderSimplePanel(g, font, panelX, panelRightEdge, sidebarTop, mouseX, mouseY);
        }
    }

    // ── Simple toggle panel ────────────────────────────────────

    private void renderSimplePanel(GuiGraphics g, Font font, int panelX, int panelRight, int sidebarTop, int mouseX, int mouseY) {
        SimpleCategory cat = simpleCategories.get(selectedCategory);
        if (cat == null) return;

        int panelY = sidebarTop + 10 - scrollOffset;
        g.drawString(font, cat.name + " Settings", panelX, panelY, categoryColors.get(selectedCategory));
        panelY += 18;
        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        for (int i = 0; i < cat.entries.size(); i++) {
            ToggleEntry entry = cat.entries.get(i);
            boolean enabled = entry.getter.get(SkyHelperConfig.get());
            int rowY = panelY + i * TOGGLE_ROW_HEIGHT;

            g.drawString(font, entry.label, panelX, rowY + 2, TEXT_WHITE);
            g.drawString(font, entry.description, panelX, rowY + 13, TEXT_GRAY);

            int toggleX = panelRight - TOGGLE_W;
            renderToggle(g, toggleX, rowY + 2, enabled, mouseX, mouseY);
        }
    }

    // ── HUD panel (toggles + Edit Layout button) ───────────────

    private void renderHudPanel(GuiGraphics g, Font font, int panelX, int panelRight, int sidebarTop, int mouseX, int mouseY) {
        SimpleCategory cat = simpleCategories.get(CAT_HUD);
        if (cat == null) return;

        int panelY = sidebarTop + 10 - scrollOffset;
        g.drawString(font, "HUD Settings", panelX, panelY, categoryColors.get(CAT_HUD));
        panelY += 18;
        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        for (int i = 0; i < cat.entries.size(); i++) {
            ToggleEntry entry = cat.entries.get(i);
            boolean enabled = entry.getter.get(SkyHelperConfig.get());
            int rowY = panelY + i * TOGGLE_ROW_HEIGHT;

            g.drawString(font, entry.label, panelX, rowY + 2, TEXT_WHITE);
            g.drawString(font, entry.description, panelX, rowY + 13, TEXT_GRAY);

            int toggleX = panelRight - TOGGLE_W;
            renderToggle(g, toggleX, rowY + 2, enabled, mouseX, mouseY);
        }

        // Edit HUD Layout button
        int btnY = panelY + cat.entries.size() * TOGGLE_ROW_HEIGHT + 10;
        int btnW = 140;
        int btnH = 20;
        boolean hBtn = mouseX >= panelX && mouseX <= panelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        g.fill(panelX, btnY, panelX + btnW, btnY + btnH, hBtn ? BTN_HOVER : BTN_BG);
        g.drawCenteredString(font, "Edit HUD Layout (F6)", panelX + btnW / 2, btnY + 6, TEXT_WHITE);

        // Pest Repellent test buttons
        int pestY = btnY + btnH + 12;
        g.drawString(font, "Pest Repellent Timer Test", panelX, pestY, TEXT_WHITE);
        pestY += 14;

        int pestBtnW = 90;
        int pestBtnH = 16;
        // Test Regular
        boolean hTestReg = mouseX >= panelX && mouseX <= panelX + pestBtnW && mouseY >= pestY && mouseY <= pestY + pestBtnH;
        g.fill(panelX, pestY, panelX + pestBtnW, pestY + pestBtnH, hTestReg ? BTN_ORANGE_H : BTN_ORANGE);
        g.drawCenteredString(font, "Test Regular", panelX + pestBtnW / 2, pestY + 4, TEXT_WHITE);

        // Test MAX
        int maxX = panelX + pestBtnW + 6;
        boolean hTestMax = mouseX >= maxX && mouseX <= maxX + pestBtnW && mouseY >= pestY && mouseY <= pestY + pestBtnH;
        g.fill(maxX, pestY, maxX + pestBtnW, pestY + pestBtnH, hTestMax ? BTN_ORANGE_H : BTN_ORANGE);
        g.drawCenteredString(font, "Test MAX", maxX + pestBtnW / 2, pestY + 4, TEXT_WHITE);

        // Clear
        int clearX = maxX + pestBtnW + 6;
        int clearW = 50;
        boolean hClear = mouseX >= clearX && mouseX <= clearX + clearW && mouseY >= pestY && mouseY <= pestY + pestBtnH;
        g.fill(clearX, pestY, clearX + clearW, pestY + pestBtnH, hClear ? BTN_RED_H : BTN_RED);
        g.drawCenteredString(font, "Clear", clearX + clearW / 2, pestY + 4, TEXT_WHITE);
    }

    // ── Keybinds panel ─────────────────────────────────────────

    private void renderKeybindsPanel(GuiGraphics g, Font font, int panelX, int panelRight, int sidebarTop, int mouseX, int mouseY) {
        int panelY = sidebarTop + 10 - scrollOffset;
        int catColor = categoryColors.get(CAT_KEYBINDS);

        g.drawString(font, "Keybinds Settings", panelX, panelY, catColor);
        panelY += 18;
        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        // Master toggle
        g.drawString(font, "Enable Keybinds", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Master toggle for all command keybinds", panelX, panelY + 13, TEXT_GRAY);
        int masterToggleX = panelRight - TOGGLE_W;
        renderToggle(g, masterToggleX, panelY + 2, SkyHelperConfig.get().keybindsEnabled, mouseX, mouseY);
        panelY += TOGGLE_ROW_HEIGHT + 5;

        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 8;

        // Keybind list
        List<SkyHelperConfig.KeybindEntry> keybinds = SkyHelperConfig.get().keybinds;
        for (int i = 0; i < keybinds.size(); i++) {
            SkyHelperConfig.KeybindEntry kb = keybinds.get(i);
            int rowY = panelY + i * KEYBIND_ROW_H;

            // Alternating row background
            if (i % 2 == 1) {
                g.fill(panelX, rowY, panelRight, rowY + KEYBIND_ROW_H, ROW_ALT);
            }

            // Checkbox (enable/disable)
            int cbSize = 10;
            int cbX = panelX;
            int cbY = rowY + (KEYBIND_ROW_H - cbSize) / 2;
            g.fill(cbX, cbY, cbX + cbSize, cbY + cbSize, CHECKBOX_BG);
            if (kb.enabled) {
                g.fill(cbX + 2, cbY + 2, cbX + cbSize - 2, cbY + cbSize - 2, CHECKBOX_CHECK);
            }

            // Label + key + command
            String display = (kb.label != null ? kb.label : "?") + "  ";
            int labelW = font.width(display);
            g.drawString(font, display, panelX + cbSize + 4, rowY + 7, TEXT_WHITE);

            String keyStr = "[" + (kb.key != null ? kb.key : "?") + "]";
            g.drawString(font, keyStr, panelX + cbSize + 4 + labelW, rowY + 7, TEXT_KEY);

            String cmdStr = " -> " + (kb.command != null ? kb.command : "?");
            g.drawString(font, cmdStr, panelX + cbSize + 4 + labelW + font.width(keyStr), rowY + 7, TEXT_GRAY);

            // Edit button
            int editX = panelRight - SMALL_BTN_W * 2 - 6;
            int editY = rowY + (KEYBIND_ROW_H - SMALL_BTN_H) / 2;
            boolean hEdit = mouseX >= editX && mouseX <= editX + SMALL_BTN_W && mouseY >= editY && mouseY <= editY + SMALL_BTN_H;
            g.fill(editX, editY, editX + SMALL_BTN_W, editY + SMALL_BTN_H, hEdit ? BTN_HOVER : BTN_BG);
            g.drawCenteredString(font, "Edit", editX + SMALL_BTN_W / 2, editY + 3, TEXT_WHITE);

            // Delete button
            int delX = panelRight - SMALL_BTN_W - 2;
            int delY = editY;
            boolean hDel = mouseX >= delX && mouseX <= delX + SMALL_BTN_W && mouseY >= delY && mouseY <= delY + SMALL_BTN_H;
            g.fill(delX, delY, delX + SMALL_BTN_W, delY + SMALL_BTN_H, hDel ? BTN_RED_H : BTN_RED);
            g.drawCenteredString(font, "Del", delX + SMALL_BTN_W / 2, delY + 3, TEXT_WHITE);
        }

        // Add New Keybind button
        int addY = panelY + keybinds.size() * KEYBIND_ROW_H + 8;
        int addW = 130;
        int addH = 18;
        boolean hAdd = mouseX >= panelX && mouseX <= panelX + addW && mouseY >= addY && mouseY <= addY + addH;
        g.fill(panelX, addY, panelX + addW, addY + addH, hAdd ? BTN_GREEN_H : BTN_GREEN);
        g.drawCenteredString(font, "+ Add New Keybind", panelX + addW / 2, addY + 5, TEXT_WHITE);
    }

    // ── Alerts panel ───────────────────────────────────────────

    private void renderAlertsPanel(GuiGraphics g, Font font, int panelX, int panelRight, int sidebarTop, int mouseX, int mouseY) {
        SkyHelperConfig cfg = SkyHelperConfig.get();
        int panelY = sidebarTop + 10 - scrollOffset;
        int catColor = categoryColors.get(CAT_ALERTS);

        g.drawString(font, "Alerts Settings", panelX, panelY, catColor);
        panelY += 18;
        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        // Master toggle
        g.drawString(font, "Enable Alerts", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Master toggle for all alert types", panelX, panelY + 13, TEXT_GRAY);
        renderToggle(g, panelRight - TOGGLE_W, panelY + 2, cfg.alertsEnabled, mouseX, mouseY);
        panelY += TOGGLE_ROW_HEIGHT + 5;

        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        // Reboot Alert toggle + test
        g.drawString(font, "Reboot Warning", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Alert when server is about to restart", panelX, panelY + 13, TEXT_GRAY);
        renderToggle(g, panelRight - TOGGLE_W, panelY + 2, cfg.rebootAlertEnabled, mouseX, mouseY);
        // Test button
        int testX = panelRight - TOGGLE_W - TEST_BTN_W - 8;
        int testY = panelY + 2;
        boolean hTest1 = mouseX >= testX && mouseX <= testX + TEST_BTN_W && mouseY >= testY && mouseY <= testY + TEST_BTN_H;
        g.fill(testX, testY, testX + TEST_BTN_W, testY + TEST_BTN_H, hTest1 ? BTN_ORANGE_H : BTN_ORANGE);
        g.drawCenteredString(font, "Test", testX + TEST_BTN_W / 2, testY + 3, TEXT_WHITE);
        // Stop button (next to Test)
        int stopX = testX - TEST_BTN_W - 4;
        boolean hStop = mouseX >= stopX && mouseX <= stopX + TEST_BTN_W && mouseY >= testY && mouseY <= testY + TEST_BTN_H;
        g.fill(stopX, testY, stopX + TEST_BTN_W, testY + TEST_BTN_H, hStop ? BTN_RED_H : BTN_RED);
        g.drawCenteredString(font, "Stop", stopX + TEST_BTN_W / 2, testY + 3, TEXT_WHITE);
        panelY += TOGGLE_ROW_HEIGHT;

        // Flash Screen toggle
        g.drawString(font, "Reboot Flash Screen", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Flash the screen red during reboot warning", panelX, panelY + 13, TEXT_GRAY);
        renderToggle(g, panelRight - TOGGLE_W, panelY + 2, cfg.rebootFlashScreen, mouseX, mouseY);
        panelY += TOGGLE_ROW_HEIGHT;

        // Transfer Alert toggle + test
        g.drawString(font, "Transfer Alert", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Alert when transferred to another server", panelX, panelY + 13, TEXT_GRAY);
        renderToggle(g, panelRight - TOGGLE_W, panelY + 2, cfg.transferAlertEnabled, mouseX, mouseY);
        testX = panelRight - TOGGLE_W - TEST_BTN_W - 8;
        testY = panelY + 2;
        boolean hTest2 = mouseX >= testX && mouseX <= testX + TEST_BTN_W && mouseY >= testY && mouseY <= testY + TEST_BTN_H;
        g.fill(testX, testY, testX + TEST_BTN_W, testY + TEST_BTN_H, hTest2 ? BTN_ORANGE_H : BTN_ORANGE);
        g.drawCenteredString(font, "Test", testX + TEST_BTN_W / 2, testY + 3, TEXT_WHITE);
        panelY += TOGGLE_ROW_HEIGHT;

        // Admin Check Alert toggle + test
        g.drawString(font, "Admin Check Alert", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Detect teleport/rotation changes (possible admin check)", panelX, panelY + 13, TEXT_GRAY);
        renderToggle(g, panelRight - TOGGLE_W, panelY + 2, cfg.adminCheckAlertEnabled, mouseX, mouseY);
        testX = panelRight - TOGGLE_W - TEST_BTN_W - 8;
        testY = panelY + 2;
        boolean hTest3 = mouseX >= testX && mouseX <= testX + TEST_BTN_W && mouseY >= testY && mouseY <= testY + TEST_BTN_H;
        g.fill(testX, testY, testX + TEST_BTN_W, testY + TEST_BTN_H, hTest3 ? BTN_ORANGE_H : BTN_ORANGE);
        g.drawCenteredString(font, "Test", testX + TEST_BTN_W / 2, testY + 3, TEXT_WHITE);
        panelY += TOGGLE_ROW_HEIGHT;

        // Admin Check Chat Alert toggle
        g.drawString(font, "Admin Check Chat Message", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Also send a chat message on admin check detection", panelX, panelY + 13, TEXT_GRAY);
        renderToggle(g, panelRight - TOGGLE_W, panelY + 2, cfg.adminCheckChatAlert, mouseX, mouseY);
        panelY += TOGGLE_ROW_HEIGHT;

        // Only While Farming toggle
        g.drawString(font, "Only Alert While Farming", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Transfer + admin alerts only trigger while breaking crops with a hoe", panelX, panelY + 13, TEXT_GRAY);
        renderToggle(g, panelRight - TOGGLE_W, panelY + 2, cfg.alertsOnlyWhileFarming, mouseX, mouseY);
        panelY += TOGGLE_ROW_HEIGHT + 5;

        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        // Volume selector
        g.drawString(font, "Alert Volume", panelX, panelY + 2, TEXT_WHITE);
        String[] volLabels = {"Low", "Medium", "Max"};
        int volX = panelRight - (VOL_BTN_W + 4) * 3;
        for (int i = 0; i < 3; i++) {
            int bx = volX + i * (VOL_BTN_W + 4);
            boolean selected = cfg.alertVolume == i;
            boolean hovered = mouseX >= bx && mouseX <= bx + VOL_BTN_W && mouseY >= panelY && mouseY <= panelY + VOL_BTN_H;
            int color = selected ? BTN_GREEN : (hovered ? BTN_HOVER : BTN_BG);
            g.fill(bx, panelY, bx + VOL_BTN_W, panelY + VOL_BTN_H, color);
            g.drawCenteredString(font, volLabels[i], bx + VOL_BTN_W / 2, panelY + 4, TEXT_WHITE);
        }
    }

    // ── Deployables panel ───────────────────────────────────────

    private void renderDeployablesPanel(GuiGraphics g, Font font, int panelX, int panelRight, int sidebarTop, int mouseX, int mouseY) {
        SkyHelperConfig cfg = SkyHelperConfig.get();
        int panelY = sidebarTop + 10 - scrollOffset;
        int catColor = categoryColors.get(CAT_DEPLOYABLES);

        g.drawString(font, "Deployables Settings", panelX, panelY, catColor);
        panelY += 18;
        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        // Master toggle
        g.drawString(font, "Show Deployable Radius", panelX, panelY + 2, TEXT_WHITE);
        g.drawString(font, "Master toggle for all deployable radius circles", panelX, panelY + 13, TEXT_GRAY);
        renderToggle(g, panelRight - TOGGLE_W, panelY + 2, cfg.deployablesEnabled, mouseX, mouseY);
        panelY += TOGGLE_ROW_HEIGHT + 5;
        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        // Category toggles
        String[][] toggles = {
                {"Show Power Orb Radius", "Radiant, Mana Flux, Overflux, Plasmaflux"},
                {"Show Flare Radius", "Warning Flare, Alert Flare, SOS Flare"},
                {"Show Black Hole Radius", "Small and Medium Pocket Black Holes"},
                {"Show Lantern / Will-o'-wisp Radius", "Dwarven, Mithril, Titanium, Glacite Lanterns + Will-o'-wisp"},
                {"Show Fishing Deployable Radius", "Umberella, Totem of Corruption"},
                {"Show Other Players' Deployables", "Also show radius for nearby players' deployables"},
                {"Show Elapsed Timer Label", "Billboard text with deployable name and elapsed time"},
        };
        boolean[] toggleValues = {
                cfg.showPowerOrbRadius, cfg.showFlareRadius, cfg.showBlackHoleRadius,
                cfg.showLanternRadius, cfg.showFishingDeployableRadius,
                cfg.showOtherPlayersDeployables, cfg.showElapsedTimerLabel
        };

        for (int i = 0; i < toggles.length; i++) {
            int rowY = panelY + i * TOGGLE_ROW_HEIGHT;
            g.drawString(font, toggles[i][0], panelX, rowY + 2, TEXT_WHITE);
            g.drawString(font, toggles[i][1], panelX, rowY + 13, TEXT_GRAY);
            renderToggle(g, panelRight - TOGGLE_W, rowY + 2, toggleValues[i], mouseX, mouseY);
        }

        panelY += toggles.length * TOGGLE_ROW_HEIGHT + 5;
        g.fill(panelX, panelY, panelRight, panelY + 1, DIVIDER);
        panelY += 10;

        // Particle Density selector
        g.drawString(font, "Particle Density", panelX, panelY + 2, TEXT_WHITE);
        String[] densityLabels = {"Low", "Medium", "High"};
        int selX = panelRight - (VOL_BTN_W + 4) * 3;
        for (int i = 0; i < 3; i++) {
            int bx = selX + i * (VOL_BTN_W + 4);
            boolean selected = cfg.deployableParticleDensity == i;
            boolean hovered = mouseX >= bx && mouseX <= bx + VOL_BTN_W && mouseY >= panelY && mouseY <= panelY + VOL_BTN_H;
            int color = selected ? BTN_GREEN : (hovered ? BTN_HOVER : BTN_BG);
            g.fill(bx, panelY, bx + VOL_BTN_W, panelY + VOL_BTN_H, color);
            g.drawCenteredString(font, densityLabels[i], bx + VOL_BTN_W / 2, panelY + 4, TEXT_WHITE);
        }
        panelY += VOL_BTN_H + 12;

        // Circle Opacity selector
        g.drawString(font, "Circle Opacity", panelX, panelY + 2, TEXT_WHITE);
        String[] opacityLabels = {"25%", "50%", "75%", "100%"};
        int opSelX = panelRight - (VOL_BTN_W + 4) * 4;
        for (int i = 0; i < 4; i++) {
            int bx = opSelX + i * (VOL_BTN_W + 4);
            boolean selected = cfg.deployableCircleOpacity == i;
            boolean hovered = mouseX >= bx && mouseX <= bx + VOL_BTN_W && mouseY >= panelY && mouseY <= panelY + VOL_BTN_H;
            int color = selected ? BTN_GREEN : (hovered ? BTN_HOVER : BTN_BG);
            g.fill(bx, panelY, bx + VOL_BTN_W, panelY + VOL_BTN_H, color);
            g.drawCenteredString(font, opacityLabels[i], bx + VOL_BTN_W / 2, panelY + 4, TEXT_WHITE);
        }
    }

    private boolean handleDeployablesClick(double mouseX, double mouseY, int panelX, int panelRight, int sidebarTop) {
        SkyHelperConfig cfg = SkyHelperConfig.get();
        int panelY = sidebarTop + 10 - scrollOffset + 18 + 10; // title + divider gap
        int toggleX = panelRight - TOGGLE_W;

        // Master toggle
        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            cfg.deployablesEnabled = !cfg.deployablesEnabled;
            SkyHelperConfig.save();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT + 5 + 10; // after divider

        // Category toggles (7 entries)
        for (int i = 0; i < 7; i++) {
            int rowY = panelY + i * TOGGLE_ROW_HEIGHT;
            if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                    && mouseY >= rowY + 2 && mouseY <= rowY + 2 + TOGGLE_H) {
                switch (i) {
                    case 0 -> cfg.showPowerOrbRadius = !cfg.showPowerOrbRadius;
                    case 1 -> cfg.showFlareRadius = !cfg.showFlareRadius;
                    case 2 -> cfg.showBlackHoleRadius = !cfg.showBlackHoleRadius;
                    case 3 -> cfg.showLanternRadius = !cfg.showLanternRadius;
                    case 4 -> cfg.showFishingDeployableRadius = !cfg.showFishingDeployableRadius;
                    case 5 -> cfg.showOtherPlayersDeployables = !cfg.showOtherPlayersDeployables;
                    case 6 -> cfg.showElapsedTimerLabel = !cfg.showElapsedTimerLabel;
                }
                SkyHelperConfig.save();
                return true;
            }
        }

        panelY += 7 * TOGGLE_ROW_HEIGHT + 5 + 10; // after divider

        // Particle Density buttons
        int selX = panelRight - (VOL_BTN_W + 4) * 3;
        for (int i = 0; i < 3; i++) {
            int bx = selX + i * (VOL_BTN_W + 4);
            if (mouseX >= bx && mouseX <= bx + VOL_BTN_W && mouseY >= panelY && mouseY <= panelY + VOL_BTN_H) {
                cfg.deployableParticleDensity = i;
                SkyHelperConfig.save();
                return true;
            }
        }
        panelY += VOL_BTN_H + 12;

        // Circle Opacity buttons
        int opSelX = panelRight - (VOL_BTN_W + 4) * 4;
        for (int i = 0; i < 4; i++) {
            int bx = opSelX + i * (VOL_BTN_W + 4);
            if (mouseX >= bx && mouseX <= bx + VOL_BTN_W && mouseY >= panelY && mouseY <= panelY + VOL_BTN_H) {
                cfg.deployableCircleOpacity = i;
                SkyHelperConfig.save();
                return true;
            }
        }

        return true;
    }

    // ── Toggle widget ──────────────────────────────────────────

    private void renderToggle(GuiGraphics g, int x, int y, boolean on, int mouseX, int mouseY) {
        g.fill(x, y, x + TOGGLE_W, y + TOGGLE_H, on ? TOGGLE_ON : TOGGLE_OFF);

        g.fill(x, y, x + 2, y + 2, BG_DARK);
        g.fill(x + TOGGLE_W - 2, y, x + TOGGLE_W, y + 2, BG_DARK);
        g.fill(x, y + TOGGLE_H - 2, x + 2, y + TOGGLE_H, BG_DARK);
        g.fill(x + TOGGLE_W - 2, y + TOGGLE_H - 2, x + TOGGLE_W, y + TOGGLE_H, BG_DARK);

        int knobSize = TOGGLE_H - 4;
        int knobX = on ? x + TOGGLE_W - knobSize - 2 : x + 2;
        g.fill(knobX, y + 2, knobX + knobSize, y + 2 + knobSize, TOGGLE_KNOB);

        Font font = Minecraft.getInstance().font;
        if (on) {
            g.drawString(font, "ON", x + 4, y + (TOGGLE_H - 8) / 2, TEXT_WHITE);
        } else {
            g.drawString(font, "OFF", x + TOGGLE_W - font.width("OFF") - 4, y + (TOGGLE_H - 8) / 2, TEXT_WHITE);
        }
    }

    // ── Mouse handling ─────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() != 0) return super.mouseClicked(event, bl);

        double mouseX = event.x();
        double mouseY = event.y();
        int sidebarTop = HEADER_HEIGHT;

        // Sidebar category click
        if (mouseX >= 0 && mouseX < SIDEBAR_WIDTH && mouseY >= sidebarTop) {
            int index = (int) ((mouseY - sidebarTop) / CATEGORY_HEIGHT);
            if (index >= 0 && index < categoryNames.size()) {
                selectedCategory = index;
                scrollOffset = 0;
                return true;
            }
        }

        int panelX = SIDEBAR_WIDTH + 15;
        int panelRight = this.width - 15;

        // Keybinds panel clicks
        if (selectedCategory == CAT_KEYBINDS) {
            return handleKeybindsClick(mouseX, mouseY, panelX, panelRight, sidebarTop);
        }

        // HUD panel clicks
        if (selectedCategory == CAT_HUD) {
            return handleHudClick(mouseX, mouseY, panelX, panelRight, sidebarTop);
        }

        // Alerts panel clicks
        if (selectedCategory == CAT_ALERTS) {
            return handleAlertsClick(mouseX, mouseY, panelX, panelRight, sidebarTop);
        }

        // Deployables panel clicks
        if (selectedCategory == CAT_DEPLOYABLES) {
            return handleDeployablesClick(mouseX, mouseY, panelX, panelRight, sidebarTop);
        }

        // Simple toggle clicks
        if (selectedCategory >= 0 && selectedCategory < simpleCategories.size()) {
            SimpleCategory cat = simpleCategories.get(selectedCategory);
            if (cat == null) return super.mouseClicked(event, bl);

            int panelY = sidebarTop + 10 - scrollOffset + 18 + 10;
            for (int i = 0; i < cat.entries.size(); i++) {
                ToggleEntry entry = cat.entries.get(i);
                int rowY = panelY + i * TOGGLE_ROW_HEIGHT;
                int toggleX = panelRight - TOGGLE_W;

                if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                        && mouseY >= rowY + 2 && mouseY <= rowY + 2 + TOGGLE_H) {
                    boolean current = entry.getter.get(SkyHelperConfig.get());
                    entry.setter.set(SkyHelperConfig.get(), !current);
                    SkyHelperConfig.save();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, bl);
    }

    private boolean handleKeybindsClick(double mouseX, double mouseY, int panelX, int panelRight, int sidebarTop) {
        int panelY = sidebarTop + 10 - scrollOffset;
        panelY += 18 + 10; // title + divider gap

        // Master toggle
        int masterToggleX = panelRight - TOGGLE_W;
        if (mouseX >= masterToggleX && mouseX <= masterToggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            SkyHelperConfig cfg = SkyHelperConfig.get();
            cfg.keybindsEnabled = !cfg.keybindsEnabled;
            SkyHelperConfig.save();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT + 5 + 8; // after divider

        List<SkyHelperConfig.KeybindEntry> keybinds = SkyHelperConfig.get().keybinds;
        for (int i = 0; i < keybinds.size(); i++) {
            int rowY = panelY + i * KEYBIND_ROW_H;

            // Checkbox
            int cbSize = 10;
            int cbX = panelX;
            int cbY = rowY + (KEYBIND_ROW_H - cbSize) / 2;
            if (mouseX >= cbX && mouseX <= cbX + cbSize && mouseY >= cbY && mouseY <= cbY + cbSize) {
                keybinds.get(i).enabled = !keybinds.get(i).enabled;
                SkyHelperConfig.save();
                return true;
            }

            // Edit button
            int editX = panelRight - SMALL_BTN_W * 2 - 6;
            int editY = rowY + (KEYBIND_ROW_H - SMALL_BTN_H) / 2;
            if (mouseX >= editX && mouseX <= editX + SMALL_BTN_W && mouseY >= editY && mouseY <= editY + SMALL_BTN_H) {
                Minecraft.getInstance().setScreen(new KeybindEditPopup(this, keybinds.get(i), i));
                return true;
            }

            // Delete button
            int delX = panelRight - SMALL_BTN_W - 2;
            if (mouseX >= delX && mouseX <= delX + SMALL_BTN_W && mouseY >= editY && mouseY <= editY + SMALL_BTN_H) {
                keybinds.remove(i);
                SkyHelperConfig.save();
                return true;
            }
        }

        // Add button
        int addY = panelY + keybinds.size() * KEYBIND_ROW_H + 8;
        int addW = 130;
        int addH = 18;
        if (mouseX >= panelX && mouseX <= panelX + addW && mouseY >= addY && mouseY <= addY + addH) {
            Minecraft.getInstance().setScreen(new KeybindEditPopup(this, null, -1));
            return true;
        }

        return true;
    }

    private boolean handleHudClick(double mouseX, double mouseY, int panelX, int panelRight, int sidebarTop) {
        SimpleCategory cat = simpleCategories.get(CAT_HUD);
        if (cat == null) return true;

        int panelY = sidebarTop + 10 - scrollOffset + 18 + 10;

        // Toggle clicks
        for (int i = 0; i < cat.entries.size(); i++) {
            ToggleEntry entry = cat.entries.get(i);
            int rowY = panelY + i * TOGGLE_ROW_HEIGHT;
            int toggleX = panelRight - TOGGLE_W;

            if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                    && mouseY >= rowY + 2 && mouseY <= rowY + 2 + TOGGLE_H) {
                boolean current = entry.getter.get(SkyHelperConfig.get());
                entry.setter.set(SkyHelperConfig.get(), !current);
                SkyHelperConfig.save();
                return true;
            }
        }

        // Edit HUD Layout button
        int btnY = panelY + cat.entries.size() * TOGGLE_ROW_HEIGHT + 10;
        int btnW = 140;
        int btnH = 20;
        if (mouseX >= panelX && mouseX <= panelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            Minecraft.getInstance().setScreen(new HudEditorScreen(this));
            return true;
        }

        // Pest Repellent test buttons
        int pestY = btnY + btnH + 12 + 14;
        int pestBtnW = 90;
        int pestBtnH = 16;

        // Test Regular
        if (mouseX >= panelX && mouseX <= panelX + pestBtnW && mouseY >= pestY && mouseY <= pestY + pestBtnH) {
            PestRepellentHandler.testActivate();
            return true;
        }
        // Test MAX
        int maxX = panelX + pestBtnW + 6;
        if (mouseX >= maxX && mouseX <= maxX + pestBtnW && mouseY >= pestY && mouseY <= pestY + pestBtnH) {
            PestRepellentHandler.testActivateMax();
            return true;
        }
        // Clear
        int clearX = maxX + pestBtnW + 6;
        int clearW = 50;
        if (mouseX >= clearX && mouseX <= clearX + clearW && mouseY >= pestY && mouseY <= pestY + pestBtnH) {
            PestRepellentHandler.testClear();
            return true;
        }

        return true;
    }

    private boolean handleAlertsClick(double mouseX, double mouseY, int panelX, int panelRight, int sidebarTop) {
        SkyHelperConfig cfg = SkyHelperConfig.get();
        int panelY = sidebarTop + 10 - scrollOffset + 18 + 10; // title + divider gap

        // Master toggle
        int toggleX = panelRight - TOGGLE_W;
        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            cfg.alertsEnabled = !cfg.alertsEnabled;
            SkyHelperConfig.save();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT + 5 + 10; // after divider

        // Reboot Alert toggle
        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            cfg.rebootAlertEnabled = !cfg.rebootAlertEnabled;
            SkyHelperConfig.save();
            return true;
        }
        // Reboot Test button
        int testX = panelRight - TOGGLE_W - TEST_BTN_W - 8;
        int testY = panelY + 2;
        if (mouseX >= testX && mouseX <= testX + TEST_BTN_W && mouseY >= testY && mouseY <= testY + TEST_BTN_H) {
            AlertHandler.testRebootAlert();
            return true;
        }
        // Reboot Stop button
        int stopX = testX - TEST_BTN_W - 4;
        if (mouseX >= stopX && mouseX <= stopX + TEST_BTN_W && mouseY >= testY && mouseY <= testY + TEST_BTN_H) {
            AlertHandler.stopRebootTest();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT;

        // Flash Screen toggle
        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            cfg.rebootFlashScreen = !cfg.rebootFlashScreen;
            SkyHelperConfig.save();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT;

        // Transfer Alert toggle
        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            cfg.transferAlertEnabled = !cfg.transferAlertEnabled;
            SkyHelperConfig.save();
            return true;
        }
        // Transfer Test button
        testX = panelRight - TOGGLE_W - TEST_BTN_W - 8;
        testY = panelY + 2;
        if (mouseX >= testX && mouseX <= testX + TEST_BTN_W && mouseY >= testY && mouseY <= testY + TEST_BTN_H) {
            AlertHandler.testTransferAlert();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT;

        // Admin Check Alert toggle
        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            cfg.adminCheckAlertEnabled = !cfg.adminCheckAlertEnabled;
            SkyHelperConfig.save();
            return true;
        }
        // Admin Check Test button
        testX = panelRight - TOGGLE_W - TEST_BTN_W - 8;
        testY = panelY + 2;
        if (mouseX >= testX && mouseX <= testX + TEST_BTN_W && mouseY >= testY && mouseY <= testY + TEST_BTN_H) {
            AlertHandler.testAdminCheckAlert();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT;

        // Admin Check Chat Alert toggle
        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            cfg.adminCheckChatAlert = !cfg.adminCheckChatAlert;
            SkyHelperConfig.save();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT;

        // Only While Farming toggle
        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W
                && mouseY >= panelY + 2 && mouseY <= panelY + 2 + TOGGLE_H) {
            cfg.alertsOnlyWhileFarming = !cfg.alertsOnlyWhileFarming;
            SkyHelperConfig.save();
            return true;
        }
        panelY += TOGGLE_ROW_HEIGHT + 5 + 10; // after divider

        // Volume buttons
        int volX = panelRight - (VOL_BTN_W + 4) * 3;
        for (int i = 0; i < 3; i++) {
            int bx = volX + i * (VOL_BTN_W + 4);
            if (mouseX >= bx && mouseX <= bx + VOL_BTN_W && mouseY >= panelY && mouseY <= panelY + VOL_BTN_H) {
                cfg.alertVolume = i;
                SkyHelperConfig.save();
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, scrollOffset - (int) (verticalAmount * 10));
        return true;
    }

    @Override
    public void onClose() {
        SkyHelperConfig.save();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

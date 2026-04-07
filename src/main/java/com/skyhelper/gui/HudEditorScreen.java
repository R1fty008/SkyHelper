package com.skyhelper.gui;

import com.skyhelper.config.SkyHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-screen HUD editor with draggable, resizable panels.
 * SAVE / RESET ALL / CANCEL toolbar + per-panel right-click context menu.
 */
public class HudEditorScreen extends Screen {

    // Colors
    private static final int PANEL_BG = 0xAA000000;
    private static final int PANEL_BORDER = 0xFF5588CC;
    private static final int PANEL_BORDER_DRAG = 0xFFFFAA00;
    private static final int RESIZE_HANDLE = 0xCCFFFFFF;
    private static final int GRID_COLOR = 0x20FFFFFF;
    private static final int SNAP_LINE_COLOR = 0x80FFFF00;
    private static final int TOOLBAR_BG = 0xE0202040;
    private static final int BTN_BG = 0xFF334488;
    private static final int BTN_HOVER = 0xFF4466AA;
    private static final int BTN_RED = 0xFF884433;
    private static final int BTN_RED_H = 0xFFAA6644;
    private static final int BTN_GREEN = 0xFF337733;
    private static final int BTN_GREEN_H = 0xFF44AA44;
    private static final int CONTEXT_BG = 0xF0181830;
    private static final int CONTEXT_HOVER = 0xFF333366;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_HINT = 0xFF888888;

    private static final int GRID_SIZE = 20;
    private static final int RESIZE_HANDLE_SIZE = 8;
    private static final int TOOLBAR_H = 28;
    private static final int SNAP_THRESHOLD = 8;

    // Panel definitions
    private record PanelDef(String id, String displayName, int minW, int minH) {}

    private static final List<PanelDef> PANEL_DEFS = List.of(
            new PanelDef("skill_xp", "Skill XP", 100, 24),
            new PanelDef("potion_effects", "Potion Effects", 100, 40),
            new PanelDef("pest_repellent", "Pest Repellent", 100, 30)
    );

    // Editable state (copies from config, only committed on Save)
    private final Map<String, EditablePanel> panels = new LinkedHashMap<>();

    private boolean showGrid = false;
    private String dragging = null;
    private String resizing = null;
    private double dragOffsetX, dragOffsetY;

    // Context menu
    private boolean contextOpen = false;
    private int contextX, contextY;
    private String contextPanelId = null;

    // Confirm reset
    private boolean confirmingReset = false;

    private final Screen parent;

    private static class EditablePanel {
        int x, y, w, h;
        boolean visible;
        double scale;
        String customLabel;

        EditablePanel(int x, int y, int w, int h, boolean visible, double scale, String customLabel) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.visible = visible; this.scale = scale; this.customLabel = customLabel;
        }
    }

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        loadFromConfig();
    }

    private void loadFromConfig() {
        panels.clear();
        SkyHelperConfig cfg = SkyHelperConfig.get();
        for (PanelDef def : PANEL_DEFS) {
            SkyHelperConfig.HudPanelLayout layout = cfg.hudLayout.get(def.id);
            if (layout == null) {
                SkyHelperConfig.resetPanel(def.id);
                layout = cfg.hudLayout.get(def.id);
            }
            // Convert percentage position to pixels
            int px = clampX((int) (layout.x * this.width), layout.width);
            int py = clampY((int) (layout.y * this.height), layout.height);
            panels.put(def.id, new EditablePanel(px, py, layout.width, layout.height,
                    layout.visible, layout.scale, layout.customLabel));
        }
    }

    // ── Rendering ──────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Don't call super — we want the game world visible behind us
        Font font = Minecraft.getInstance().font;

        // Semi-transparent overlay
        g.fill(0, 0, this.width, this.height, 0x40000000);

        // Grid
        if (showGrid) {
            for (int gx = 0; gx < this.width; gx += GRID_SIZE) {
                g.fill(gx, 0, gx + 1, this.height, GRID_COLOR);
            }
            for (int gy = 0; gy < this.height; gy += GRID_SIZE) {
                g.fill(0, gy, this.width, gy + 1, GRID_COLOR);
            }
        }

        // Snap lines
        if (dragging != null) {
            drawSnapLines(g, panels.get(dragging));
        }

        // Panels
        for (var entry : panels.entrySet()) {
            String id = entry.getKey();
            EditablePanel panel = entry.getValue();
            if (!panel.visible) continue;

            PanelDef def = PANEL_DEFS.stream().filter(d -> d.id.equals(id)).findFirst().orElse(null);
            if (def == null) continue;

            boolean isDragging = id.equals(dragging) || id.equals(resizing);
            int borderColor = isDragging ? PANEL_BORDER_DRAG : PANEL_BORDER;

            // Border
            g.fill(panel.x - 1, panel.y - 1, panel.x + panel.w + 1, panel.y + panel.h + 1, borderColor);
            // Background
            g.fill(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h, PANEL_BG);

            // Panel label
            String label = panel.customLabel != null ? panel.customLabel : def.displayName;
            g.drawString(font, label, panel.x + 4, panel.y + 4, TEXT_WHITE);

            // Size label if resizing
            if (id.equals(resizing)) {
                String sizeText = panel.w + " x " + panel.h;
                g.drawString(font, sizeText, panel.x + 4, panel.y + panel.h - 12, TEXT_GRAY);
            }

            // Resize handle (bottom-right triangle)
            int hx = panel.x + panel.w - RESIZE_HANDLE_SIZE;
            int hy = panel.y + panel.h - RESIZE_HANDLE_SIZE;
            for (int i = 0; i < RESIZE_HANDLE_SIZE; i++) {
                g.fill(hx + i, hy + (RESIZE_HANDLE_SIZE - i - 1),
                        hx + RESIZE_HANDLE_SIZE, hy + RESIZE_HANDLE_SIZE, RESIZE_HANDLE);
            }

            // Preview content
            renderPanelPreview(g, font, id, panel);
        }

        // Toolbar
        renderToolbar(g, font, mouseX, mouseY);

        // Hint text
        String hint = "Drag to move  |  Drag corner to resize  |  Shift = snap  |  G = grid  |  Escape = cancel";
        g.drawCenteredString(font, hint, this.width / 2, this.height - 14, TEXT_HINT);

        // Context menu
        if (contextOpen) {
            renderContextMenu(g, font, mouseX, mouseY);
        }

        // Reset confirmation
        if (confirmingReset) {
            renderResetConfirm(g, font, mouseX, mouseY);
        }
    }

    private void renderPanelPreview(GuiGraphics g, Font font, String id, EditablePanel panel) {
        // Simple preview content to show what the panel will look like
        int px = panel.x + 4;
        int py = panel.y + 16;
        switch (id) {
            case "skill_xp" -> {
                if (py + 10 < panel.y + panel.h) {
                    int barW = Math.max(panel.w - 8, 10);
                    g.fill(px, py, px + barW, py + 8, 0xFF333333);
                    g.fill(px, py, px + (int) (barW * 0.65), py + 8, 0xFF44AAFF);
                    g.drawString(font, "65%", px + barW / 2 - 8, py, TEXT_WHITE);
                }
            }
            case "potion_effects" -> {
                if (py + 10 < panel.y + panel.h) {
                    g.drawString(font, "Speed II      3:45", px, py, 0xFF88FF88);
                }
                if (py + 24 < panel.y + panel.h) {
                    g.drawString(font, "Haste III     5:00", px, py + 14, 0xFF88FF88);
                }
            }
            case "pest_repellent" -> {
                g.drawString(font, "47:23", px, py, TEXT_WHITE);
                // Mini progress bar preview
                int barW = Math.max(panel.w - 8, 10);
                int barY2 = panel.y + panel.h - 6;
                g.fill(px, barY2, px + barW, barY2 + 3, 0xFF333333);
                g.fill(px, barY2, px + (int)(barW * 0.78), barY2 + 3, 0xFF44CC44);
            }
        }
    }

    private void renderToolbar(GuiGraphics g, Font font, int mouseX, int mouseY) {
        int toolbarW = 340;
        int tx = (this.width - toolbarW) / 2;
        int ty = 4;

        g.fill(tx, ty, tx + toolbarW, ty + TOOLBAR_H, TOOLBAR_BG);

        int btnW = 70;
        int btnH = 18;
        int btnY = ty + 5;
        int gap = 10;
        int startX = tx + 15;

        // Save
        boolean hSave = isInside(mouseX, mouseY, startX, btnY, btnW, btnH);
        g.fill(startX, btnY, startX + btnW, btnY + btnH, hSave ? BTN_GREEN_H : BTN_GREEN);
        g.drawCenteredString(font, "Save", startX + btnW / 2, btnY + 5, TEXT_WHITE);

        // Reset All
        int resetX = startX + btnW + gap;
        boolean hReset = isInside(mouseX, mouseY, resetX, btnY, btnW, btnH);
        g.fill(resetX, btnY, resetX + btnW, btnY + btnH, hReset ? BTN_RED_H : BTN_RED);
        g.drawCenteredString(font, "Reset All", resetX + btnW / 2, btnY + 5, TEXT_WHITE);

        // Cancel
        int cancelX = resetX + btnW + gap;
        boolean hCancel = isInside(mouseX, mouseY, cancelX, btnY, btnW, btnH);
        g.fill(cancelX, btnY, cancelX + btnW, btnY + btnH, hCancel ? BTN_HOVER : BTN_BG);
        g.drawCenteredString(font, "Cancel", cancelX + btnW / 2, btnY + 5, TEXT_WHITE);

        // Grid toggle
        int gridX = cancelX + btnW + gap;
        int gridW = 50;
        boolean hGrid = isInside(mouseX, mouseY, gridX, btnY, gridW, btnH);
        g.fill(gridX, btnY, gridX + gridW, btnY + btnH, hGrid ? BTN_HOVER : BTN_BG);
        g.drawCenteredString(font, showGrid ? "Grid: ON" : "Grid: OFF", gridX + gridW / 2, btnY + 5, TEXT_WHITE);
    }

    private void renderContextMenu(GuiGraphics g, Font font, int mouseX, int mouseY) {
        int itemW = 130;
        int itemH = 18;
        int menuH = itemH * 3;

        g.fill(contextX, contextY, contextX + itemW, contextY + menuH, CONTEXT_BG);

        String[] labels = {"Reset this panel", "Hide this panel", "Rename label"};
        for (int i = 0; i < labels.length; i++) {
            int iy = contextY + i * itemH;
            boolean hovered = mouseX >= contextX && mouseX <= contextX + itemW
                    && mouseY >= iy && mouseY < iy + itemH;
            if (hovered) {
                g.fill(contextX, iy, contextX + itemW, iy + itemH, CONTEXT_HOVER);
            }
            g.drawString(font, labels[i], contextX + 5, iy + 5, TEXT_WHITE);
        }
    }

    private void renderResetConfirm(GuiGraphics g, Font font, int mouseX, int mouseY) {
        int pw = 220, ph = 70;
        int px = this.width / 2 - pw / 2;
        int py = this.height / 2 - ph / 2;

        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(px, py, px + pw, py + ph, CONTEXT_BG);
        g.drawCenteredString(font, "Reset all panels to default?", this.width / 2, py + 10, TEXT_WHITE);

        int btnW = 70, btnH = 18;
        int yesX = this.width / 2 - btnW - 5;
        int noX = this.width / 2 + 5;
        int btnY = py + 40;

        boolean hYes = isInside(mouseX, mouseY, yesX, btnY, btnW, btnH);
        boolean hNo = isInside(mouseX, mouseY, noX, btnY, btnW, btnH);

        g.fill(yesX, btnY, yesX + btnW, btnY + btnH, hYes ? BTN_RED_H : BTN_RED);
        g.drawCenteredString(font, "Yes", yesX + btnW / 2, btnY + 5, TEXT_WHITE);
        g.fill(noX, btnY, noX + btnW, btnY + btnH, hNo ? BTN_HOVER : BTN_BG);
        g.drawCenteredString(font, "No", noX + btnW / 2, btnY + 5, TEXT_WHITE);
    }

    private void drawSnapLines(GuiGraphics g, EditablePanel panel) {
        if (panel == null) return;
        // Edge snap lines
        for (var other : panels.values()) {
            if (other == panel || !other.visible) continue;
            // Left edge alignment
            if (Math.abs(panel.x - other.x) < SNAP_THRESHOLD) {
                g.fill(other.x, 0, other.x + 1, this.height, SNAP_LINE_COLOR);
            }
            // Right edge alignment
            if (Math.abs((panel.x + panel.w) - (other.x + other.w)) < SNAP_THRESHOLD) {
                g.fill(other.x + other.w, 0, other.x + other.w + 1, this.height, SNAP_LINE_COLOR);
            }
            // Top edge alignment
            if (Math.abs(panel.y - other.y) < SNAP_THRESHOLD) {
                g.fill(0, other.y, this.width, other.y + 1, SNAP_LINE_COLOR);
            }
            // Bottom edge alignment
            if (Math.abs((panel.y + panel.h) - (other.y + other.h)) < SNAP_THRESHOLD) {
                g.fill(0, other.y + other.h, this.width, other.y + other.h + 1, SNAP_LINE_COLOR);
            }
        }
        // Screen edge snap
        if (panel.x < SNAP_THRESHOLD) g.fill(0, 0, 1, this.height, SNAP_LINE_COLOR);
        if (panel.y < SNAP_THRESHOLD) g.fill(0, 0, this.width, 1, SNAP_LINE_COLOR);
        if (panel.x + panel.w > this.width - SNAP_THRESHOLD)
            g.fill(this.width - 1, 0, this.width, this.height, SNAP_LINE_COLOR);
        if (panel.y + panel.h > this.height - SNAP_THRESHOLD)
            g.fill(0, this.height - 1, this.width, this.height, SNAP_LINE_COLOR);
    }

    // ── Input ──────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        // Close context menu on any click outside it
        if (contextOpen) {
            if (button == 0) {
                int itemW = 130, itemH = 18;
                if (mx >= contextX && mx <= contextX + itemW && my >= contextY && my < contextY + itemH * 3) {
                    int idx = (int) ((my - contextY) / itemH);
                    handleContextAction(idx);
                }
            }
            contextOpen = false;
            return true;
        }

        // Reset confirmation
        if (confirmingReset) {
            int pw = 220, ph = 70;
            int px = this.width / 2 - pw / 2;
            int py = this.height / 2 - ph / 2;
            int btnW = 70, btnH = 18;
            int yesX = this.width / 2 - btnW - 5;
            int noX = this.width / 2 + 5;
            int btnY = py + 40;

            if (isInside(mx, my, yesX, btnY, btnW, btnH)) {
                // Reset all
                SkyHelperConfig.resetHudDefaults();
                loadFromConfig();
                confirmingReset = false;
                return true;
            }
            if (isInside(mx, my, noX, btnY, btnW, btnH)) {
                confirmingReset = false;
                return true;
            }
            return true;
        }

        // Toolbar clicks (left click)
        if (button == 0) {
            int toolbarW = 340;
            int tx = (this.width - toolbarW) / 2;
            int ty = 4;
            int btnW = 70, btnH = 18, btnY = ty + 5, gap = 10;
            int startX = tx + 15;

            // Save
            if (isInside(mx, my, startX, btnY, btnW, btnH)) {
                saveToConfig();
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
            // Reset All
            int resetX = startX + btnW + gap;
            if (isInside(mx, my, resetX, btnY, btnW, btnH)) {
                confirmingReset = true;
                return true;
            }
            // Cancel
            int cancelX = resetX + btnW + gap;
            if (isInside(mx, my, cancelX, btnY, btnW, btnH)) {
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
            // Grid toggle
            int gridX = cancelX + btnW + gap;
            int gridW = 50;
            if (isInside(mx, my, gridX, btnY, gridW, btnH)) {
                showGrid = !showGrid;
                return true;
            }
        }

        // Panel interactions
        if (button == 0) {
            // Check resize handles first (bottom-right corner)
            for (var entry : panels.entrySet()) {
                EditablePanel panel = entry.getValue();
                if (!panel.visible) continue;
                int hx = panel.x + panel.w - RESIZE_HANDLE_SIZE;
                int hy = panel.y + panel.h - RESIZE_HANDLE_SIZE;
                if (mx >= hx && mx <= panel.x + panel.w && my >= hy && my <= panel.y + panel.h) {
                    resizing = entry.getKey();
                    dragOffsetX = mx;
                    dragOffsetY = my;
                    return true;
                }
            }
            // Check drag (click on panel body)
            for (var entry : panels.entrySet()) {
                EditablePanel panel = entry.getValue();
                if (!panel.visible) continue;
                if (mx >= panel.x && mx <= panel.x + panel.w && my >= panel.y && my <= panel.y + panel.h) {
                    dragging = entry.getKey();
                    dragOffsetX = mx - panel.x;
                    dragOffsetY = my - panel.y;
                    return true;
                }
            }
        }

        // Right click — context menu
        if (button == 1) {
            for (var entry : panels.entrySet()) {
                EditablePanel panel = entry.getValue();
                if (!panel.visible) continue;
                if (mx >= panel.x && mx <= panel.x + panel.w && my >= panel.y && my <= panel.y + panel.h) {
                    contextOpen = true;
                    contextX = (int) mx;
                    contextY = (int) my;
                    contextPanelId = entry.getKey();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mx = event.x();
        double my = event.y();

        if (dragging != null) {
            EditablePanel panel = panels.get(dragging);
            if (panel != null) {
                int newX = (int) (mx - dragOffsetX);
                int newY = (int) (my - dragOffsetY);

                // Shift = snap to grid
                boolean shiftHeld = GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                if (shiftHeld) {
                    newX = (newX / GRID_SIZE) * GRID_SIZE;
                    newY = (newY / GRID_SIZE) * GRID_SIZE;
                }

                // Edge snapping
                newX = snapX(newX, panel.w);
                newY = snapY(newY, panel.h);

                panel.x = clampX(newX, panel.w);
                panel.y = clampY(newY, panel.h);
            }
            return true;
        }

        if (resizing != null) {
            EditablePanel panel = panels.get(resizing);
            PanelDef def = PANEL_DEFS.stream().filter(d -> d.id.equals(resizing)).findFirst().orElse(null);
            if (panel != null && def != null) {
                int newW = Math.max(def.minW, (int) (mx - panel.x));
                int newH = Math.max(def.minH, (int) (my - panel.y));
                // Max: don't go off screen
                newW = Math.min(newW, this.width - panel.x);
                newH = Math.min(newH, this.height - panel.y);
                panel.w = newW;
                panel.h = newH;
            }
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        resizing = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int key = keyEvent.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        if (key == GLFW.GLFW_KEY_G) {
            showGrid = !showGrid;
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    // ── Context menu actions ───────────────────────────────────

    private void handleContextAction(int idx) {
        if (contextPanelId == null) return;
        EditablePanel panel = panels.get(contextPanelId);
        if (panel == null) return;

        switch (idx) {
            case 0 -> { // Reset this panel
                SkyHelperConfig.resetPanel(contextPanelId);
                SkyHelperConfig.HudPanelLayout layout = SkyHelperConfig.getPanel(contextPanelId);
                if (layout != null) {
                    panel.x = (int) (layout.x * this.width);
                    panel.y = (int) (layout.y * this.height);
                    panel.w = layout.width;
                    panel.h = layout.height;
                    panel.scale = layout.scale;
                    panel.visible = layout.visible;
                    panel.customLabel = null;
                }
            }
            case 1 -> panel.visible = false; // Hide
            case 2 -> { /* Rename — would need a text input popup; skip for now, label via config */ }
        }
    }

    // ── Save / Load ────────────────────────────────────────────

    private void saveToConfig() {
        SkyHelperConfig cfg = SkyHelperConfig.get();
        for (var entry : panels.entrySet()) {
            EditablePanel panel = entry.getValue();
            SkyHelperConfig.HudPanelLayout layout = cfg.hudLayout.get(entry.getKey());
            if (layout == null) {
                layout = new SkyHelperConfig.HudPanelLayout();
                cfg.hudLayout.put(entry.getKey(), layout);
            }
            layout.x = (double) panel.x / this.width;
            layout.y = (double) panel.y / this.height;
            layout.width = panel.w;
            layout.height = panel.h;
            layout.visible = panel.visible;
            layout.scale = panel.scale;
            layout.customLabel = panel.customLabel;
        }
        SkyHelperConfig.save();
    }

    // ── Helpers ─────────────────────────────────────────────────

    private int clampX(int x, int w) { return Math.max(0, Math.min(x, this.width - w)); }
    private int clampY(int y, int h) { return Math.max(0, Math.min(y, this.height - h)); }

    private int snapX(int x, int w) {
        if (x < SNAP_THRESHOLD) return 0;
        if (x + w > this.width - SNAP_THRESHOLD) return this.width - w;
        for (var other : panels.values()) {
            if (!other.visible) continue;
            if (Math.abs(x - other.x) < SNAP_THRESHOLD) return other.x;
            if (Math.abs(x - (other.x + other.w)) < SNAP_THRESHOLD) return other.x + other.w;
            if (Math.abs((x + w) - other.x) < SNAP_THRESHOLD) return other.x - w;
            if (Math.abs((x + w) - (other.x + other.w)) < SNAP_THRESHOLD) return other.x + other.w - w;
        }
        return x;
    }

    private int snapY(int y, int h) {
        if (y < SNAP_THRESHOLD) return 0;
        if (y + h > this.height - SNAP_THRESHOLD) return this.height - h;
        for (var other : panels.values()) {
            if (!other.visible) continue;
            if (Math.abs(y - other.y) < SNAP_THRESHOLD) return other.y;
            if (Math.abs(y - (other.y + other.h)) < SNAP_THRESHOLD) return other.y + other.h;
            if (Math.abs((y + h) - other.y) < SNAP_THRESHOLD) return other.y - h;
            if (Math.abs((y + h) - (other.y + other.h)) < SNAP_THRESHOLD) return other.y + other.h - h;
        }
        return y;
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

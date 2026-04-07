package com.skyhelper.gui;

import com.skyhelper.config.SkyHelperConfig;
import com.skyhelper.features.keybinds.KeybindHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Popup for adding or editing a keybind.
 * Fields: Label, Command, Key (press-to-set).
 */
public class KeybindEditPopup extends Screen {

    private static final int POPUP_W = 260;
    private static final int POPUP_H = 200;

    // Colors
    private static final int BG = 0xF0101020;
    private static final int BORDER = 0xFF5555AA;
    private static final int FIELD_BG = 0xFF222244;
    private static final int FIELD_ACTIVE = 0xFF333366;
    private static final int BTN_BG = 0xFF334488;
    private static final int BTN_HOVER = 0xFF4466AA;
    private static final int BTN_CANCEL = 0xFF664433;
    private static final int BTN_CANCEL_H = 0xFF886644;
    private static final int BTN_CONFIRM = 0xFF337733;
    private static final int BTN_CONFIRM_H = 0xFF44AA44;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_RED = 0xFFFF5555;
    private static final int TEXT_YELLOW = 0xFFFFFF55;

    private final Screen parent;
    private final SkyHelperConfig.KeybindEntry editing; // null = new keybind
    private final int editIndex; // -1 = new

    private String label;
    private String command;
    private String keyName; // human readable
    private int capturedGlfw = GLFW.GLFW_KEY_UNKNOWN;

    // Which field is focused: 0=label, 1=command, 2=key
    private int focusedField = -1;
    private boolean waitingForKey = false;

    private String errorMessage = "";
    private boolean showVanillaWarning = false;

    public KeybindEditPopup(Screen parent, SkyHelperConfig.KeybindEntry editing, int editIndex) {
        super(Component.literal(editing == null ? "Add Keybind" : "Edit Keybind"));
        this.parent = parent;
        this.editing = editing;
        this.editIndex = editIndex;

        if (editing != null) {
            this.label = editing.label != null ? editing.label : "";
            this.command = editing.command != null ? editing.command : "";
            this.keyName = editing.key != null ? editing.key : "";
            this.capturedGlfw = KeybindHandler.keyNameToGlfw(editing.key);
        } else {
            this.label = "";
            this.command = "";
            this.keyName = "";
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        Font font = Minecraft.getInstance().font;
        int cx = this.width / 2;
        int cy = this.height / 2;
        int px = cx - POPUP_W / 2;
        int py = cy - POPUP_H / 2;

        // Dim background
        g.fill(0, 0, this.width, this.height, 0x88000000);

        // Popup background + border
        g.fill(px - 1, py - 1, px + POPUP_W + 1, py + POPUP_H + 1, BORDER);
        g.fill(px, py, px + POPUP_W, py + POPUP_H, BG);

        // Title
        String titleStr = editing == null ? "Add New Keybind" : "Edit Keybind";
        g.drawCenteredString(font, titleStr, cx, py + 8, TEXT_WHITE);

        int fieldX = px + 15;
        int fieldW = POPUP_W - 30;
        int fieldH = 16;

        // Label field
        int ly = py + 30;
        g.drawString(font, "Label:", fieldX, ly, TEXT_GRAY);
        ly += 12;
        g.fill(fieldX, ly, fieldX + fieldW, ly + fieldH, focusedField == 0 ? FIELD_ACTIVE : FIELD_BG);
        g.drawString(font, label + (focusedField == 0 ? "_" : ""), fieldX + 3, ly + 4, TEXT_WHITE);

        // Command field
        int coy = ly + fieldH + 10;
        g.drawString(font, "Command:", fieldX, coy, TEXT_GRAY);
        coy += 12;
        g.fill(fieldX, coy, fieldX + fieldW, coy + fieldH, focusedField == 1 ? FIELD_ACTIVE : FIELD_BG);
        g.drawString(font, command + (focusedField == 1 ? "_" : ""), fieldX + 3, coy + 4, TEXT_WHITE);

        // Key field
        int ky = coy + fieldH + 10;
        g.drawString(font, "Key:", fieldX, ky, TEXT_GRAY);
        ky += 12;
        g.fill(fieldX, ky, fieldX + fieldW, ky + fieldH, focusedField == 2 ? FIELD_ACTIVE : FIELD_BG);
        String keyDisplay = waitingForKey ? "Press a key..." : (keyName.isEmpty() ? "Click to set" : keyName);
        int keyColor = waitingForKey ? TEXT_YELLOW : (keyName.isEmpty() ? TEXT_GRAY : TEXT_WHITE);
        g.drawString(font, keyDisplay, fieldX + 3, ky + 4, keyColor);

        // Error / warning messages
        int msgY = ky + fieldH + 6;
        if (!errorMessage.isEmpty()) {
            g.drawString(font, errorMessage, fieldX, msgY, TEXT_RED);
            msgY += 12;
        }

        // Vanilla warning buttons
        if (showVanillaWarning) {
            g.drawString(font, "Warning: used by Minecraft. May conflict.", fieldX, msgY, TEXT_YELLOW);
            msgY += 14;

            // Confirm / Cancel buttons for the warning
            int btnW = 80;
            int btnH = 16;
            int confirmX = cx - btnW - 5;
            int cancelX = cx + 5;

            boolean hConfirm = mouseX >= confirmX && mouseX <= confirmX + btnW && mouseY >= msgY && mouseY <= msgY + btnH;
            boolean hCancel = mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= msgY && mouseY <= msgY + btnH;

            g.fill(confirmX, msgY, confirmX + btnW, msgY + btnH, hConfirm ? BTN_CONFIRM_H : BTN_CONFIRM);
            g.drawCenteredString(font, "Confirm", confirmX + btnW / 2, msgY + 4, TEXT_WHITE);

            g.fill(cancelX, msgY, cancelX + btnW, msgY + btnH, hCancel ? BTN_CANCEL_H : BTN_CANCEL);
            g.drawCenteredString(font, "Cancel", cancelX + btnW / 2, msgY + 4, TEXT_WHITE);
        } else {
            // Save / Cancel buttons
            int btnW = 80;
            int btnH = 18;
            int btnY = py + POPUP_H - btnH - 10;

            int saveX = cx - btnW - 5;
            int cancelX = cx + 5;

            boolean hSave = mouseX >= saveX && mouseX <= saveX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            boolean hCancel = mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

            g.fill(saveX, btnY, saveX + btnW, btnY + btnH, hSave ? BTN_HOVER : BTN_BG);
            g.drawCenteredString(font, "Save", saveX + btnW / 2, btnY + 5, TEXT_WHITE);

            g.fill(cancelX, btnY, cancelX + btnW, btnY + btnH, hCancel ? BTN_CANCEL_H : BTN_CANCEL);
            g.drawCenteredString(font, "Cancel", cancelX + btnW / 2, btnY + 5, TEXT_WHITE);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() != 0) return super.mouseClicked(event, bl);

        double mouseX = event.x();
        double mouseY = event.y();
        Font font = Minecraft.getInstance().font;

        int cx = this.width / 2;
        int cy = this.height / 2;
        int px = cx - POPUP_W / 2;
        int py = cy - POPUP_H / 2;
        int fieldX = px + 15;
        int fieldW = POPUP_W - 30;
        int fieldH = 16;

        // Calculate field positions
        int ly = py + 30 + 12;
        int coy = ly + fieldH + 10 + 12;
        int ky = coy + fieldH + 10 + 12;

        // Click on label field
        if (mouseX >= fieldX && mouseX <= fieldX + fieldW && mouseY >= ly && mouseY <= ly + fieldH) {
            focusedField = 0;
            waitingForKey = false;
            return true;
        }
        // Click on command field
        if (mouseX >= fieldX && mouseX <= fieldX + fieldW && mouseY >= coy && mouseY <= coy + fieldH) {
            focusedField = 1;
            waitingForKey = false;
            return true;
        }
        // Click on key field
        if (mouseX >= fieldX && mouseX <= fieldX + fieldW && mouseY >= ky && mouseY <= ky + fieldH) {
            focusedField = 2;
            waitingForKey = true;
            errorMessage = "";
            return true;
        }

        // Vanilla warning confirm/cancel
        if (showVanillaWarning) {
            int msgY = ky + fieldH + 6 + 14; // after warning text
            if (!errorMessage.isEmpty()) msgY += 12;
            int btnW = 80;
            int btnH = 16;
            int confirmX = cx - btnW - 5;
            int cancelBtnX = cx + 5;

            if (mouseX >= confirmX && mouseX <= confirmX + btnW && mouseY >= msgY && mouseY <= msgY + btnH) {
                // Force save despite vanilla conflict
                showVanillaWarning = false;
                doSave();
                return true;
            }
            if (mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= msgY && mouseY <= msgY + btnH) {
                showVanillaWarning = false;
                keyName = "";
                capturedGlfw = GLFW.GLFW_KEY_UNKNOWN;
                return true;
            }
            return true;
        }

        // Save / Cancel buttons
        int btnW = 80;
        int btnH = 18;
        int btnY = py + POPUP_H - btnH - 10;
        int saveX = cx - btnW - 5;
        int cancelBtnX = cx + 5;

        if (mouseX >= saveX && mouseX <= saveX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            attemptSave();
            return true;
        }
        if (mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        focusedField = -1;
        waitingForKey = false;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int key = keyEvent.key();

        // Key capture mode
        if (waitingForKey) {
            if (KeybindHandler.isModifierKey(key)) {
                errorMessage = "Modifier keys are not supported. Please press a single key.";
                return true;
            }
            capturedGlfw = key;
            keyName = KeybindHandler.glfwToKeyName(key);
            waitingForKey = false;
            errorMessage = "";

            // Check duplicate
            String dup = checkDuplicate(key);
            if (dup != null) {
                errorMessage = "Key already in use by: " + dup;
                keyName = "";
                capturedGlfw = GLFW.GLFW_KEY_UNKNOWN;
            }
            // Check vanilla conflict
            else if (KeybindHandler.conflictsWithVanilla(key)) {
                showVanillaWarning = true;
            }
            return true;
        }

        // Text field input
        if (focusedField == 0 || focusedField == 1) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (focusedField == 0 && !label.isEmpty()) {
                    label = label.substring(0, label.length() - 1);
                } else if (focusedField == 1 && !command.isEmpty()) {
                    command = command.substring(0, command.length() - 1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_TAB) {
                focusedField = (focusedField + 1) % 3;
                if (focusedField == 2) waitingForKey = true;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                attemptSave();
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent charEvent) {
        if (waitingForKey) return true;

        char c = (char) charEvent.codepoint();
        if (focusedField == 0) {
            if (label.length() < 30) label += c;
            return true;
        }
        if (focusedField == 1) {
            if (command.length() < 50) command += c;
            return true;
        }
        return super.charTyped(charEvent);
    }

    private String checkDuplicate(int glfwKey) {
        List<SkyHelperConfig.KeybindEntry> keybinds = SkyHelperConfig.get().keybinds;
        for (int i = 0; i < keybinds.size(); i++) {
            if (i == editIndex) continue; // skip self when editing
            SkyHelperConfig.KeybindEntry other = keybinds.get(i);
            if (other.key != null && KeybindHandler.keyNameToGlfw(other.key) == glfwKey) {
                return other.label;
            }
        }
        return null;
    }

    private void attemptSave() {
        if (label.trim().isEmpty()) {
            errorMessage = "Label cannot be empty.";
            return;
        }
        if (command.trim().isEmpty()) {
            errorMessage = "Command cannot be empty.";
            return;
        }
        if (capturedGlfw == GLFW.GLFW_KEY_UNKNOWN || keyName.isEmpty()) {
            errorMessage = "Please set a key.";
            return;
        }

        // Re-check duplicates
        String dup = checkDuplicate(capturedGlfw);
        if (dup != null) {
            errorMessage = "Key already in use by: " + dup;
            return;
        }

        // Check vanilla warning
        if (KeybindHandler.conflictsWithVanilla(capturedGlfw) && !showVanillaWarning) {
            showVanillaWarning = true;
            return;
        }

        doSave();
    }

    private void doSave() {
        String cmd = command.trim();
        if (!cmd.startsWith("/")) cmd = "/" + cmd;

        if (editing != null && editIndex >= 0) {
            editing.label = label.trim();
            editing.command = cmd;
            editing.key = keyName;
        } else {
            SkyHelperConfig.get().keybinds.add(
                    new SkyHelperConfig.KeybindEntry(keyName, cmd, label.trim(), true)
            );
        }
        SkyHelperConfig.save();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

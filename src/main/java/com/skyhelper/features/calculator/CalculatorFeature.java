package com.skyhelper.features.calculator;

import com.skyhelper.config.SkyHelperConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class CalculatorFeature {

    private static final CalculatorOverlay OVERLAY = new CalculatorOverlay();

    public static CalculatorOverlay getOverlay() {
        return OVERLAY;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!SkyHelperConfig.get().calculatorEnabled) return;
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            OVERLAY.onScreenOpen(containerScreen);

            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    OVERLAY.render(graphics, s.width, s.height, mouseX, mouseY)
            );

            ScreenEvents.remove(screen).register(s -> OVERLAY.onScreenClose());

            ScreenKeyboardEvents.allowKeyPress(screen).register((s, keyEvent) ->
                    OVERLAY.onKeyPress(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers())
            );

            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseEvent) -> {
                if (mouseEvent.button() == 0) {
                    return OVERLAY.onMouseClick(mouseEvent.x(), mouseEvent.y(), s.width, s.height);
                }
                return true;
            });
        });
    }
}

package com.skyhelper;

import com.skyhelper.config.SkyHelperConfig;
import com.skyhelper.events.ChatFilterHandler;
import com.skyhelper.events.HudRenderHandler;
import com.skyhelper.features.calculator.CalculatorFeature;
import com.skyhelper.features.alerts.AlertHandler;
import com.skyhelper.features.deployables.DeployableRenderer;
import com.skyhelper.features.deployables.DeployableTracker;
import com.skyhelper.features.hud.PestRepellentHandler;
import com.skyhelper.features.keybinds.KeybindHandler;
import com.skyhelper.gui.ConfigScreen;
import com.skyhelper.gui.HudEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class SkyHelperMod implements ClientModInitializer {

    public static final String MOD_ID = "skyhelper";
    private static boolean openConfigNextTick = false;

    private static final KeyMapping.Category SKYHELPER_CATEGORY = KeyMapping.Category.register(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("skyhelper", "keys")
    );

    private static final KeyMapping CALCULATOR_KEY = new KeyMapping(
            "key.skyhelper.calculator",
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            SKYHELPER_CATEGORY
    );

    private static final KeyMapping HUD_EDITOR_KEY = new KeyMapping(
            "key.skyhelper.hud_editor",
            GLFW.GLFW_KEY_F6,
            SKYHELPER_CATEGORY
    );

    @Override
    public void onInitializeClient() {
        SkyHelperConfig.load();

        KeyBindingHelper.registerKeyBinding(CALCULATOR_KEY);
        KeyBindingHelper.registerKeyBinding(HUD_EDITOR_KEY);

        CalculatorFeature.init();
        ChatFilterHandler.init();
        HudRenderHandler.init();
        KeybindHandler.init();
        AlertHandler.init();
        PestRepellentHandler.init();
        DeployableTracker.init();
        DeployableRenderer.init();

        // Register /skyhelper gui command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("skyhelper")
                    .then(ClientCommandManager.literal("gui")
                            .executes(context -> {
                                openConfigNextTick = true;
                                return 1;
                            })
                    )
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigNextTick) {
                openConfigNextTick = false;
                Minecraft.getInstance().setScreen(new ConfigScreen(null));
            }
            if (CALCULATOR_KEY.consumeClick()) {
                CalculatorFeature.getOverlay().toggleFromKeybind();
            }
            if (HUD_EDITOR_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new HudEditorScreen(null));
            }
        });
    }
}

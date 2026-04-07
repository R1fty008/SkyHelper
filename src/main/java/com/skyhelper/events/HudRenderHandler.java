package com.skyhelper.events;

import com.skyhelper.config.SkyHelperConfig;
import com.skyhelper.features.alerts.AlertRenderer;
import com.skyhelper.features.hud.PestRepellentOverlay;
import com.skyhelper.features.hud.PotionOverlay;
import com.skyhelper.features.hud.SkillOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class HudRenderHandler {

    public static void init() {
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            SkyHelperConfig config = SkyHelperConfig.get();
            if (config.skillOverlayEnabled) {
                SkillOverlay.render(graphics);
            }
            if (config.potionOverlayEnabled) {
                PotionOverlay.render(graphics);
            }
            if (config.pestRepellentTimerEnabled) {
                PestRepellentOverlay.render(graphics);
            }
            AlertRenderer.render(graphics);
        });
    }
}

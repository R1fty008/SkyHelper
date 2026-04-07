package com.skyhelper.events;

import com.skyhelper.config.SkyHelperConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

public class ChatFilterHandler {

    private static final String HYPERION_BLOCKED_MSG = "There are blocks in the way!";

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!SkyHelperConfig.get().hyperionFilterEnabled) return true;
            String text = message.getString();
            // Cancel the "There are blocks in the way!" message
            return !text.equals(HYPERION_BLOCKED_MSG);
        });
    }
}

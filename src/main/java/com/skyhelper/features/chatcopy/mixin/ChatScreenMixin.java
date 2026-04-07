package com.skyhelper.features.chatcopy.mixin;

import com.skyhelper.config.SkyHelperConfig;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (!SkyHelperConfig.get().copyChatEnabled) return;
        if (event.button() != 0) return;

        Minecraft mc = Minecraft.getInstance();

        // Require Ctrl + left-click
        boolean ctrlHeld = InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_RCONTROL);
        if (!ctrlHeld) return;

        ChatComponent chat = mc.gui.getChat();
        ChatComponentAccessor accessor = (ChatComponentAccessor) chat;

        double mouseX = event.x();
        double mouseY = event.y();
        double chatScale = accessor.invokeGetScale();
        int lineHeight = accessor.invokeGetLineHeight();

        // Convert screen Y to chat-relative Y (BOTTOM_MARGIN = 40)
        double relativeY = ((double) mc.getWindow().getGuiScaledHeight() - mouseY - 40.0) / chatScale;
        if (relativeY < 0) return;

        // Reject clicks outside the chat width
        int chatWidth = ChatComponent.getWidth(mc.options.chatWidth().get());
        if (mouseX < 0 || mouseX > chatWidth * chatScale + 10) return;

        // Calculate which trimmedMessages line was clicked
        int lineIndex = (int) (relativeY / (double) lineHeight + (double) accessor.getChatScrollbarPos());

        List<GuiMessage.Line> trimmed = accessor.getTrimmedMessages();
        if (lineIndex < 0 || lineIndex >= trimmed.size()) return;

        // Verify the line is actually visible on screen
        int maxVisible = Math.min(chat.getLinesPerPage(), trimmed.size());
        int visualLine = lineIndex - accessor.getChatScrollbarPos();
        if (visualLine < 0 || visualLine >= maxVisible) return;

        // Find the bottom of the message (endOfEntry=true is on the lowest-index line)
        int msgBottom = lineIndex;
        while (!trimmed.get(msgBottom).endOfEntry()) {
            if (msgBottom == 0) break;
            msgBottom--;
        }

        // Find the top of the message
        int msgTop = msgBottom;
        while (msgTop + 1 < trimmed.size() && !trimmed.get(msgTop + 1).endOfEntry()) {
            msgTop++;
        }

        // Extract visible text in reading order
        StringBuilder sb = new StringBuilder();
        for (int i = msgTop; i >= msgBottom; i--) {
            if (i < msgTop) sb.append(' ');
            trimmed.get(i).content().accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
        }

        String messageText = sb.toString().trim();
        if (messageText.isEmpty()) return;

        mc.keyboardHandler.setClipboard(messageText);

        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("Copied!"), true);
        }

        cir.setReturnValue(true);
    }
}

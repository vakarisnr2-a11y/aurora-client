package dev.aurora.client.chat;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.misc.BetterChat;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

/**
 * Every chat line passes through here before it is added to the chat (ChatHudMixin, head of
 * {@code ChatHud.addMessage(Text, MessageSignatureData, MessageIndicator)}). Modules may replace the text and the
 * indicator; the mixin then re-adds the changed message once, with this pipeline bypassed.
 */
public final class ChatPipeline {
    /** Replacement for a message. Reused, valid until the next call. */
    public static final class Result {
        public Text text;
        public MessageIndicator indicator;
    }

    private static final Result RESULT = new Result();
    private static boolean busy;

    private ChatPipeline() {
    }

    /** True while the mixin re-adds a processed message. */
    public static boolean busy() {
        return busy;
    }

    public static void begin() {
        busy = true;
    }

    public static void end() {
        busy = false;
    }

    /** @return the replacement, or null to add the message unchanged */
    public static Result process(ChatHud hud, Text message, MessageIndicator indicator) {
        if (AuroraClient.get() == null) return null;
        Text text = message;
        MessageIndicator ind = indicator;
        BetterChat better = AuroraClient.module(BetterChat.class);
        if (better != null && better.isEnabled()) text = better.process(hud, message, text);
        if (text == message && ind == indicator) return null;
        RESULT.text = text;
        RESULT.indicator = ind;
        return RESULT;
    }

    /** Chat history length (messages and wrapped lines) replacing vanilla's 100. */
    public static int historySize(int vanilla) {
        BetterChat better = AuroraClient.get() == null ? null : AuroraClient.module(BetterChat.class);
        return better == null ? vanilla : better.historySize(vanilla);
    }
}

package dev.aurora.client.module.modules.misc;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.chat.ChatSearchState;
import dev.aurora.client.mixin.ChatHudAccessor;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/** Click a chat message (with the chat open) to copy its text to the clipboard. */
public final class ChatCopy extends Module {
    public enum Button {
        RIGHT_CLICK("Right Click"), CTRL_CLICK("Ctrl + Left Click"), MIDDLE_CLICK("Middle Click");

        private final String label;

        Button(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final EnumSetting<Button> button = add(new EnumSetting<>("Button", "How to click a message to copy it", Button.RIGHT_CLICK));
    private final BooleanSetting stripDecorations = add(new BooleanSetting("Remove Timestamps",
        "Leave out Better Chat's timestamp and [xN] counter", true));
    private final BooleanSetting notify = add(new BooleanSetting("Notify", "Show a notification after copying", true));

    public ChatCopy() {
        super("Chat Copy", "Click a chat message (right-click by default) to copy it to the clipboard.", Category.MISC);
    }

    public boolean matches(int mouseButton) {
        return switch (button.get()) {
            case RIGHT_CLICK -> mouseButton == 1;
            case CTRL_CLICK -> mouseButton == 0 && Screen.hasControlDown();
            case MIDDLE_CLICK -> mouseButton == 2;
        };
    }

    /** Copies the message under the mouse. Returns false when there is no message there. */
    public boolean copyAt(ChatHud hud, double mouseX, double mouseY) {
        ChatHudAccessor chat = (ChatHudAccessor) hud;
        int line = chat.aurora$getMessageLineIndex(chat.aurora$toChatLineX(mouseX), chat.aurora$toChatLineY(mouseY));
        if (line < 0) return false;
        List<ChatHudLine.Visible> visible = chat.aurora$getVisibleMessages();
        List<ChatHudLine> messages = chat.aurora$getMessages();
        String text = null;
        int entry = entryIndex(visible, line);
        // The wrapped lines only map one-to-one onto messages while chat search is not hiding any.
        if (!ChatSearchState.isFiltering() && entry >= 0 && entry < messages.size()) text = messages.get(entry).content().getString();
        if (text == null) text = joinLines(visible, line);
        text = ChatAlerts.stripFormatting(text);
        if (stripDecorations.get()) text = BetterChat.strip(text);
        MinecraftClient.getInstance().keyboard.setClipboard(text);
        if (notify.get()) {
            String preview = text.length() > 40 ? text.substring(0, 39) + "…" : text;
            AuroraClient.notifications().post("Copied message", preview, NotificationManager.Type.SUCCESS, this);
        }
        return true;
    }

    /** Which message (0 = newest) a wrapped line belongs to: each message's last line carries {@code endOfEntry}. */
    public static int entryIndex(List<ChatHudLine.Visible> visible, int line) {
        int entries = 0;
        for (int i = 0; i <= line && i < visible.size(); i++) if (visible.get(i).endOfEntry()) entries++;
        return entries - 1;
    }

    /** [first, last] wrapped-line indices of the message containing {@code line} (newest-first list). */
    public static int[] entryLines(List<ChatHudLine.Visible> visible, int line) {
        int first = line;
        while (first > 0 && !visible.get(first).endOfEntry()) first--;
        int last = line;
        while (last + 1 < visible.size() && !visible.get(last + 1).endOfEntry()) last++;
        return new int[]{first, last};
    }

    /** The message's text rebuilt from its wrapped lines (oldest line first). */
    private static String joinLines(List<ChatHudLine.Visible> visible, int line) {
        int[] range = entryLines(visible, line);
        StringBuilder sb = new StringBuilder();
        for (int i = range[1]; i >= range[0]; i--) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') sb.append(' ');
            visible.get(i).content().accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
        }
        return sb.toString().trim();
    }
}

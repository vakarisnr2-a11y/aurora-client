package dev.aurora.client.module.modules.misc;

import dev.aurora.client.chat.ChatSearchState;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.mixin.ChatHudAccessor;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chat timestamps, stacking of repeated lines ("message [x3]") and a longer chat history.
 *
 * <p>Works on every line added to the chat (ChatPipeline). A repeated line replaces the previous copy, which is only
 * removed when it is still the newest message, so nothing else is ever deleted.</p>
 */
public final class BetterChat extends Module {
    public enum TimeFormat {
        HH_MM("HH:mm", "14:05"), HH_MM_SS("HH:mm:ss", "14:05:09"), H_MM_A("h:mm a", "2:05 PM");

        final DateTimeFormatter formatter;
        private final String example;

        TimeFormat(String pattern, String example) {
            this.formatter = DateTimeFormatter.ofPattern(pattern, Locale.ROOT);
            this.example = example;
        }

        @Override
        public String toString() {
            return example;
        }
    }

    /** Matches the decorations this module adds, so copied messages can be cleaned up. */
    private static final Pattern TIMESTAMP_PREFIX = Pattern.compile("^\\[\\d{1,2}:\\d{2}(:\\d{2})?( [AP]M)?] ");
    private static final Pattern STACK_SUFFIX = Pattern.compile(" \\[x\\d+]$");

    private final BooleanSetting timestamps = add(new BooleanSetting("Timestamps", "Show the time each message arrived", true));
    private final EnumSetting<TimeFormat> format = add(new EnumSetting<>("Time Format", "Timestamp format", TimeFormat.HH_MM))
        .visibleWhen(timestamps::get);
    private final ColorSetting timestampColor = add(new ColorSetting("Timestamp Color", "Color of the timestamp", 0xFF8A8F9E, false))
        .visibleWhen(timestamps::get);
    private final BooleanSetting stack = add(new BooleanSetting("Stack Repeats",
        "Merge a message that repeats the previous one into a single line with a counter, e.g. [x3]", true));
    private final NumberSetting history = add(new NumberSetting("History Size",
        "How many messages (and sent messages) the chat keeps; vanilla keeps 100", 500, 100, 2000, 50));

    private String lastPlain;
    private Text lastShown;
    private int repeats;

    public BetterChat() {
        super("Better Chat", "Chat timestamps, stacked repeated messages and a longer chat history.", Category.MISC);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    protected void onDisable() {
        lastPlain = null;
        lastShown = null;
        repeats = 0;
    }

    public int historySize(int vanilla) {
        return isEnabled() ? Math.max(vanilla, history.getInt()) : vanilla;
    }

    /**
     * Decorates a chat line and removes the previous copy when it repeats.
     *
     * @param original the message as received (compared for stacking)
     * @param text     the message after earlier pipeline steps (decorated further)
     */
    public Text process(ChatHud hud, Text original, Text text) {
        String plain = original.getString();
        int count = 1;
        if (stack.get() && plain.equals(lastPlain) && lastShown != null) {
            ChatHudAccessor chat = (ChatHudAccessor) hud;
            // While chat search hides lines, the wrapped lines no longer match the messages: rebuild them instead.
            boolean filtering = ChatSearchState.isFiltering();
            if (removeNewest(chat.aurora$getMessages(), filtering ? null : chat.aurora$getVisibleMessages(), lastShown)) {
                count = repeats + 1;
                if (filtering) chat.aurora$refresh();
            }
        }
        lastPlain = plain;
        repeats = count;
        if (!timestamps.get() && count == 1) {
            lastShown = text;
            return text;
        }
        MutableText out = Text.empty();
        if (timestamps.get()) {
            out.append(Text.literal(stamp(format.get(), LocalTime.now()) + " ")
                .styled(s -> s.withColor(TextColor.fromRgb(timestampColor.get() & 0xFFFFFF))));
        }
        out.append(text);
        if (count > 1) out.append(Text.literal(stackSuffix(count)).formatted(Formatting.GRAY));
        lastShown = out;
        return out;
    }

    public static String stamp(TimeFormat format, LocalTime time) {
        return "[" + format.formatter.format(time) + "]";
    }

    public static String stackSuffix(int count) {
        return " [x" + count + "]";
    }

    /** Removes the timestamp and stack counter this module adds (for copying a message). */
    public static String strip(String line) {
        return STACK_SUFFIX.matcher(TIMESTAMP_PREFIX.matcher(line).replaceFirst("")).replaceFirst("");
    }

    /**
     * Removes the newest chat message and its wrapped lines if it is {@code expected}.
     *
     * @param visible the wrapped lines, or null to leave them alone (the caller rebuilds them)
     * @return false (and changes nothing) when the newest message is something else
     */
    public static boolean removeNewest(List<ChatHudLine> messages, List<ChatHudLine.Visible> visible, Text expected) {
        if (messages.isEmpty() || messages.get(0).content() != expected) return false;
        messages.remove(0);
        // The newest message's lines are at the front, starting with its last line (endOfEntry).
        if (visible != null && !visible.isEmpty()) {
            visible.remove(0);
            while (!visible.isEmpty() && !visible.get(0).endOfEntry()) visible.remove(0);
        }
        return true;
    }
}

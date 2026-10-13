package dev.aurora.client.module.modules.misc;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Highlights chat messages that mention your name (or extra nicknames): the name is recolored, a colored bar marks
 * the message (hover it for "Mentions you") and an optional sound plays. Matching is case-insensitive and whole-word,
 * and your own messages are skipped.
 */
public final class ChatMentions extends Module {
    private static final long SOUND_COOLDOWN_MS = 500;

    private final StringSetting extraNames = add(new StringSetting("Extra Names",
        "Other names that count as a mention, comma-separated (your account name always counts)", "", 256));
    private final ColorSetting color = add(new ColorSetting("Color", "Highlight color", 0xFFFFC857, false));
    private final BooleanSetting bold = add(new BooleanSetting("Bold", "Make your name bold", true));
    private final BooleanSetting tint = add(new BooleanSetting("Tint Message", "Also color the rest of the message (parts without their own color)", false));
    private final BooleanSetting sound = add(new BooleanSetting("Sound", "Play a sound when you are mentioned", true));
    private final EnumSetting<ChatAlerts.AlertSound> soundType = add(new EnumSetting<>("Sound Type", "Sound to play", ChatAlerts.AlertSound.BELL))
        .visibleWhen(sound::get);
    private final NumberSetting volume = add(new NumberSetting("Volume", "Sound volume", 1.0, 0.1, 1.0, 0.05)).visibleWhen(sound::get);
    private final BooleanSetting ignoreOwn = add(new BooleanSetting("Ignore Own", "Do not highlight messages you sent", true));

    private long lastSoundMs;
    private long lastPreviewMs;
    private MessageIndicator indicator;
    private int indicatorColor = -1;

    public ChatMentions() {
        super("Chat Mentions", "Highlights chat messages that mention your name, with an optional sound.", Category.MISC);
    }

    /** The indicator bar for mention messages (rebuilt when the color changes). */
    public MessageIndicator indicator() {
        int c = color.get() & 0xFFFFFF;
        if (indicator == null || indicatorColor != c) {
            indicator = new MessageIndicator(c, null, Text.literal("Mentions you"), "Mention");
            indicatorColor = c;
        }
        return indicator;
    }

    /** Returns the highlighted message, or null when it does not mention you. Plays the sound for mentions. */
    public Text process(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String own = mc.getSession() == null ? null : mc.getSession().getUsername();
        List<String> names = names(own, extraNames.get());
        if (names.isEmpty()) return null;
        String plain = ChatAlerts.stripFormatting(message.getString());
        if (ignoreOwn.get() && own != null && ChatAlerts.isOwnMessage(plain, own)) return null;
        if (ChatAlerts.findKeyword(plain, names, false, true) == null) return null;
        Text out = highlight(message, names);
        long now = System.currentTimeMillis();
        if (sound.get() && now - lastSoundMs > SOUND_COOLDOWN_MS) {
            lastSoundMs = now;
            play();
        }
        return out;
    }

    private void play() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(soundType.get().event(), 1f, volume.getFloat()));
    }

    @Override
    protected void onSettingChanged(Setting<?> setting) {
        if (setting != soundType && setting != volume) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || !(mc.currentScreen instanceof ClickGuiScreen)) return;
        long now = System.currentTimeMillis();
        if (now - lastPreviewMs < 250) return;
        lastPreviewMs = now;
        play();
    }

    private Text highlight(Text message, List<String> names) {
        MutableText out = Text.empty();
        TextColor highlight = TextColor.fromRgb(color.get() & 0xFFFFFF);
        boolean b = bold.get();
        boolean t = tint.get();
        message.visit((style, segment) -> {
            List<int[]> hits = findMentions(segment, names);
            Style base = t && style.getColor() == null ? style.withColor(highlight) : style;
            Style name = style.withColor(highlight).withBold(b);
            int pos = 0;
            for (int[] hit : hits) {
                if (hit[0] > pos) out.append(Text.literal(carry(segment, pos) + segment.substring(pos, hit[0])).setStyle(base));
                out.append(Text.literal(segment.substring(hit[0], hit[1])).setStyle(name));
                pos = hit[1];
            }
            if (pos < segment.length()) out.append(Text.literal(carry(segment, pos) + segment.substring(pos)).setStyle(base));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** Legacy color codes from earlier in a split literal, so the text after a highlighted name keeps its color. */
    private static String carry(String segment, int pos) {
        return pos == 0 ? "" : activeCodes(segment, pos);
    }

    // ------------------------------------------------------------------ pure helpers

    /** Your account name plus the extra names, without blanks or duplicates. */
    public static List<String> names(String own, String extra) {
        List<String> out = new ArrayList<>();
        if (own != null && !own.isBlank()) out.add(own.trim());
        for (String n : ChatAlerts.parseKeywords(extra)) {
            boolean dup = false;
            for (String e : out) dup |= e.equalsIgnoreCase(n);
            if (!dup) out.add(n);
        }
        return out;
    }

    /** Whole-word, case-insensitive matches of any name in {@code text} as sorted, non-overlapping [start, end) ranges. */
    public static List<int[]> findMentions(String text, List<String> names) {
        List<int[]> hits = new ArrayList<>();
        String hay = text.toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < hay.length()) {
            int[] best = null;
            for (String name : names) {
                String needle = name.toLowerCase(Locale.ROOT);
                int at = hay.indexOf(needle, i);
                while (at >= 0 && !(boundary(hay, at - 1) && boundary(hay, at + needle.length()))) at = hay.indexOf(needle, at + 1);
                if (at >= 0 && (best == null || at < best[0] || (at == best[0] && at + needle.length() > best[1]))) {
                    best = new int[]{at, at + needle.length()};
                }
            }
            if (best == null) break;
            hits.add(best);
            i = best[1];
        }
        return hits;
    }

    private static boolean boundary(String s, int index) {
        if (index < 0 || index >= s.length()) return true;
        char c = s.charAt(index);
        return !(Character.isLetterOrDigit(c) || c == '_');
    }

    /** The legacy formatting codes (§x) active at {@code index} of a literal, e.g. "§a§l". */
    public static String activeCodes(String s, int index) {
        StringBuilder codes = new StringBuilder();
        for (int i = 0; i < Math.min(index, s.length()) - 1; i++) {
            if (s.charAt(i) != '§') continue;
            char c = Character.toLowerCase(s.charAt(i + 1));
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r') codes.setLength(0); // a color resets formats
            if (c != 'r') codes.append('§').append(c);
            i++;
        }
        return codes.toString();
    }
}

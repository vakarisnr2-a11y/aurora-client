package dev.aurora.client.module.modules.misc;

import dev.aurora.client.event.events.ChatReceiveEvent;
import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Plays a sound when a received chat message contains one of your keywords.
 *
 * <p>Keywords are comma-separated and empty by default (the module does nothing until you add
 * some). Color codes are ignored and your own messages are skipped, so typing a keyword does not
 * ping you. Action bar messages never trigger an alert.</p>
 */
public final class ChatAlerts extends Module {
    public enum AlertSound {
        PLING, BELL, CHIME, HARP, BIT, EXPERIENCE, LEVEL_UP, ANVIL;

        /** Resolved on use: touching SoundEvents earlier would need a bootstrapped game (e.g. in tests). */
        public SoundEvent event() {
            return switch (this) {
                case PLING -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
                case BELL -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
                case CHIME -> SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value();
                case HARP -> SoundEvents.BLOCK_NOTE_BLOCK_HARP.value();
                case BIT -> SoundEvents.BLOCK_NOTE_BLOCK_BIT.value();
                case EXPERIENCE -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
                case LEVEL_UP -> SoundEvents.ENTITY_PLAYER_LEVELUP;
                case ANVIL -> SoundEvents.BLOCK_ANVIL_LAND;
            };
        }
    }

    private static final long PREVIEW_INTERVAL_MS = 250;

    private final StringSetting keywords = add(new StringSetting("Keywords",
        "Comma-separated words or phrases that trigger the sound, e.g. bed, diamond, final kill", "", 256));
    private final EnumSetting<AlertSound> sound = add(new EnumSetting<>("Sound", "Sound to play", AlertSound.PLING));
    private final NumberSetting volume = add(new NumberSetting("Volume", "Alert volume", 1.0, 0.1, 1.0, 0.05));
    private final NumberSetting pitch = add(new NumberSetting("Pitch", "Alert pitch", 1.0, 0.5, 2.0, 0.05));
    private final BooleanSetting caseSensitive = add(new BooleanSetting("Case Sensitive", "Match upper/lower case exactly", false));
    private final BooleanSetting wholeWords = add(new BooleanSetting("Whole Words",
        "Only match complete words (\"bed\" will not match \"bedrock\")", false));
    private final BooleanSetting ignoreOwn = add(new BooleanSetting("Ignore Own", "Do not alert on messages you sent", true));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Minimum seconds between two alerts", 0.5, 0, 5, 0.1));

    private List<String> parsed = List.of();
    private String parsedFrom = "";
    private long lastAlertMs;
    private long lastPreviewMs;

    public ChatAlerts() {
        super("Chat Alerts", "Plays a sound when a chat message contains one of your keywords.", Category.MISC);
        on(ChatReceiveEvent.class, this::onChat);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    private void onChat(ChatReceiveEvent e) {
        if (e.overlay || e.message == null) return;
        List<String> list = keywordList();
        if (list.isEmpty()) return;
        MinecraftClient mc = mc();
        if (mc.player == null) return;
        String plain = stripFormatting(e.message.getString());
        if (ignoreOwn.get()) {
            // Signed chat names its sender; server-formatted system chat (most servers) needs the name heuristic.
            boolean own = e.sender != null ? e.sender.equals(mc.player.getUuid())
                : isOwnMessage(plain, mc.player.getGameProfile().getName());
            if (own) return;
        }
        if (findKeyword(plain, list, caseSensitive.get(), wholeWords.get()) == null) return;
        long now = System.currentTimeMillis();
        if (now - lastAlertMs < (long) (cooldown.get() * 1000)) return;
        lastAlertMs = now;
        play();
    }

    private void play() {
        mc().getSoundManager().play(PositionedSoundInstance.master(sound.get().event(), pitch.getFloat(), volume.getFloat()));
    }

    /** Previews the sound while it is being adjusted in the ClickGUI (not when a profile is loaded). */
    @Override
    protected void onSettingChanged(Setting<?> setting) {
        if (setting != sound && setting != volume && setting != pitch) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || !(mc.currentScreen instanceof ClickGuiScreen)) return;
        long now = System.currentTimeMillis();
        if (now - lastPreviewMs < PREVIEW_INTERVAL_MS) return;
        lastPreviewMs = now;
        play();
    }

    private List<String> keywordList() {
        String raw = keywords.get();
        if (!raw.equals(parsedFrom)) {
            parsed = parseKeywords(raw);
            parsedFrom = raw;
        }
        return parsed;
    }

    /** Splits a comma-separated keyword list, trimming entries and dropping empty ones. */
    public static List<String> parseKeywords(String raw) {
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String keyword = part.trim();
            if (!keyword.isEmpty()) out.add(keyword);
        }
        return List.copyOf(out);
    }

    /** Returns the first keyword found in {@code message}, or null if none matches. */
    public static String findKeyword(String message, List<String> keywords, boolean caseSensitive, boolean wholeWords) {
        String haystack = caseSensitive ? message : message.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            String needle = caseSensitive ? keyword : keyword.toLowerCase(Locale.ROOT);
            int i = haystack.indexOf(needle);
            while (i >= 0) {
                if (!wholeWords || (isBoundary(haystack, i - 1) && isBoundary(haystack, i + needle.length()))) return keyword;
                i = haystack.indexOf(needle, i + 1);
            }
        }
        return null;
    }

    private static boolean isBoundary(String s, int index) {
        return index < 0 || index >= s.length() || !Character.isLetterOrDigit(s.charAt(index));
    }

    /**
     * Whether a server-formatted chat line was written by {@code ownName}: "&lt;Name&gt; text" (vanilla) or
     * "... Name: text" (ranks, party/team prefixes and so on before the name).
     */
    public static boolean isOwnMessage(String plain, String ownName) {
        if (ownName == null || ownName.isEmpty()) return false;
        if (plain.startsWith("<" + ownName + ">")) return true;
        int colon = plain.indexOf(": ");
        if (colon < 0) return false;
        String head = plain.substring(0, colon);
        if (!head.endsWith(ownName)) return false;
        int before = head.length() - ownName.length() - 1;
        return before < 0 || !isNameChar(head.charAt(before));
    }

    private static boolean isNameChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    /** Removes legacy section-sign color codes that some servers put in literal text. */
    public static String stripFormatting(String s) {
        if (s.indexOf('§') < 0) return s;
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§') {
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}

package dev.aurora.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.aurora.client.util.ColorUtil;

import java.util.Locale;

/** An ARGB color with optional alpha channel and an optional animated rainbow mode. */
public final class ColorSetting extends Setting<Integer> {
    private final boolean allowAlpha;
    private boolean rainbow;

    public ColorSetting(String name, String description, int argb) {
        this(name, description, argb, true);
    }

    public ColorSetting(String name, String description, int argb, boolean allowAlpha) {
        super(name, description, allowAlpha ? argb : argb | 0xFF000000);
        this.allowAlpha = allowAlpha;
    }

    @Override
    protected Integer sanitize(Integer candidate) {
        return allowAlpha ? candidate : candidate | 0xFF000000;
    }

    public boolean allowsAlpha() {
        return allowAlpha;
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public void setRainbow(boolean rainbow) {
        if (this.rainbow != rainbow) {
            this.rainbow = rainbow;
            fireChanged();
        }
    }

    /** The color to draw with: the stored color, or the animated rainbow hue keeping the stored alpha. */
    public int getColor() {
        if (!rainbow) return value;
        return ColorUtil.withAlpha(ColorUtil.rainbow(0, 1.0f, 0.75f, 1f), ColorUtil.alpha(value));
    }

    /** Rainbow color with a phase offset, used for gradients across lines of text. */
    public int getColor(long offsetMs) {
        if (!rainbow) return value;
        return ColorUtil.withAlpha(ColorUtil.rainbow(offsetMs, 1.0f, 0.75f, 1f), ColorUtil.alpha(value));
    }

    @Override
    public void reset() {
        setRainbow(false);
        super.reset();
    }

    @Override
    public boolean isDefault() {
        return !rainbow && super.isDefault();
    }

    public static String toHex(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    /** Parses #RRGGBB, #AARRGGBB (with or without '#'). Returns null if invalid. */
    public static Integer parseHex(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (s.length() != 6 && s.length() != 8) return null;
        try {
            long parsed = Long.parseLong(s, 16);
            return s.length() == 6 ? (int) (parsed | 0xFF000000L) : (int) parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("value", toHex(value));
        obj.addProperty("rainbow", rainbow);
        return obj;
    }

    @Override
    public boolean load(JsonElement json) {
        if (json == null) return false;
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            boolean ok = loadValue(obj.get("value"));
            JsonElement rb = obj.get("rainbow");
            if (rb != null && rb.isJsonPrimitive() && rb.getAsJsonPrimitive().isBoolean()) {
                setRainbow(rb.getAsBoolean());
            } else if (rb != null) {
                ok = false;
            }
            return ok;
        }
        return loadValue(json);
    }

    private boolean loadValue(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) return false;
        JsonPrimitive p = json.getAsJsonPrimitive();
        if (p.isNumber()) {
            set(p.getAsInt());
            return true;
        }
        if (p.isString()) {
            Integer parsed = parseHex(p.getAsString());
            if (parsed != null) {
                set(parsed);
                return true;
            }
        }
        return false;
    }
}

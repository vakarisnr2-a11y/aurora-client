package dev.aurora.client.util;

/**
 * Remembers the string for the last number it formatted, so HUD values that rarely change (FPS, ping, counts) are
 * not turned into a new string every frame.
 */
public final class CachedText {
    private final String suffix;
    private int last = Integer.MIN_VALUE;
    private String text = "";

    public CachedText() {
        this("");
    }

    public CachedText(String suffix) {
        this.suffix = suffix;
    }

    public String of(int value) {
        if (value != last || text.isEmpty()) {
            last = value;
            text = suffix.isEmpty() ? String.valueOf(value) : value + suffix;
        }
        return text;
    }
}

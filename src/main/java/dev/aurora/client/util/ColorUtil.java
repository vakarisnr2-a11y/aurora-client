package dev.aurora.client.util;

/** Allocation-free ARGB color helpers. */
public final class ColorUtil {
    private ColorUtil() {
    }

    public static int alpha(int c) {
        return c >>> 24;
    }

    public static int red(int c) {
        return (c >> 16) & 0xFF;
    }

    public static int green(int c) {
        return (c >> 8) & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int withAlpha(int c, int alpha) {
        return (clamp(alpha) << 24) | (c & 0x00FFFFFF);
    }

    /** Multiplies the existing alpha by {@code factor} (0..1). */
    public static int fade(int c, float factor) {
        return withAlpha(c, Math.round(alpha(c) * Math.max(0f, Math.min(1f, factor))));
    }

    public static int lerp(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return argb(
            Math.round(alpha(from) + (alpha(to) - alpha(from)) * t),
            Math.round(red(from) + (red(to) - red(from)) * t),
            Math.round(green(from) + (green(to) - green(from)) * t),
            Math.round(blue(from) + (blue(to) - blue(from)) * t));
    }

    public static int brighten(int c, float amount) {
        return lerp(c, withAlpha(0xFFFFFF, alpha(c)), amount);
    }

    public static int darken(int c, float amount) {
        return lerp(c, withAlpha(0x000000, alpha(c)), amount);
    }

    /** HSB (all 0..1) to opaque RGB. */
    public static int hsb(float hue, float saturation, float brightness) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        int sector = (int) h;
        float f = h - sector;
        float p = brightness * (1f - saturation);
        float q = brightness * (1f - saturation * f);
        float t = brightness * (1f - saturation * (1f - f));
        float r, g, b;
        switch (sector) {
            case 0 -> { r = brightness; g = t; b = p; }
            case 1 -> { r = q; g = brightness; b = p; }
            case 2 -> { r = p; g = brightness; b = t; }
            case 3 -> { r = p; g = q; b = brightness; }
            case 4 -> { r = t; g = p; b = brightness; }
            default -> { r = brightness; g = p; b = q; }
        }
        return argb(255, Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    /** Converts RGB to HSB, writing into {@code out} (length 3). */
    public static float[] toHsb(int c, float[] out) {
        float r = red(c) / 255f, g = green(c) / 255f, b = blue(c) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0) hue = 0;
        else if (max == r) hue = ((g - b) / delta) / 6f;
        else if (max == g) hue = ((b - r) / delta + 2f) / 6f;
        else hue = ((r - g) / delta + 4f) / 6f;
        if (hue < 0) hue += 1f;
        out[0] = hue;
        out[1] = max == 0 ? 0 : delta / max;
        out[2] = max;
        return out;
    }

    /** Animated rainbow; one full cycle every 4 seconds divided by speed. */
    public static int rainbow(long offsetMs, float speed, float saturation, float brightness) {
        long period = (long) (4000 / Math.max(0.05f, speed));
        float hue = ((System.currentTimeMillis() + offsetMs) % period) / (float) period;
        return hsb(hue, saturation, brightness);
    }

    /** Red to green for 0..1 (used for health / durability). */
    public static int healthColor(float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        return hsb(fraction / 3f, 0.75f, 0.95f);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}

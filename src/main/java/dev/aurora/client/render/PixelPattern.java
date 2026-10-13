package dev.aurora.client.render;

import java.util.Locale;

/**
 * A 15x15 one-bit image for the pixel crosshair. Stored as 15 rows of 4 hex digits (bit x = column x), e.g.
 * {@code "0000...0080..."}, so it fits a single string setting and survives hand editing.
 */
public final class PixelPattern {
    public static final int SIZE = 15;
    public static final int CENTER = SIZE / 2;
    private static final int ENCODED_LENGTH = SIZE * 4;

    public enum Preset {
        CROSS, GAP_CROSS, DOT, CIRCLE, X, T_SHAPE
    }

    public static final String DEFAULT = encode(preset(Preset.GAP_CROSS));

    private PixelPattern() {
    }

    public static boolean[] empty() {
        return new boolean[SIZE * SIZE];
    }

    public static int index(int x, int y) {
        return y * SIZE + x;
    }

    public static String encode(boolean[] pixels) {
        StringBuilder sb = new StringBuilder(ENCODED_LENGTH);
        for (int y = 0; y < SIZE; y++) {
            int row = 0;
            for (int x = 0; x < SIZE; x++) if (pixels[index(x, y)]) row |= 1 << x;
            sb.append(String.format(Locale.ROOT, "%04x", row));
        }
        return sb.toString();
    }

    /** Decodes a pattern; returns null for text that is not a valid pattern. */
    public static boolean[] decode(String text) {
        if (text == null || text.length() != ENCODED_LENGTH) return null;
        boolean[] out = empty();
        for (int y = 0; y < SIZE; y++) {
            int row;
            try {
                row = Integer.parseInt(text.substring(y * 4, y * 4 + 4), 16);
            } catch (NumberFormatException e) {
                return null;
            }
            if (row >= 1 << SIZE) return null;
            for (int x = 0; x < SIZE; x++) out[index(x, y)] = (row & (1 << x)) != 0;
        }
        return out;
    }

    public static int count(boolean[] pixels) {
        int n = 0;
        for (boolean p : pixels) if (p) n++;
        return n;
    }

    public static boolean[] invert(boolean[] pixels) {
        boolean[] out = empty();
        for (int i = 0; i < out.length; i++) out[i] = !pixels[i];
        return out;
    }

    /** The cells a brush stroke at (x, y) touches with optional horizontal/vertical mirroring (1, 2 or 4 cells). */
    public static int[] mirrored(int x, int y, boolean mirrorX, boolean mirrorY) {
        int mx = SIZE - 1 - x;
        int my = SIZE - 1 - y;
        if (mirrorX && mirrorY) return new int[]{index(x, y), index(mx, y), index(x, my), index(mx, my)};
        if (mirrorX) return new int[]{index(x, y), index(mx, y)};
        if (mirrorY) return new int[]{index(x, y), index(x, my)};
        return new int[]{index(x, y)};
    }

    public static boolean[] preset(Preset preset) {
        boolean[] p = empty();
        int c = CENTER;
        switch (preset) {
            case CROSS -> {
                for (int i = c - 4; i <= c + 4; i++) {
                    p[index(i, c)] = true;
                    p[index(c, i)] = true;
                }
            }
            case GAP_CROSS -> {
                for (int i = 2; i <= 5; i++) {
                    p[index(c - i, c)] = true;
                    p[index(c + i, c)] = true;
                    p[index(c, c - i)] = true;
                    p[index(c, c + i)] = true;
                }
            }
            case DOT -> {
                p[index(c, c)] = true;
            }
            case CIRCLE -> {
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        double d = Math.hypot(x - c, y - c);
                        if (d >= 3.5 && d < 4.5) p[index(x, y)] = true;
                    }
                }
                p[index(c, c)] = true;
            }
            case X -> {
                for (int i = 2; i <= 4; i++) {
                    p[index(c - i, c - i)] = true;
                    p[index(c + i, c - i)] = true;
                    p[index(c - i, c + i)] = true;
                    p[index(c + i, c + i)] = true;
                }
            }
            case T_SHAPE -> {
                for (int i = 2; i <= 5; i++) {
                    p[index(c - i, c)] = true;
                    p[index(c + i, c)] = true;
                    p[index(c, c + i)] = true;
                }
            }
        }
        return p;
    }

    /**
     * Merges each row's set pixels into horizontal runs, written as {x, y, w, h} quadruples (in pixel units, relative
     * to the pattern's top-left corner) into {@code out}. Returns the number of runs.
     */
    public static int runs(boolean[] pixels, float[] out) {
        int n = 0;
        for (int y = 0; y < SIZE; y++) {
            int x = 0;
            while (x < SIZE) {
                if (!pixels[index(x, y)]) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < SIZE && pixels[index(x, y)]) x++;
                out[n * 4] = start;
                out[n * 4 + 1] = y;
                out[n * 4 + 2] = x - start;
                out[n * 4 + 3] = 1;
                n++;
            }
        }
        return n;
    }

    /** Upper bound of {@link #runs}: at most 8 runs per row. */
    public static int maxRuns() {
        return SIZE * ((SIZE + 1) / 2);
    }
}

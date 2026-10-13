package dev.aurora.client.gui.menu;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/** The animated Aurora backdrop: a deep night gradient, slowly drifting aurora ribbons in the theme accent and stars. */
public final class AuroraBackground {
    private static final int STARS = 70;
    private static final float[] STAR_A = new float[STARS * 4];
    private static final float[] STAR_B = new float[STARS * 4];
    private static final float[] SEEDS = new float[STARS * 3];

    static {
        long seed = 0x5DEECE66DL;
        for (int i = 0; i < SEEDS.length; i++) {
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
            SEEDS[i] = (seed >>> 17) / (float) (1L << 31);
        }
    }

    private AuroraBackground() {
    }

    public static void render(DrawContext ctx, int width, int height) {
        render(ctx, width, height, 1f);
    }

    /** @param alpha 0..1, for fading in */
    public static void render(DrawContext ctx, int width, int height, float alpha) {
        double t = (System.currentTimeMillis() % 3_600_000L) / 1000.0;
        int accent = Theme.accent();
        Render2D.gradientV(ctx, 0, 0, width, height, ColorUtil.fade(0xFF05060C, alpha), ColorUtil.fade(0xFF0D1020, alpha));
        // Stars in two brightness groups that twinkle out of phase.
        int na = 0, nb = 0;
        for (int i = 0; i < STARS; i++) {
            float x = SEEDS[i * 3] * width;
            float y = SEEDS[i * 3 + 1] * height * 0.75f;
            float size = SEEDS[i * 3 + 2] > 0.85f ? 1.5f : 1f;
            float[] target = (i & 1) == 0 ? STAR_A : STAR_B;
            int n = (i & 1) == 0 ? na++ : nb++;
            target[n * 4] = x;
            target[n * 4 + 1] = y;
            target[n * 4 + 2] = size;
            target[n * 4 + 3] = size;
        }
        float twinkle = (float) (0.5 + 0.5 * Math.sin(t * 1.3));
        Render2D.rects(ctx, 0, 0, 1, 0, STAR_A, na, ColorUtil.withAlpha(0xFFFFFF, Math.round((70 + 90 * twinkle) * alpha)));
        Render2D.rects(ctx, 0, 0, 1, 0, STAR_B, nb, ColorUtil.withAlpha(0xFFFFFF, Math.round((70 + 90 * (1 - twinkle)) * alpha)));
        // Aurora ribbons: the accent, a cooler and a warmer neighbour hue.
        float[] hsb = ColorUtil.toHsb(accent, new float[3]);
        int cool = ColorUtil.hsb(hsb[0] - 0.12f, Math.min(1, hsb[1] + 0.1f), 0.95f);
        int warm = ColorUtil.hsb(hsb[0] + 0.08f, hsb[1], 0.9f);
        float h = height;
        Render2D.wave(ctx, width, h * 0.30f, h * 0.05f, h * 0.22f, width * 0.9f, (float) (t * 0.35), ColorUtil.withAlpha(cool, Math.round(90 * alpha)), 96);
        Render2D.wave(ctx, width, h * 0.38f, h * 0.06f, h * 0.18f, width * 0.7f, (float) (-t * 0.27 + 1.7), ColorUtil.withAlpha(accent, Math.round(110 * alpha)), 96);
        Render2D.wave(ctx, width, h * 0.47f, h * 0.04f, h * 0.14f, width * 1.2f, (float) (t * 0.2 + 3.1), ColorUtil.withAlpha(warm, Math.round(70 * alpha)), 96);
        // Darken towards the bottom so buttons stay readable.
        Render2D.gradientV(ctx, 0, h * 0.55f, width, h * 0.45f, 0x00000000, ColorUtil.fade(0xB0000000, alpha));
    }
}

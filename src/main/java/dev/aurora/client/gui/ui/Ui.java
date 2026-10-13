package dev.aurora.client.gui.ui;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * Aurora's design system: one palette, radii, spacing and the shared controls (windows, cards, buttons, switches,
 * sliders, fields) used by the ClickGUI, editors, notifications and menus. Text goes through {@link UiFont}.
 */
public final class Ui {
    // Palette. Window, sidebar, surface and accent colors come from the ClickGUI theme preset (see Theme).
    public static final int BORDER = 0x1AFFFFFF;
    public static final int BORDER_STRONG = 0x33FFFFFF;
    public static final int TEXT = 0xFFF1F3F9;
    public static final int SUBTEXT = 0xFFA3AAC0;
    public static final int MUTED = 0xFF6B7287;
    public static final int SUCCESS = 0xFF34D399;
    public static final int WARNING = 0xFFFBBF24;
    public static final int DANGER = 0xFFF87171;

    // Shape and spacing.
    public static final float R_SMALL = 4;
    public static final float R_MEDIUM = 6;
    public static final float R_LARGE = 10;
    public static final int PAD = 12;
    public static final int GAP = 8;

    private Ui() {
    }

    public static int accent() {
        return Theme.accent();
    }

    /** Accent at the given alpha (0..255). */
    public static int accent(int alpha) {
        return ColorUtil.withAlpha(Theme.accent(), alpha);
    }

    /** Lighter accent for gradients. */
    public static int accentLight() {
        return ColorUtil.brighten(Theme.accent(), 0.35f);
    }

    /** Raised surface (cards, small buttons, chips) in the current theme preset. */
    public static int surface() {
        return Theme.cardColor();
    }

    public static int surfaceHover() {
        return ColorUtil.brighten(Theme.cardColor(), 0.04f);
    }

    /** Recessed input fields and dropdown lists in the current theme preset. */
    public static int field() {
        return ColorUtil.darken(Theme.windowColor(), 0.18f);
    }

    /** A floating window: soft shadow, background, hairline border. */
    public static void window(DrawContext ctx, float x, float y, float w, float h, float radius) {
        Render2D.shadow(ctx, x, y, w, h, radius, 10, 0xA0000000);
        Render2D.roundedRect(ctx, x, y, w, h, radius, Theme.windowColor());
        Render2D.roundedOutline(ctx, x, y, w, h, radius, 1, BORDER);
    }

    /** A card; {@code hover} and {@code active} are 0..1 animation values. */
    public static void card(DrawContext ctx, float x, float y, float w, float h, float hover, float active) {
        int card = Theme.cardColor();
        int bg = ColorUtil.lerp(card, ColorUtil.brighten(card, 0.06f), hover);
        bg = ColorUtil.lerp(bg, ColorUtil.lerp(bg, accent(0xFF), 0.10f), active);
        Render2D.roundedRect(ctx, x, y, w, h, R_MEDIUM, bg);
        int border = ColorUtil.lerp(BORDER, BORDER_STRONG, hover);
        border = ColorUtil.lerp(border, accent(0xA0), active);
        Render2D.roundedOutline(ctx, x, y, w, h, R_MEDIUM, 1, border);
    }

    /** A text button; {@code hover} 0..1. Primary buttons are filled with the accent. */
    public static void button(DrawContext ctx, float x, float y, float w, float h, String label, float hover, boolean primary) {
        int bg = primary ? ColorUtil.lerp(accent(0xD0), accent(0xFF), hover)
            : ColorUtil.lerp(surface(), surfaceHover(), hover);
        Render2D.roundedRect(ctx, x, y, w, h, Math.min(R_MEDIUM, h / 2f), bg);
        if (!primary) Render2D.roundedOutline(ctx, x, y, w, h, Math.min(R_MEDIUM, h / 2f), 1, ColorUtil.lerp(BORDER, BORDER_STRONG, hover));
        int color = primary ? 0xFFFFFFFF : ColorUtil.lerp(SUBTEXT, TEXT, hover);
        UiFont.draw(ctx, label, x + (w - UiFont.width(label)) / 2f, y + (h - 8) / 2f, color);
    }

    public static final float SWITCH_W = 24;
    public static final float SWITCH_H = 13;

    /** A switch scaled by {@code scale} (for dense lists). */
    public static void toggle(DrawContext ctx, float x, float y, float on, float scale) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        toggle(ctx, 0, 0, on);
        ctx.getMatrices().pop();
    }

    /** A toggle switch at (x, y); {@code on} 0..1 animates the knob. */
    public static void toggle(DrawContext ctx, float x, float y, float on) {
        int track = ColorUtil.lerp(0xFF2A3042, accent(0xFF), on);
        Render2D.roundedRect(ctx, x, y, SWITCH_W, SWITCH_H, SWITCH_H / 2f, track);
        float knob = SWITCH_H - 4;
        Render2D.circle(ctx, x + 2 + knob / 2f + on * (SWITCH_W - knob - 4), y + SWITCH_H / 2f, knob / 2f, 0xFFF7F8FC);
    }

    /** A slider track with fill and thumb; {@code progress} 0..1, {@code hover} 0..1. */
    public static void slider(DrawContext ctx, float x, float y, float w, float progress, float hover) {
        float p = Math.max(0, Math.min(1, progress));
        Render2D.roundedRect(ctx, x, y, w, 4, 2, 0xFF262B3B);
        Render2D.roundedRect(ctx, x, y, Math.max(4, w * p), 4, 2, accent(0xFF));
        float thumb = 4.5f + hover * 1f;
        Render2D.circle(ctx, x + w * p, y + 2, thumb + 1, accent(0x60));
        Render2D.circle(ctx, x + w * p, y + 2, thumb, 0xFFF7F8FC);
    }

    /** An input field background; {@code focus} 0..1. */
    public static void field(DrawContext ctx, float x, float y, float w, float h, float focus) {
        Render2D.roundedRect(ctx, x, y, w, h, Math.min(R_MEDIUM, h / 2f), field());
        Render2D.roundedOutline(ctx, x, y, w, h, Math.min(R_MEDIUM, h / 2f), 1, ColorUtil.lerp(BORDER, accent(0xFF), focus));
    }

    /** A small rounded label, e.g. a key name. */
    public static float chip(DrawContext ctx, float x, float y, String label, int textColor, int bg) {
        float w = UiFont.width(label) + 10;
        Render2D.roundedRect(ctx, x, y, w, 13, R_SMALL, bg);
        UiFont.draw(ctx, label, x + 5, y + 2.5f, textColor);
        return w;
    }

    public static void divider(DrawContext ctx, float x, float y, float w) {
        Render2D.rect(ctx, x, y, w, 1, BORDER);
    }

    /** Wraps {@code text} into at most {@code maxLines} lines of {@code width}. */
    public static String[] wrap(String text, int width, int maxLines) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (UiFont.width(candidate) <= width || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        if (lines.size() > maxLines) {
            String last = lines.get(maxLines - 1) + " " + lines.get(maxLines);
            lines = new java.util.ArrayList<>(lines.subList(0, maxLines));
            lines.set(maxLines - 1, UiFont.trim(last, width, UiFont.Size.BODY, false));
        }
        return lines.toArray(new String[0]);
    }
}

package dev.aurora.client.hud;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.module.modules.visuals.HudTheme;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * The shared look of every HUD element, driven by the {@link HudTheme} module: background, corner radius, shadow,
 * outline, accent, padding, text colors and text shadow. All methods are allocation-free.
 *
 * <p>Fading (enable/disable animations) is applied by {@link HudRenderer} through the shader color, so elements draw
 * with fully opaque colors and never need to know their own alpha.</p>
 */
public final class HudStyle {
    /** Default padding, kept for code that lays out before a theme exists. */
    public static final int PADDING = 4;
    private static final int WIDTH_CACHE_SIZE = 512;
    private static final Map<String, Integer> WIDTHS = new HashMap<>();
    private static final int DEFAULT_TEXT = 0xFFECEEF4;
    private static final int MUTED = 0xFF6B7186;

    private static HudTheme theme;

    private HudStyle() {
    }

    /** The HUD Theme module, or null before the client is initialized (unit tests). */
    private static HudTheme theme() {
        if (theme == null && AuroraClient.get() != null) theme = AuroraClient.module(HudTheme.class);
        return theme;
    }

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    // ------------------------------------------------------------------ theme values

    public static int padding() {
        HudTheme t = theme();
        return t == null ? PADDING : t.padding();
    }

    public static float radius() {
        HudTheme t = theme();
        return t == null ? 5 : t.radius();
    }

    public static int accent() {
        HudTheme t = theme();
        return t != null && t.customAccent() ? t.accent() : Theme.accent();
    }

    /** Accent with a phase offset for animated rainbow accents (module list rows). */
    public static int accent(long offsetMs) {
        HudTheme t = theme();
        return t != null && t.customAccent() ? t.accent() : Theme.accent(offsetMs);
    }

    /** Color of values and normal text. */
    public static int text() {
        HudTheme t = theme();
        return t == null ? DEFAULT_TEXT : t.textColor();
    }

    /** Color of labels such as "FPS" or "Ping". */
    public static int label() {
        HudTheme t = theme();
        return t == null || t.accentLabels() ? accent() : text();
    }

    /** Dimmer text for secondary information (times, units). */
    public static int sub() {
        return ColorUtil.lerp(text(), MUTED, 0.45f);
    }

    public static int background() {
        HudTheme t = theme();
        return t == null ? 0xA80B0E17 : t.background();
    }

    /** Background for inner cells (keys, icon boxes): the theme background, never fully invisible. */
    public static int cell() {
        int bg = background();
        return ColorUtil.alpha(bg) >= 0x50 ? bg : ColorUtil.withAlpha(bg, 0x50);
    }

    public static boolean textShadow() {
        HudTheme t = theme();
        return t == null || t.textShadow();
    }

    public static boolean animations() {
        HudTheme t = theme();
        return t == null || t.animations();
    }

    public static double animationSpeed() {
        HudTheme t = theme();
        return t == null ? 1 : t.animationSpeed();
    }

    public static int lineHeight() {
        return font().fontHeight + padding() * 2 - 1;
    }

    // ------------------------------------------------------------------ drawing

    /** The themed panel behind an element: shadow, background, outline and accent bar. */
    public static void background(DrawContext ctx, float x, float y, float w, float h) {
        if (w <= 0 || h <= 0) return;
        HudTheme t = theme();
        float r = Math.min(radius(), Math.min(w, h) / 2f);
        int bg = background();
        int accent = accent();
        if (t == null || t.shadow()) {
            float size = t == null ? 4 : t.shadowSize();
            int color = t != null && t.glow() ? ColorUtil.withAlpha(accent, 150) : 0x80000000;
            Render2D.shadow(ctx, x, y, w, h, r, size, color);
        }
        if (ColorUtil.alpha(bg) > 0) {
            if (t != null && t.gradient()) {
                Render2D.roundedRect(ctx, x, y, w, h, r, ColorUtil.brighten(bg, 0.12f), ColorUtil.darken(bg, 0.12f));
            } else {
                Render2D.roundedRect(ctx, x, y, w, h, r, bg);
            }
        }
        if (t == null || t.outline()) {
            Render2D.roundedOutline(ctx, x, y, w, h, r, 1, t == null ? 0x1CFFFFFF : t.outlineAccent() ? accent : t.outlineColor());
        }
        HudTheme.AccentBar bar = t == null ? HudTheme.AccentBar.NONE : t.accentBar();
        switch (bar) {
            case BOTTOM -> Render2D.rect(ctx, x + r * 0.5f + 1.5f, y + h - 1, w - r - 3, 1, ColorUtil.fade(accent, 0.85f));
            case TOP -> Render2D.rect(ctx, x + r * 0.5f + 1.5f, y, w - r - 3, 1, ColorUtil.fade(accent, 0.85f));
            case LEFT -> Render2D.roundedRect(ctx, x + 1.5f, y + 3, 1.5f, h - 6, 0.75f, accent);
            case NONE -> {
            }
        }
    }

    /** A rounded inner box (keystroke key, icon slot) using the theme radius. */
    public static void cell(DrawContext ctx, float x, float y, float w, float h, int color) {
        Render2D.roundedRect(ctx, x, y, w, h, Math.min(radius(), Math.min(w, h) / 2f), color);
        HudTheme t = theme();
        if (t != null && t.outline()) {
            Render2D.roundedOutline(ctx, x, y, w, h, Math.min(radius(), Math.min(w, h) / 2f), 1,
                t.outlineAccent() ? accent() : t.outlineColor());
        }
    }

    /** A thin progress bar: dark track and a fill of {@code fraction} (0..1). */
    public static void bar(DrawContext ctx, float x, float y, float w, float h, float fraction, int color) {
        float r = h / 2f;
        Render2D.roundedRect(ctx, x, y, w, h, r, 0x70000000);
        float f = Math.max(0f, Math.min(1f, fraction));
        if (f > 0) Render2D.roundedRect(ctx, x, y, Math.max(h, w * f), h, r, color, ColorUtil.darken(color, 0.2f));
    }

    /** Draws text with the theme's text shadow. Text whose alpha is too low to draw is skipped. */
    public static void text(DrawContext ctx, String s, float x, float y, int color) {
        text(ctx, s, x, y, color, textShadow());
    }

    public static void text(DrawContext ctx, String s, float x, float y, int color, boolean shadow) {
        if (ColorUtil.alpha(color) < 4 || s.isEmpty()) return; // the font treats alpha below 4 as opaque
        if (auroraFont()) {
            ctx.drawText(font(), UiFont.text(s, UiFont.styleFor(ctx, UiFont.Size.BODY, false)), Math.round(x), Math.round(y), color, shadow);
        } else {
            ctx.drawText(font(), s, Math.round(x), Math.round(y), color, shadow);
        }
    }

    /** Styled text (scoreboard lines, names); the HUD font is applied where the text has no font of its own. */
    public static void text(DrawContext ctx, Text t, float x, float y, int color, boolean shadow) {
        if (ColorUtil.alpha(color) < 4) return;
        ctx.drawText(font(), auroraFont() ? t.copy().fillStyle(UiFont.styleFor(ctx, UiFont.Size.BODY, false)) : t,
            Math.round(x), Math.round(y), color, shadow);
    }

    /**
     * Width of HUD text in the HUD font. Use this (never TextRenderer#getWidth) for HUD layout. Aurora-font widths are
     * cached because HUD elements measure the same few strings every frame.
     */
    public static int width(String s) {
        if (s.isEmpty()) return 0;
        if (!auroraFont()) return font().getWidth(s);
        Integer cached = WIDTHS.get(s);
        if (cached != null) return cached;
        if (WIDTHS.size() >= WIDTH_CACHE_SIZE) WIDTHS.clear(); // changing values (timers, coordinates) churn through
        int w = font().getWidth(UiFont.text(s, UiFont.measuringStyle(UiFont.Size.BODY, false)));
        WIDTHS.put(s, w);
        return w;
    }

    /** {@code s} cut to {@code maxWidth} HUD-font pixels with an ellipsis. */
    public static String trim(String s, int maxWidth) {
        if (width(s) <= maxWidth) return s;
        int end = s.length();
        while (end > 0 && width(s.substring(0, end) + "…") > maxWidth) end--;
        return s.substring(0, end) + "…";
    }

    public static int width(Text t) {
        return auroraFont() ? font().getWidth(t.copy().fillStyle(UiFont.measuringStyle(UiFont.Size.BODY, false))) : font().getWidth(t);
    }

    /** Whether HUD text uses Aurora's interface font (the default) instead of Minecraft's. */
    public static boolean auroraFont() {
        HudTheme t = theme();
        return t != null && t.font() == HudTheme.Font.AURORA;
    }

    /** One-line "label value" pill without animations. Returns the width. */
    public static int drawLabelValue(DrawContext ctx, String label, String value, int valueColor, boolean background) {
        int pad = padding();
        int labelW = label.isEmpty() ? 0 : width(label) + 4;
        int w = labelW + width(value) + pad * 2;
        if (background) background(ctx, 0, 0, w, lineHeight());
        if (!label.isEmpty()) text(ctx, label, pad, pad, label());
        text(ctx, value, pad + labelW, pad, valueColor);
        return w;
    }
}

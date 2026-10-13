package dev.aurora.client.gui.ui;

import dev.aurora.client.AuroraClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Identifier;

/**
 * Aurora's interface font (Poppins, SIL OFL 1.1) in two sizes and two weights. Menus, the ClickGUI, notifications and
 * editor screens draw all their text through this class; HUD elements use it through {@link
 * dev.aurora.client.hud.HudStyle} unless HUD Theme's Font is set to Minecraft.
 *
 * <p>Minecraft rasterizes TrueType glyphs once at a fixed oversampling and samples them without filtering, so each
 * size is defined at several oversampling factors (fonts {@code aurora:ui_N}, {@code aurora:ui_title_N}, ...) and the
 * one matching the current pixel scale (GUI scale times any matrix scale) is used. That keeps text sharp at every GUI
 * scale.</p>
 */
public final class UiFont {
    public enum Size {
        /** Regular interface text (about the height of Minecraft's font). */
        BODY("ui"),
        /** Screen and section titles. */
        TITLE("ui_title");

        final String base;

        Size(String base) {
            this.base = base;
        }
    }

    private static final int[] OVERSAMPLES = {1, 2, 3, 4, 6};
    /** [size][bold][oversample index] */
    private static final Style[][][] STYLES = new Style[2][2][OVERSAMPLES.length];

    static {
        for (Size size : Size.values()) {
            for (int bold = 0; bold < 2; bold++) {
                for (int i = 0; i < OVERSAMPLES.length; i++) {
                    String id = size.base + (bold == 1 ? "_bold_" : "_") + OVERSAMPLES[i];
                    STYLES[size.ordinal()][bold][i] = Style.EMPTY.withFont(Identifier.of(AuroraClient.MOD_ID, id));
                }
            }
        }
    }

    private UiFont() {
    }

    private static TextRenderer tr() {
        return MinecraftClient.getInstance().textRenderer;
    }

    /** Index of the oversampling closest to the actual pixel scale. */
    public static int oversampleIndex(double pixelScale) {
        int best = 0;
        for (int i = 1; i < OVERSAMPLES.length; i++) {
            if (Math.abs(OVERSAMPLES[i] - pixelScale) < Math.abs(OVERSAMPLES[best] - pixelScale)) best = i;
        }
        return best;
    }

    private static Style style(DrawContext ctx, Size size, boolean bold) {
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        if (ctx != null) scale *= Math.abs(ctx.getMatrices().peek().getPositionMatrix().m00());
        return STYLES[size.ordinal()][bold ? 1 : 0][oversampleIndex(scale)];
    }

    private static Style measureStyle(Size size, boolean bold) {
        return STYLES[size.ordinal()][bold ? 1 : 0][1];
    }

    /** {@code s} in {@code style}, with legacy § formatting codes applied like Minecraft's own string drawing. */
    public static OrderedText text(String s, Style style) {
        return visitor -> TextVisitFactory.visitFormatted(s, style, visitor);
    }

    /** The style for drawing at the current matrix scale (exposed for HUD text). */
    public static Style styleFor(DrawContext ctx, Size size, boolean bold) {
        return style(ctx, size, bold);
    }

    /** The style used for measuring. */
    public static Style measuringStyle(Size size, boolean bold) {
        return measureStyle(size, bold);
    }

    public static int width(String s) {
        return width(s, Size.BODY, false);
    }

    public static int width(String s, Size size, boolean bold) {
        return tr().getWidth(text(s, measureStyle(size, bold)));
    }

    /** Line height in GUI pixels. */
    public static int height(Size size) {
        return size == Size.TITLE ? 13 : 9;
    }

    public static void draw(DrawContext ctx, String s, float x, float y, int color) {
        draw(ctx, s, x, y, color, Size.BODY, false);
    }

    public static void bold(DrawContext ctx, String s, float x, float y, int color) {
        draw(ctx, s, x, y, color, Size.BODY, true);
    }

    public static void title(DrawContext ctx, String s, float x, float y, int color) {
        draw(ctx, s, x, y, color, Size.TITLE, true);
    }

    public static void draw(DrawContext ctx, String s, float x, float y, int color, Size size, boolean bold) {
        if ((color >>> 24) < 4 || s.isEmpty()) return;
        ctx.drawText(tr(), text(s, style(ctx, size, bold)), Math.round(x), Math.round(y), color, false);
    }

    public static void centered(DrawContext ctx, String s, float centerX, float y, int color, Size size, boolean bold) {
        draw(ctx, s, centerX - width(s, size, bold) / 2f, y, color, size, bold);
    }

    /** {@code s} cut to {@code maxWidth} with an ellipsis. */
    public static String trim(String s, int maxWidth, Size size, boolean bold) {
        if (width(s, size, bold) <= maxWidth) return s;
        String ellipsis = "…";
        int end = s.length();
        while (end > 0 && width(s.substring(0, end) + ellipsis, size, bold) > maxWidth) end--;
        return s.substring(0, end) + ellipsis;
    }
}

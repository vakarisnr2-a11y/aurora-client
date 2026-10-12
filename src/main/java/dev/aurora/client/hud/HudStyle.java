package dev.aurora.client.hud;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Shared look for simple text HUD elements: a rounded translucent pill with label and value. */
public final class HudStyle {
    public static final int PADDING = 4;

    private HudStyle() {
    }

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    /** Draws "label value" in a pill and returns {width, height}. */
    public static int drawLabelValue(DrawContext ctx, String label, String value, int valueColor, boolean background) {
        TextRenderer tr = font();
        int labelW = label.isEmpty() ? 0 : tr.getWidth(label) + 4;
        int w = labelW + tr.getWidth(value) + PADDING * 2;
        int h = tr.fontHeight + PADDING * 2 - 1;
        if (background) panel(ctx, 0, 0, w, h);
        if (!label.isEmpty()) ctx.drawText(tr, label, PADDING, PADDING, Theme.accent(), true);
        ctx.drawText(tr, value, PADDING + labelW, PADDING, valueColor, true);
        return w;
    }

    public static int lineHeight() {
        return font().fontHeight + PADDING * 2 - 1;
    }

    public static void panel(DrawContext ctx, float x, float y, float w, float h) {
        Render2D.roundedRect(ctx, x, y, w, h, Theme.radius(), Theme.hudBackground());
        Render2D.rect(ctx, x + 1.5f, y + h - 1f, w - 3f, 1f, ColorUtil.fade(Theme.accent(), 0.85f));
    }
}

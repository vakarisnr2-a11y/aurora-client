package dev.aurora.client.hud;

import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * A themed one-line "label value" element (FPS, Ping, CPS...). The background width eases toward the text width and,
 * when enabled, a changed value slides in from below while the old one fades out upwards.
 *
 * <p>One instance per HUD element; it only keeps the last value and an animation, so drawing allocates nothing.</p>
 */
public final class HudLine {
    /** Duration of a value transition at animation speed 1. */
    static final long TRANSITION_MS = 180;
    /** Vertical travel of a sliding value. */
    static final float SLIDE = 4;

    private final boolean animateValue;
    private final SmoothBox box = new SmoothBox();
    private String value;
    private String previous;
    private int previousColor;
    private int lastColor;
    private long changedAt;

    /** @param animateValue false for values that change every tick (coordinates, speed), which only animate the width */
    public HudLine(boolean animateValue) {
        this.animateValue = animateValue;
    }

    /** Draws the line at (0,0) and returns its current width. */
    public int draw(DrawContext ctx, String label, String value, int valueColor, boolean background) {
        TextRenderer tr = HudStyle.font();
        int pad = HudStyle.padding();
        int labelW = label.isEmpty() ? 0 : tr.getWidth(label) + 4;
        int h = HudStyle.lineHeight();
        box.update(labelW + tr.getWidth(value) + pad * 2, h);
        float w = box.width();
        if (background) HudStyle.background(ctx, 0, 0, w, h);
        if (!label.isEmpty()) HudStyle.text(ctx, label, pad, pad, HudStyle.label());
        drawValue(ctx, value, pad + labelW, pad, valueColor);
        return Math.round(w);
    }

    /** Draws a value at (x, y), sliding between the previous and the new value when it changes. */
    public void drawValue(DrawContext ctx, String newValue, float x, float y, int color) {
        long now = System.currentTimeMillis();
        if (value == null) {
            value = newValue;
        } else if (!value.equals(newValue)) {
            previous = value;
            previousColor = lastColor;
            value = newValue;
            changedAt = now;
        }
        lastColor = color;
        float t = previous == null || !animateValue || !HudStyle.animations() ? 1f
            : progress(now - changedAt, HudStyle.animationSpeed());
        if (t >= 1f) {
            previous = null;
            HudStyle.text(ctx, value, x, y, color);
            return;
        }
        HudStyle.text(ctx, previous, x, y - SLIDE * t, ColorUtil.fade(previousColor, 1f - t));
        HudStyle.text(ctx, value, x, y + SLIDE * (1f - t), ColorUtil.fade(color, t));
    }

    /** Eased 0..1 progress of a value transition {@code elapsedMs} after the change. */
    public static float progress(long elapsedMs, double speed) {
        double duration = TRANSITION_MS / Math.max(0.05, speed);
        return (float) Animation.easeOutCubic(elapsedMs / duration);
    }

    /** The current (animated) width, for elements that draw more next to the line. */
    public float width() {
        return box.width();
    }
}

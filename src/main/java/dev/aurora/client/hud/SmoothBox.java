package dev.aurora.client.hud;

import dev.aurora.client.util.Animation;

/**
 * Size of a HUD panel that eases toward its content size, so backgrounds grow and shrink smoothly when text
 * changes width or rows appear. Jumps straight to the first size and whenever HUD animations are off.
 */
public final class SmoothBox {
    private final Animation width = new Animation(0, 18);
    private final Animation height = new Animation(0, 18);
    private boolean started;

    public void update(float targetWidth, float targetHeight) {
        boolean animate = HudStyle.animations();
        if (!started || !animate) {
            width.snap(targetWidth);
            height.snap(targetHeight);
            started = true;
            return;
        }
        double speed = 18 * HudStyle.animationSpeed();
        width.setSpeed(speed);
        height.setSpeed(speed);
        width.setTarget(targetWidth);
        height.setTarget(targetHeight);
        width.update();
        height.update();
    }

    public float width() {
        return width.getFloat();
    }

    public float height() {
        return height.getFloat();
    }

    /** Width rounded up, for {@code setSize}. */
    public int intWidth() {
        return (int) Math.ceil(width.get() - 1e-3);
    }

    public int intHeight() {
        return (int) Math.ceil(height.get() - 1e-3);
    }
}

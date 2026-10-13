package dev.aurora.client.util;

/**
 * Frame-rate independent exponential easing toward a target value.
 *
 * <p>{@code value += (target - value) * (1 - e^(-speed * dt))} converges at the same visual
 * speed regardless of FPS, and needs no allocation per frame.</p>
 */
public final class Animation {
    private double value;
    private double target;
    private double speed;
    private long lastUpdate = -1;

    public Animation(double initial, double speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public Animation setTarget(double target) {
        this.target = target;
        return this;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    /** Jump to a value without animating. */
    public void snap(double v) {
        this.value = v;
        this.target = v;
    }

    public double update() {
        long now = System.nanoTime();
        if (lastUpdate < 0) lastUpdate = now;
        double dt = (now - lastUpdate) / 1_000_000_000d;
        // A long gap means the animation was not drawn for a while (screen closed, HUD hidden): continue from where
        // it stopped instead of jumping most of the way in one frame.
        if (dt > 0.25) dt = 1 / 60d;
        lastUpdate = now;
        value += (target - value) * (1 - Math.exp(-speed * dt));
        if (Math.abs(target - value) < 1e-4) value = target;
        return value;
    }

    public double get() {
        return value;
    }

    public float getFloat() {
        return (float) value;
    }

    public double getTarget() {
        return target;
    }

    public boolean isDone() {
        return value == target;
    }

    public static double easeOutCubic(double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }
}

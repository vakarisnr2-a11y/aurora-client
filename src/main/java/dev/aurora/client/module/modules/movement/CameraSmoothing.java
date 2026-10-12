package dev.aurora.client.module.modules.movement;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Smooths mouse look. Incoming rotation is accumulated and released exponentially over time,
 * using the real frame time so the feel is the same at any FPS.
 */
public final class CameraSmoothing extends Module {
    private final NumberSetting smoothness = add(new NumberSetting("Smoothness", "Higher values glide longer", 0.5, 0.05, 0.95, 0.05));

    private double pendingX;
    private double pendingY;
    private long lastFrame = -1;

    public CameraSmoothing() {
        super("Camera Smoothing", "Cinematic, frame-rate independent smoothing of mouse look.", Category.MOVEMENT);
    }

    public void apply(ClientPlayerEntity player, double dx, double dy) {
        pendingX += dx;
        pendingY += dy;
        long now = System.nanoTime();
        double dt = lastFrame < 0 ? 1 / 60.0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        double speed = 3 + (1 - smoothness.get()) * 40;
        double alpha = 1 - Math.exp(-speed * dt);
        double stepX = pendingX * alpha;
        double stepY = pendingY * alpha;
        pendingX -= stepX;
        pendingY -= stepY;
        player.changeLookDirection(stepX, stepY);
    }

    @Override
    protected void onDisable() {
        pendingX = 0;
        pendingY = 0;
        lastFrame = -1;
    }
}

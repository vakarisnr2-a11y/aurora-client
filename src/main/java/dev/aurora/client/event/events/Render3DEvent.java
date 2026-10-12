package dev.aurora.client.event.events;

import dev.aurora.client.event.Event;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Fired at the end of world rendering. Geometry drawn through {@link dev.aurora.client.render.Render3D}
 * uses coordinates relative to {@link #cameraPos}; {@link #view} holds the camera rotation and
 * {@link #projection} the world projection matrix (including view bobbing).
 */
public final class Render3DEvent extends Event {
    public static final Render3DEvent INSTANCE = new Render3DEvent();

    public float tickDelta;
    public Camera camera;
    public Vec3d cameraPos = Vec3d.ZERO;
    public final Matrix4f view = new Matrix4f();
    public final Matrix4f projection = new Matrix4f();

    public Render3DEvent set(float tickDelta, Camera camera) {
        resetCancelled();
        this.tickDelta = tickDelta;
        this.camera = camera;
        this.cameraPos = camera.getPos();
        return this;
    }
}

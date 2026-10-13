package dev.aurora.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.event.events.Render3DEvent;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4fStack;

/**
 * World-space line and box rendering for {@link Render3DEvent} handlers.
 *
 * <p>{@link #begin} loads the camera rotation into the model-view matrix, so callers emit
 * vertices relative to the camera position. Lines use vanilla's {@code rendertype_lines} shader,
 * which expands each segment into a screen-space quad, so the width is honoured on every driver
 * (plain GL line widths above 1 are not supported in core profile).</p>
 */
public final class Render3D {
    private Render3D() {
    }

    public static void begin(Render3DEvent event, boolean depthTest) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.view);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (depthTest) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
    }

    public static void end() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    public static BufferBuilder lines(float width) {
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(width);
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
    }

    public static BufferBuilder quads() {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    /** One line segment; the normal tells the lines shader the segment direction. */
    public static void line(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len;
        dy /= len;
        dz /= len;
        b.vertex(x1, y1, z1).color(color).normal(dx, dy, dz);
        b.vertex(x2, y2, z2).color(color).normal(dx, dy, dz);
    }

    /** The 12 edges of a box. */
    public static void boxEdges(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        line(b, x1, y1, z1, x2, y1, z1, c);
        line(b, x2, y1, z1, x2, y1, z2, c);
        line(b, x2, y1, z2, x1, y1, z2, c);
        line(b, x1, y1, z2, x1, y1, z1, c);
        line(b, x1, y2, z1, x2, y2, z1, c);
        line(b, x2, y2, z1, x2, y2, z2, c);
        line(b, x2, y2, z2, x1, y2, z2, c);
        line(b, x1, y2, z2, x1, y2, z1, c);
        line(b, x1, y1, z1, x1, y2, z1, c);
        line(b, x2, y1, z1, x2, y2, z1, c);
        line(b, x2, y1, z2, x2, y2, z2, c);
        line(b, x1, y1, z2, x1, y2, z2, c);
    }

    public static void boxFaces(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        // bottom / top
        b.vertex(x1, y1, z1).color(c); b.vertex(x2, y1, z1).color(c); b.vertex(x2, y1, z2).color(c); b.vertex(x1, y1, z2).color(c);
        b.vertex(x1, y2, z1).color(c); b.vertex(x1, y2, z2).color(c); b.vertex(x2, y2, z2).color(c); b.vertex(x2, y2, z1).color(c);
        // north / south
        b.vertex(x1, y1, z1).color(c); b.vertex(x1, y2, z1).color(c); b.vertex(x2, y2, z1).color(c); b.vertex(x2, y1, z1).color(c);
        b.vertex(x1, y1, z2).color(c); b.vertex(x2, y1, z2).color(c); b.vertex(x2, y2, z2).color(c); b.vertex(x1, y2, z2).color(c);
        // west / east
        b.vertex(x1, y1, z1).color(c); b.vertex(x1, y1, z2).color(c); b.vertex(x1, y2, z2).color(c); b.vertex(x1, y2, z1).color(c);
        b.vertex(x2, y1, z1).color(c); b.vertex(x2, y2, z1).color(c); b.vertex(x2, y2, z2).color(c); b.vertex(x2, y1, z2).color(c);
    }

    public static void draw(BufferBuilder b) {
        BuiltBuffer built = b.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }
}

package dev.aurora.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import dev.aurora.client.util.ColorUtil;

/**
 * Immediate-mode 2D shapes with sub-pixel precision (rounded rectangles, gradients, circles).
 *
 * <p>Vanilla {@link DrawContext} only offers integer rectangles. These helpers build geometry
 * with the position-color shader using the context's current matrix, so they respect any
 * translation/scale applied by the caller (GUI scale, animations). Pending DrawContext batches are
 * flushed first to keep draw order correct. Culling is disabled while drawing because the
 * triangle fans are emitted clockwise in screen space.</p>
 */
public final class Render2D {
    private Render2D() {
    }

    private static BufferBuilder begin(DrawContext ctx, VertexFormat.DrawMode mode) {
        ctx.draw();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        return Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR);
    }

    private static void end(BufferBuilder buffer) {
        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void rect(DrawContext ctx, float x, float y, float w, float h, int color) {
        gradient(ctx, x, y, w, h, color, color, color, color);
    }

    public static void gradientV(DrawContext ctx, float x, float y, float w, float h, int top, int bottom) {
        gradient(ctx, x, y, w, h, top, top, bottom, bottom);
    }

    public static void gradientH(DrawContext ctx, float x, float y, float w, float h, int left, int right) {
        gradient(ctx, x, y, w, h, left, right, right, left);
    }

    /** Quad with per-corner colors: top-left, top-right, bottom-right, bottom-left. */
    public static void gradient(DrawContext ctx, float x, float y, float w, float h, int tl, int tr, int br, int bl) {
        if (w <= 0 || h <= 0) return;
        BufferBuilder b = begin(ctx, VertexFormat.DrawMode.QUADS);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        b.vertex(m, x, y, 0).color(tl);
        b.vertex(m, x, y + h, 0).color(bl);
        b.vertex(m, x + w, y + h, 0).color(br);
        b.vertex(m, x + w, y, 0).color(tr);
        end(b);
    }

    public static void roundedRect(DrawContext ctx, float x, float y, float w, float h, float radius, int color) {
        roundedRect(ctx, x, y, w, h, radius, color, color);
    }

    /** Rounded rectangle with a vertical gradient, drawn as one triangle fan. */
    public static void roundedRect(DrawContext ctx, float x, float y, float w, float h, float radius, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        float r = Math.max(0, Math.min(radius, Math.min(w, h) / 2f));
        BufferBuilder b = begin(ctx, VertexFormat.DrawMode.TRIANGLE_FAN);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        b.vertex(m, x + w / 2f, y + h / 2f, 0).color(ColorUtil.lerp(top, bottom, 0.5f));
        int segments = segments(r);
        // Corners clockwise starting at the top-left. Angles in screen space (y points down).
        corner(b, m, x + r, y + r, r, 180, segments, y, h, top, bottom);
        corner(b, m, x + w - r, y + r, r, 270, segments, y, h, top, bottom);
        corner(b, m, x + w - r, y + h - r, r, 0, segments, y, h, top, bottom);
        corner(b, m, x + r, y + h - r, r, 90, segments, y, h, top, bottom);
        // Close the fan on the first perimeter vertex.
        b.vertex(m, x, y + r, 0).color(ColorUtil.lerp(top, bottom, r / h));
        end(b);
    }

    private static void corner(BufferBuilder b, Matrix4f m, float cx, float cy, float r, int startDeg, int segments,
                               float top, float height, int topColor, int bottomColor) {
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg + 90.0 * i / segments);
            float px = cx + (float) Math.cos(a) * r;
            float py = cy + (float) Math.sin(a) * r;
            b.vertex(m, px, py, 0).color(ColorUtil.lerp(topColor, bottomColor, (py - top) / height));
        }
    }

    /** Rounded outline of the given thickness (drawn as a triangle strip between two rounded paths). */
    public static void roundedOutline(DrawContext ctx, float x, float y, float w, float h, float radius, float thickness, int color) {
        if (w <= 0 || h <= 0) return;
        float r = Math.max(0, Math.min(radius, Math.min(w, h) / 2f));
        float ri = Math.max(0, r - thickness);
        BufferBuilder b = begin(ctx, VertexFormat.DrawMode.TRIANGLE_STRIP);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        int segments = segments(r);
        float[][] centers = {{x + r, y + r}, {x + w - r, y + r}, {x + w - r, y + h - r}, {x + r, y + h - r}};
        int[] starts = {180, 270, 0, 90};
        for (int c = 0; c < 4; c++) {
            for (int i = 0; i <= segments; i++) {
                double a = Math.toRadians(starts[c] + 90.0 * i / segments);
                float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
                b.vertex(m, centers[c][0] + cos * r, centers[c][1] + sin * r, 0).color(color);
                b.vertex(m, centers[c][0] + cos * ri, centers[c][1] + sin * ri, 0).color(color);
            }
        }
        b.vertex(m, x, y + r, 0).color(color);
        b.vertex(m, x + thickness, y + r, 0).color(color);
        end(b);
    }

    /** Soft drop shadow: a few expanding, increasingly transparent rounded rectangles. */
    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float radius, float size, int color) {
        int layers = 4;
        for (int i = layers; i >= 1; i--) {
            float grow = size * i / layers;
            int c = ColorUtil.fade(color, 0.35f / i);
            roundedRect(ctx, x - grow, y - grow + size * 0.25f, w + grow * 2, h + grow * 2, radius + grow, c);
        }
    }

    public static void circle(DrawContext ctx, float cx, float cy, float radius, int color) {
        BufferBuilder b = begin(ctx, VertexFormat.DrawMode.TRIANGLE_FAN);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        b.vertex(m, cx, cy, 0).color(color);
        int segments = Math.max(12, (int) (radius * 4));
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2 * i / segments;
            b.vertex(m, cx + (float) Math.cos(a) * radius, cy + (float) Math.sin(a) * radius, 0).color(color);
        }
        end(b);
    }

    /** Ring (circle outline) used by the circle crosshair. */
    public static void ring(DrawContext ctx, float cx, float cy, float radius, float thickness, int color) {
        BufferBuilder b = begin(ctx, VertexFormat.DrawMode.TRIANGLE_STRIP);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        int segments = Math.max(16, (int) (radius * 5));
        float inner = Math.max(0, radius - thickness);
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2 * i / segments;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            b.vertex(m, cx + cos * radius, cy + sin * radius, 0).color(color);
            b.vertex(m, cx + cos * inner, cy + sin * inner, 0).color(color);
        }
        end(b);
    }

    private static int segments(float r) {
        return Math.max(2, Math.min(10, (int) Math.ceil(r * 1.5f)));
    }
}

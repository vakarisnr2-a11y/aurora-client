package dev.aurora.client.gui;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudRenderer;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Drag HUD elements with the mouse. Elements snap to screen edges and centre lines (hold Shift
 * to disable snapping). Scroll resizes, right-click hides, R resets the hovered element.
 */
public final class HudEditorScreen extends Screen {
    private static final float SNAP = 5;

    private final Screen parent;
    private HudModule dragging;
    private double offsetX;
    private double offsetY;
    private float guideX = -1;
    private float guideY = -1;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    private static List<HudModule> elements() {
        return AuroraClient.hud().getElements();
    }

    private HudModule elementAt(double mx, double my) {
        List<HudModule> list = elements();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule h = list.get(i);
            if (!h.isEnabled()) continue;
            float x = h.getX(width), y = h.getY(height);
            if (mx >= x - 2 && mx <= x + h.getScaledWidth() + 2 && my >= y - 2 && my <= y + h.getScaledHeight() + 2) return h;
        }
        return null;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x55000000);
        int grid = 20;
        for (int x = 0; x < width; x += grid) ctx.fill(x, 0, x + 1, height, 0x0FFFFFFF);
        for (int y = 0; y < height; y += grid) ctx.fill(0, y, width, y + 1, 0x0FFFFFFF);
        ctx.fill(width / 2, 0, width / 2 + 1, height, 0x22FFFFFF);
        ctx.fill(0, height / 2, width, height / 2 + 1, 0x22FFFFFF);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        if (dragging != null) drag(mouseX, mouseY);
        HudModule hovered = dragging != null ? dragging : elementAt(mouseX, mouseY);
        for (HudModule h : elements()) {
            if (!h.isEnabled()) continue;
            HudRenderer.renderElement(ctx, h, delta, width, height, true);
            float x = h.getX(width), y = h.getY(height);
            float w = h.getScaledWidth(), hh = h.getScaledHeight();
            boolean active = h == hovered;
            int color = active ? Theme.accent() : 0x50FFFFFF;
            Render2D.roundedOutline(ctx, x - 2, y - 2, w + 4, hh + 4, 3, 1, color);
            if (active) {
                String label = h.getName() + " · " + Math.round(h.getScale() * 100) + "%";
                int lw = textRenderer.getWidth(label);
                float ly = y - 14 < 0 ? y + hh + 4 : y - 14;
                Render2D.roundedRect(ctx, x - 2, ly, lw + 8, 11, 3, ColorUtil.withAlpha(Theme.accent(), 210));
                ctx.drawText(textRenderer, label, (int) x + 2, (int) ly + 2, 0xFF0E0F14, false);
            }
        }
        if (guideX >= 0) Render2D.rect(ctx, guideX - 0.5f, 0, 1, height, ColorUtil.withAlpha(Theme.accent(), 180));
        if (guideY >= 0) Render2D.rect(ctx, 0, guideY - 0.5f, width, 1, ColorUtil.withAlpha(Theme.accent(), 180));

        String help = "Drag: move · Shift: no snap · Scroll: resize · Right-click: hide · R: reset · Esc: done";
        int hw = textRenderer.getWidth(help) + 16;
        Render2D.roundedRect(ctx, width / 2f - hw / 2f, height - 22, hw, 16, 8, Theme.panel());
        ctx.drawText(textRenderer, help, width / 2 - hw / 2 + 8, height - 18, Theme.SUBTEXT, false);
    }

    private void drag(double mouseX, double mouseY) {
        float w = dragging.getScaledWidth();
        float h = dragging.getScaledHeight();
        float x = (float) (mouseX - offsetX);
        float y = (float) (mouseY - offsetY);
        guideX = -1;
        guideY = -1;
        if (!hasShiftDown()) {
            // Snap left/right edges and the horizontal centre; same vertically.
            if (Math.abs(x) < SNAP) { x = 0; guideX = 0; }
            else if (Math.abs(x + w - width) < SNAP) { x = width - w; guideX = width; }
            else if (Math.abs(x + w / 2 - width / 2f) < SNAP) { x = width / 2f - w / 2; guideX = width / 2f; }
            if (Math.abs(y) < SNAP) { y = 0; guideY = 0; }
            else if (Math.abs(y + h - height) < SNAP) { y = height - h; guideY = height; }
            else if (Math.abs(y + h / 2 - height / 2f) < SNAP) { y = height / 2f - h / 2; guideY = height / 2f; }
        }
        x = Math.max(0, Math.min(width - w, x));
        y = Math.max(0, Math.min(height - h, y));
        dragging.setPosition(x, y, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        HudModule h = elementAt(mouseX, mouseY);
        if (h == null) return false;
        if (button == 0) {
            dragging = h;
            offsetX = mouseX - h.getX(width);
            offsetY = mouseY - h.getY(height);
        } else if (button == 1) {
            h.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        guideX = -1;
        guideY = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudModule h = elementAt(mouseX, mouseY);
        if (h == null) return false;
        h.getScaleSetting().increment(verticalAmount > 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R && client != null) {
            HudModule h = elementAt(client.mouse.getX() * width / client.getWindow().getWidth(),
                client.mouse.getY() * height / client.getWindow().getHeight());
            if (h != null) {
                h.resetPosition();
                h.getScaleSetting().reset();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

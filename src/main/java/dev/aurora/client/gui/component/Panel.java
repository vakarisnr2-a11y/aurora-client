package dev.aurora.client.gui.component;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A draggable, collapsible, scrollable window. Position and open state are stored in hidden
 * ClickGUI settings, so the layout persists in profiles.
 */
public class Panel {
    public static final float WIDTH = 122;
    public static final float HEADER = 20;

    protected final ClickGuiScreen screen;
    protected final List<Component> children = new ArrayList<>();
    private final String title;
    private final String icon;
    private final NumberSetting xSetting;
    private final NumberSetting ySetting;
    private final BooleanSetting openSetting;
    private final Animation openAnim;
    private final Animation scrollAnim = new Animation(0, 18);
    private double scrollTarget;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private float bodyHeight;
    private float contentHeight;

    public Panel(ClickGuiScreen screen, String key, String title, String icon) {
        this.screen = screen;
        this.title = title;
        this.icon = icon;
        this.xSetting = screen.settings().panelX(key);
        this.ySetting = screen.settings().panelY(key);
        this.openSetting = screen.settings().panelOpen(key);
        this.openAnim = new Animation(openSetting.get() ? 1 : 0, 14);
    }

    public List<Component> getChildren() {
        return children;
    }

    public float getX() {
        return xSetting.getFloat();
    }

    public float getY() {
        return ySetting.getFloat();
    }

    public boolean hasPosition() {
        return xSetting.get() >= 0 && ySetting.get() >= 0;
    }

    public void setPosition(float x, float y) {
        float maxX = Math.max(0, screen.guiWidth() - WIDTH);
        float maxY = Math.max(0, screen.guiHeight() - HEADER);
        xSetting.set((double) Math.max(0, Math.min(maxX, x)));
        ySetting.set((double) Math.max(0, Math.min(maxY, y)));
    }

    public boolean isOpen() {
        return openSetting.get();
    }

    public void setOpen(boolean open) {
        openSetting.set(open);
    }

    public float getTotalHeight() {
        return HEADER + bodyHeight + 3;
    }

    public void render(DrawContext ctx, double mx, double my, float delta, String query) {
        if (dragging) setPosition((float) (mx - dragOffsetX), (float) (my - dragOffsetY));
        float x = getX();
        float y = getY();
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        double speed = screen.animationSpeed();

        contentHeight = 0;
        int matching = 0;
        for (Component c : children) {
            if (c instanceof ModuleButton mb) {
                mb.tick(query);
                if (mb.matches(query)) matching++;
            }
            contentHeight += c.getHeight();
        }
        openAnim.setSpeed(14 * speed);
        openAnim.setTarget(isOpen() ? 1 : 0);
        float o = (float) openAnim.update();
        float maxBody = Math.max(60, screen.guiHeight() - y - HEADER - 10);
        float visible = Math.min(contentHeight, maxBody);
        bodyHeight = visible * o;
        double maxScroll = Math.max(0, contentHeight - visible);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scrollAnim.setSpeed(18 * speed);
        scrollAnim.setTarget(scrollTarget);
        float scroll = (float) scrollAnim.update();

        boolean dim = !query.isEmpty() && matching == 0 && hasModules();
        float alpha = dim ? 0.45f : 1f;
        float radius = Theme.radius() + 1;
        float total = getTotalHeight();
        Render2D.shadow(ctx, x, y, WIDTH, total, radius, 6, ColorUtil.fade(0x90000000, alpha));
        Render2D.roundedRect(ctx, x, y, WIDTH, total, radius, ColorUtil.fade(Theme.panel(), alpha));
        Render2D.roundedRect(ctx, x, y, WIDTH, HEADER, radius, ColorUtil.fade(Theme.header(), alpha),
            ColorUtil.fade(ColorUtil.lerp(Theme.header(), Theme.panel(), 0.6f), alpha));
        int accent = Theme.accent();
        Render2D.gradientH(ctx, x + 8, y + HEADER - 1, (WIDTH - 16) / 2, 1, ColorUtil.withAlpha(accent, 0), accent);
        Render2D.gradientH(ctx, x + WIDTH / 2, y + HEADER - 1, (WIDTH - 16) / 2, 1, accent, ColorUtil.withAlpha(accent, 0));
        ctx.drawText(tr, icon, (int) x + 7, (int) y + 6, ColorUtil.fade(accent, alpha), false);
        ctx.drawText(tr, title, (int) x + 19, (int) y + 6, ColorUtil.fade(Theme.TEXT, alpha), false);
        String count = hasModules() ? (query.isEmpty() ? String.valueOf(children.size()) : matching + "/" + children.size()) : "";
        String arrow = isOpen() ? "▾" : "▸";
        String right = count.isEmpty() ? arrow : count + " " + arrow;
        ctx.drawText(tr, right, (int) (x + WIDTH - 7 - tr.getWidth(right)), (int) y + 6, Theme.MUTED, false);

        if (bodyHeight > 0.5f) {
            float top = y + HEADER;
            screen.setMouseInBody(mx >= x && mx < x + WIDTH && my >= top && my < top + bodyHeight);
            ctx.enableScissor(screen.toScreen(x), screen.toScreen(top), screen.toScreen(x + WIDTH), screen.toScreen(top + bodyHeight));
            float cy = top + 1 - scroll;
            for (Component c : children) {
                float h = c.getHeight();
                c.setBounds(x + 2, cy, WIDTH - 4);
                if (h > 0.01f && cy + h >= top && cy <= top + bodyHeight) c.render(ctx, mx, my, delta);
                cy += h;
            }
            ctx.disableScissor();
            if (maxScroll > 0) {
                float barH = Math.max(12, bodyHeight * visible / contentHeight);
                float barY = top + (float) (scroll / maxScroll) * (bodyHeight - barH);
                Render2D.roundedRect(ctx, x + WIDTH - 3, barY, 2, barH, 1, ColorUtil.withAlpha(accent, 140));
            }
            screen.setMouseInBody(false);
        }
    }

    private boolean hasModules() {
        return !children.isEmpty() && children.get(0) instanceof ModuleButton;
    }

    public boolean isOverHeader(double mx, double my) {
        return mx >= getX() && mx < getX() + WIDTH && my >= getY() && my < getY() + HEADER;
    }

    public boolean isOver(double mx, double my) {
        return mx >= getX() && mx < getX() + WIDTH && my >= getY() && my < getY() + getTotalHeight();
    }

    private boolean isOverBody(double mx, double my) {
        float top = getY() + HEADER;
        return mx >= getX() && mx < getX() + WIDTH && my >= top && my < top + bodyHeight;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (isOverHeader(mx, my)) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = mx - getX();
                dragOffsetY = my - getY();
            } else if (button == 1) {
                setOpen(!isOpen());
            }
            return true;
        }
        if (isOverBody(mx, my)) {
            for (Component c : children) {
                if (c.getHeight() > 0.01f && c.mouseClicked(mx, my, button)) return true;
            }
            return true;
        }
        // Expanded dropdowns / pickers may still receive clicks for "click outside" handling.
        for (Component c : children) c.mouseClicked(Double.NaN, Double.NaN, -1);
        return false;
    }

    public void mouseReleased(double mx, double my, int button) {
        dragging = false;
        for (Component c : children) c.mouseReleased(mx, my, button);
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isOverBody(mx, my)) return false;
        for (Component c : children) {
            if (c.getHeight() > 0.01f && c.mouseScrolled(mx, my, amount)) return true;
        }
        scrollTarget -= amount * 24;
        return true;
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        for (Component c : children) if (c.keyPressed(key, scancode, modifiers)) return true;
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        for (Component c : children) if (c.charTyped(chr, modifiers)) return true;
        return false;
    }

    public String getTooltip(double mx, double my) {
        if (!isOverBody(mx, my)) return null;
        for (Component c : children) {
            if (c.getHeight() > 0.01f && c.isHovered(mx, my)) return c.getTooltip(mx, my);
        }
        return null;
    }

    /** Scrolls so that the given child is inside the visible body. */
    public void ensureVisible(Component target) {
        float offset = 0;
        for (Component c : children) {
            if (c == target) break;
            offset += c.getHeight();
        }
        float visible = Math.max(20, bodyHeight);
        if (offset < scrollTarget) scrollTarget = offset;
        else if (offset + ModuleButton.ROW_HEIGHT > scrollTarget + visible) scrollTarget = offset + ModuleButton.ROW_HEIGHT - visible;
    }
}

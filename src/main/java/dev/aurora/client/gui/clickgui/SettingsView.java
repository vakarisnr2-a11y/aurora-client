package dev.aurora.client.gui.clickgui;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.ui.Icons;
import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.util.Animation;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** A module's settings page: back button, name, description, switch and the list of setting controls. */
public final class SettingsView {
    private static final float HEADER = 56;

    private final ClickGuiScreen screen;
    private final Module module;
    private final List<SettingRow> rows = new ArrayList<>();
    private final Animation toggle;
    private final Animation scroll = new Animation(0, 18);
    private double scrollTarget;
    private float x, y, w, h;
    private float contentHeight;

    public SettingsView(ClickGuiScreen screen, Module module) {
        this.screen = screen;
        this.module = module;
        this.toggle = new Animation(module.isEnabled() ? 1 : 0, 16);
        for (Setting<?> s : module.getSettings()) {
            if (!s.isHidden()) rows.add(SettingRow.create(screen, s));
        }
    }

    public Module getModule() {
        return module;
    }

    private float bodyTop() {
        return y + HEADER;
    }

    private boolean overBack(double mx, double my) {
        return mx >= x + 10 && mx < x + 30 && my >= y + 14 && my < y + 34;
    }

    /** Left of the window's close button. */
    private float switchX() {
        return x + w - Ui.PAD - 22 - 10 - Ui.SWITCH_W;
    }

    private boolean overSwitch(double mx, double my) {
        float sx = switchX();
        return mx >= sx - 2 && mx < sx + Ui.SWITCH_W + 2 && my >= y + 16 && my < y + 32;
    }

    public void render(DrawContext ctx, float x, float y, float w, float h, double mx, double my, float delta) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        boolean back = overBack(mx, my);
        Render2D.roundedRect(ctx, x + 10, y + 14, 20, 20, 10, back ? Ui.surfaceHover() : Ui.surface());
        Icons.draw(ctx, Icons.Icon.BACK, x + 13, y + 17, back ? Ui.TEXT : Ui.SUBTEXT);
        int textW = (int) (switchX() - (x + 38) - 10);
        UiFont.title(ctx, UiFont.trim(module.getName(), textW, UiFont.Size.TITLE, true), x + 38, y + 12, Ui.TEXT);
        UiFont.draw(ctx, UiFont.trim(module.getCategory().getDisplayName() + "  ·  " + module.getDescription(), textW, UiFont.Size.BODY, false),
            x + 38, y + 30, Ui.SUBTEXT);
        toggle.setSpeed(16 * screen.animationSpeed());
        toggle.setTarget(module.isEnabled() ? 1 : 0);
        Ui.toggle(ctx, switchX(), y + 18, (float) toggle.update());
        Ui.divider(ctx, x + Ui.PAD, y + HEADER - 8, w - Ui.PAD * 2);

        contentHeight = 0;
        for (SettingRow r : rows) {
            r.tick();
            contentHeight += r.getHeight();
        }
        float visible = h - HEADER;
        double maxScroll = Math.max(0, contentHeight - visible + 8);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll.setSpeed(18 * screen.animationSpeed());
        scroll.setTarget(scrollTarget);
        float s = (float) scroll.update();

        float top = bodyTop();
        screen.setMouseInContent(mx >= x && mx < x + w && my >= top && my < y + h);
        ctx.enableScissor(screen.toScreenX(x), screen.toScreenY(top - 4), screen.toScreenX(x + w), screen.toScreenY(y + h));
        float cy = top - s;
        float rx = x + Ui.PAD + 4, rw = w - Ui.PAD * 2 - 8;
        for (SettingRow r : rows) {
            float rh = r.getHeight();
            r.setBounds(rx, cy, rw);
            if (r.isShown() && cy + rh >= top - 4 && cy <= y + h) r.render(ctx, mx, my, delta);
            cy += rh;
        }
        ctx.disableScissor();
        if (maxScroll > 0) {
            float barH = Math.max(16, visible * visible / (contentHeight + 8));
            float barY = top + (float) (s / maxScroll) * (visible - barH - 4);
            Render2D.roundedRect(ctx, x + w - 5, barY, 3, barH, 1.5f, Ui.accent(0x90));
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (overBack(mx, my)) {
            screen.closeSettings();
            return true;
        }
        if (overSwitch(mx, my) && button == 0) {
            module.toggle();
            return true;
        }
        boolean inBody = my >= bodyTop() && my < y + h && mx >= x && mx < x + w;
        for (SettingRow r : rows) {
            if (!r.isFullyShown()) continue;
            if (inBody) {
                if (r.mouseClicked(mx, my, button)) return true;
            } else {
                r.mouseClicked(Double.NaN, Double.NaN, -1); // lets open dropdowns close
            }
        }
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void mouseReleased(double mx, double my, int button) {
        for (SettingRow r : rows) r.mouseReleased(mx, my, button);
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (mx < x || mx >= x + w || my < y || my >= y + h) return false;
        for (SettingRow r : rows) if (r.isFullyShown() && r.mouseScrolled(mx, my, amount)) return true;
        scrollTarget -= amount * 28;
        return true;
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        for (SettingRow r : rows) if (r.keyPressed(key, scancode, modifiers)) return true;
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        for (SettingRow r : rows) if (r.charTyped(chr, modifiers)) return true;
        return false;
    }

    public String getTooltip(double mx, double my) {
        if (overBack(mx, my)) return "Back (Esc)";
        if (my < bodyTop()) return null;
        for (SettingRow r : rows) if (r.isFullyShown() && r.isHovered(mx, my)) return r.getTooltip(mx, my);
        return null;
    }
}

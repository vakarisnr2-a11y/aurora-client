package dev.aurora.client.gui.component;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import dev.aurora.client.util.KeyNames;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A module row: left-click toggles, right-click expands its settings. */
public final class ModuleButton extends Component {
    public static final float ROW_HEIGHT = 16;

    private final ClickGuiScreen screen;
    private final Module module;
    private final List<SettingComponent> settings = new ArrayList<>();
    private final Animation enabledAnim;
    private final Animation hoverAnim = new Animation(0, 18);
    private final Animation expandAnim = new Animation(0, 14);
    private final Animation matchAnim = new Animation(1, 16);
    private boolean expanded;

    public ModuleButton(ClickGuiScreen screen, Module module) {
        this.screen = screen;
        this.module = module;
        this.enabledAnim = new Animation(module.isEnabled() ? 1 : 0, 16);
        for (Setting<?> s : module.getSettings()) {
            if (!s.isHidden()) settings.add(SettingComponents.create(screen, s));
        }
    }

    public Module getModule() {
        return module;
    }

    public boolean matches(String query) {
        if (query.isEmpty()) return true;
        String q = query.toLowerCase(Locale.ROOT);
        return module.getName().toLowerCase(Locale.ROOT).contains(q) || module.getDescription().toLowerCase(Locale.ROOT).contains(q);
    }

    public boolean isVisibleInList() {
        return matchAnim.get() > 0.02;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded && !settings.isEmpty();
    }

    public boolean isExpanded() {
        return expanded;
    }

    private float settingsHeight() {
        float h = 0;
        for (SettingComponent s : settings) h += s.getHeight();
        return h;
    }

    @Override
    public float getHeight() {
        return (ROW_HEIGHT + settingsHeight() * expandAnim.getFloat() + (expandAnim.get() > 0.01 ? 2 : 0)) * matchAnim.getFloat();
    }

    /** Advances animations; called once per frame before layout. */
    public void tick(String query) {
        double speed = screen.animationSpeed();
        matchAnim.setSpeed(16 * speed);
        expandAnim.setSpeed(14 * speed);
        matchAnim.setTarget(matches(query) ? 1 : 0);
        matchAnim.update();
        expandAnim.setTarget(expanded ? 1 : 0);
        expandAnim.update();
        for (SettingComponent s : settings) s.updateVisibility();
    }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        float m = matchAnim.getFloat();
        if (m < 0.02f) return;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        enabledAnim.setTarget(module.isEnabled() ? 1 : 0);
        float e = (float) enabledAnim.update();
        boolean rowHover = mx >= x && mx < x + width && my >= y && my < y + ROW_HEIGHT && screen.isMouseInBody();
        hoverAnim.setTarget(rowHover ? 1 : 0);
        float h = (float) hoverAnim.update();

        int base = ColorUtil.lerp(Theme.surface(), Theme.hover(), h);
        int bg = ColorUtil.lerp(base, ColorUtil.withAlpha(Theme.accent(), 70), e * 0.85f);
        Render2D.rect(ctx, x, y, width, ROW_HEIGHT * m, bg);
        if (e > 0.01f) Render2D.roundedRect(ctx, x + 1, y + 3, 2, (ROW_HEIGHT - 6) * m, 1, ColorUtil.fade(Theme.accent(), e));
        if (screen.getNavFocus() == this) Render2D.roundedOutline(ctx, x + 0.5f, y + 0.5f, width - 1, ROW_HEIGHT - 1, 2, 1, Theme.accent());

        int textColor = ColorUtil.lerp(Theme.SUBTEXT, Theme.TEXT, Math.max(e, h * 0.6f));
        ctx.drawText(tr, module.getName(), (int) (x + 7 + e * 2), (int) (y + 4), textColor, false);

        String right = "";
        if (module.getKeybind().isBound()) right = KeyNames.of(module.getKeybind().get());
        if (!settings.isEmpty()) right = right + (right.isEmpty() ? "" : " ") + (expanded ? "−" : "+");
        int rightX = (int) (x + width - 5 - tr.getWidth(right));
        if (!right.isEmpty()) ctx.drawText(tr, right, rightX, (int) (y + 4), Theme.MUTED, false);
        if (screen.isFavorite(module)) {
            String star = "★";
            int starX = rightX - tr.getWidth(star) - (right.isEmpty() ? 0 : 3);
            ctx.drawText(tr, star, starX, (int) (y + 4), ColorUtil.withAlpha(Theme.accent(), 220), false);
        }

        float ex = expandAnim.getFloat();
        if (ex > 0.01f) {
            float sy = y + ROW_HEIGHT;
            float total = settingsHeight() * ex + 2;
            Render2D.rect(ctx, x, sy, width, total, ColorUtil.fade(Theme.header(), 0.85f));
            Render2D.rect(ctx, x + 3, sy + 1, 1, total - 2, ColorUtil.fade(Theme.accent(), 0.35f));
            ctx.enableScissor(screen.toScreen(x), screen.toScreen(sy), screen.toScreen(x + width), screen.toScreen(sy + total));
            float cy = sy + 1;
            for (SettingComponent s : settings) {
                if (!s.isShown()) continue;
                s.setBounds(x + 4, cy, width - 6);
                s.render(ctx, mx, my, delta);
                cy += s.getHeight();
            }
            ctx.disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isVisibleInList()) return false;
        if (mx >= x && mx < x + width && my >= y && my < y + ROW_HEIGHT) {
            if (button == 2 || (button == 0 && Screen.hasShiftDown())) screen.toggleFavorite(module);
            else if (button == 0) module.toggle();
            else if (button == 1) setExpanded(!expanded);
            screen.setNavFocus(this);
            return true;
        }
        if (expandAnim.get() > 0.9) {
            for (SettingComponent s : settings) {
                if (s.isFullyShown() && s.mouseClicked(mx, my, button)) return true;
            }
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        for (SettingComponent s : settings) s.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (expandAnim.get() < 0.9) return false;
        for (SettingComponent s : settings) {
            if (s.isFullyShown() && s.mouseScrolled(mx, my, amount)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        for (SettingComponent s : settings) if (s.keyPressed(key, scancode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (SettingComponent s : settings) if (s.charTyped(chr, modifiers)) return true;
        return false;
    }

    @Override
    public String getTooltip(double mx, double my) {
        if (my >= y && my < y + ROW_HEIGHT) return module.getDescription() + (screen.isFavorite(module) ? " (Pinned: middle-click to unpin)" : "");
        if (expandAnim.get() > 0.9) {
            for (SettingComponent s : settings) {
                if (s.isFullyShown() && s.isHovered(mx, my)) return s.getTooltip(mx, my);
            }
        }
        return null;
    }
}

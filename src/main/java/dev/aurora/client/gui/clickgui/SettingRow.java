package dev.aurora.client.gui.clickgui;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.setting.StringSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/** One setting in the module settings page. Its height animates to zero while its visibility condition is false. */
public abstract class SettingRow extends Element {
    protected final ClickGuiScreen screen;
    protected final Setting<?> setting;
    private final Animation visibility;
    protected final Animation hover = new Animation(0, 18);

    protected SettingRow(ClickGuiScreen screen, Setting<?> setting) {
        this.screen = screen;
        this.setting = setting;
        this.visibility = new Animation(setting.isVisible() ? 1 : 0, 14);
    }

    public static SettingRow create(ClickGuiScreen screen, Setting<?> setting) {
        if (setting instanceof BooleanSetting s) return new SettingWidgets.Toggle(screen, s);
        if (setting instanceof NumberSetting s) return new SettingWidgets.Slider(screen, s);
        if (setting instanceof EnumSetting<?> s) return new SettingWidgets.Choice(screen, s);
        if (setting instanceof ColorSetting s) return new SettingWidgets.ColorPicker(screen, s);
        if (setting instanceof KeybindSetting s) return new SettingWidgets.Keybind(screen, s);
        if (setting instanceof StringSetting s) return new SettingWidgets.TextField(screen, s);
        throw new IllegalArgumentException("No widget for " + setting.getClass().getSimpleName());
    }

    /** Height when fully visible. */
    protected abstract float fullHeight();

    @Override
    public final float getHeight() {
        return fullHeight() * visibility.getFloat();
    }

    public void tick() {
        visibility.setSpeed(14 * screen.animationSpeed());
        visibility.setTarget(setting.isVisible() ? 1 : 0);
        visibility.update();
    }

    public boolean isShown() {
        return visibility.get() > 0.02;
    }

    public boolean isFullyShown() {
        return visibility.get() > 0.98;
    }

    /** Label on the left, vertically centred in the first {@code rowHeight} pixels. */
    protected void drawLabel(DrawContext ctx, double mx, double my, float rowHeight) {
        hover.setTarget(isHovered(mx, my) && screen.isMouseInContent() ? 1 : 0);
        float h = (float) hover.update();
        if (h > 0.01f) Render2D.roundedRect(ctx, x - 4, y, width + 8, getHeight(), Ui.R_SMALL, ColorUtil.withAlpha(0xFFFFFF, Math.round(10 * h)));
        UiFont.draw(ctx, setting.getName(), x + 4, y + (rowHeight - 8) / 2f, Ui.TEXT);
    }

    @Override
    public String getTooltip(double mouseX, double mouseY) {
        return setting.getDescription().isEmpty() ? null : setting.getDescription();
    }
}

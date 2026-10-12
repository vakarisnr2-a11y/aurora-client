package dev.aurora.client.gui.component;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.util.Animation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** A setting row. Height animates to zero while the setting's visibility condition is false. */
public abstract class SettingComponent extends Component {
    protected final ClickGuiScreen screen;
    protected final Setting<?> setting;
    private final Animation visibility;

    protected SettingComponent(ClickGuiScreen screen, Setting<?> setting) {
        this.screen = screen;
        this.setting = setting;
        this.visibility = new Animation(setting.isVisible() ? 1 : 0, 14);
    }

    /** Height when fully visible. */
    protected abstract float fullHeight();

    @Override
    public final float getHeight() {
        return fullHeight() * visibility.getFloat();
    }

    public void updateVisibility() {
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

    protected static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    protected void drawLabel(DrawContext ctx, String label) {
        ctx.drawText(font(), label, (int) x + 4, (int) y + 3, Theme.SUBTEXT, false);
    }

    @Override
    public String getTooltip(double mouseX, double mouseY) {
        return setting.getDescription().isEmpty() ? null : setting.getDescription();
    }
}

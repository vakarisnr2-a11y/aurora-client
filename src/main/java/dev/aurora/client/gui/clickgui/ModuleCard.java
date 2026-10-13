package dev.aurora.client.gui.clickgui;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.ui.Icons;
import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.KeyNames;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * A module in the grid: name, one-line description, switch, keybind chip, favorite star and a settings button.
 * Click toggles, right-click (or the gear) opens the settings, middle-click or Shift+click pins it to Favorites.
 */
public final class ModuleCard extends Element {
    public static final float HEIGHT = 56;

    private final ClickGuiScreen screen;
    private final Module module;
    private final boolean hasSettings;
    private final Animation hover = new Animation(0, 16);
    private final Animation enabled;
    private final Animation appear = new Animation(0, 12);

    public ModuleCard(ClickGuiScreen screen, Module module) {
        this.screen = screen;
        this.module = module;
        this.enabled = new Animation(module.isEnabled() ? 1 : 0, 14);
        boolean any = false;
        for (Setting<?> s : module.getSettings()) any |= !s.isHidden();
        this.hasSettings = any;
    }

    public Module getModule() {
        return module;
    }

    /** Restarts the fade-in (when the page or search changes). */
    public void resetAppear(int index) {
        appear.snap(-Math.min(0.6, index * 0.04));
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    private boolean overGear(double mx, double my) {
        return hasSettings && mx >= x + width - 24 && mx < x + width - 4 && my >= y + HEIGHT - 22 && my < y + HEIGHT - 2;
    }

    @Override
    public void render(DrawContext ctx, double mx, double my, float delta) {
        double speed = screen.animationSpeed();
        appear.setSpeed(12 * speed);
        appear.setTarget(1);
        float a = (float) Math.max(0, appear.update());
        if (a < 0.01f) return;
        boolean hovered = isHovered(mx, my) && screen.isMouseInContent();
        hover.setSpeed(16 * speed);
        hover.setTarget(hovered ? 1 : 0);
        enabled.setSpeed(14 * speed);
        enabled.setTarget(module.isEnabled() ? 1 : 0);
        float h = (float) hover.update();
        float e = (float) enabled.update();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, (1 - a) * 6 - h * 1f, 0);
        Ui.card(ctx, x, y, width, HEIGHT, h, e);
        boolean favorite = screen.isFavorite(module);
        float nameMax = width - 20 - Ui.SWITCH_W - (favorite ? 14 : 0);
        UiFont.bold(ctx, UiFont.trim(module.getName(), (int) nameMax, UiFont.Size.BODY, true), x + 10, y + 10, Ui.TEXT);
        if (favorite) Icons.star(ctx, x + width - Ui.SWITCH_W - 18, y + 14, 4.2f, 1.8f, Ui.WARNING);
        Ui.toggle(ctx, x + width - Ui.SWITCH_W - 8, y + 8, e);
        UiFont.draw(ctx, UiFont.trim(module.getDescription(), (int) width - 20, UiFont.Size.BODY, false), x + 10, y + 24, Ui.SUBTEXT);
        float chipY = y + HEIGHT - 18;
        if (module.getKeybind().isBound()) {
            Ui.chip(ctx, x + 10, chipY, KeyNames.of(module.getKeybind().get()), Ui.SUBTEXT, 0x14FFFFFF);
        }
        if (hasSettings) {
            boolean gear = overGear(mx, my) && hovered;
            Icons.draw(ctx, Icons.Icon.GEAR, x + width - 21, y + HEIGHT - 19, gear ? Ui.accentLight() : Ui.MUTED);
        }
        ctx.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isHovered(mx, my)) return false;
        if (button == 2 || (button == 0 && Screen.hasShiftDown())) screen.toggleFavorite(module);
        else if (button == 1 || (button == 0 && overGear(mx, my))) {
            if (hasSettings) screen.openSettings(module);
        } else if (button == 0) module.toggle();
        return true;
    }

    @Override
    public String getTooltip(double mouseX, double mouseY) {
        String tip = module.getDescription();
        if (hasSettings) tip += "  Right-click for settings.";
        return tip + (screen.isFavorite(module) ? " Middle-click to unpin." : " Middle-click to pin.");
    }
}

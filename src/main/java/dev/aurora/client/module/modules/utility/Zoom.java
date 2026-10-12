package dev.aurora.client.module.modules.utility;

import dev.aurora.client.event.events.MouseScrollEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.Animation;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Hold-to-zoom (default C). The FOV is divided by the animated factor in GameRendererMixin and
 * mouse sensitivity is divided by it in LookController so aiming stays precise.
 */
public final class Zoom extends Module {
    private final NumberSetting factor = add(new NumberSetting("Factor", "Zoom magnification", 4, 1.5, 30, 0.5));
    private final BooleanSetting smooth = add(new BooleanSetting("Smooth", "Animate zooming in and out", true));
    private final BooleanSetting scroll = add(new BooleanSetting("Scroll Adjust", "Scroll while zoomed to change magnification", true));
    private final BooleanSetting sensitivity = add(new BooleanSetting("Lower Sensitivity", "Reduce mouse sensitivity while zoomed", true));

    private final Animation current = new Animation(1, 14);
    private double scrollMultiplier = 1;

    public Zoom() {
        super("Zoom", "Hold to zoom in. Scroll to adjust while zoomed.", Category.UTILITY, GLFW.GLFW_KEY_C, false);
        on(MouseScrollEvent.class, e -> {
            if (!scroll.get() || e.vertical == 0) return;
            scrollMultiplier = Math.max(0.25, Math.min(8, scrollMultiplier * (e.vertical > 0 ? 1.15 : 1 / 1.15)));
            e.cancel(); // don't scroll the hotbar while zooming
        });
    }

    @Override
    protected KeybindSetting.Mode defaultBindMode() {
        return KeybindSetting.Mode.HOLD;
    }

    @Override
    protected void onDisable() {
        scrollMultiplier = 1;
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }

    /** Called every frame from getFov; returns the factor to divide the FOV by. */
    public double updateAndGetFactor() {
        double target = isEnabled() ? Math.max(1, factor.get() * scrollMultiplier) : 1;
        current.setTarget(target);
        if (smooth.get()) current.update();
        else current.snap(target);
        return current.get();
    }

    public double getSensitivityFactor() {
        return sensitivity.get() ? 1.0 / Math.max(1, current.get()) : 1.0;
    }

    @Override
    public String getDisplayInfo() {
        return String.format(Locale.ROOT, "%.1fx", factor.get() * scrollMultiplier);
    }
}

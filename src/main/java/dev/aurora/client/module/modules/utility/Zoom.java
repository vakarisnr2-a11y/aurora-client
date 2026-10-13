package dev.aurora.client.module.modules.utility;

import dev.aurora.client.event.events.MouseScrollEvent;
import dev.aurora.client.event.events.Render2DEvent;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Hold-to-zoom (default C). The FOV is divided by the animated factor in GameRendererMixin and mouse sensitivity is
 * divided by it in LookController so aiming stays precise. Scrolling while zoomed changes the magnification in steps
 * between Min and Max Zoom, with a short "4.0x" readout under the crosshair.
 */
public final class Zoom extends Module {
    private static final long LEVEL_SHOWN_MS = 1200;

    private final NumberSetting factor = add(new NumberSetting("Factor", "Zoom magnification when you start zooming", 4, 1.5, 30, 0.5));
    private final BooleanSetting smooth = add(new BooleanSetting("Smooth", "Animate zooming in and out", true));
    private final NumberSetting speed = add(new NumberSetting("Zoom Speed", "How fast the smooth zoom moves", 1.0, 0.25, 3.0, 0.05))
        .visibleWhen(smooth::get);
    private final BooleanSetting scroll = add(new BooleanSetting("Scroll Adjust", "Scroll while zoomed to change magnification", true));
    private final NumberSetting step = add(new NumberSetting("Scroll Step", "Change per scroll notch, in percent", 15, 5, 50, 1))
        .visibleWhen(scroll::get);
    private final NumberSetting minZoom = add(new NumberSetting("Min Zoom", "Lowest magnification reachable by scrolling", 1.5, 1.1, 10, 0.1))
        .visibleWhen(scroll::get);
    private final NumberSetting maxZoom = add(new NumberSetting("Max Zoom", "Highest magnification reachable by scrolling", 50, 2, 100, 1))
        .visibleWhen(scroll::get);
    private final BooleanSetting remember = add(new BooleanSetting("Remember Level", "Start the next zoom at the last scrolled magnification", false))
        .visibleWhen(scroll::get);
    private final BooleanSetting showLevel = add(new BooleanSetting("Show Level", "Show the magnification under the crosshair while scrolling", true))
        .visibleWhen(scroll::get);
    private final BooleanSetting sensitivity = add(new BooleanSetting("Lower Sensitivity", "Reduce mouse sensitivity while zoomed", true));
    private final BooleanSetting cinematic = add(new BooleanSetting("Cinematic Camera", "Use vanilla's smooth camera while zoomed", false));

    private final Animation current = new Animation(1, 14);
    private double level = -1;
    private double remembered = -1;
    private long levelChangedAt;
    private boolean restoreSmoothCamera;

    public Zoom() {
        super("Zoom", "Hold to zoom in. Scroll to adjust while zoomed.", Category.UTILITY, GLFW.GLFW_KEY_C, false);
        on(MouseScrollEvent.class, e -> {
            if (!scroll.get() || e.vertical == 0) return;
            level = scrollLevel(currentLevel(), e.vertical, step.get() / 100.0, minZoom.get(), maxZoom.get());
            levelChangedAt = System.currentTimeMillis();
            e.cancel(); // don't scroll the hotbar while zooming
        });
        on(Render2DEvent.class, this::renderLevel);
    }

    @Override
    protected KeybindSetting.Mode defaultBindMode() {
        return KeybindSetting.Mode.HOLD;
    }

    @Override
    protected void onEnable() {
        level = remember.get() && remembered > 0 ? remembered : factor.get();
        levelChangedAt = 0;
        MinecraftClient mc = mc();
        if (cinematic.get() && mc != null && mc.options != null && !mc.options.smoothCameraEnabled) {
            mc.options.smoothCameraEnabled = true;
            restoreSmoothCamera = true;
        }
    }

    @Override
    protected void onDisable() {
        if (remember.get()) remembered = currentLevel();
        level = -1;
        MinecraftClient mc = mc();
        if (restoreSmoothCamera && mc != null && mc.options != null) mc.options.smoothCameraEnabled = false;
        restoreSmoothCamera = false;
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }

    private double currentLevel() {
        return level > 0 ? level : factor.get();
    }

    /** Called every frame from getFov; returns the factor to divide the FOV by. */
    public double updateAndGetFactor() {
        double target = isEnabled() ? Math.max(1, currentLevel()) : 1;
        current.setSpeed(14 * speed.get());
        current.setTarget(target);
        if (smooth.get()) current.update();
        else current.snap(target);
        return current.get();
    }

    public double getSensitivityFactor() {
        return sensitivity.get() ? 1.0 / Math.max(1, current.get()) : 1.0;
    }

    private void renderLevel(Render2DEvent e) {
        long age = System.currentTimeMillis() - levelChangedAt;
        if (!showLevel.get() || age > LEVEL_SHOWN_MS) return;
        float alpha = age < LEVEL_SHOWN_MS - 300 ? 1f : (LEVEL_SHOWN_MS - age) / 300f;
        String text = String.format(Locale.ROOT, "%.1fx", currentLevel());
        var tr = MinecraftClient.getInstance().textRenderer;
        int x = e.width / 2 - tr.getWidth(text) / 2;
        int y = e.height / 2 + 12;
        HudStyle.text(e.context, text, x, y, ColorUtil.fade(HudStyle.accent() | 0xFF000000, alpha));
    }

    /** One scroll step multiplies or divides the level by (1 + step), clamped to [min, max]. */
    public static double scrollLevel(double level, double vertical, double step, double min, double max) {
        double next = vertical > 0 ? level * (1 + step) : level / (1 + step);
        return Math.max(Math.min(min, max), Math.min(Math.max(min, max), next));
    }

    @Override
    public String getDisplayInfo() {
        return String.format(Locale.ROOT, "%.1fx", currentLevel());
    }
}

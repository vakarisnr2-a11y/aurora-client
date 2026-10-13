package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;

/**
 * One shared look for every HUD element (see {@link dev.aurora.client.hud.HudStyle}).
 *
 * <p>While the module is off, HUD elements use the default Dark look (the default value of every setting). Turning it
 * on applies the settings below. Picking a preset writes all values at once; they can be tweaked afterwards.</p>
 */
public final class HudTheme extends Module {
    public enum AccentBar {
        NONE, BOTTOM, LEFT, TOP
    }

    public enum Preset {
        DARK("Dark", 0xA00E1016, false, 4, true, 4, false, false, false, 0x40FFFFFF, AccentBar.BOTTOM, 4),
        GLASS("Glass", 0x38FFFFFF, true, 6, true, 3, false, true, false, 0x50FFFFFF, AccentBar.NONE, 5),
        MINIMAL("Minimal", 0x00000000, false, 0, false, 3, false, false, false, 0x40FFFFFF, AccentBar.NONE, 2),
        NEON("Neon", 0xC00A0A12, false, 3, true, 5, true, true, true, 0xFFFFFFFF, AccentBar.NONE, 4);

        private final String label;
        final int background;
        final boolean gradient;
        final double radius;
        final boolean shadow;
        final double shadowSize;
        final boolean glow;
        final boolean outline;
        final boolean outlineAccent;
        final int outlineColor;
        final AccentBar accentBar;
        final double padding;

        Preset(String label, int background, boolean gradient, double radius, boolean shadow, double shadowSize, boolean glow,
               boolean outline, boolean outlineAccent, int outlineColor, AccentBar accentBar, double padding) {
            this.label = label;
            this.background = background;
            this.gradient = gradient;
            this.radius = radius;
            this.shadow = shadow;
            this.shadowSize = shadowSize;
            this.glow = glow;
            this.outline = outline;
            this.outlineAccent = outlineAccent;
            this.outlineColor = outlineColor;
            this.accentBar = accentBar;
            this.padding = padding;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final Preset DEFAULT = Preset.DARK;

    private final EnumSetting<Preset> preset = add(new EnumSetting<>("Preset",
        "Sets every value below at once (they can be tweaked afterwards)", DEFAULT));
    private final ColorSetting background = add(new ColorSetting("Background", "Background color and transparency", DEFAULT.background));
    private final BooleanSetting gradient = add(new BooleanSetting("Gradient", "Lighter top edge, darker bottom", DEFAULT.gradient));
    private final NumberSetting radius = add(new NumberSetting("Corner Radius", "Rounding of HUD backgrounds", DEFAULT.radius, 0, 10, 0.5));
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", "Soft shadow behind HUD backgrounds", DEFAULT.shadow));
    private final NumberSetting shadowSize = add(new NumberSetting("Shadow Size", "How far the shadow spreads", DEFAULT.shadowSize, 1, 10, 0.5))
        .visibleWhen(this::hasShadow);
    private final BooleanSetting glow = add(new BooleanSetting("Accent Glow", "Tint the shadow with the accent color", DEFAULT.glow))
        .visibleWhen(this::hasShadow);
    private final BooleanSetting outline = add(new BooleanSetting("Outline", "Thin outline around HUD backgrounds", DEFAULT.outline));
    private final BooleanSetting outlineAccent = add(new BooleanSetting("Accent Outline", "Draw the outline in the accent color", DEFAULT.outlineAccent))
        .visibleWhen(this::hasOutline);
    private final ColorSetting outlineColor = add(new ColorSetting("Outline Color", "Outline color", DEFAULT.outlineColor))
        .visibleWhen(() -> hasOutline() && !outlineAccent.get());
    private final EnumSetting<AccentBar> accentBar = add(new EnumSetting<>("Accent Bar", "Accent line along one edge", DEFAULT.accentBar));
    private final BooleanSetting customAccent = add(new BooleanSetting("Custom Accent", "Use your own accent instead of the ClickGUI accent", false));
    private final ColorSetting accent = add(new ColorSetting("Accent", "HUD accent color", 0xFF8B5CF6, false)).visibleWhen(customAccent::get);
    private final ColorSetting textColor = add(new ColorSetting("Text Color", "Color of values", 0xFFECEEF4, false));
    private final BooleanSetting accentLabels = add(new BooleanSetting("Accent Labels", "Draw labels (FPS, Ping...) in the accent color", true));
    private final BooleanSetting textShadow = add(new BooleanSetting("Text Shadow", "Drop shadow under HUD text", true));
    private final NumberSetting padding = add(new NumberSetting("Padding", "Space between the background edge and the content", DEFAULT.padding, 2, 8, 1));
    private final BooleanSetting animations = add(new BooleanSetting("Animations", "Fade elements in and out and slide changing values", true));
    private final NumberSetting animationSpeed = add(new NumberSetting("Animation Speed", "Speed of HUD animations", 1.0, 0.25, 3.0, 0.05))
        .visibleWhen(animations::get);

    public HudTheme() {
        super("HUD Theme", "Shared look of all HUD elements: background, corners, shadow, outline, accent and presets. "
            + "While off, HUDs use the default Dark look.", Category.VISUALS);
    }

    @Override
    protected void onSettingChanged(Setting<?> setting) {
        if (setting == preset) applyPreset(preset.get());
    }

    private void applyPreset(Preset p) {
        background.set(p.background);
        background.setRainbow(false);
        gradient.set(p.gradient);
        radius.set(p.radius);
        shadow.set(p.shadow);
        shadowSize.set(p.shadowSize);
        glow.set(p.glow);
        outline.set(p.outline);
        outlineAccent.set(p.outlineAccent);
        outlineColor.set(p.outlineColor);
        accentBar.set(p.accentBar);
        padding.set(p.padding);
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }

    private boolean hasShadow() {
        return shadow.get();
    }

    private boolean hasOutline() {
        return outline.get();
    }

    // Getters fall back to the defaults (the Dark look) while the module is off. Defaults are stored boxed values,
    // so none of this allocates.

    private <T> T v(Setting<T> s) {
        return isEnabled() ? s.get() : s.getDefault();
    }

    public int background() {
        return isEnabled() ? background.getColor() : background.getDefault();
    }

    public boolean gradient() {
        return v(gradient);
    }

    public float radius() {
        return v(radius).floatValue();
    }

    public boolean shadow() {
        return v(shadow);
    }

    public float shadowSize() {
        return v(shadowSize).floatValue();
    }

    public boolean glow() {
        return v(glow);
    }

    public boolean outline() {
        return v(outline);
    }

    public boolean outlineAccent() {
        return v(outlineAccent);
    }

    public int outlineColor() {
        return isEnabled() ? outlineColor.getColor() : outlineColor.getDefault();
    }

    public AccentBar accentBar() {
        return v(accentBar);
    }

    /** True when {@link #accent()} replaces the ClickGUI accent. */
    public boolean customAccent() {
        return v(customAccent);
    }

    public int accent() {
        return accent.getColor();
    }

    public int textColor() {
        return isEnabled() ? textColor.getColor() : textColor.getDefault();
    }

    public boolean accentLabels() {
        return v(accentLabels);
    }

    public boolean textShadow() {
        return v(textShadow);
    }

    public int padding() {
        return v(padding).intValue();
    }

    public boolean animations() {
        return v(animations);
    }

    public double animationSpeed() {
        return v(animationSpeed);
    }
}

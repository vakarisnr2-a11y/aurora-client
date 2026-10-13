package dev.aurora.client.module.modules.misc;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.setting.StringSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Opens the ClickGUI and stores every theme / layout setting of it. */
public final class ClickGui extends Module {
    public static final String PROFILES_PANEL = "PROFILES";
    public static final String FAVORITES_PANEL = "FAVORITES";

    private final EnumSetting<Theme.Preset> preset = add(new EnumSetting<>("Theme",
        "Color theme of the GUI, HUD and notifications", Theme.Preset.MIDNIGHT));
    private final ColorSetting accent = add(new ColorSetting("Accent",
        "Accent color. Picking a theme resets it to that theme's accent.", Theme.Preset.MIDNIGHT.accent, false));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", "Opacity of GUI panels", 0.94, 0.3, 1.0, 0.01));
    private final NumberSetting guiScale = add(new NumberSetting("GUI Scale", "Size of the ClickGUI", 1.0, 0.6, 1.6, 0.05));
    private final NumberSetting radius = add(new NumberSetting("Corner Radius", "Rounding of panels and HUD backgrounds", 4, 0, 8, 0.5));
    private final BooleanSetting blur = add(new BooleanSetting("Blur", "Blur the world behind the GUI", true));
    private final NumberSetting animationSpeed = add(new NumberSetting("Animation Speed", "Speed of GUI transitions", 1.0, 0.25, 3.0, 0.05));
    private final BooleanSetting tooltips = add(new BooleanSetting("Tooltips", "Show descriptions when hovering modules and settings", true));
    private final BooleanSetting pauseGame = add(new BooleanSetting("Pause Singleplayer", "Pause the integrated server while the GUI is open", false));

    private final StringSetting favorites = add(new StringSetting("Favorites", "Ids of pinned modules", "", 4096)).hide();

    private final Map<String, NumberSetting> panelX = new HashMap<>();
    private final Map<String, NumberSetting> panelY = new HashMap<>();
    private final Map<String, BooleanSetting> panelOpen = new HashMap<>();

    public ClickGui() {
        super("ClickGUI", "Opens this menu. Also stores the theme, scale and animation settings.", Category.MISC,
            GLFW.GLFW_KEY_RIGHT_SHIFT, false);
        for (Category c : Category.values()) addPanel(c.name());
        addPanel(PROFILES_PANEL);
        addPanel(FAVORITES_PANEL);
    }

    private void addPanel(String key) {
        panelX.put(key, add(new NumberSetting("Panel " + key + " X", "", -1, -1, 8000, 0.5)).hide());
        panelY.put(key, add(new NumberSetting("Panel " + key + " Y", "", -1, -1, 8000, 0.5)).hide());
        panelOpen.put(key, add(new BooleanSetting("Panel " + key + " Open", "", true)).hide());
    }

    @Override
    protected void onEnable() {
        // Open on the next frame: this runs inside Keyboard#onKey, and vanilla would otherwise forward the
        // same Right Shift press to the new screen, whose keybind handler closes it again immediately.
        net.minecraft.client.MinecraftClient mc = mc();
        if (mc != null) mc.send(() -> {
            if (!(mc.currentScreen instanceof ClickGuiScreen)) mc.setScreen(new ClickGuiScreen(this));
        });
        setEnabled(false, false);
    }

    @Override
    protected void onSettingChanged(Setting<?> setting) {
        if (setting == preset) accent.set(preset.get().accent);
    }

    @Override
    public boolean savesEnabledState() {
        return false;
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }

    public Theme.Preset getPreset() {
        return preset.get();
    }

    public int getAccent() {
        return accent.getColor();
    }

    public int getAccent(long offsetMs) {
        return accent.getColor(offsetMs);
    }

    public float getOpacity() {
        return opacity.getFloat();
    }

    public float getGuiScale() {
        return guiScale.getFloat();
    }

    public float getRadius() {
        return radius.getFloat();
    }

    public boolean isBlur() {
        return blur.get();
    }

    public double getAnimationSpeed() {
        return animationSpeed.get();
    }

    public boolean showTooltips() {
        return tooltips.get();
    }

    public boolean pausesGame() {
        return pauseGame.get();
    }

    public NumberSetting panelX(String key) {
        return panelX.get(key);
    }

    public NumberSetting panelY(String key) {
        return panelY.get(key);
    }

    public BooleanSetting panelOpen(String key) {
        return panelOpen.get(key);
    }

    // ------------------------------------------------------------------ favorites

    public boolean isFavorite(Module module) {
        return parseFavorites(favorites.get()).contains(module.getId());
    }

    /** Pins or unpins a module; returns true if it is now a favorite. */
    public boolean toggleFavorite(Module module) {
        Set<String> ids = parseFavorites(favorites.get());
        boolean now = !ids.remove(module.getId());
        if (now) ids.add(module.getId());
        favorites.set(String.join(",", ids));
        return now;
    }

    /** Favorite module ids in the order they were pinned. */
    public List<String> favoriteIds() {
        return new ArrayList<>(parseFavorites(favorites.get()));
    }

    /** Splits the stored comma-separated ids, ignoring blanks and duplicates. */
    public static Set<String> parseFavorites(String raw) {
        Set<String> out = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String id = part.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) out.add(id);
        }
        return out;
    }
}

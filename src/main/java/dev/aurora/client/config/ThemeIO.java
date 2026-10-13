package dev.aurora.client.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.Setting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Exports and imports a look: the ClickGUI theme values (preset, accent, opacity, corner radius, blur, animation speed)
 * and every HUD Theme setting plus whether HUD Theme is on. Layout values (panel positions, GUI scale) are not part
 * of a theme. Imported values go through each setting's validation, like profiles.
 */
public final class ThemeIO {
    public static final String TYPE = "aurora-theme";
    public static final int FORMAT = 1;
    /** ClickGUI settings that make up its look, in load order (the preset first, it resets the accent). */
    static final String[] CLICKGUI_SETTINGS = {"Theme", "Accent", "Opacity", "Corner Radius", "Blur", "Animation Speed"};
    private static final Set<String> SKIPPED = Set.of("keybind", "show in list");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ThemeIO() {
    }

    public static JsonObject export(Module clickGui, Module hudTheme) {
        JsonObject root = new JsonObject();
        root.addProperty("type", TYPE);
        root.addProperty("format", FORMAT);
        JsonObject gui = new JsonObject();
        for (String name : CLICKGUI_SETTINGS) {
            Setting<?> s = clickGui.getSetting(name);
            if (s != null) gui.add(name, s.toJson());
        }
        root.add("clickgui", gui);
        JsonObject hud = new JsonObject();
        hud.addProperty("enabled", hudTheme.isEnabled());
        for (Setting<?> s : hudTheme.getSettings()) {
            if (!SKIPPED.contains(s.getName().toLowerCase(java.util.Locale.ROOT))) hud.add(s.getName(), s.toJson());
        }
        root.add("hudtheme", hud);
        return root;
    }

    public static String exportString(Module clickGui, Module hudTheme) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(export(clickGui, hudTheme));
    }

    /** Writes a timestamped theme file into {@code dir} and returns it. */
    public static Path exportTo(Path dir, Module clickGui, Module hudTheme) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve("theme-" + LocalDateTime.now().format(STAMP) + ".json");
        Files.writeString(file, exportString(clickGui, hudTheme), StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Applies a theme from JSON text.
     *
     * @return number of values applied
     * @throws IllegalArgumentException if the text is not an Aurora theme
     */
    public static int importString(String json, Module clickGui, Module hudTheme) {
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json == null ? "" : json);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Not valid JSON", e);
        }
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Not an Aurora theme");
        return apply(parsed.getAsJsonObject(), clickGui, hudTheme);
    }

    public static int apply(JsonObject root, Module clickGui, Module hudTheme) {
        JsonElement type = root.get("type");
        if (type == null || !type.isJsonPrimitive() || !TYPE.equals(type.getAsString())) {
            throw new IllegalArgumentException("Not an Aurora theme (type must be '" + TYPE + "')");
        }
        int applied = 0;
        JsonElement gui = root.get("clickgui");
        if (gui != null && gui.isJsonObject()) {
            for (String name : CLICKGUI_SETTINGS) applied += load(clickGui.getSetting(name), gui.getAsJsonObject().get(name));
        }
        JsonElement hud = root.get("hudtheme");
        if (hud != null && hud.isJsonObject()) {
            JsonObject obj = hud.getAsJsonObject();
            for (Setting<?> s : hudTheme.getSettings()) {
                if (!SKIPPED.contains(s.getName().toLowerCase(java.util.Locale.ROOT))) applied += load(s, obj.get(s.getName()));
            }
            JsonElement enabled = obj.get("enabled");
            if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
                hudTheme.setEnabled(enabled.getAsBoolean());
                applied++;
            }
        }
        return applied;
    }

    private static int load(Setting<?> setting, JsonElement value) {
        return setting != null && value != null && setting.load(value) ? 1 : 0;
    }
}

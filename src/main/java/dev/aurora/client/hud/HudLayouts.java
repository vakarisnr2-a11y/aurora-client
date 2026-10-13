package dev.aurora.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.aurora.client.config.ConfigManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Saved HUD layouts: the position, anchor, scale and enabled state of every HUD element, one JSON file per layout in
 * {@code config/aurora/layouts}. Layouts are independent of profiles, so one layout can be loaded into any profile.
 */
public final class HudLayouts {
    public static final String TYPE = "aurora-hud-layout";
    public static final int FORMAT = 1;

    private final Path dir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public HudLayouts(Path dir) {
        this.dir = dir;
    }

    public Path getDir() {
        return dir;
    }

    /** Layout names use the same rules as profile names (letters, digits, - and _). */
    public static boolean isValidName(String name) {
        return ConfigManager.isValidProfileName(name);
    }

    public List<String> list() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(dir)) return names;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : stream) {
                String file = p.getFileName().toString();
                String name = file.substring(0, file.length() - 5);
                if (isValidName(name)) names.add(name);
            }
        } catch (IOException e) {
            return names;
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void save(String name, List<HudModule> elements) throws IOException {
        if (!isValidName(name)) throw new IllegalArgumentException("Invalid layout name: " + name);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name + ".json"), gson.toJson(toJson(elements)), StandardCharsets.UTF_8);
    }

    /** Loads a layout into the elements and returns how many elements it contained. */
    public int load(String name, List<HudModule> elements) throws IOException {
        if (!isValidName(name)) throw new IllegalArgumentException("Invalid layout name: " + name);
        String json = Files.readString(dir.resolve(name + ".json"), StandardCharsets.UTF_8);
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Layout is not valid JSON", e);
        }
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Layout is not a JSON object");
        return apply(parsed.getAsJsonObject(), elements);
    }

    public boolean delete(String name) {
        if (!isValidName(name)) return false;
        try {
            return Files.deleteIfExists(dir.resolve(name + ".json"));
        } catch (IOException e) {
            return false;
        }
    }

    public static JsonObject toJson(List<HudModule> elements) {
        JsonObject root = new JsonObject();
        root.addProperty("type", TYPE);
        root.addProperty("format", FORMAT);
        JsonObject map = new JsonObject();
        for (HudModule hud : elements) map.add(hud.getId(), hud.layoutToJson());
        root.add("elements", map);
        return root;
    }

    /**
     * Applies a layout. Elements the layout does not mention are left as they are; unknown ids are ignored.
     *
     * @return number of elements found in the layout
     * @throws IllegalArgumentException if the JSON has no "elements" object
     */
    public static int apply(JsonObject root, List<HudModule> elements) {
        JsonElement el = root.get("elements");
        if (el == null || !el.isJsonObject()) throw new IllegalArgumentException("Not a HUD layout (missing 'elements')");
        JsonObject map = el.getAsJsonObject();
        int applied = 0;
        for (HudModule hud : elements) {
            JsonElement entry = map.get(hud.getId());
            if (entry != null && entry.isJsonObject()) {
                hud.applyLayout(entry.getAsJsonObject());
                applied++;
            }
        }
        return applied;
    }
}

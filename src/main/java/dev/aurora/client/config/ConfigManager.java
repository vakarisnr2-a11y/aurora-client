package dev.aurora.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.setting.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Saves and loads module state as JSON profiles.
 *
 * <pre>
 * config/aurora/client.json            active profile name
 * config/aurora/profiles/NAME.json     one file per profile
 * config/aurora/exports/               exported copies
 * </pre>
 *
 * <p>Loading is defensive: unknown modules/settings are ignored, values of the wrong type fall
 * back to defaults (and are reported), and an unparsable file is renamed to
 * {@code NAME.json.corrupt-TIMESTAMP} before defaults are written in its place.</p>
 */
public final class ConfigManager {
    public static final int FORMAT_VERSION = 1;
    public static final String DEFAULT_PROFILE = "default";
    private static final Logger LOGGER = LoggerFactory.getLogger("Aurora/Config");
    private static final Pattern PROFILE_NAME = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final long AUTOSAVE_DELAY_MS = 1500;

    private final Path root;
    private final Path profilesDir;
    private final Path exportsDir;
    private final Path clientFile;
    private final ModuleManager modules;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private String activeProfile = DEFAULT_PROFILE;
    private volatile boolean dirty;
    private volatile long lastChange;
    private boolean applying;

    public ConfigManager(Path root, ModuleManager modules) {
        this.root = root;
        this.profilesDir = root.resolve("profiles");
        this.exportsDir = root.resolve("exports");
        this.clientFile = root.resolve("client.json");
        this.modules = modules;
        modules.setDirtyListener(this::markDirty);
    }

    public static boolean isValidProfileName(String name) {
        return name != null && PROFILE_NAME.matcher(name).matches();
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public Path getRoot() {
        return root;
    }

    public Path getExportsDir() {
        return exportsDir;
    }

    // ------------------------------------------------------------------ autosave

    public void markDirty() {
        if (applying) return;
        dirty = true;
        lastChange = System.currentTimeMillis();
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Called every client tick: saves once changes have settled for a moment. */
    public void tick() {
        if (dirty && System.currentTimeMillis() - lastChange >= AUTOSAVE_DELAY_MS) {
            save();
        }
    }

    // ------------------------------------------------------------------ load / save

    /** Reads client.json and loads the active profile. */
    public synchronized LoadReport loadActive() {
        String name = DEFAULT_PROFILE;
        if (Files.isRegularFile(clientFile)) {
            try {
                JsonElement parsed = JsonParser.parseString(Files.readString(clientFile, StandardCharsets.UTF_8));
                if (parsed.isJsonObject() && parsed.getAsJsonObject().has("activeProfile")) {
                    String stored = parsed.getAsJsonObject().get("activeProfile").getAsString();
                    if (isValidProfileName(stored)) name = stored;
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("client.json is unreadable, falling back to the default profile", e);
                backupCorrupt(clientFile);
            }
        }
        return loadProfile(name);
    }

    public synchronized LoadReport loadProfile(String name) {
        if (!isValidProfileName(name)) throw new IllegalArgumentException("Invalid profile name: " + name);
        if (dirty && !name.equals(activeProfile)) save(); // don't lose pending changes of the old profile

        LoadReport report = new LoadReport(name);
        Path file = profileFile(name);
        activeProfile = name;
        if (!Files.isRegularFile(file)) {
            applyDefaults();
            report.markCreated();
            save();
            return report;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new JsonParseException("Root is not a JSON object");
            apply(parsed.getAsJsonObject(), report);
            dirty = false;
            if (!report.getWarnings().isEmpty()) {
                LOGGER.warn("Profile '{}' loaded with {} warning(s): {}", name, report.getWarnings().size(), report.getWarnings());
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Profile '{}' is malformed; backing it up and restoring defaults", name, e);
            String backup = backupCorrupt(file);
            report.markRecovered(backup);
            applyDefaults();
            save();
        }
        return report;
    }

    /** Writes the active profile and client.json. Returns false (and logs) on I/O failure. */
    public synchronized boolean save() {
        try {
            Files.createDirectories(profilesDir);
            writeAtomic(profileFile(activeProfile), gson.toJson(serialize()));
            JsonObject client = new JsonObject();
            client.addProperty("activeProfile", activeProfile);
            client.addProperty("format", FORMAT_VERSION);
            writeAtomic(clientFile, gson.toJson(client));
            dirty = false;
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save profile '{}'", activeProfile, e);
            return false;
        }
    }

    // ------------------------------------------------------------------ serialization

    public JsonObject serialize() {
        JsonObject rootObj = new JsonObject();
        rootObj.addProperty("format", FORMAT_VERSION);
        rootObj.addProperty("client", "aurora");
        JsonObject mods = new JsonObject();
        for (Module m : modules.getAll()) {
            JsonObject mo = new JsonObject();
            if (m.savesEnabledState()) mo.addProperty("enabled", m.isEnabled());
            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) settings.add(s.getName(), s.toJson());
            mo.add("settings", settings);
            mods.add(m.getId(), mo);
        }
        rootObj.add("modules", mods);
        return rootObj;
    }

    /** Applies a profile object on top of defaults. Missing values become defaults. */
    public synchronized LoadReport apply(JsonObject rootObj, LoadReport report) {
        applying = true;
        try {
            resetAll();
            JsonElement format = rootObj.get("format");
            if (format == null || !format.isJsonPrimitive() || !format.getAsJsonPrimitive().isNumber()) {
                report.warn("Missing format version; loading best-effort");
            } else if (format.getAsInt() > FORMAT_VERSION) {
                report.warn("Profile was written by a newer version (format " + format.getAsInt() + ")");
            }
            JsonElement modsEl = rootObj.get("modules");
            if (modsEl == null || !modsEl.isJsonObject()) {
                report.warn("No 'modules' section; using defaults");
                applyEnabledDefaults();
                return report;
            }
            JsonObject mods = modsEl.getAsJsonObject();
            for (Module m : modules.getAll()) {
                JsonElement el = mods.get(m.getId());
                if (el == null) {
                    m.setEnabled(m.isEnabledByDefault() && m.savesEnabledState(), false);
                    continue;
                }
                if (!el.isJsonObject()) {
                    report.warn(m.getName() + ": entry is not an object");
                    m.setEnabled(m.isEnabledByDefault() && m.savesEnabledState(), false);
                    continue;
                }
                JsonObject mo = el.getAsJsonObject();
                JsonElement settingsEl = mo.get("settings");
                if (settingsEl != null && settingsEl.isJsonObject()) {
                    JsonObject settings = settingsEl.getAsJsonObject();
                    for (Setting<?> s : m.getSettings()) {
                        JsonElement value = settings.get(s.getName());
                        if (value != null && !s.load(value)) {
                            s.reset();
                            report.warn(m.getName() + "." + s.getName() + ": invalid value " + value + ", using default");
                        }
                    }
                } else if (settingsEl != null) {
                    report.warn(m.getName() + ": 'settings' is not an object");
                }
                boolean enable = m.isEnabledByDefault();
                JsonElement en = mo.get("enabled");
                if (en != null) {
                    if (en.isJsonPrimitive() && en.getAsJsonPrimitive().isBoolean()) enable = en.getAsBoolean();
                    else report.warn(m.getName() + ": 'enabled' is not a boolean");
                }
                m.setEnabled(enable && m.savesEnabledState(), false);
                report.moduleLoaded();
            }
            for (Map.Entry<String, JsonElement> entry : mods.entrySet()) {
                if (modules.find(entry.getKey()).isEmpty()) report.warn("Unknown module '" + entry.getKey() + "' ignored");
            }
            return report;
        } finally {
            applying = false;
        }
    }

    private void resetAll() {
        for (Module m : modules.getAll()) m.resetSettings();
    }

    private void applyEnabledDefaults() {
        for (Module m : modules.getAll()) m.setEnabled(m.isEnabledByDefault() && m.savesEnabledState(), false);
    }

    private void applyDefaults() {
        applying = true;
        try {
            resetAll();
            applyEnabledDefaults();
        } finally {
            applying = false;
        }
    }

    // ------------------------------------------------------------------ profiles

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (Files.isDirectory(profilesDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(profilesDir, "*.json")) {
                for (Path p : stream) {
                    String file = p.getFileName().toString();
                    String name = file.substring(0, file.length() - 5);
                    if (isValidProfileName(name)) names.add(name);
                }
            } catch (IOException e) {
                LOGGER.error("Could not list profiles", e);
            }
        }
        if (!names.contains(activeProfile)) names.add(activeProfile);
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Creates a profile from the current settings (copy) or from defaults and switches to it. */
    public synchronized boolean createProfile(String name, boolean copyCurrent) {
        if (!isValidProfileName(name) || Files.exists(profileFile(name))) return false;
        if (dirty) save();
        if (!copyCurrent) applyDefaults();
        activeProfile = name;
        return save();
    }

    /** Deletes a profile file. The active profile cannot be deleted. */
    public synchronized boolean deleteProfile(String name) {
        if (!isValidProfileName(name) || name.equals(activeProfile)) return false;
        try {
            return Files.deleteIfExists(profileFile(name));
        } catch (IOException e) {
            LOGGER.error("Could not delete profile '{}'", name, e);
            return false;
        }
    }

    /** Writes a timestamped copy of the active profile into the exports folder. */
    public synchronized Path exportActive() throws IOException {
        Files.createDirectories(exportsDir);
        Path target = exportsDir.resolve(activeProfile + "-" + LocalDateTime.now().format(STAMP) + ".json");
        writeAtomic(target, gson.toJson(serialize()));
        return target;
    }

    public String exportToString() {
        return gson.toJson(serialize());
    }

    /**
     * Validates JSON text and stores it as a new profile, then switches to it.
     *
     * @throws IllegalArgumentException if the name is invalid or the JSON is not a profile
     */
    public synchronized LoadReport importProfile(String name, String json) throws IOException {
        if (!isValidProfileName(name)) throw new IllegalArgumentException("Invalid profile name: " + name);
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Not valid JSON: " + e.getMessage(), e);
        }
        if (!parsed.isJsonObject() || !parsed.getAsJsonObject().has("modules")) {
            throw new IllegalArgumentException("JSON is not an Aurora profile (missing 'modules')");
        }
        if (dirty) save();
        LoadReport report = new LoadReport(name);
        activeProfile = name;
        apply(parsed.getAsJsonObject(), report);
        save(); // re-serialize: the stored file only contains validated values
        return report;
    }

    public LoadReport importFile(Path file, String name) throws IOException {
        return importProfile(name, Files.readString(file, StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ helpers

    private Path profileFile(String name) {
        return profilesDir.resolve(name + ".json");
    }

    private void writeAtomic(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String backupCorrupt(Path file) {
        Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + LocalDateTime.now().format(STAMP));
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup.getFileName().toString();
        } catch (IOException e) {
            LOGGER.error("Could not back up corrupt file {}", file, e);
            return null;
        }
    }
}

package dev.aurora.client;

import com.google.gson.JsonObject;
import dev.aurora.client.config.ConfigManager;
import dev.aurora.client.config.LoadReport;
import dev.aurora.client.event.EventBus;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigAndSettingsTest {
    enum Mode { A, B }

    static final class TestModule extends Module {
        final BooleanSetting flag = add(new BooleanSetting("Flag", "", false));
        final NumberSetting number = add(new NumberSetting("Number", "", 5, 0, 10, 0.5));
        final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", "", Mode.A));
        final ColorSetting color = add(new ColorSetting("Color", "", 0xFF112233));

        TestModule(String name) {
            super(name, "test", Category.MISC);
        }
    }

    private static ModuleManager manager(TestModule... modules) {
        ModuleManager m = new ModuleManager(new EventBus());
        for (TestModule t : modules) m.register(t);
        return m;
    }

    @Test
    void numberSettingClampsAndSnaps() {
        NumberSetting s = new NumberSetting("N", "", 1, 0, 2, 0.25);
        s.set(5.0);
        assertEquals(2.0, s.get());
        s.set(0.3);
        assertEquals(0.25, s.get());
        s.set(Double.NaN);
        assertEquals(1.0, s.get());
    }

    @Test
    void keybindRejectsInvalidCodes() {
        KeybindSetting k = new KeybindSetting("K", "", 65);
        k.set(5000);
        assertEquals(KeybindSetting.NONE, k.get());
        k.set(KeybindSetting.MOUSE_OFFSET + 3);
        assertTrue(k.matchesMouse(3));
        assertEquals(Integer.valueOf(0xFF00FF00), ColorSetting.parseHex("#00FF00"));
    }

    @Test
    void duplicateModulesAreRejected() {
        ModuleManager m = manager(new TestModule("Alpha"));
        assertThrows(IllegalStateException.class, () -> m.register(new TestModule("Alpha")));
        assertTrue(m.find("ALPHA").isPresent());
    }

    @Test
    void profileRoundTrip(@TempDir Path dir) {
        TestModule module = new TestModule("Alpha");
        ConfigManager config = new ConfigManager(dir, manager(module));
        config.loadActive();
        module.flag.set(true);
        module.number.set(7.5);
        module.mode.set(Mode.B);
        module.color.setRainbow(true);
        module.getKeybind().set(82);
        module.setEnabled(true, false);
        assertTrue(config.save());

        TestModule fresh = new TestModule("Alpha");
        ConfigManager reloaded = new ConfigManager(dir, manager(fresh));
        LoadReport report = reloaded.loadActive();
        assertTrue(report.isClean(), report.getWarnings().toString());
        assertTrue(fresh.flag.get());
        assertEquals(7.5, fresh.number.get());
        assertEquals(Mode.B, fresh.mode.get());
        assertTrue(fresh.color.isRainbow());
        assertEquals(82, fresh.getKeybind().get());
        assertTrue(fresh.isEnabled());
    }

    @Test
    void invalidValuesFallBackToDefaults(@TempDir Path dir) {
        TestModule module = new TestModule("Alpha");
        ConfigManager config = new ConfigManager(dir, manager(module));
        JsonObject root = config.serialize();
        JsonObject settings = root.getAsJsonObject("modules").getAsJsonObject("alpha").getAsJsonObject("settings");
        settings.addProperty("Number", "not a number");
        settings.addProperty("Mode", "NOPE");
        LoadReport report = config.apply(root, new LoadReport("test"));
        assertEquals(2, report.getWarnings().size());
        assertEquals(5.0, module.number.get());
        assertEquals(Mode.A, module.mode.get());
    }

    @Test
    void malformedProfileIsBackedUpAndRecovered(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("profiles"));
        Files.writeString(dir.resolve("profiles/default.json"), "{ this is not json");
        TestModule module = new TestModule("Alpha");
        ConfigManager config = new ConfigManager(dir, manager(module));
        LoadReport report = config.loadActive();
        assertTrue(report.isRecovered());
        assertTrue(Files.exists(dir.resolve("profiles").resolve(report.getBackupFile())));
        assertFalse(module.flag.get());
        assertTrue(Files.readString(dir.resolve("profiles/default.json")).contains("\"modules\""));
    }

    @Test
    void importRejectsNonProfiles(@TempDir Path dir) {
        ConfigManager config = new ConfigManager(dir, manager(new TestModule("Alpha")));
        assertThrows(IllegalArgumentException.class, () -> config.importProfile("x", "[1,2,3]"));
        assertThrows(IllegalArgumentException.class, () -> config.importProfile("bad name!", "{}"));
    }
}

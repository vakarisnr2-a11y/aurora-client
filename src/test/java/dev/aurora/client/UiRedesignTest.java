package dev.aurora.client;

import com.google.gson.JsonObject;
import dev.aurora.client.config.ConfigManager;
import dev.aurora.client.config.LoadReport;
import dev.aurora.client.event.EventBus;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.Modules;
import dev.aurora.client.module.modules.misc.HudEditor;
import dev.aurora.client.module.modules.utility.Zoom;
import dev.aurora.client.module.modules.visuals.AuroraMenu;
import dev.aurora.client.module.modules.visuals.HudTheme;
import dev.aurora.client.module.modules.world.LowEndMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Part 4 (UI redesign): defaults, profile migration and font selection. Drawing needs a running client. */
class UiRedesignTest {
    private static ModuleManager allModules() {
        ModuleManager manager = new ModuleManager(new EventBus());
        manager.registerAll(Modules.createAll());
        return manager;
    }

    private static <T extends Module> T get(ModuleManager manager, Class<T> type) {
        return manager.getAll().stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    private static JsonObject settingsOf(JsonObject profile, Module module) {
        return profile.getAsJsonObject("modules").getAsJsonObject(module.getId()).getAsJsonObject("settings");
    }

    @Test
    void quietModulesStayOutOfTheModuleListByDefault() {
        ModuleManager manager = allModules();
        Module lowEnd = get(manager, LowEndMode.class);
        Module hudEditor = get(manager, HudEditor.class);
        Module zoom = get(manager, Zoom.class);
        assertFalse(lowEnd.isDrawn(), "background modules should not clutter the module list");
        assertFalse(hudEditor.isDrawn());
        assertTrue(zoom.isDrawn(), "gameplay modules are still listed");
    }

    @Test
    void formatOneProfilesGetTheNewShowInListDefault(@TempDir Path dir) {
        ModuleManager manager = allModules();
        ConfigManager config = new ConfigManager(dir, manager);
        JsonObject profile = config.serialize();
        profile.addProperty("format", 1);
        settingsOf(profile, get(manager, LowEndMode.class)).addProperty("Show in List", true); // old default
        settingsOf(profile, get(manager, Zoom.class)).addProperty("Show in List", false); // a real user choice

        LoadReport report = config.apply(profile, new LoadReport("old"));
        assertTrue(report.isClean(), report.getWarnings().toString());
        assertFalse(get(manager, LowEndMode.class).isDrawn(), "old default is replaced by the new one");
        assertFalse(get(manager, Zoom.class).isDrawn(), "choices for listed modules are kept");
    }

    @Test
    void formatTwoProfilesKeepShowInList(@TempDir Path dir) {
        ModuleManager manager = allModules();
        ConfigManager config = new ConfigManager(dir, manager);
        JsonObject profile = config.serialize();
        assertEquals(ConfigManager.FORMAT_VERSION, profile.get("format").getAsInt());
        settingsOf(profile, get(manager, LowEndMode.class)).addProperty("Show in List", true);
        config.apply(profile, new LoadReport("new"));
        assertTrue(get(manager, LowEndMode.class).isDrawn());
    }

    @Test
    void formatOneProfilesTurnOnHudThemeUnlessItWasChosen(@TempDir Path dir) {
        ModuleManager manager = allModules();
        ConfigManager config = new ConfigManager(dir, manager);
        HudTheme theme = get(manager, HudTheme.class);
        AuroraMenu menu = get(manager, AuroraMenu.class);

        JsonObject old = config.serialize();
        old.addProperty("format", 1);
        JsonObject themeEntry = old.getAsJsonObject("modules").getAsJsonObject(theme.getId());
        themeEntry.addProperty("enabled", false); // the old default
        themeEntry.getAsJsonObject("settings").addProperty("Preset", "DARK");
        old.getAsJsonObject("modules").getAsJsonObject(menu.getId()).addProperty("enabled", false);
        config.apply(old, new LoadReport("old"));
        assertTrue(theme.isEnabled(), "upgraded profiles get the new look");
        assertEquals(HudTheme.Preset.AURORA, theme.getSetting("Preset").get());
        assertTrue(menu.isEnabled());

        themeEntry.addProperty("enabled", true); // turned on by the user: keep their values
        config.apply(old, new LoadReport("chosen"));
        assertTrue(theme.isEnabled());
        assertEquals(HudTheme.Preset.DARK, theme.getSetting("Preset").get());

        old.addProperty("format", 2); // a new profile that has it off keeps it off
        themeEntry.addProperty("enabled", false);
        config.apply(old, new LoadReport("new"));
        assertFalse(theme.isEnabled());
    }

    @Test
    void hudThemeUsesTheAuroraFontByDefault() {
        HudTheme theme = get(allModules(), HudTheme.class);
        assertTrue(theme.isEnabledByDefault());
        assertEquals(HudTheme.Font.AURORA, theme.font());
    }

    @Test
    void fontOversamplingFollowsThePixelScale() {
        assertEquals(0, UiFont.oversampleIndex(1.0));
        assertEquals(1, UiFont.oversampleIndex(2.0));
        assertEquals(1, UiFont.oversampleIndex(2.4));
        assertEquals(2, UiFont.oversampleIndex(3.0));
        assertEquals(3, UiFont.oversampleIndex(3.6));
        assertEquals(4, UiFont.oversampleIndex(5.5));
        assertEquals(4, UiFont.oversampleIndex(12));
        assertEquals(0, UiFont.oversampleIndex(0.5));
    }
}

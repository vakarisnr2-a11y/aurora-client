package dev.aurora.client;

import com.google.gson.JsonObject;
import dev.aurora.client.event.EventBus;
import dev.aurora.client.hud.HudLayouts;
import dev.aurora.client.hud.HudLine;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudModule.Anchor;
import dev.aurora.client.hud.HudRenderer;
import dev.aurora.client.hud.HudSnapper;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.Modules;
import dev.aurora.client.module.modules.utility.FpsDisplay;
import dev.aurora.client.module.modules.utility.PingDisplay;
import dev.aurora.client.module.modules.visuals.HudTheme;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.util.CachedText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Part 1: HUD theme, anchoring, snapping, layouts and value animations (drawing needs a running client). */
class HudOverhaulTest {
    private static ModuleManager manager() {
        ModuleManager manager = new ModuleManager(new EventBus());
        manager.registerAll(Modules.createAll());
        return manager;
    }

    // ------------------------------------------------------------------ HUD Theme

    @Test
    void hudThemeIsRegisteredOffByDefaultAndNotAHudElement() {
        ModuleManager manager = manager();
        HudTheme theme = manager.get(HudTheme.class);
        assertFalse(theme.isEnabledByDefault());
        assertFalse(new HudRenderer(manager).getElements().contains(theme));
    }

    @Test
    @SuppressWarnings("unchecked")
    void presetsWriteEveryValueAndOffMeansDark() {
        HudTheme theme = new HudTheme();
        EnumSetting<HudTheme.Preset> preset = (EnumSetting<HudTheme.Preset>) theme.getSetting("Preset");
        // While disabled the getters return the defaults (the Dark look) whatever the settings hold.
        preset.set(HudTheme.Preset.NEON);
        assertFalse(theme.outline());
        assertEquals(HudTheme.AccentBar.BOTTOM, theme.accentBar());

        theme.setEnabled(true);
        assertTrue(theme.outline());
        assertTrue(theme.outlineAccent());
        assertTrue(theme.glow());
        assertEquals(HudTheme.AccentBar.NONE, theme.accentBar());

        preset.set(HudTheme.Preset.MINIMAL);
        assertEquals(0, theme.background() >>> 24, "minimal has no background");
        assertFalse(theme.shadow());
        assertFalse(theme.outline());
        assertEquals(0f, theme.radius());
        assertEquals(2, theme.padding());

        preset.set(HudTheme.Preset.GLASS);
        assertTrue(theme.gradient());
        assertTrue(theme.outline());
        assertFalse(theme.outlineAccent());

        preset.set(HudTheme.Preset.DARK);
        assertTrue(theme.shadow());
        assertEquals(4, theme.padding());
        for (var s : theme.getSettings()) {
            if (s != theme.getKeybind() && !s.getName().equals("Show in List")) assertTrue(s.isDefault(), s.getName() + " should be default");
        }
    }

    // ------------------------------------------------------------------ anchoring

    @Test
    void resolveAndOffsetAreInverse() {
        for (int align = 0; align <= 2; align++) {
            float pos = HudModule.resolve(align, 12.5f, 427, 60);
            assertEquals(12.5f, HudModule.offsetFor(align, pos, 427, 60), 1e-4f, "align " + align);
        }
        assertEquals(10, HudModule.resolve(0, 10, 400, 50));
        assertEquals(175 + 10, HudModule.resolve(1, 10, 400, 50));
        assertEquals(340, HudModule.resolve(2, 10, 400, 50));
    }

    @Test
    void anchorFollowsTheThirdOfTheScreen() {
        assertEquals(Anchor.TOP_LEFT, HudModule.anchorFor(2, 2, 40, 10, 300, 200));
        assertEquals(Anchor.TOP_RIGHT, HudModule.anchorFor(250, 5, 40, 10, 300, 200));
        assertEquals(Anchor.CENTER, HudModule.anchorFor(130, 95, 40, 10, 300, 200));
        assertEquals(Anchor.BOTTOM_CENTER, HudModule.anchorFor(130, 185, 40, 10, 300, 200));
        assertEquals(Anchor.MIDDLE_LEFT, HudModule.anchorFor(0, 95, 40, 10, 300, 200));
    }

    @Test
    void anchoredElementKeepsItsDistanceToTheEdgeWhenTheWindowResizes() {
        FpsDisplay fps = new FpsDisplay(); // default size 40x10, scale 1
        fps.setPosition(250, 5, 300, 200);
        assertEquals(Anchor.TOP_RIGHT, fps.getAnchor());
        assertEquals(250, fps.getX(300), 1e-4f);
        assertEquals(350, fps.getX(400), 1e-4f, "10 px from the right edge in a wider window");
        assertEquals(5, fps.getY(600), 1e-4f, "top anchored");
        assertTrue(fps.isRightSide());

        fps.setPosition(130, 95, 300, 200);
        assertEquals(Anchor.CENTER, fps.getAnchor());
        assertEquals(180, fps.getX(400), 1e-4f, "centred elements stay centred");
        assertEquals(145, fps.getY(300), 1e-4f);

        fps.setPosition(10, 10, 300, 200);
        assertEquals(0, fps.getX(5), 1e-4f, "never pushed off a tiny screen");
    }

    @Test
    void autoAnchorKeepsTheOldFractionPositions() {
        FpsDisplay fps = new FpsDisplay();
        assertEquals(Anchor.AUTO, fps.getAnchor());
        PingDisplay ping = new PingDisplay();
        assertEquals(0, ping.getX(300), 1e-4f);
        assertEquals(0.13f * (200 - 10), ping.getY(200), 1e-3f);
        fps.setPosition(100, 50, 300, 200);
        fps.resetPosition();
        assertEquals(Anchor.AUTO, fps.getAnchor());
        assertEquals(0.08f * (200 - 10), fps.getY(200), 1e-3f);
    }

    // ------------------------------------------------------------------ snapping

    @Test
    void snapsToScreenEdgesAndCentre() {
        HudSnapper.Result r = new HudSnapper.Result();
        HudSnapper.snap(3, 50, 40, 10, 300, 200, new float[0], 0, 5, 0, true, r);
        assertEquals(0, r.x);
        assertEquals(0, r.guideX);
        assertTrue(Float.isNaN(r.guideY));
        assertEquals(50, r.y);

        HudSnapper.snap(128, 93, 40, 10, 300, 200, new float[0], 0, 5, 0, true, r);
        assertEquals(130, r.x);
        assertEquals(150, r.guideX);
        assertEquals(95, r.y);
        assertEquals(100, r.guideY);

        HudSnapper.snap(258, 188, 40, 10, 300, 200, new float[0], 0, 5, 0, true, r);
        assertEquals(260, r.x);
        assertEquals(300, r.guideX);
        assertEquals(190, r.y);
    }

    @Test
    void snapsToOtherElements() {
        HudSnapper.Result r = new HudSnapper.Result();
        float[] other = {50, 40, 60, 20};
        // Just below the other element, left edges almost aligned.
        HudSnapper.snap(52, 63, 30, 10, 300, 200, other, 1, 5, 0, true, r);
        assertEquals(50, r.x, "left edges aligned");
        assertEquals(50, r.guideX);
        assertEquals(60, r.y, "placed right under it");
        assertEquals(60, r.guideY);
        // Element snapping off: only the free position remains.
        HudSnapper.snap(52, 63, 30, 10, 300, 200, other, 1, 5, 0, false, r);
        assertEquals(52, r.x);
        assertEquals(63, r.y);
    }

    @Test
    void gridAppliesOnlyWhereNothingElseSnapped() {
        HudSnapper.Result r = new HudSnapper.Result();
        HudSnapper.snap(43, 3, 30, 10, 300, 200, new float[0], 0, 5, 8, true, r);
        assertEquals(40, r.x, "grid");
        assertEquals(0, r.y, "screen edge wins over the grid");
        HudSnapper.snap(-20, 500, 30, 10, 300, 200, new float[0], 0, 5, 0, true, r);
        assertEquals(0, r.x, "clamped");
        assertEquals(190, r.y, "clamped");
    }

    // ------------------------------------------------------------------ layouts

    @Test
    void layoutRoundTrip(@TempDir Path dir) throws IOException {
        FpsDisplay fps = new FpsDisplay();
        PingDisplay ping = new PingDisplay();
        List<HudModule> elements = List.of(fps, ping);
        fps.setPosition(250, 5, 300, 200);
        fps.getScaleSetting().set(1.5);
        fps.setEnabled(true);
        ping.setEnabled(false);

        HudLayouts layouts = new HudLayouts(dir);
        layouts.save("pvp", elements);
        assertEquals(List.of("pvp"), layouts.list());

        fps.resetPosition();
        fps.getScaleSetting().reset();
        fps.setEnabled(false);
        ping.setEnabled(true);
        assertEquals(2, layouts.load("pvp", elements));
        assertEquals(Anchor.TOP_RIGHT, fps.getAnchor());
        assertEquals(400 - 60 - 10, fps.getX(400), 1e-3f, "same offset from the right edge at the saved scale");
        assertEquals(1.5f, fps.getScale());
        assertTrue(fps.isEnabled());
        assertFalse(ping.isEnabled());

        assertTrue(layouts.delete("pvp"));
        assertTrue(layouts.list().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> layouts.save("bad name!", elements));
    }

    @Test
    void layoutIgnoresUnknownElementsAndRejectsOtherJson() {
        FpsDisplay fps = new FpsDisplay();
        JsonObject root = HudLayouts.toJson(List.of(fps));
        root.getAsJsonObject("elements").add("unknownelement", new JsonObject());
        assertEquals(1, HudLayouts.apply(root, List.of(fps)));
        assertEquals(0, HudLayouts.apply(root, List.of(new PingDisplay())));
        assertThrows(IllegalArgumentException.class, () -> HudLayouts.apply(new JsonObject(), List.of(fps)));
    }

    // ------------------------------------------------------------------ animation helpers

    @Test
    void valueTransitionProgress() {
        assertEquals(0f, HudLine.progress(0, 1));
        assertEquals(1f, HudLine.progress(180, 1), 1e-6f);
        assertEquals(1f, HudLine.progress(10_000, 1));
        assertTrue(HudLine.progress(60, 1) > 0.5f, "ease-out: fast at the start");
        assertTrue(HudLine.progress(60, 2) > HudLine.progress(60, 1), "faster at a higher speed");
        float last = 0;
        for (int ms = 0; ms <= 200; ms += 10) {
            float p = HudLine.progress(ms, 1);
            assertTrue(p >= last);
            last = p;
        }
    }

    @Test
    void cachedTextReusesTheString() {
        CachedText text = new CachedText(" ms");
        String a = text.of(42);
        assertEquals("42 ms", a);
        assertSame(a, text.of(42));
        assertEquals("43 ms", text.of(43));
        assertEquals("0", new CachedText().of(0));
    }
}

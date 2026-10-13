package dev.aurora.client;

import com.google.gson.JsonObject;
import dev.aurora.client.chat.ChatSearchState;
import dev.aurora.client.config.ThemeIO;
import dev.aurora.client.event.EventBus;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudRenderer;
import dev.aurora.client.keybind.KeybindConflicts;
import dev.aurora.client.keybind.KeybindConflicts.Binding;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.Modules;
import dev.aurora.client.module.modules.misc.BetterChat;
import dev.aurora.client.module.modules.misc.ClickGui;
import dev.aurora.client.module.modules.player.ItemLock;
import dev.aurora.client.module.modules.utility.UnfocusedFps;
import dev.aurora.client.module.modules.visuals.HudTheme;
import dev.aurora.client.module.modules.world.EntityDistance;
import dev.aurora.client.module.modules.world.LowEndMode;
import dev.aurora.client.render.PixelPattern;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Part 2 (Batch C): game-independent logic. Screens, rendering and mixins need a running client. */
class BatchCTest {
    private static final Map<String, Boolean> NEW_MODULES = Map.of(
        "betterchat", false, "scoreboard", true, "crosshaireditor", false, "unfocusedfps", false,
        "entitydistance", false, "low-endmode", false, "itemlock", false);

    @Test
    void newModulesAreRegisteredAndOffByDefault() {
        ModuleManager manager = new ModuleManager(new EventBus());
        manager.registerAll(Modules.createAll());
        List<HudModule> huds = new HudRenderer(manager).getElements();
        NEW_MODULES.forEach((id, hud) -> {
            Module module = manager.find(id).orElse(null);
            assertNotNull(module, id + " is not registered");
            assertFalse(module.isEnabledByDefault(), id + " should be off by default");
            assertEquals(hud, huds.contains(module), id + (hud ? " should" : " should not") + " be a HUD element");
        });
    }

    // ------------------------------------------------------------------ better chat

    @Test
    void timestampsAndCleanup() {
        LocalTime t = LocalTime.of(14, 5, 9);
        assertEquals("[14:05]", BetterChat.stamp(BetterChat.TimeFormat.HH_MM, t));
        assertEquals("[14:05:09]", BetterChat.stamp(BetterChat.TimeFormat.HH_MM_SS, t));
        assertEquals("[2:05 PM]", BetterChat.stamp(BetterChat.TimeFormat.H_MM_A, t));
        assertEquals(" [x3]", BetterChat.stackSuffix(3));
        assertEquals("<Steve> hi", BetterChat.strip("[14:05] <Steve> hi [x3]"));
        assertEquals("<Steve> hi", BetterChat.strip("[2:05 PM] <Steve> hi"));
        assertEquals("[Party] hi [x2] there", BetterChat.strip("[Party] hi [x2] there"), "only our own decorations are removed");
    }

    @Test
    void historyOnlyGrowsWhileEnabled() {
        BetterChat chat = new BetterChat();
        assertEquals(100, chat.historySize(100));
        chat.setEnabled(true);
        assertEquals(500, chat.historySize(100));
    }

    @Test
    void stackingRemovesOnlyTheNewestMatchingMessage() {
        Text old = Text.literal("old");
        Text repeated = Text.literal("hello");
        List<ChatHudLine> messages = new ArrayList<>(List.of(
            new ChatHudLine(2, repeated, null, null), new ChatHudLine(1, old, null, null)));
        // Newest first; "hello" wrapped into two lines, its last line (endOfEntry) at the front.
        List<ChatHudLine.Visible> visible = new ArrayList<>(List.of(
            new ChatHudLine.Visible(2, OrderedText.EMPTY, null, true),
            new ChatHudLine.Visible(2, OrderedText.EMPTY, null, false),
            new ChatHudLine.Visible(1, OrderedText.EMPTY, null, true)));

        assertFalse(BetterChat.removeNewest(messages, visible, old), "not the newest message");
        assertEquals(2, messages.size());
        assertTrue(BetterChat.removeNewest(messages, visible, repeated));
        assertEquals(1, messages.size());
        assertEquals(1, visible.size());
        assertEquals(1, visible.get(0).addedTime(), "the older message keeps its line");
        assertTrue(BetterChat.removeNewest(messages, null, old), "wrapped lines can be left for a refresh");
        assertTrue(messages.isEmpty());
        assertFalse(BetterChat.removeNewest(messages, visible, old));
    }

    @Test
    void chatSearchMatching() {
        try {
            assertFalse(ChatSearchState.isFiltering());
            assertTrue(ChatSearchState.matches("anything"));
            assertTrue(ChatSearchState.setQuery("  Bed "));
            assertFalse(ChatSearchState.setQuery("Bed"), "same query after trimming");
            assertTrue(ChatSearchState.isFiltering());
            assertTrue(ChatSearchState.matches("Your BED was destroyed"));
            assertFalse(ChatSearchState.matches("final kill"));
        } finally {
            ChatSearchState.setQuery("");
        }
    }

    // ------------------------------------------------------------------ keybinds

    @Test
    void conflictsInvolveAuroraBindsOnly() {
        Binding zoom = new Binding("Zoom", 67, false, null);
        Binding saveHotbar = new Binding("Save Hotbar Activator", 67, true, null);
        Binding vanillaA = new Binding("Attack", 1000, true, null);
        Binding vanillaB = new Binding("Other Attack", 1000, true, null);
        Binding gui = new Binding("ClickGUI", 344, false, null);
        Binding unboundA = new Binding("Sprint", KeybindSetting.NONE, false, null);
        Binding unboundB = new Binding("Step", KeybindSetting.NONE, false, null);
        Binding freelook = new Binding("Freelook", 342, false, null);
        Binding perspective = new Binding("Perspective", 342, false, null);
        Map<Binding, List<Binding>> found = KeybindConflicts.find(List.of(zoom, saveHotbar, vanillaA, vanillaB, gui, unboundA, unboundB,
            freelook, perspective));
        assertEquals(List.of(saveHotbar), found.get(zoom));
        assertEquals(List.of(perspective), found.get(freelook));
        assertEquals(List.of(freelook), found.get(perspective));
        assertNull(found.get(gui));
        assertNull(found.get(unboundA), "unbound binds never conflict");
        assertNull(found.get(vanillaA), "vanilla-only conflicts are left to vanilla");
        assertEquals(3, found.size());
        assertEquals("Also: Save Hotbar Activator", KeybindConflicts.describe(found.get(zoom)));
    }

    // ------------------------------------------------------------------ favorites

    @Test
    void favoritesKeepPinOrder() {
        ClickGui gui = new ClickGui();
        Module zoom = new dev.aurora.client.module.modules.utility.Zoom();
        Module fps = new dev.aurora.client.module.modules.utility.FpsDisplay();
        assertFalse(gui.isFavorite(zoom));
        assertTrue(gui.toggleFavorite(zoom));
        assertTrue(gui.toggleFavorite(fps));
        assertEquals(List.of("zoom", "fps"), gui.favoriteIds());
        assertFalse(gui.toggleFavorite(zoom));
        assertEquals(List.of("fps"), gui.favoriteIds());
        assertEquals(List.of("a", "b"), new ArrayList<>(ClickGui.parseFavorites(" A,,b , a ")));
    }

    // ------------------------------------------------------------------ theme import/export

    @Test
    @SuppressWarnings("unchecked")
    void themeRoundTrip() {
        ClickGui gui = new ClickGui();
        HudTheme hud = new HudTheme();
        ((EnumSetting<dev.aurora.client.gui.Theme.Preset>) gui.getSetting("Theme")).set(dev.aurora.client.gui.Theme.Preset.OCEAN);
        ((ColorSetting) gui.getSetting("Accent")).set(0xFF123456);
        ((NumberSetting) gui.getSetting("GUI Scale")).set(1.4);
        ((EnumSetting<HudTheme.Preset>) hud.getSetting("Preset")).set(HudTheme.Preset.NEON);
        ((NumberSetting) hud.getSetting("Padding")).set(6.0);
        hud.setEnabled(true);
        String json = ThemeIO.exportString(gui, hud);

        ClickGui gui2 = new ClickGui();
        HudTheme hud2 = new HudTheme();
        assertTrue(ThemeIO.importString(json, gui2, hud2) > 10);
        assertEquals(dev.aurora.client.gui.Theme.Preset.OCEAN, gui2.getPreset());
        assertEquals(0xFF123456, gui2.getAccent(), "accent survives the preset (which resets it) because it loads after");
        assertEquals(1.0f, gui2.getGuiScale(), "layout values are not part of a theme");
        assertTrue(hud2.isEnabled());
        assertTrue(hud2.outline(), "neon preset applied");
        assertEquals(6, hud2.padding(), "tweak after the preset kept");
    }

    @Test
    void themeImportRejectsOtherJson() {
        ClickGui gui = new ClickGui();
        HudTheme hud = new HudTheme();
        assertThrows(IllegalArgumentException.class, () -> ThemeIO.importString("not json {", gui, hud));
        assertThrows(IllegalArgumentException.class, () -> ThemeIO.importString("{\"modules\":{}}", gui, hud));
        assertThrows(IllegalArgumentException.class, () -> ThemeIO.importString("[1,2]", gui, hud));
        JsonObject minimal = new JsonObject();
        minimal.addProperty("type", ThemeIO.TYPE);
        assertEquals(0, ThemeIO.apply(minimal, gui, hud));
    }

    // ------------------------------------------------------------------ pixel crosshair

    @Test
    void pixelPatternRoundTrip() {
        for (PixelPattern.Preset p : PixelPattern.Preset.values()) {
            boolean[] pixels = PixelPattern.preset(p);
            assertTrue(PixelPattern.count(pixels) > 0, p + " is empty");
            assertArrayEquals(pixels, PixelPattern.decode(PixelPattern.encode(pixels)), p.name());
        }
        assertEquals(60, PixelPattern.DEFAULT.length());
        assertNull(PixelPattern.decode("123"));
        assertNull(PixelPattern.decode("zz" + PixelPattern.DEFAULT.substring(2)));
        assertNull(PixelPattern.decode("8000" + PixelPattern.DEFAULT.substring(4)), "bit 15 is outside the grid");
        boolean[] dot = PixelPattern.preset(PixelPattern.Preset.DOT);
        assertTrue(dot[PixelPattern.index(PixelPattern.CENTER, PixelPattern.CENTER)]);
        assertEquals(PixelPattern.SIZE * PixelPattern.SIZE - 1, PixelPattern.count(PixelPattern.invert(dot)));
    }

    @Test
    void presetsAreSymmetric() {
        for (PixelPattern.Preset p : new PixelPattern.Preset[]{PixelPattern.Preset.CROSS, PixelPattern.Preset.GAP_CROSS,
            PixelPattern.Preset.CIRCLE, PixelPattern.Preset.X}) {
            boolean[] px = PixelPattern.preset(p);
            for (int y = 0; y < PixelPattern.SIZE; y++) {
                for (int x = 0; x < PixelPattern.SIZE; x++) {
                    assertEquals(px[PixelPattern.index(x, y)], px[PixelPattern.index(PixelPattern.SIZE - 1 - x, y)], p + " mirror x");
                    assertEquals(px[PixelPattern.index(x, y)], px[PixelPattern.index(x, PixelPattern.SIZE - 1 - y)], p + " mirror y");
                }
            }
        }
    }

    @Test
    void runsMergeRows() {
        boolean[] px = PixelPattern.empty();
        px[PixelPattern.index(1, 0)] = true;
        px[PixelPattern.index(2, 0)] = true;
        px[PixelPattern.index(3, 0)] = true;
        px[PixelPattern.index(6, 0)] = true;
        px[PixelPattern.index(14, 14)] = true;
        float[] out = new float[PixelPattern.maxRuns() * 4];
        assertEquals(3, PixelPattern.runs(px, out));
        assertArrayEquals(new float[]{1, 0, 3, 1, 6, 0, 1, 1, 14, 14, 1, 1}, java.util.Arrays.copyOf(out, 12));
        boolean[] checker = PixelPattern.empty();
        for (int i = 0; i < checker.length; i += 2) checker[i] = true;
        assertTrue(PixelPattern.runs(checker, new float[PixelPattern.maxRuns() * 4]) <= PixelPattern.maxRuns());
    }

    @Test
    void mirroredBrush() {
        assertEquals(1, PixelPattern.mirrored(3, 4, false, false).length);
        assertArrayEquals(new int[]{PixelPattern.index(3, 4), PixelPattern.index(11, 4)}, PixelPattern.mirrored(3, 4, true, false));
        assertEquals(4, PixelPattern.mirrored(3, 4, true, true).length);
    }

    // ------------------------------------------------------------------ performance modules

    @Test
    void unfocusedFrameLimit() {
        assertEquals(144, UnfocusedFps.limit(144, true, false, 15, 5));
        assertEquals(15, UnfocusedFps.limit(144, false, false, 15, 5));
        assertEquals(5, UnfocusedFps.limit(144, false, true, 15, 5));
        assertEquals(10, UnfocusedFps.limit(10, false, false, 15, 5), "never raises vanilla's limit");
    }

    @Test
    void entityDistance() {
        assertFalse(EntityDistance.beyond(30, 0, 40, 50));
        assertTrue(EntityDistance.beyond(30, 1, 40, 50));
        assertTrue(EntityDistance.beyond(0, -65, 0, 64));
    }

    @Test
    void lowEndBackupFormat() {
        Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("particles", "ALL");
        values.put("viewDistance", "12");
        assertEquals(values, LowEndMode.decode(LowEndMode.encode(values)));
        assertEquals(Map.of("a", "1"), LowEndMode.decode("a=1\r\ngarbage\n=x\n"));
    }

    // ------------------------------------------------------------------ item lock

    @Test
    void itemLockSlots() {
        long mask = ItemLock.parse("0, 8,40,41,-1,x");
        assertTrue(ItemLock.isSet(mask, 0));
        assertTrue(ItemLock.isSet(mask, 8));
        assertTrue(ItemLock.isSet(mask, 40));
        assertFalse(ItemLock.isSet(mask, 41));
        assertFalse(ItemLock.isSet(mask, 1));
        assertEquals("0,8,40", ItemLock.format(mask));
        assertEquals(0, ItemLock.parse(""));
        assertEquals("Hotbar slot 1", ItemLock.slotName(0));
        assertEquals("Inventory slot 1", ItemLock.slotName(9));
        assertEquals("Armor slot 4", ItemLock.slotName(39));
        assertEquals("Offhand slot", ItemLock.slotName(40));
        assertFalse(new ItemLock().isLocked(0), "nothing is locked while the module is off");
    }
}

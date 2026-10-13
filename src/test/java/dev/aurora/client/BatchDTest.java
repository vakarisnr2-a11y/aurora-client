package dev.aurora.client;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudRenderer;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.Modules;
import dev.aurora.client.module.modules.combat.HitSounds;
import dev.aurora.client.module.modules.misc.ChatCopy;
import dev.aurora.client.module.modules.misc.ChatMentions;
import dev.aurora.client.module.modules.utility.ArmorWarning;
import dev.aurora.client.module.modules.utility.BetterTab;
import dev.aurora.client.module.modules.utility.BetterTooltips;
import dev.aurora.client.module.modules.utility.Screenshot;
import dev.aurora.client.module.modules.utility.Zoom;
import dev.aurora.client.module.modules.visuals.AuroraMenu;
import dev.aurora.client.module.modules.visuals.LowHealthWarning;
import dev.aurora.client.module.modules.visuals.SaturationOverlay;
import dev.aurora.client.util.ImageClipboard;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Part 3 (Batch D): game-independent logic. Rendering, screens and mixins need a running client. */
class BatchDTest {
    private static final List<String> NEW_MODULES = List.of("chatmentions", "chatcopy", "chatsearch", "bettertab",
        "saturationoverlay", "bettertooltips", "lowhealthwarning", "armorwarning", "hitsounds");

    @Test
    void newModulesAreRegisteredOffByDefaultAndNotHudElements() {
        ModuleManager manager = new ModuleManager(new EventBus());
        manager.registerAll(Modules.createAll());
        List<HudModule> huds = new HudRenderer(manager).getElements();
        for (String id : NEW_MODULES) {
            Module module = manager.find(id).orElse(null);
            assertNotNull(module, id + " is not registered");
            assertFalse(module.isEnabledByDefault(), id + " should be off by default");
            assertFalse(huds.contains(module), id + " is not a HUD element");
        }
    }

    // ------------------------------------------------------------------ chat mentions

    @Test
    void mentionNames() {
        assertEquals(List.of("Steve", "Stevie", "st"), ChatMentions.names("Steve", "Stevie, steve, st ,"));
        assertEquals(List.of("nick"), ChatMentions.names(null, "nick"));
        assertTrue(ChatMentions.names(" ", "").isEmpty());
    }

    @Test
    void mentionsAreWholeWordAndCaseInsensitive() {
        List<String> names = List.of("Steve", "Steve_Pro");
        assertRanges(new int[][]{{4, 9}}, ChatMentions.findMentions("hey steve!", names));
        assertRanges(new int[][]{}, ChatMentions.findMentions("Steven and xSteve", names));
        assertRanges(new int[][]{{0, 9}}, ChatMentions.findMentions("Steve_Pro joined", names));
        assertRanges(new int[][]{{0, 5}, {10, 15}}, ChatMentions.findMentions("STEVE vs. Steve", names));
        assertRanges(new int[][]{{8, 13}}, ChatMentions.findMentions("<Alex> @Steve gg", names));
    }

    private static void assertRanges(int[][] expected, List<int[]> actual) {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) assertArrayEquals(expected[i], actual.get(i));
    }

    @Test
    void legacyColorCodesCarryOver() {
        assertEquals("", ChatMentions.activeCodes("hello Steve", 6));
        assertEquals("§a", ChatMentions.activeCodes("§ahello Steve", 8));
        assertEquals("§c§l", ChatMentions.activeCodes("§a§lx §c§lhey Steve", 13));
        assertEquals("", ChatMentions.activeCodes("§ahi §rthere Steve", 13));
    }

    // ------------------------------------------------------------------ chat copy

    @Test
    void copyFindsTheWholeMessage() {
        // Newest first: message 0 has two lines (its last line first), message 1 one line, message 2 three lines.
        List<ChatHudLine.Visible> lines = List.of(
            line(true), line(false),
            line(true),
            line(true), line(false), line(false));
        assertEquals(0, ChatCopy.entryIndex(lines, 0));
        assertEquals(0, ChatCopy.entryIndex(lines, 1));
        assertEquals(1, ChatCopy.entryIndex(lines, 2));
        assertEquals(2, ChatCopy.entryIndex(lines, 5));
        assertArrayEquals(new int[]{0, 1}, ChatCopy.entryLines(lines, 1));
        assertArrayEquals(new int[]{2, 2}, ChatCopy.entryLines(lines, 2));
        assertArrayEquals(new int[]{3, 5}, ChatCopy.entryLines(lines, 4));
    }

    private static ChatHudLine.Visible line(boolean endOfEntry) {
        return new ChatHudLine.Visible(0, OrderedText.EMPTY, null, endOfEntry);
    }

    // ------------------------------------------------------------------ tab, tooltips, overlays

    @Test
    void tabOrdering() {
        // name:ping:team
        List<String> players = List.of("bob:120:red", "Alice:30:blue", "carl:-1:blue", "dave:80:red");
        java.util.function.Function<String, String> name = s -> s.split(":")[0];
        java.util.function.ToIntFunction<String> ping = s -> Integer.parseInt(s.split(":")[1]);
        java.util.function.Predicate<String> blue = s -> s.endsWith("blue");
        assertEquals(players, BetterTab.order(players, name, ping, blue, BetterTab.Sort.VANILLA, false));
        assertEquals(List.of("Alice:30:blue", "bob:120:red", "carl:-1:blue", "dave:80:red"),
            BetterTab.order(players, name, ping, blue, BetterTab.Sort.NAME, false));
        assertEquals(List.of("Alice:30:blue", "dave:80:red", "bob:120:red", "carl:-1:blue"),
            BetterTab.order(players, name, ping, blue, BetterTab.Sort.PING, false), "unknown ping last");
        assertEquals(List.of("Alice:30:blue", "carl:-1:blue", "bob:120:red", "dave:80:red"),
            BetterTab.order(players, name, ping, blue, BetterTab.Sort.VANILLA, true), "own team first, order kept");
    }

    @Test
    void enchantmentLevelNumbers() {
        assertEquals("5", BetterTooltips.levelNumber("enchantment.level.5"));
        assertEquals("255", BetterTooltips.levelNumber("enchantment.level.255"));
        assertNull(BetterTooltips.levelNumber("enchantment.level."));
        assertNull(BetterTooltips.levelNumber("enchantment.minecraft.sharpness"));
        assertNull(BetterTooltips.levelNumber("enchantment.level.x"));
        assertNull(BetterTooltips.levelNumber(null));
    }

    @Test
    void saturationPerIcon() {
        assertEquals(1f, SaturationOverlay.fraction(5f, 0));
        assertEquals(1f, SaturationOverlay.fraction(5f, 1));
        assertEquals(0.5f, SaturationOverlay.fraction(5f, 2));
        assertEquals(0f, SaturationOverlay.fraction(5f, 3));
        assertEquals(0f, SaturationOverlay.fraction(0f, 0));
        assertEquals(1f, SaturationOverlay.fraction(20f, 9));
    }

    @Test
    void lowHealthStrength() {
        assertEquals(0f, LowHealthWarning.strength(9, 8));
        assertEquals(0.35f, LowHealthWarning.strength(8, 8), 1e-6f);
        assertEquals(1f, LowHealthWarning.strength(0, 8), 1e-6f);
        assertTrue(LowHealthWarning.strength(2, 8) > LowHealthWarning.strength(6, 8));
        assertEquals(0f, LowHealthWarning.strength(1, 0));
    }

    @Test
    void armorWarnsOncePerDrop() {
        assertEquals(10f, ArmorWarning.percent(55, 550), 1e-4f);
        assertEquals(100f, ArmorWarning.percent(0, 0));
        assertTrue(ArmorWarning.shouldWarn(false, true));
        assertFalse(ArmorWarning.shouldWarn(true, true), "already warned while it stays low");
        assertFalse(ArmorWarning.shouldWarn(false, false));
    }

    // ------------------------------------------------------------------ hit sounds, zoom, screenshots

    @Test
    void vanillaAttackSounds() {
        assertTrue(HitSounds.isVanillaAttack(Identifier.of("minecraft", "entity.player.attack.crit")));
        assertTrue(HitSounds.isVanillaAttack(Identifier.of("minecraft", "entity.player.attack.nodamage")));
        assertFalse(HitSounds.isVanillaAttack(Identifier.of("minecraft", "entity.player.hurt")));
        assertFalse(HitSounds.isVanillaAttack(Identifier.of("othermod", "entity.player.attack.crit")));
        assertFalse(HitSounds.isVanillaAttack(null));
        assertEquals(15, HitSounds.HitSound.values().length);
    }

    @Test
    void zoomScrollSteps() {
        assertEquals(4.6, Zoom.scrollLevel(4, 1, 0.15, 1.5, 50), 1e-9);
        assertEquals(4 / 1.15, Zoom.scrollLevel(4, -1, 0.15, 1.5, 50), 1e-9);
        assertEquals(50, Zoom.scrollLevel(48, 1, 0.15, 1.5, 50), 1e-9, "clamped to max");
        assertEquals(1.5, Zoom.scrollLevel(1.6, -1, 0.15, 1.5, 50), 1e-9, "clamped to min");
        assertEquals(6, Zoom.scrollLevel(4, 1, 0.5, 10, 5), 1e-9, "swapped bounds are treated as a range");
        assertEquals(10, Zoom.scrollLevel(9, 1, 0.5, 10, 5), 1e-9);
    }

    @Test
    void screenshotNamesLikeVanilla() {
        LocalDateTime t = LocalDateTime.of(2026, 5, 1, 14, 3, 22);
        assertEquals("2026-05-01_14.03.22.png", Screenshot.fileName(t, n -> false));
        Set<String> taken = Set.of("2026-05-01_14.03.22.png", "2026-05-01_14.03.22_1.png");
        assertEquals("2026-05-01_14.03.22_2.png", Screenshot.fileName(t, taken::contains));
    }

    @Test
    void imageClipboardCommands() {
        Path file = Path.of("/tmp/it's \"shot\".png");
        List<String> win = ImageClipboard.command(ImageClipboard.Os.WINDOWS, file, false);
        assertEquals("powershell", win.get(0));
        assertTrue(win.get(win.size() - 1).contains("'/tmp/it''s \"shot\".png'"), "single quotes doubled for PowerShell");
        List<String> mac = ImageClipboard.command(ImageClipboard.Os.MAC, file, false);
        assertTrue(mac.get(2).contains("POSIX file \"/tmp/it's \\\"shot\\\".png\""), mac.get(2));
        assertEquals(List.of("wl-copy", "--type", "image/png"), ImageClipboard.command(ImageClipboard.Os.LINUX, file, true));
        assertEquals("xclip", ImageClipboard.command(ImageClipboard.Os.LINUX, file, false).get(0));
        assertNull(ImageClipboard.command(ImageClipboard.Os.OTHER, file, false));
    }

    // ------------------------------------------------------------------ menu

    @Test
    void auroraMenuIsOnByDefault() {
        assertTrue(new AuroraMenu().isEnabledByDefault(), "part of the redesigned look");
    }

    @Test
    void vanillaMenuOnce() {
        AuroraMenu menu = new AuroraMenu();
        assertFalse(menu.replaceTitleScreen(), "nothing is replaced while the module is disabled");
        menu.setEnabled(true);
        assertTrue(menu.replaceTitleScreen());
        AuroraMenu.showVanillaOnce();
        assertFalse(menu.replaceTitleScreen(), "the vanilla menu was requested once");
        assertTrue(menu.replaceTitleScreen());
    }
}

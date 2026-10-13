package dev.aurora.client;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.hud.HudRenderer;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.Modules;
import dev.aurora.client.module.modules.misc.ChatAlerts;
import dev.aurora.client.module.modules.utility.ResourceCounter;
import dev.aurora.client.module.modules.utility.TabPing;
import dev.aurora.client.module.modules.visuals.GearTags;
import dev.aurora.client.module.modules.visuals.GearTags.Tier;
import dev.aurora.client.module.modules.world.TntTimer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Game-independent logic of the Bedwars modules (rendering and mixins need a running client). */
class BedwarsFeaturesTest {
    @Test
    void newModulesAreRegistered() {
        ModuleManager manager = new ModuleManager(new EventBus());
        manager.registerAll(Modules.createAll());
        for (String id : List.of("resourcecounter", "tnttimer", "chatalerts", "geartags", "tabping")) {
            assertTrue(manager.find(id).isPresent(), id + " is not registered");
        }
        assertTrue(new HudRenderer(manager).getElements().contains(manager.get(ResourceCounter.class)), "Resource Counter is not a HUD element");
    }

    // ------------------------------------------------------------------ chat alerts

    @Test
    void chatAlertKeywordsAreEmptyByDefault() {
        ChatAlerts alerts = new ChatAlerts();
        assertEquals("", alerts.getSetting("Keywords").get());
        assertTrue(ChatAlerts.parseKeywords("").isEmpty());
        assertTrue(ChatAlerts.parseKeywords(" , ,").isEmpty());
        assertNull(ChatAlerts.findKeyword("anything at all", ChatAlerts.parseKeywords(""), false, false));
    }

    @Test
    void chatAlertKeywordsAreTrimmedAndSplitOnCommas() {
        assertEquals(List.of("bed", "final kill", "diamond"), ChatAlerts.parseKeywords(" bed ,final kill,, diamond "));
    }

    @Test
    void chatAlertMatching() {
        List<String> keywords = List.of("bed", "Final Kill");
        assertEquals("bed", ChatAlerts.findKeyword("BED DESTRUCTION > Your Bed was destroyed", keywords, false, false));
        assertNull(ChatAlerts.findKeyword("BED DESTRUCTION", keywords, true, false));
        assertEquals("Final Kill", ChatAlerts.findKeyword("Steve was killed by Alex. FINAL KILL!", keywords, false, true));
        // Whole words: "bed" must not fire on "bedrock", but substring mode does.
        assertNull(ChatAlerts.findKeyword("you hit bedrock", List.of("bed"), false, true));
        assertEquals("bed", ChatAlerts.findKeyword("you hit bedrock", List.of("bed"), false, false));
        assertEquals("bed", ChatAlerts.findKeyword("bedrock, then bed.", List.of("bed"), false, true));
    }

    @Test
    void chatAlertIgnoresColorCodesAndOwnMessages() {
        assertEquals("Your Bed was destroyed", ChatAlerts.stripFormatting("§cYour §lBed§r was destroyed"));
        assertEquals("plain", ChatAlerts.stripFormatting("plain"));
        assertTrue(ChatAlerts.isOwnMessage("<Steve> bed", "Steve"));
        assertTrue(ChatAlerts.isOwnMessage("[MVP+] Steve: bed please", "Steve"));
        assertTrue(ChatAlerts.isOwnMessage("Party > [VIP] Steve: bed", "Steve"));
        assertTrue(ChatAlerts.isOwnMessage("Steve: bed", "Steve"));
        assertFalse(ChatAlerts.isOwnMessage("[MVP+] xSteve: bed", "Steve"));
        assertFalse(ChatAlerts.isOwnMessage("Alex: Steve, bed", "Steve"));
        assertFalse(ChatAlerts.isOwnMessage("Your bed was destroyed by Steve", "Steve"));
        assertFalse(ChatAlerts.isOwnMessage("Steve: bed", ""));
    }

    // ------------------------------------------------------------------ gear tags

    @Test
    void gearTiersFromItemIds() {
        assertEquals(Tier.LEATHER, GearTags.armorTier("leather_helmet"));
        assertEquals(Tier.CHAINMAIL, GearTags.armorTier("chainmail_leggings"));
        assertEquals(Tier.IRON, GearTags.armorTier("iron_boots"));
        assertEquals(Tier.DIAMOND, GearTags.armorTier("diamond_chestplate"));
        assertEquals(Tier.NETHERITE, GearTags.armorTier("netherite_boots"));
        assertEquals(Tier.GOLD, GearTags.armorTier("golden_helmet"));
        assertNull(GearTags.armorTier("turtle_helmet"));
        assertNull(GearTags.armorTier("diamond_sword"));
        assertNull(GearTags.armorTier(""));

        assertEquals(Tier.WOOD, GearTags.swordTier("wooden_sword"));
        assertEquals(Tier.STONE, GearTags.swordTier("stone_sword"));
        assertEquals(Tier.GOLD, GearTags.swordTier("golden_sword"));
        assertEquals(Tier.DIAMOND, GearTags.swordTier("diamond_sword"));
        assertNull(GearTags.swordTier("diamond_pickaxe"));
        assertNull(GearTags.swordTier("stick"));
    }

    @Test
    void bestArmorTierWins() {
        // Bedwars: leather helmet and chestplate, upgraded leggings and boots.
        Tier best = null;
        for (String piece : List.of("leather_helmet", "leather_chestplate", "iron_leggings", "iron_boots")) {
            best = GearTags.better(best, GearTags.armorTier(piece));
        }
        assertEquals(Tier.IRON, best);
        assertEquals(Tier.CHAINMAIL, GearTags.better(Tier.GOLD, Tier.CHAINMAIL));
        assertEquals(Tier.DIAMOND, GearTags.better(Tier.DIAMOND, null));
        assertNull(GearTags.better(null, null));
    }

    // ------------------------------------------------------------------ TNT timer

    @Test
    void tntTimerFormatting() {
        assertEquals("2.0s", TntTimer.format(40, 1));
        assertEquals("2.40s", TntTimer.format(48, 2));
        assertEquals("0.0s", TntTimer.format(-3, 1));
        // No decimals rounds up: 0.05 s left still reads "1s", only an expired fuse reads "0s".
        assertEquals("1s", TntTimer.format(1, 0));
        assertEquals("2s", TntTimer.format(30, 0));
        assertEquals("0s", TntTimer.format(0, 0));
    }

    @Test
    void tntTimerInterpolatesWithinTheTick() {
        assertEquals(79.5f, TntTimer.ticksLeft(80, 0.5f), 1e-6);
        assertEquals(0f, TntTimer.ticksLeft(0, 0.9f), 1e-6);
    }

    // ------------------------------------------------------------------ tab ping

    @Test
    void tabPingFormatting() {
        assertEquals("42ms", TabPing.format(42, true));
        assertEquals("42", TabPing.format(42, false));
        assertEquals("0ms", TabPing.format(0, true));
        assertEquals("?", TabPing.format(-1, true));
    }

    @Test
    void tabPingColorGetsWorseWithLatency() {
        assertEquals(TabPing.color(0), TabPing.color(80));
        assertNotEquals(TabPing.color(80), TabPing.color(81));
        assertNotEquals(TabPing.color(160), TabPing.color(161));
        assertNotEquals(TabPing.color(300), TabPing.color(301));
        assertEquals(TabPing.color(301), TabPing.color(5000));
    }

    // ------------------------------------------------------------------ resource counter

    @Test
    void resourceVisibility() {
        assertTrue(ResourceCounter.isShown(true, 5, true, false));
        assertTrue(ResourceCounter.isShown(true, 0, false, false));
        assertFalse(ResourceCounter.isShown(true, 0, true, false));
        assertTrue(ResourceCounter.isShown(true, 0, true, true), "the HUD editor shows empty resources so the element can be placed");
        assertFalse(ResourceCounter.isShown(false, 64, false, true));
    }
}

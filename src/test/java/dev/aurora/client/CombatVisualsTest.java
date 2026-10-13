package dev.aurora.client;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudRenderer;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.Modules;
import dev.aurora.client.module.modules.combat.ArrowCounter;
import dev.aurora.client.module.modules.combat.BowCharge;
import dev.aurora.client.module.modules.combat.DamageNumbers;
import dev.aurora.client.module.modules.combat.DamageNumbers.Unit;
import dev.aurora.client.module.modules.combat.HitParticles;
import dev.aurora.client.module.modules.combat.Hitboxes;
import dev.aurora.client.module.modules.combat.Hitboxes.Kind;
import dev.aurora.client.module.modules.combat.TotemPops;
import dev.aurora.client.module.modules.visuals.HotbarStyle;
import dev.aurora.client.module.modules.visuals.HotbarStyle.Part;
import dev.aurora.client.module.modules.visuals.OldAnimations;
import dev.aurora.client.module.modules.world.ItemPhysics;
import dev.aurora.client.module.modules.world.ParticleLimiter;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Game-independent logic of the Batch B combat/visual modules (rendering and mixins need a running client). */
class CombatVisualsTest {
    private static final Map<String, Boolean> NEW_MODULES = Map.of(
        "oldanimations", false, "damagenumbers", false, "hitparticles", false, "hitboxes", false, "arrowcounter", true,
        "bowcharge", true, "totempops", true, "itemphysics", false, "particlelimiter", false, "hotbarstyle", false);

    @Test
    void newModulesAreRegisteredAndOffByDefault() {
        ModuleManager manager = new ModuleManager(new EventBus());
        manager.registerAll(Modules.createAll());
        List<HudModule> huds = new HudRenderer(manager).getElements();
        NEW_MODULES.forEach((id, hud) -> {
            Module module = manager.find(id).orElse(null);
            assertTrue(module != null, id + " is not registered");
            assertFalse(module.isEnabledByDefault(), id + " should be off by default");
            assertEquals(hud, huds.contains(module), id + (hud ? " should" : " should not") + " be a HUD element");
        });
    }

    // ------------------------------------------------------------------ old animations

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    /**
     * 1.8 drew the blocking sword with transformFirstPersonItem (Y 45, scale 0.4) + doBlockTransformations and a
     * first-person model rotated Y -135. Modern models are rotated Y -90 instead and include the 0.4 scale, so the
     * modern pose followed by the modern model rotation must equal the 1.8 chain with the scale taken out.
     */
    @Test
    void swordBlockPoseMatches18() {
        Matrix4f old = new Matrix4f().rotateY(rad(45)).scale(0.4f)
            .translate(-0.5f, 0.2f, 0f).rotateY(rad(30)).rotateX(rad(-80)).rotateY(rad(60))
            .rotateY(rad(-135)).scale(1 / 0.4f);
        MatrixStack modern = new MatrixStack();
        OldAnimations.applyBlockTransform(modern, true);
        modern.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
        assertTrue(modern.peek().getPositionMatrix().equals(old, 1e-5f), "modern pose differs from 1.8:\n" + modern.peek().getPositionMatrix() + "\n" + old);
    }

    @Test
    void leftHandSwordBlockIsMirrored() {
        MatrixStack right = new MatrixStack();
        OldAnimations.applyBlockTransform(right, true);
        MatrixStack left = new MatrixStack();
        OldAnimations.applyBlockTransform(left, false);
        Matrix4f mirrorX = new Matrix4f().scale(-1, 1, 1);
        Matrix4f mirrored = new Matrix4f(mirrorX).mul(right.peek().getPositionMatrix()).mul(mirrorX);
        assertTrue(left.peek().getPositionMatrix().equals(mirrored, 1e-5f));
    }

    @Test
    void swordBlockOnlyForASwordInTheMainHandWhileUseIsHeld() {
        assertTrue(OldAnimations.swordBlockPose(true, true, true, false, true));
        assertFalse(OldAnimations.swordBlockPose(false, true, true, false, true), "setting off");
        assertFalse(OldAnimations.swordBlockPose(true, false, true, false, true), "offhand");
        assertFalse(OldAnimations.swordBlockPose(true, true, false, false, true), "use not held");
        assertFalse(OldAnimations.swordBlockPose(true, true, true, true, true), "eating or using a shield");
        assertFalse(OldAnimations.swordBlockPose(true, true, true, false, false), "not a sword");
    }

    // ------------------------------------------------------------------ damage numbers

    @Test
    void damageNumberFormatting() {
        assertEquals("4", DamageNumbers.format(-4f, Unit.HEALTH, 1, false));
        assertEquals("4.5", DamageNumbers.format(4.5f, Unit.HEALTH, 1, false));
        assertEquals("4.25", DamageNumbers.format(4.25f, Unit.HEALTH, 2, false));
        assertEquals("10", DamageNumbers.format(10f, Unit.HEALTH, 0, false));
        assertEquals("20", DamageNumbers.format(20f, Unit.HEALTH, 2, false));
        assertEquals("2.5❤", DamageNumbers.format(5f, Unit.HEARTS, 1, false));
        assertEquals("+2", DamageNumbers.format(2f, Unit.HEALTH, 1, true));
        assertEquals("+1❤", DamageNumbers.format(2f, Unit.HEARTS, 1, true));
    }

    @Test
    void damageNeedsTheHurtFlashAndHealingIgnoresRespawns() {
        assertTrue(DamageNumbers.isDamage(-3f, true));
        assertFalse(DamageNumbers.isDamage(-3f, false), "health arriving late without a hit is not damage");
        assertFalse(DamageNumbers.isDamage(-0.01f, true), "rounding noise");
        assertFalse(DamageNumbers.isDamage(2f, true));
        assertTrue(DamageNumbers.isHeal(2f, 10f));
        assertFalse(DamageNumbers.isHeal(20f, 0f), "respawn");
        assertFalse(DamageNumbers.isHeal(-2f, 10f));
    }

    @Test
    void damageNumberAnimation() {
        assertEquals(255, DamageNumbers.alpha(0f));
        assertEquals(255, DamageNumbers.alpha(0.6f));
        assertTrue(Math.abs(DamageNumbers.alpha(0.8f) - 128) <= 1, "half faded");
        assertEquals(0, DamageNumbers.alpha(1f));
        assertEquals(0f, DamageNumbers.rise(0f), 1e-6);
        assertEquals(0.6f, DamageNumbers.rise(1f), 1e-6);
        assertTrue(DamageNumbers.rise(0.3f) < DamageNumbers.rise(0.6f));
        assertEquals(1.3f, DamageNumbers.popScale(0f), 1e-6);
        assertEquals(1f, DamageNumbers.popScale(0.1f), 1e-6);
    }

    // ------------------------------------------------------------------ hit particles / particle limiter

    @Test
    void hitParticleMultiplier() {
        assertEquals(0, HitParticles.extra(1));
        assertEquals(2, HitParticles.extra(3));
        assertEquals(0, HitParticles.extra(0));
        assertTrue(HitParticles.hadServerEmitter(5, 1000, 5, 1200));
        assertFalse(HitParticles.hadServerEmitter(5, 1000, 6, 1200), "other entity");
        assertFalse(HitParticles.hadServerEmitter(5, 1000, 5, 2000), "too old");
    }

    @Test
    void thinnerKeepsAnEvenFraction() {
        assertEquals(100, kept(1.0, 100));
        assertEquals(0, kept(0.0, 100));
        assertEquals(50, kept(0.5, 100));
        assertEquals(25, kept(0.25, 100));
        int thirty = kept(0.3, 100);
        assertTrue(thirty == 29 || thirty == 30, "kept " + thirty);

        ParticleLimiter.Thinner half = new ParticleLimiter.Thinner();
        List<Boolean> pattern = new ArrayList<>();
        for (int i = 0; i < 4; i++) pattern.add(half.keep(0.5));
        assertEquals(List.of(false, true, false, true), pattern, "spread evenly, not in bursts");
    }

    private static int kept(double fraction, int calls) {
        ParticleLimiter.Thinner thinner = new ParticleLimiter.Thinner();
        int kept = 0;
        for (int i = 0; i < calls; i++) if (thinner.keep(fraction)) kept++;
        return kept;
    }

    @Test
    void particleCap() {
        ParticleLimiter limiter = new ParticleLimiter();
        assertTrue(limiter.allowParticle(3999));
        assertFalse(limiter.allowParticle(4000));
        limiter.getSetting("Max Particles").load(com.google.gson.JsonParser.parseString("100"));
        assertFalse(limiter.allowParticle(100));
    }

    // ------------------------------------------------------------------ hitboxes

    @Test
    void hitboxKinds() {
        assertEquals(Kind.PLAYER, Hitboxes.kind(true, true, false, false));
        assertEquals(Kind.MOB, Hitboxes.kind(false, true, false, false));
        assertEquals(Kind.ITEM, Hitboxes.kind(false, false, true, false));
        assertEquals(Kind.PROJECTILE, Hitboxes.kind(false, false, false, true));
        assertEquals(Kind.OTHER, Hitboxes.kind(false, false, false, false));
    }

    // ------------------------------------------------------------------ HUDs

    @Test
    void arrowCounter() {
        assertTrue(ArrowCounter.isShown(false, false, false));
        assertFalse(ArrowCounter.isShown(true, false, false));
        assertTrue(ArrowCounter.isShown(true, true, false));
        assertTrue(ArrowCounter.isShown(true, false, true), "always placeable in the HUD editor");
        assertEquals(ArrowCounter.color(8, 8), ArrowCounter.color(0, 8));
        assertEquals(ArrowCounter.color(9, 8), ArrowCounter.color(0, 0), "no warning above the threshold or when off");
        assertFalse(ArrowCounter.color(8, 8) == ArrowCounter.color(9, 8));
    }

    @Test
    void bowChargeCurves() {
        assertEquals(0f, BowCharge.bowProgress(0), 1e-6);
        assertEquals((0.25f + 1f) / 3f, BowCharge.bowProgress(10), 1e-6);
        assertEquals(1f, BowCharge.bowProgress(20), 1e-6);
        assertEquals(1f, BowCharge.bowProgress(100), 1e-6);
        for (int t = 1; t <= 20; t++) assertTrue(BowCharge.bowProgress(t) > BowCharge.bowProgress(t - 1));

        assertEquals(0.48f, BowCharge.crossbowProgress(12, 25), 1e-6);
        assertEquals(1f, BowCharge.crossbowProgress(25, 25), 1e-6);
        assertEquals(1f, BowCharge.crossbowProgress(40, 25), 1e-6);
        assertEquals(1f, BowCharge.crossbowProgress(3, 0), 1e-6, "no division by zero");
    }

    @Test
    void totemPopTracking() {
        TotemPops.Tracker tracker = new TotemPops.Tracker();
        UUID steve = new UUID(0, 1);
        UUID alex = new UUID(0, 2);
        assertEquals(1, tracker.pop(steve, "Steve", 1000));
        assertEquals(2, tracker.pop(steve, "Steve", 2000));
        assertEquals(1, tracker.pop(alex, "Alex", 3000));

        List<TotemPops.Tracker.Entry> rows = new ArrayList<>();
        tracker.mostRecent(5, rows);
        assertEquals(List.of("Alex", "Steve"), rows.stream().map(e -> e.name).toList(), "most recent first");
        rows.clear();
        tracker.mostRecent(1, rows);
        assertEquals(1, rows.size());

        tracker.expire(62_500, 60_000);
        assertEquals(0, tracker.pops(steve), "expired after the timeout");
        assertEquals(1, tracker.pops(alex));

        tracker.remove(alex);
        assertEquals(0, tracker.pops(alex), "death resets the count");
        assertEquals(1, tracker.pop(alex, "Alex", 70_000));

        assertEquals("Steve popped 1 totem", TotemPops.popMessage("Steve", 1));
        assertEquals("Steve popped 3 totems", TotemPops.popMessage("Steve", 3));
        assertEquals("Alex died after popping 2 totems", TotemPops.deathMessage("Alex", 2));
    }

    @Test
    void hotbarParts() {
        assertEquals(Part.BAR, Part.of("hud/hotbar"));
        assertEquals(Part.SELECTION, Part.of("hud/hotbar_selection"));
        assertEquals(Part.OFFHAND_LEFT, Part.of("hud/hotbar_offhand_left"));
        assertEquals(Part.OFFHAND_RIGHT, Part.of("hud/hotbar_offhand_right"));
        assertEquals(Part.OTHER, Part.of("hud/hotbar_attack_indicator_background"));
        // Vanilla draws the selection at barLeft - 1 + slot * 20.
        for (int slot = 0; slot < 9; slot++) assertEquals(slot, HotbarStyle.slotAt(100 - 1 + slot * 20, 100));
    }

    // ------------------------------------------------------------------ item physics

    /** item/generated.json: ground translation 2 px up, half size. */
    private static final Transformation GENERATED = new Transformation(new Vector3f(), new Vector3f(0, 2 / 16f, 0), new Vector3f(0.5f));
    /** block/block.json: ground translation 3 px up, quarter size. */
    private static final Transformation BLOCK = new Transformation(new Vector3f(), new Vector3f(0, 3 / 16f, 0), new Vector3f(0.25f));
    private static final float GAP = 0.005f;

    @Test
    void renderedCopiesMatchVanilla() {
        int[][] cases = {{0, 1}, {1, 1}, {2, 2}, {16, 2}, {17, 3}, {32, 3}, {33, 4}, {48, 4}, {49, 5}, {64, 5}};
        for (int[] c : cases) assertEquals(c[1], ItemPhysics.renderedCopies(c[0]), "count " + c[0]);
    }

    /**
     * Runs the rest of vanilla's chain (renderStack's centering, the model's ground transform, ItemRenderer's
     * -0.5 centering) for the lowest copy and checks that the 1/16-thick sprite lies flat just above the ground.
     */
    @Test
    void flatItemsLieOnTheGround() {
        for (int copies = 1; copies <= 5; copies++) {
            for (float yaw : new float[]{0f, 1.3f, 4f}) {
                MatrixStack m = new MatrixStack();
                ItemPhysics.layFlat(m, copies, yaw, GENERATED);
                m.translate(0, 0, -0.09375f * (copies - 1) * 0.5f * GENERATED.scale.z());
                GENERATED.apply(false, m);
                m.translate(-0.5f, -0.5f, -0.5f);
                for (float x : new float[]{0, 1}) {
                    for (float y : new float[]{0, 1}) {
                        assertEquals(GAP, worldY(m, x, y, 7.5f / 16), 1e-5, "bottom face, copies " + copies + " yaw " + yaw);
                        assertEquals(GAP + 0.5f / 16, worldY(m, x, y, 8.5f / 16), 1e-5, "top face");
                    }
                }
                // Centered over the entity: the middle of the sprite stays above the item's position.
                Vector3f center = m.peek().getPositionMatrix().transformPosition(new Vector3f(0.5f, 0.5f, 0.5f));
                assertEquals(0f, center.x, 1e-5);
                assertEquals(0f, center.z, 1e-5);
            }
        }
    }

    @Test
    void blocksStandOnTheGround() {
        MatrixStack m = new MatrixStack();
        ItemPhysics.standBlock(m, 0.7f, BLOCK);
        BLOCK.apply(false, m);
        m.translate(-0.5f, -0.5f, -0.5f);
        assertEquals(GAP, worldY(m, 0, 0, 0), 1e-5);
        assertEquals(GAP, worldY(m, 1, 0, 1), 1e-5);
        assertEquals(GAP + 0.25f, worldY(m, 1, 1, 1), 1e-5);
    }

    private static float worldY(MatrixStack m, float x, float y, float z) {
        return m.peek().getPositionMatrix().transformPosition(new Vector3f(x, y, z)).y;
    }
}

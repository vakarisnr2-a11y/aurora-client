package dev.aurora.client.module.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.event.events.Render3DEvent;
import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.ColorUtil;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Floating numbers showing how much health nearby entities lose (or gain).
 *
 * <p>The numbers come from the health the server already syncs to the client: a drop only counts while the entity
 * shows the hurt flash, so health that merely arrives late (e.g. after spawning) is not mistaken for damage. The text
 * is depth-tested, so numbers are hidden behind blocks like the entities themselves.</p>
 */
public final class DamageNumbers extends Module {
    public enum Unit {
        HEALTH, HEARTS
    }

    private static final double RANGE_SQ = 48 * 48;
    private static final int MAX_POPUPS = 64;
    private static final float MIN_CHANGE = 0.05f;
    /** Blocks a number rises over its lifetime. */
    private static final float RISE = 0.6f;

    private final EnumSetting<Unit> unit = add(new EnumSetting<>("Unit", "Health points (2 per heart) or hearts", Unit.HEALTH));
    private final NumberSetting decimals = add(new NumberSetting("Decimals", "Digits after the decimal point", 1, 0, 2, 1));
    private final NumberSetting duration = add(new NumberSetting("Duration", "Seconds each number stays", 1.2, 0.5, 3, 0.1));
    private final NumberSetting scale = add(new NumberSetting("Scale", "Text size", 1.0, 0.5, 3, 0.05));
    private final ColorSetting color = add(new ColorSetting("Color", "Damage color", 0xFFFF5555, false));
    private final BooleanSetting healing = add(new BooleanSetting("Healing", "Also show health gained", false));
    private final ColorSetting healColor = add(new ColorSetting("Heal Color", "Healing color", 0xFF55FF55, false)).visibleWhen(healing::get);
    private final BooleanSetting playersOnly = add(new BooleanSetting("Players Only", "Only show numbers on players", false));
    private final BooleanSetting self = add(new BooleanSetting("Self", "Show numbers on yourself (third person)", false));
    private final BooleanSetting shadow = add(new BooleanSetting("Shadow", "Draw a text shadow", true));

    private Int2FloatOpenHashMap lastHealth = new Int2FloatOpenHashMap();
    private Int2FloatOpenHashMap nextHealth = new Int2FloatOpenHashMap();
    private final List<Popup> popups = new ArrayList<>();
    private final MatrixStack matrices = new MatrixStack();
    private final Random random = new Random();

    private record Popup(double x, double y, double z, String text, int color, long startMs) {
    }

    public DamageNumbers() {
        super("Damage Numbers", "Floating numbers showing the damage entities take.", Category.COMBAT);
        on(TickEvent.Post.class, e -> tick());
        on(Render3DEvent.class, this::render);
    }

    @Override
    protected void onDisable() {
        lastHealth.clear();
        popups.clear();
    }

    private void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            lastHealth.clear();
            popups.clear();
            return;
        }
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();
        nextHealth.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStandEntity) continue;
            if (entity.squaredDistanceTo(cam) > RANGE_SQ) continue;
            float total = living.getHealth() + living.getAbsorptionAmount();
            nextHealth.put(entity.getId(), total);
            if (!lastHealth.containsKey(entity.getId())) continue;
            if (playersOnly.get() && !(entity instanceof PlayerEntity)) continue;
            if (entity == mc.player && (!self.get() || firstPerson)) continue;

            float before = lastHealth.get(entity.getId());
            float change = total - before;
            boolean hurt = living.hurtTime > 0 || living.isDead();
            if (isDamage(change, hurt)) {
                spawn(entity, cam, format(-change, unit.get(), decimals.getInt(), false), color.get());
            } else if (healing.get() && isHeal(change, before)) {
                spawn(entity, cam, format(change, unit.get(), decimals.getInt(), true), healColor.get());
            }
        }
        Int2FloatOpenHashMap swap = lastHealth;
        lastHealth = nextHealth;
        nextHealth = swap;
    }

    /** Places the number in front of the entity (seen from the camera) so its own body does not hide it. */
    private void spawn(Entity entity, Vec3d cam, String text, int argb) {
        if (popups.size() >= MAX_POPUPS) popups.remove(0);
        double x = entity.getX();
        double z = entity.getZ();
        double dx = cam.x - x;
        double dz = cam.z - z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 1e-3) {
            double push = entity.getWidth() * 0.5 + 0.15;
            x += dx / len * push;
            z += dz / len * push;
        }
        double y = entity.getY() + entity.getHeight() * 0.7 + (random.nextDouble() - 0.5) * 0.3;
        x += (random.nextDouble() - 0.5) * 0.4;
        z += (random.nextDouble() - 0.5) * 0.4;
        popups.add(new Popup(x, y, z, text, argb, System.currentTimeMillis()));
    }

    private void render(Render3DEvent event) {
        if (popups.isEmpty()) return;
        long now = System.currentTimeMillis();
        long life = Math.round(duration.get() * 1000);
        popups.removeIf(p -> now - p.startMs() >= life);
        if (popups.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d cam = event.cameraPos;
        // Same setup as entity labels: camera rotation in the model-view matrix, positions relative to the camera.
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.view);
        RenderSystem.applyModelViewMatrix();
        try {
            for (Popup p : popups) {
                float t = (now - p.startMs()) / (float) life;
                int alpha = alpha(t);
                if (alpha < 8) continue; // the font renderer treats alpha below 4 as opaque
                matrices.push();
                matrices.translate(p.x() - cam.x, p.y() + rise(t) - cam.y, p.z() - cam.z);
                matrices.multiply(event.camera.getRotation());
                float s = 0.025f * scale.getFloat() * popScale(t);
                matrices.scale(s, -s, s);
                tr.draw(p.text(), -tr.getWidth(p.text()) / 2f, -tr.fontHeight / 2f, ColorUtil.withAlpha(p.color(), alpha), shadow.get(),
                    matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);
                matrices.pop();
            }
            consumers.draw();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /** A health drop counts as damage only while the entity shows the hurt flash (or died). */
    public static boolean isDamage(float change, boolean hurt) {
        return change <= -MIN_CHANGE && hurt;
    }

    /** Gains from zero health (respawning) are not healing. */
    public static boolean isHeal(float change, float before) {
        return change >= MIN_CHANGE && before > 0;
    }

    /** "4.5", "2.3❤" or "+1" style label; trailing zeros are dropped ("4" instead of "4.0"). */
    public static String format(float amount, Unit unit, int decimals, boolean heal) {
        float value = Math.abs(amount) / (unit == Unit.HEARTS ? 2f : 1f);
        String number = String.format(Locale.ROOT, "%." + Math.max(0, decimals) + "f", value);
        if (number.indexOf('.') >= 0) {
            number = number.replaceAll("0+$", "");
            if (number.endsWith(".")) number = number.substring(0, number.length() - 1);
        }
        return (heal ? "+" : "") + number + (unit == Unit.HEARTS ? "❤" : "");
    }

    /** Fully visible for the first 60% of the lifetime, then fades out. */
    public static int alpha(float t) {
        if (t <= 0.6f) return 255;
        return Math.max(0, Math.round(255 * (1f - (Math.min(1f, t) - 0.6f) / 0.4f)));
    }

    /** Ease-out rise in blocks. */
    public static float rise(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return RISE * (1f - (1f - c) * (1f - c));
    }

    /** Pops in slightly larger, then settles. */
    public static float popScale(float t) {
        return t < 0.1f ? 1.3f - 3f * t : 1f;
    }
}

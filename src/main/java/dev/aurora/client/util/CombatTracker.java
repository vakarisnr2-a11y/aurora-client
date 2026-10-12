package dev.aurora.client.util;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.event.events.TickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Tracks the local player's combat: last target, reach of the last swing and the current combo.
 *
 * <p>A swing only counts as a landed hit once the server confirms damage, which the client sees
 * as the target's {@code hurtTime} becoming positive within a few ticks of the attack. The combo
 * resets when the local player takes damage or stops landing hits for a while.</p>
 */
public final class CombatTracker {
    public static final CombatTracker INSTANCE = new CombatTracker();
    private static final int CONFIRM_TICKS = 4;
    private static final long COMBO_TIMEOUT_MS = 3000;

    private LivingEntity target;
    private long lastAttackMs;
    private long lastHitMs;
    private double lastReach;
    private int combo;
    private LivingEntity pending;
    private int pendingTicks;
    private int lastPlayerHurtTime;

    private CombatTracker() {
    }

    public void register(EventBus bus) {
        bus.subscribe(this, TickEvent.Post.class, e -> tick());
    }

    public void onAttack(PlayerEntity player, Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (player != mc.player || !(entity instanceof LivingEntity living)) return;
        target = living;
        lastAttackMs = System.currentTimeMillis();
        lastReach = reach(player.getEyePos(), entity.getBoundingBox());
        pending = living.hurtTime == 0 ? living : null; // a target still flashing red can't take this hit
        pendingTicks = CONFIRM_TICKS;
    }

    private void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            reset();
            return;
        }
        if (pending != null) {
            if (pending.hurtTime > 0) {
                combo = System.currentTimeMillis() - lastHitMs > COMBO_TIMEOUT_MS ? 1 : combo + 1;
                lastHitMs = System.currentTimeMillis();
                pending = null;
            } else if (--pendingTicks <= 0) {
                pending = null;
            }
        }
        int hurt = mc.player.hurtTime;
        if (hurt > 0 && lastPlayerHurtTime == 0) combo = 0;
        lastPlayerHurtTime = hurt;
        if (combo > 0 && System.currentTimeMillis() - lastHitMs > COMBO_TIMEOUT_MS) combo = 0;
        if (target != null && (target.isRemoved() || !target.isAlive())) target = null;
    }

    private void reset() {
        target = null;
        pending = null;
        combo = 0;
    }

    /** Distance from the eye to the closest point of the target's hitbox. */
    public static double reach(Vec3d eye, Box box) {
        double x = Math.max(box.minX, Math.min(eye.x, box.maxX));
        double y = Math.max(box.minY, Math.min(eye.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(eye.z, box.maxZ));
        return eye.distanceTo(new Vec3d(x, y, z));
    }

    public LivingEntity getTarget() {
        return target;
    }

    public long getLastAttackMs() {
        return lastAttackMs;
    }

    public double getLastReach() {
        return lastReach;
    }

    public int getCombo() {
        return combo;
    }

    public long getLastHitMs() {
        return lastHitMs;
    }
}

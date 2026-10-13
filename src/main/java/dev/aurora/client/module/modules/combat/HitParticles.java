package dev.aurora.client.module.modules.combat;

import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.CombatTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

/**
 * Spawns crit and sharpness (enchanted hit) particles several times over.
 *
 * <p>The server tells the client when a hit was a crit or enchanted; the client then adds a particle emitter
 * on the entity. ParticleManagerMixin adds {@code multiplier - 1} more emitters right after each one. The
 * "Always" options add those emitters on every hit you land (confirmed like the Combo Counter's hits).</p>
 */
public final class HitParticles extends Module {
    /** A server emitter this recent on the same entity means the hit already has particles. */
    private static final long SERVER_EMITTER_WINDOW_MS = 300;

    private final NumberSetting critMultiplier = add(new NumberSetting("Crit Multiplier", "How many times crit particles are spawned", 2, 1, 10, 1));
    private final NumberSetting sharpMultiplier = add(new NumberSetting("Sharpness Multiplier",
        "How many times sharpness (enchanted hit) particles are spawned", 2, 1, 10, 1));
    private final BooleanSetting alwaysCrit = add(new BooleanSetting("Always Crit", "Crit particles on every hit you land, not only real crits", false));
    private final BooleanSetting alwaysSharp = add(new BooleanSetting("Always Sharpness", "Sharpness particles on every hit you land", false));

    private long lastHitMs;
    private int lastCritEntity = -1;
    private long lastCritMs;
    private int lastSharpEntity = -1;
    private long lastSharpMs;

    public HitParticles() {
        super("Hit Particles", "Multiplies crit and sharpness hit particles.", Category.COMBAT);
        on(TickEvent.Post.class, e -> tick());
    }

    @Override
    protected void onEnable() {
        lastHitMs = CombatTracker.INSTANCE.getLastHitMs();
    }

    /** How many extra emitters to add after vanilla added one for {@code effect}. */
    public int extraEmitters(ParticleEffect effect) {
        ParticleType<?> type = effect.getType();
        if (type == ParticleTypes.CRIT) return extra(critMultiplier.getInt());
        if (type == ParticleTypes.ENCHANTED_HIT) return extra(sharpMultiplier.getInt());
        return 0;
    }

    /** Remembers server-sent hit emitters so the "Always" options do not double them. */
    public void onEmitter(Entity entity, ParticleEffect effect) {
        ParticleType<?> type = effect.getType();
        if (type == ParticleTypes.CRIT) {
            lastCritEntity = entity.getId();
            lastCritMs = System.currentTimeMillis();
        } else if (type == ParticleTypes.ENCHANTED_HIT) {
            lastSharpEntity = entity.getId();
            lastSharpMs = System.currentTimeMillis();
        }
    }

    private void tick() {
        CombatTracker tracker = CombatTracker.INSTANCE;
        long hit = tracker.getLastHitMs();
        if (hit == lastHitMs) return;
        lastHitMs = hit;
        LivingEntity target = tracker.getTarget();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (target == null || mc.particleManager == null) return;
        long now = System.currentTimeMillis();
        // Emitters added here pass through the mixin too, so they are multiplied like the server's.
        if (alwaysCrit.get() && !hadServerEmitter(lastCritEntity, lastCritMs, target.getId(), now)) {
            mc.particleManager.addEmitter(target, ParticleTypes.CRIT);
        }
        if (alwaysSharp.get() && !hadServerEmitter(lastSharpEntity, lastSharpMs, target.getId(), now)) {
            mc.particleManager.addEmitter(target, ParticleTypes.ENCHANTED_HIT);
        }
    }

    public static int extra(int multiplier) {
        return Math.max(0, multiplier - 1);
    }

    public static boolean hadServerEmitter(int emitterEntity, long emitterMs, int targetEntity, long now) {
        return emitterEntity == targetEntity && now - emitterMs <= SERVER_EMITTER_WINDOW_MS;
    }
}

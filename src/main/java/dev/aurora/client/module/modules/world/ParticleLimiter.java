package dev.aurora.client.module.modules.world;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

/**
 * Caps and thins out particles for FPS (explosions, block breaking, big fights).
 *
 * <p>WorldRendererMixin asks {@link #allowEffect} before a world particle is spawned (Density, Explosions);
 * ParticleManagerMixin asks {@link #allowParticle} before any particle is added (the cap) and skips block break
 * particles. Particles that mods or vanilla add straight to the particle manager (fireworks) only count toward the cap.</p>
 */
public final class ParticleLimiter extends Module {
    private final NumberSetting maxParticles = add(new NumberSetting("Max Particles",
        "Most particles alive at once; new ones are skipped above this (vanilla allows 16384 per kind)", 4000, 100, 16000, 100));
    private final NumberSetting density = add(new NumberSetting("Density", "Percentage of new particles that are spawned", 100, 0, 100, 5));
    private final BooleanSetting keepCombat = add(new BooleanSetting("Keep Combat", "Crit and sharpness particles ignore Density", true));
    private final BooleanSetting explosions = add(new BooleanSetting("Explosions", "Show explosion particles", true));
    private final BooleanSetting blockBreaking = add(new BooleanSetting("Block Breaking", "Show block breaking and mining particles", true));

    private final Thinner thinner = new Thinner();

    public ParticleLimiter() {
        super("Particle Limiter", "Limits how many particles are spawned to save FPS.", Category.WORLD);
    }

    @Override
    public String getDisplayInfo() {
        return density.getInt() + "%";
    }

    /** Called before a particle is created from {@code effect}; false skips it. */
    public boolean allowEffect(ParticleEffect effect) {
        ParticleType<?> type = effect.getType();
        if (!explosions.get() && (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER)) return false;
        if (keepCombat.get() && (type == ParticleTypes.CRIT || type == ParticleTypes.ENCHANTED_HIT)) return true;
        return thinner.keep(density.get() / 100.0);
    }

    /** Called before any particle is added, with the number currently alive or queued. */
    public boolean allowParticle(int alive) {
        return alive < maxParticles.getInt();
    }

    public boolean showsBlockBreaking() {
        return blockBreaking.get();
    }

    /** Keeps an exact fraction of calls, spread evenly (0.5 keeps every second one) instead of randomly. */
    public static final class Thinner {
        private double budget;

        public boolean keep(double fraction) {
            if (fraction >= 1.0) return true;
            if (fraction <= 0.0) return false;
            budget += fraction;
            if (budget >= 1.0) {
                budget -= 1.0;
                return true;
            }
            return false;
        }
    }
}

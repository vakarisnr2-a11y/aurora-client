package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.world.ParticleLimiter;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    /**
     * World particles (server particle packets, emitters, explosions) are spawned here. Vanilla already returns null
     * here for distant particles or the Minimal setting, and every caller checks for null (checked with javap against
     * 1.21.1), so skipping a particle is safe. ParticleManager.addParticle itself is not filtered: fireworks cast its result.
     */
    @Inject(method = "spawnParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"), cancellable = true)
    private void aurora$limitParticles(ParticleEffect effect, boolean alwaysSpawn, boolean canSpawnOnMinimal, double x, double y, double z,
                                       double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        ParticleLimiter limiter = AuroraClient.module(ParticleLimiter.class);
        if (limiter != null && limiter.isEnabled() && !limiter.allowEffect(effect)) cir.setReturnValue(null);
    }
}

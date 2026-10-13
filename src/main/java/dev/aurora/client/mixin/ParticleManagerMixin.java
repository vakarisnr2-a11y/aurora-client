package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.combat.HitParticles;
import dev.aurora.client.module.modules.world.ParticleLimiter;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Shadow
    @Final
    private Map<ParticleTextureSheet, Queue<Particle>> particles;

    @Shadow
    @Final
    private Queue<Particle> newParticles;

    @Shadow
    public abstract void addEmitter(Entity entity, ParticleEffect parameters);

    @Unique
    private boolean aurora$addingExtra;

    /** Crit and enchanted-hit emitters (sent by the server for hits) are added here; Hit Particles adds more of them. */
    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V", at = @At("TAIL"))
    private void aurora$multiplyHitParticles(Entity entity, ParticleEffect effect, CallbackInfo ci) {
        if (aurora$addingExtra) return;
        HitParticles hitParticles = AuroraClient.module(HitParticles.class);
        if (hitParticles == null || !hitParticles.isEnabled()) return;
        hitParticles.onEmitter(entity, effect);
        int extra = hitParticles.extraEmitters(effect);
        if (extra <= 0) return;
        aurora$addingExtra = true;
        try {
            for (int i = 0; i < extra; i++) addEmitter(entity, effect);
        } finally {
            aurora$addingExtra = false;
        }
    }

    /** Every particle passes through here, including block break particles created directly. */
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void aurora$capParticles(Particle particle, CallbackInfo ci) {
        ParticleLimiter limiter = AuroraClient.module(ParticleLimiter.class);
        if (limiter == null || !limiter.isEnabled()) return;
        int alive = newParticles.size();
        for (Queue<Particle> queue : particles.values()) alive += queue.size();
        if (!limiter.allowParticle(alive)) ci.cancel();
    }

    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void aurora$hideBlockBreak(BlockPos pos, BlockState state, CallbackInfo ci) {
        ParticleLimiter limiter = AuroraClient.module(ParticleLimiter.class);
        if (limiter != null && limiter.isEnabled() && !limiter.showsBlockBreaking()) ci.cancel();
    }

    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void aurora$hideBlockBreaking(BlockPos pos, Direction direction, CallbackInfo ci) {
        ParticleLimiter limiter = AuroraClient.module(ParticleLimiter.class);
        if (limiter != null && limiter.isEnabled() && !limiter.showsBlockBreaking()) ci.cancel();
    }
}

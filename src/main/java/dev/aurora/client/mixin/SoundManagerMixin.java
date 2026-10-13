package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.combat.HitSounds;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    /** Hit Sounds' "Mute Vanilla": drop vanilla attack sounds played right away. */
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void aurora$muteAttackSounds(SoundInstance sound, CallbackInfo ci) {
        if (aurora$muted(sound)) ci.cancel();
    }

    /** ...and the delayed ones (vanilla delays distant sounds). */
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At("HEAD"), cancellable = true)
    private void aurora$muteDelayedAttackSounds(SoundInstance sound, int delay, CallbackInfo ci) {
        if (aurora$muted(sound)) ci.cancel();
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean aurora$muted(SoundInstance sound) {
        HitSounds hitSounds = AuroraClient.module(HitSounds.class);
        return hitSounds != null && hitSounds.isEnabled() && sound != null && hitSounds.mutes(sound.getId());
    }
}

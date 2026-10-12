package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.movement.SafeWalk;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    /** Uses vanilla's own sneak-edge logic (adjustMovementForSneaking) without having to sneak. */
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void aurora$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        SafeWalk safeWalk = AuroraClient.module(SafeWalk.class);
        if (safeWalk != null && safeWalk.isEnabled() && safeWalk.shouldClip()) cir.setReturnValue(true);
    }
}

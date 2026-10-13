package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.utility.UnfocusedFps;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    /** The frame limit passed to RenderSystem.limitDisplayFPS every frame (both returns, checked with javap). */
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void aurora$unfocusedFps(CallbackInfoReturnable<Integer> cir) {
        UnfocusedFps module = AuroraClient.module(UnfocusedFps.class);
        if (module != null && module.isEnabled()) cir.setReturnValue(module.limit(cir.getReturnValue()));
    }
}

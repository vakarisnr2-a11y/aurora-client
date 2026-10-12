package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.visuals.LowFire;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    /** Push/pop are always balanced; the translation only applies while Low Fire is enabled. */
    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void aurora$lowerFire(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        matrices.push();
        LowFire lowFire = AuroraClient.module(LowFire.class);
        if (lowFire != null && lowFire.isEnabled()) matrices.translate(0, -lowFire.getOffset(), 0);
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void aurora$restoreFire(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        matrices.pop();
    }
}

package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.utility.Zoom;
import dev.aurora.client.module.modules.visuals.NoHurtCam;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** changingFov is true for the world FOV and false for the held-item FOV, which we leave alone. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void aurora$zoomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (!changingFov) return;
        Zoom zoom = AuroraClient.module(Zoom.class);
        if (zoom == null) return;
        double factor = zoom.updateAndGetFactor();
        if (factor != 1.0) cir.setReturnValue(cir.getReturnValue() / factor);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void aurora$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoHurtCam module = AuroraClient.module(NoHurtCam.class);
        if (module != null && module.isEnabled()) ci.cancel();
    }
}

package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.world.WorldColor;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void aurora$skyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        WorldColor worldColor = AuroraClient.module(WorldColor.class);
        if (worldColor != null && worldColor.isEnabled() && worldColor.overridesSky()) {
            cir.setReturnValue(worldColor.skyColor(cir.getReturnValue()));
        }
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void aurora$cloudColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        WorldColor worldColor = AuroraClient.module(WorldColor.class);
        if (worldColor != null && worldColor.isEnabled() && worldColor.overridesClouds()) {
            cir.setReturnValue(worldColor.cloudColor(cir.getReturnValue()));
        }
    }
}

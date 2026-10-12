package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.movement.Freelook;
import dev.aurora.client.module.modules.visuals.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    public abstract boolean isThirdPerson();

    /**
     * Third person: replace the rotation right before the camera is pushed back from the player,
     * so the camera orbits around the player using the freelook angles.
     */
    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    private void aurora$freelookThirdPerson(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                            float tickDelta, CallbackInfo ci) {
        Freelook freelook = AuroraClient.module(Freelook.class);
        if (freelook != null && freelook.isEnabled()) setRotation(freelook.getYaw(), freelook.getPitch());
    }

    /** First person: only the view direction changes. */
    @Inject(method = "update", at = @At("TAIL"))
    private void aurora$freelookFirstPerson(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                            float tickDelta, CallbackInfo ci) {
        Freelook freelook = AuroraClient.module(Freelook.class);
        if (!thirdPerson && freelook != null && freelook.isEnabled()) setRotation(freelook.getYaw(), freelook.getPitch());
    }

    @ModifyVariable(method = "clipToSpace", at = @At("HEAD"), argsOnly = true)
    private float aurora$cameraDistance(float desired) {
        Perspective perspective = AuroraClient.module(Perspective.class);
        return perspective != null && perspective.isEnabled() ? perspective.getDistance() : desired;
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void aurora$cameraNoClip(float desired, CallbackInfoReturnable<Float> cir) {
        Perspective perspective = AuroraClient.module(Perspective.class);
        if (perspective != null && perspective.isEnabled() && perspective.isNoClip()) cir.setReturnValue(perspective.getDistance());
    }
}

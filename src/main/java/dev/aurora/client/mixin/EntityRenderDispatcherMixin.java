package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.combat.Hitboxes;
import dev.aurora.client.module.modules.world.EntityDistance;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    /** The Hitboxes module draws its own boxes; skip vanilla's F3+B box so it is not drawn on top. */
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void aurora$replaceHitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta,
                                             float red, float green, float blue, CallbackInfo ci) {
        Hitboxes hitboxes = AuroraClient.module(Hitboxes.class);
        if (hitboxes != null && hitboxes.isEnabled()) ci.cancel();
    }

    /** Every entity passes this check before it is drawn; (x, y, z) is the camera position. */
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void aurora$entityDistance(E entity, Frustum frustum, double x, double y, double z,
                                                          CallbackInfoReturnable<Boolean> cir) {
        EntityDistance module = AuroraClient.module(EntityDistance.class);
        if (module != null && module.isEnabled() && module.shouldHide(entity, x, y, z)) cir.setReturnValue(false);
    }
}

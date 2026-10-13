package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.combat.Hitboxes;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    /** The Hitboxes module draws its own boxes; skip vanilla's F3+B box so it is not drawn on top. */
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void aurora$replaceHitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta,
                                             float red, float green, float blue, CallbackInfo ci) {
        Hitboxes hitboxes = AuroraClient.module(Hitboxes.class);
        if (hitboxes != null && hitboxes.isEnabled()) ci.cancel();
    }
}

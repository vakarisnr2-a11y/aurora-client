package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.world.TntTimer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.TntEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.TntEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntEntityRenderer.class)
public abstract class TntEntityRendererMixin {
    /** After the block (and vanilla's own label handling) is drawn, the matrices are back at the TNT's feet. */
    @Inject(method = "render(Lnet/minecraft/entity/TntEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("TAIL"))
    private void aurora$fuseTimer(TntEntity tnt, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers,
                                  int light, CallbackInfo ci) {
        TntTimer timer = AuroraClient.module(TntTimer.class);
        if (timer != null && timer.isEnabled()) timer.renderLabel(tnt, tickDelta, matrices, consumers);
    }
}

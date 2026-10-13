package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.world.ItemPhysics;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item Physics hooks. ItemEntityRenderer.render (1.21.1, checked with javap) has exactly one translate (the bob),
 * one multiply (the spin) and one renderStack call, in that order.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Unique
    private ItemEntity aurora$entity;

    @Unique
    private float aurora$tickDelta;

    @Unique
    private boolean aurora$physics;

    @Inject(method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"))
    private void aurora$begin(ItemEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers,
                              int light, CallbackInfo ci) {
        ItemPhysics physics = AuroraClient.module(ItemPhysics.class);
        aurora$physics = physics != null && physics.isEnabled();
        aurora$entity = entity;
        aurora$tickDelta = tickDelta;
    }

    @Redirect(method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private void aurora$bob(MatrixStack matrices, float x, float y, float z) {
        if (!aurora$physics) matrices.translate(x, y, z);
    }

    @Redirect(method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V"))
    private void aurora$spin(MatrixStack matrices, Quaternionf rotation) {
        if (!aurora$physics) matrices.multiply(rotation);
    }

    @Redirect(method = "render(Lnet/minecraft/entity/ItemEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/ItemEntityRenderer;renderStack(Lnet/minecraft/client/render/item/ItemRenderer;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/BakedModel;ZLnet/minecraft/util/math/random/Random;)V"))
    private void aurora$layFlat(ItemRenderer itemRenderer, MatrixStack matrices, VertexConsumerProvider consumers, int light, ItemStack stack,
                                BakedModel model, boolean depth, Random random) {
        ItemPhysics physics = AuroraClient.module(ItemPhysics.class);
        if (aurora$physics && physics != null && aurora$entity != null) {
            physics.transform(aurora$entity, aurora$tickDelta, model, depth, stack.getCount(), matrices);
        }
        aurora$entity = null;
        ItemEntityRenderer.renderStack(itemRenderer, matrices, consumers, light, stack, model, depth, random);
    }
}

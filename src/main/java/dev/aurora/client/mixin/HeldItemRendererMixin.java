package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.visuals.OldAnimations;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow
    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
    }

    @Shadow
    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
    }

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
                                    MatrixStack matrices, VertexConsumerProvider consumers, int light);

    /**
     * The equip animation targets {@code cooldown^3}, which is what lowers the item after an attack.
     * The only getAttackCooldownProgress call in updateHeldItems (checked with javap against 1.21.1).
     */
    @Redirect(method = "updateHeldItems", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttackCooldownProgress(F)F"))
    private float aurora$oldSwing(ClientPlayerEntity player, float baseTime) {
        OldAnimations animations = AuroraClient.module(OldAnimations.class);
        if (animations != null && animations.isEnabled() && animations.oldSwing()) return 1f;
        return player.getAttackCooldownProgress(baseTime);
    }

    /** Draws a sword in the blocking pose; vanilla's own path is skipped for that one item only. */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void aurora$swordBlock(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
                                   ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider consumers,
                                   int light, CallbackInfo ci) {
        OldAnimations animations = AuroraClient.module(OldAnimations.class);
        if (animations == null || !animations.isEnabled() || !animations.showsSwordBlock(player, hand, item)) return;
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = arm == Arm.RIGHT;
        matrices.push();
        if (animations.blockHit()) {
            // Same swing translation vanilla applies to a normally held item.
            float sqrt = MathHelper.sqrt(swingProgress);
            matrices.translate((right ? 1 : -1) * -0.4f * MathHelper.sin(sqrt * (float) Math.PI),
                0.2f * MathHelper.sin(sqrt * (float) (Math.PI * 2)), -0.2f * MathHelper.sin(swingProgress * (float) Math.PI));
        }
        applyEquipOffset(matrices, arm, equipProgress);
        if (animations.blockHit()) applySwingOffset(matrices, arm, swingProgress);
        OldAnimations.applyBlockTransform(matrices, right);
        renderItem(player, item, right ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
            !right, matrices, consumers, light);
        matrices.pop();
        ci.cancel();
    }
}

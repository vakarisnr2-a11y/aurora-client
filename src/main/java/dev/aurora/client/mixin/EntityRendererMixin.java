package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.visuals.GearTags;
import dev.aurora.client.module.modules.visuals.Nametags;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    /** Appends health and gear info to the label text before vanilla (or a subclass override) draws it. */
    @ModifyArg(method = "render", index = 1, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/entity/EntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V"))
    private Text aurora$decorateLabel(Entity entity, Text text, MatrixStack matrices, VertexConsumerProvider consumers, int light, float tickDelta) {
        Nametags nametags = AuroraClient.module(Nametags.class);
        if (nametags != null && nametags.isEnabled()) text = nametags.decorate(entity, text);
        GearTags gearTags = AuroraClient.module(GearTags.class);
        if (gearTags != null && gearTags.isEnabled()) text = gearTags.decorate(entity, text);
        return text;
    }

    /** Vanilla scales labels by 0.025; multiply that by the configured nametag scale. */
    @Redirect(method = "renderLabelIfPresent", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void aurora$scaleLabel(MatrixStack matrices, float x, float y, float z) {
        Nametags nametags = AuroraClient.module(Nametags.class);
        float s = nametags != null && nametags.isEnabled() ? nametags.getScale() : 1f;
        matrices.scale(x * s, y * s, z * s);
    }
}

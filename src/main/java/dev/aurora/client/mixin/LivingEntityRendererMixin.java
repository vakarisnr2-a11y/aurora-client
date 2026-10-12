package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.visuals.Nametags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    /** Vanilla never labels the camera entity; optionally show your own nametag in third person. */
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void aurora$ownNametag(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity != mc.player || mc.options.getPerspective().isFirstPerson() || mc.options.hudHidden) return;
        Nametags nametags = AuroraClient.module(Nametags.class);
        if (nametags != null && nametags.isEnabled() && nametags.showOwn()) cir.setReturnValue(true);
    }
}

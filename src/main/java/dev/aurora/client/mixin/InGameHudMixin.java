package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.utility.PotionEffects;
import dev.aurora.client.module.modules.visuals.Crosshair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void aurora$customCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Crosshair crosshair = AuroraClient.module(Crosshair.class);
        if (crosshair != null && crosshair.isEnabled()) ci.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void aurora$hideVanillaEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        PotionEffects effects = AuroraClient.module(PotionEffects.class);
        if (effects != null && effects.isEnabled() && effects.hidesVanilla()) ci.cancel();
    }
}

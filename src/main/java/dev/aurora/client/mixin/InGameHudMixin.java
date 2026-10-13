package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.utility.PotionEffects;
import dev.aurora.client.module.modules.visuals.Crosshair;
import dev.aurora.client.module.modules.visuals.HotbarStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    /** Every 5-argument drawGuiTexture in renderHotbar: bar, selection, offhand slot and attack indicator background. */
    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    private void aurora$hotbarStyle(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        HotbarStyle style = AuroraClient.module(HotbarStyle.class);
        if (style != null && style.isEnabled() && texture.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
            && style.draw(context, texture.getPath(), x, y, width, height)) return;
        context.drawGuiTexture(texture, x, y, width, height);
    }
}

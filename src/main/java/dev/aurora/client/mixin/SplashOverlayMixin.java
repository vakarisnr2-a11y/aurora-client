package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.gui.menu.AuroraLogo;
import dev.aurora.client.module.modules.visuals.AuroraMenu;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

/**
 * The resource loading overlay in Aurora colors: dark backdrop instead of Mojang red, the Aurora icon instead of the
 * Mojang logo and an accent progress bar. Only after the profile has loaded (the very first startup overlay stays
 * vanilla) and only while Aurora Menu's Splash Screen option is on.
 */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Unique
    private static final int AURORA_BACKGROUND = 0xFF0A0C14;

    @Shadow
    private float progress;

    @Unique
    private static boolean aurora$themed() {
        AuroraMenu menu = AuroraClient.module(AuroraMenu.class);
        return menu != null && menu.themedSplash();
    }

    /** The three BRAND_ARGB lookups in render: the two background fills and the clear color. */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
    private int aurora$background(IntSupplier brand) {
        return aurora$themed() ? AURORA_BACKGROUND : brand.getAsInt();
    }

    /** The two halves of the Mojang logo (u = -0.0625 left, +0.0625 right); the icon is drawn once in their place. */
    @Redirect(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFIIII)V"))
    private void aurora$logo(DrawContext context, Identifier texture, int x, int y, int width, int height, float u, float v,
                             int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        if (!aurora$themed()) {
            context.drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
            return;
        }
        if (u < 0) AuroraLogo.icon(context, x + width - height / 2, y, height); // x + width is the logo's centre
    }

    @Inject(method = "renderProgressBar", at = @At("HEAD"), cancellable = true)
    private void aurora$progressBar(DrawContext context, int minX, int minY, int maxX, int maxY, float opacity, CallbackInfo ci) {
        if (!aurora$themed()) return;
        float w = maxX - minX;
        float y = (minY + maxY) / 2f - 2;
        int alpha = Math.round(255 * opacity);
        Render2D.roundedRect(context, minX, y, w, 4, 2, ColorUtil.withAlpha(0xFFFFFF, Math.round(40 * opacity)));
        Render2D.roundedRect(context, minX, y, Math.max(4, w * progress), 4, 2, ColorUtil.withAlpha(Theme.accent(), alpha));
        ci.cancel();
    }
}

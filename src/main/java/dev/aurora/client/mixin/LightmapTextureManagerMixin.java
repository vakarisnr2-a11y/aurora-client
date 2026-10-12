package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.visuals.Fullbright;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fullbright without touching the gamma option (so options.txt is never polluted with
 * out-of-range values): while enabled the lightmap is recomputed every frame and then blended
 * toward white before being uploaded again.
 */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow
    private boolean dirty;

    @Shadow
    @Final
    private NativeImage image;

    @Shadow
    @Final
    private NativeImageBackedTexture texture;

    @Unique
    private boolean aurora$recomputing;

    @Inject(method = "update", at = @At("HEAD"))
    private void aurora$forceRecompute(float delta, CallbackInfo ci) {
        Fullbright fullbright = AuroraClient.module(Fullbright.class);
        if (fullbright != null && fullbright.isEnabled()) dirty = true;
        aurora$recomputing = dirty; // vanilla recomputes from scratch this call, so brightening never compounds
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void aurora$brighten(float delta, CallbackInfo ci) {
        Fullbright fullbright = AuroraClient.module(Fullbright.class);
        if (aurora$recomputing && fullbright != null && fullbright.isEnabled() && MinecraftClient.getInstance().world != null) {
            fullbright.brighten(image);
            texture.upload();
        }
    }
}

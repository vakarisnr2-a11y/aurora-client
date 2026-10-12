package dev.aurora.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.world.WorldColor;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Shadow
    private static float red;

    @Shadow
    private static float green;

    @Shadow
    private static float blue;

    /** Overrides the fog/clear color computed this frame (only in air, water and lava keep vanilla fog). */
    @Inject(method = "render", at = @At("TAIL"))
    private static void aurora$fogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        WorldColor worldColor = AuroraClient.module(WorldColor.class);
        if (worldColor == null || !worldColor.isEnabled() || !worldColor.overridesFog()) return;
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) return;
        // Keep vanilla's day/night brightness by scaling the custom color with the vanilla fog brightness.
        float brightness = Math.min(1f, Math.max(red, Math.max(green, blue)) / 0.85f);
        int c = worldColor.fogColor();
        red = ((c >> 16) & 0xFF) / 255f * brightness;
        green = ((c >> 8) & 0xFF) / 255f * brightness;
        blue = (c & 0xFF) / 255f * brightness;
        RenderSystem.clearColor(red, green, blue, 0.0f);
    }
}

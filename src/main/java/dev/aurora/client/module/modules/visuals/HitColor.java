package dev.aurora.client.module.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.mixin.OverlayTextureAccessor;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Recolors the damage tint entities flash when hurt.
 *
 * <p>Vanilla stores the tint in the top 8 rows of the 16x16 overlay texture; we rewrite those
 * rows (ABGR) and re-upload. Disabling restores vanilla's translucent red.</p>
 */
public final class HitColor extends Module {
    private static final int VANILLA_ARGB = 0xB2FF0000;

    private final ColorSetting color = add(new ColorSetting("Color", "Tint shown on entities that take damage", 0x993C8CFF));

    public HitColor() {
        super("Hit Color", "Changes the red flash entities show when they take damage.", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        apply(color.get());
    }

    @Override
    protected void onDisable() {
        apply(VANILLA_ARGB);
    }

    @Override
    protected void onSettingChanged(Setting<?> setting) {
        if (setting == color && isEnabled()) apply(color.get());
    }

    private static void apply(int argb) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.gameRenderer == null) return;
        NativeImageBackedTexture texture = ((OverlayTextureAccessor) mc.gameRenderer.getOverlayTexture()).aurora$getTexture();
        NativeImage image = texture.getImage();
        if (image == null) return;
        int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int abgr = (a << 24) | (b << 16) | (g << 8) | r;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) image.setColor(x, y, abgr);
        }
        if (RenderSystem.isOnRenderThread()) texture.upload();
        else RenderSystem.recordRenderCall(texture::upload);
    }
}

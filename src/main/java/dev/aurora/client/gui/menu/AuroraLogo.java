package dev.aurora.client.gui.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/**
 * The Aurora logo: the mod icon as a texture plus the "AURORA" wordmark. The icon is read straight from the mod jar
 * into a dynamic texture, so it can also be drawn on the loading overlay while resource packs are reloading.
 */
public final class AuroraLogo {
    public static final Identifier ICON = Identifier.of(AuroraClient.MOD_ID, "dynamic/logo_icon");
    private static final int ICON_SIZE = 128;
    private static boolean loaded;
    private static boolean failed;

    private AuroraLogo() {
    }

    /** Registers the icon texture on first use; returns false if it could not be read. */
    public static boolean ensureLoaded() {
        if (loaded || failed) return loaded;
        RenderSystem.assertOnRenderThread();
        try (InputStream in = AuroraLogo.class.getResourceAsStream("/assets/aurora/icon.png")) {
            if (in == null) throw new IllegalStateException("icon.png missing");
            NativeImage image = NativeImage.read(in);
            MinecraftClient.getInstance().getTextureManager().registerTexture(ICON, new NativeImageBackedTexture(image));
            loaded = true;
        } catch (Exception e) {
            AuroraClient.LOGGER.warn("Could not load the Aurora logo", e);
            failed = true;
        }
        return loaded;
    }

    /** Draws the icon as a square of {@code size} at (x, y). */
    public static void icon(DrawContext ctx, int x, int y, int size) {
        if (!ensureLoaded()) return;
        RenderSystem.enableBlend();
        ctx.drawTexture(ICON, x, y, size, size, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    /**
     * Icon and wordmark centred on {@code centerX}, top at {@code y}. Returns the total height.
     *
     * @param alpha 0..1
     */
    public static int draw(DrawContext ctx, int centerX, int y, float alpha) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int iconSize = 40;
        String word = "AURORA";
        float scale = 3f;
        int wordW = Math.round(tr.getWidth(word) * scale);
        int total = iconSize + 8 + wordW;
        int x = centerX - total / 2;
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        icon(ctx, x, y, iconSize);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + iconSize + 8, y + (iconSize - tr.fontHeight * scale) / 2f + 2, 0);
        ctx.getMatrices().scale(scale, scale, 1);
        int accent = Theme.accent();
        int cx = 0;
        for (int i = 0; i < word.length(); i++) {
            String ch = word.substring(i, i + 1);
            float wave = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 700.0 - i * 0.55));
            int color = ColorUtil.lerp(accent, ColorUtil.brighten(accent, 0.65f), wave);
            ctx.drawText(tr, ch, cx, 0, ColorUtil.withAlpha(color, Math.max(4, Math.round(255 * alpha))), true);
            cx += tr.getWidth(ch);
        }
        ctx.getMatrices().pop();
        return iconSize;
    }
}

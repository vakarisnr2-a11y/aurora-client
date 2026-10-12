package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.texture.NativeImage;

/** Brightens the lightmap (see LightmapTextureManagerMixin). Does not modify the gamma option. */
public final class Fullbright extends Module {
    private final NumberSetting strength = add(new NumberSetting("Strength", "How close lighting gets to full brightness", 1.0, 0.1, 1.0, 0.05));

    public Fullbright() {
        super("Fullbright", "See clearly in dark areas without changing your brightness option.", Category.VISUALS);
    }

    /** Blends every lightmap texel toward white. The image stores colors as ABGR. */
    public void brighten(NativeImage image) {
        float s = strength.getFloat();
        int w = image.getWidth();
        int h = image.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = image.getColor(x, y);
                int r = c & 0xFF;
                int g = (c >> 8) & 0xFF;
                int b = (c >> 16) & 0xFF;
                r += (int) ((255 - r) * s);
                g += (int) ((255 - g) * s);
                b += (int) ((255 - b) * s);
                image.setColor(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        return Math.round(strength.get() * 100) + "%";
    }
}

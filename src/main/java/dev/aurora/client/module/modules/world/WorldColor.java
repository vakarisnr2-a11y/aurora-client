package dev.aurora.client.module.modules.world;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import net.minecraft.util.math.Vec3d;

/** Custom sky, fog and cloud colors (ClientWorldMixin / BackgroundRendererMixin). Day/night brightness is kept. */
public final class WorldColor extends Module {
    private final BooleanSetting sky = add(new BooleanSetting("Sky", "Override the sky color", true));
    private final ColorSetting skyColor = add(new ColorSetting("Sky Color", "Sky color at full daylight", 0xFF8A7CFF, false))
        .visibleWhen(sky::get);
    private final BooleanSetting fog = add(new BooleanSetting("Fog", "Override the fog / horizon color", true));
    private final ColorSetting fogColor = add(new ColorSetting("Fog Color", "Fog color at full daylight", 0xFFC9B8FF, false))
        .visibleWhen(fog::get);
    private final BooleanSetting clouds = add(new BooleanSetting("Clouds", "Override the cloud color", false));
    private final ColorSetting cloudColor = add(new ColorSetting("Cloud Color", "Cloud color at full daylight", 0xFFFFD6F5, false))
        .visibleWhen(clouds::get);

    public WorldColor() {
        super("World Color", "Customize sky, fog and cloud colors.", Category.WORLD);
    }

    public boolean overridesSky() {
        return sky.get();
    }

    public boolean overridesFog() {
        return fog.get();
    }

    public boolean overridesClouds() {
        return clouds.get();
    }

    public int fogColor() {
        return fogColor.getColor();
    }

    public Vec3d skyColor(Vec3d vanilla) {
        return tinted(skyColor.getColor(), vanilla);
    }

    public Vec3d cloudColor(Vec3d vanilla) {
        return tinted(cloudColor.getColor(), vanilla);
    }

    /** Scales the custom color by vanilla's brightness so night still gets dark. */
    private static Vec3d tinted(int color, Vec3d vanilla) {
        double brightness = Math.min(1.0, Math.max(vanilla.x, Math.max(vanilla.y, vanilla.z)));
        return new Vec3d(((color >> 16) & 0xFF) / 255.0 * brightness, ((color >> 8) & 0xFF) / 255.0 * brightness,
            (color & 0xFF) / 255.0 * brightness);
    }
}

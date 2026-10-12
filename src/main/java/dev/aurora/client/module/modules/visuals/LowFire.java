package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.NumberSetting;

/** Lowers the on-screen burning overlay so it covers less of the view (useful in Bedwars). */
public final class LowFire extends Module {
    private final NumberSetting offset = add(new NumberSetting("Offset", "How far the fire overlay is lowered", 0.3, 0.05, 0.6, 0.05));

    public LowFire() {
        super("Low Fire", "Lowers the first-person fire overlay so you can see while burning.", Category.VISUALS);
    }

    public float getOffset() {
        return offset.getFloat();
    }
}

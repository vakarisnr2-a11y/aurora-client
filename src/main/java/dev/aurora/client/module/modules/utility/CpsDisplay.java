package dev.aurora.client.module.modules.utility;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.util.CpsTracker;
import net.minecraft.client.gui.DrawContext;

public final class CpsDisplay extends HudModule {
    private final BooleanSetting right = add(new BooleanSetting("Right Click", "Also show right-click CPS", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    public CpsDisplay() {
        super("CPS", "Shows your clicks per second.", Category.UTILITY, 0.0, 0.18, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        String value = right.get()
            ? CpsTracker.INSTANCE.getLeftCps() + " §8|§r " + CpsTracker.INSTANCE.getRightCps()
            : String.valueOf(CpsTracker.INSTANCE.getLeftCps());
        setSize(HudStyle.drawLabelValue(ctx, "CPS", value, Theme.TEXT, background.get()), HudStyle.lineHeight());
    }
}

package dev.aurora.client.module.modules.utility;

import dev.aurora.client.hud.HudLine;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.util.CpsTracker;
import net.minecraft.client.gui.DrawContext;

public final class CpsDisplay extends HudModule {
    private final BooleanSetting right = add(new BooleanSetting("Right Click", "Also show right-click CPS", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private final HudLine line = new HudLine(true);
    private int lastLeft = -1;
    private int lastRight = -1;
    private boolean lastBoth;
    private String value = "";

    public CpsDisplay() {
        super("CPS", "Shows your clicks per second.", Category.UTILITY, 0.0, 0.18, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        int left = CpsTracker.INSTANCE.getLeftCps();
        int rightCps = CpsTracker.INSTANCE.getRightCps();
        boolean both = right.get();
        if (left != lastLeft || rightCps != lastRight || both != lastBoth) {
            value = both ? left + " §8|§r " + rightCps : String.valueOf(left);
            lastLeft = left;
            lastRight = rightCps;
            lastBoth = both;
        }
        setSize(line.draw(ctx, "CPS", value, HudStyle.text(), background.get()), HudStyle.lineHeight());
    }
}

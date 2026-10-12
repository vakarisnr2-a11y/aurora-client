package dev.aurora.client.module.modules.utility;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class FpsDisplay extends HudModule {
    private final BooleanSetting colored = add(new BooleanSetting("Colored", "Color the value by performance", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    public FpsDisplay() {
        super("FPS", "Shows frames per second.", Category.UTILITY, 0.0, 0.08, true);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        int fps = MinecraftClient.getInstance().getCurrentFps();
        int color = !colored.get() ? Theme.TEXT : fps >= 60 ? 0xFF4ADE80 : fps >= 30 ? 0xFFFBBF24 : 0xFFF87171;
        setSize(HudStyle.drawLabelValue(ctx, "FPS", String.valueOf(fps), color, background.get()), HudStyle.lineHeight());
    }
}

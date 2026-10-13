package dev.aurora.client.module.modules.utility;

import dev.aurora.client.hud.HudLine;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.util.CachedText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class FpsDisplay extends HudModule {
    private final BooleanSetting colored = add(new BooleanSetting("Colored", "Color the value by performance", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private final HudLine line = new HudLine(true);
    private final CachedText text = new CachedText();

    public FpsDisplay() {
        super("FPS", "Shows frames per second.", Category.UTILITY, 0.0, 0.08, true);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        int fps = MinecraftClient.getInstance().getCurrentFps();
        int color = !colored.get() ? HudStyle.text() : fps >= 60 ? 0xFF4ADE80 : fps >= 30 ? 0xFFFBBF24 : 0xFFF87171;
        setSize(line.draw(ctx, "FPS", text.of(fps), color, background.get()), HudStyle.lineHeight());
    }
}

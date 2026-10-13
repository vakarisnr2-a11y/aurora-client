package dev.aurora.client.gui.menu;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** A rounded, translucent menu button with an animated accent hover. Behaves like a vanilla button otherwise. */
public class AuroraButton extends ButtonWidget {
    private final Animation hover = new Animation(0, 14);

    public AuroraButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hover.setTarget(active && (isHovered() || isFocused()) ? 1 : 0);
        float h = (float) hover.update();
        float x = getX(), y = getY(), w = getWidth(), ht = getHeight();
        int accent = Theme.accent();
        float r = Math.min(ht / 2f, 6);
        Render2D.roundedRect(ctx, x, y, w, ht, r, ColorUtil.lerp(0x9A12141F, ColorUtil.withAlpha(ColorUtil.darken(accent, 0.35f), 0xD8), h));
        Render2D.roundedOutline(ctx, x, y, w, ht, r, 1, ColorUtil.lerp(0x30FFFFFF, accent, h));
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int color = active ? ColorUtil.lerp(Theme.SUBTEXT, Theme.TEXT, 0.5f + 0.5f * h) : Theme.MUTED;
        Text msg = getMessage();
        ctx.drawText(tr, msg, (int) (x + (w - tr.getWidth(msg)) / 2f), (int) (y + (ht - 8) / 2f), color, false);
    }
}

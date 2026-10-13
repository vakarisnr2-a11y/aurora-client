package dev.aurora.client.gui.menu;

import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** A menu button in the Aurora style (rounded, translucent, accent on hover). Behaves like a vanilla button. */
public class AuroraButton extends ButtonWidget {
    private final Animation hover = new Animation(0, 14);
    private final boolean primary;

    public AuroraButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        this(x, y, width, height, message, onPress, false);
    }

    public AuroraButton(int x, int y, int width, int height, Text message, PressAction onPress, boolean primary) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.primary = primary;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hover.setTarget(active && (isHovered() || isFocused()) ? 1 : 0);
        float h = (float) hover.update();
        float x = getX(), y = getY(), w = getWidth(), ht = getHeight();
        float r = Math.min(ht / 2f, Ui.R_MEDIUM + 2);
        if (primary) {
            Render2D.shadow(ctx, x, y, w, ht, r, 4, ColorUtil.withAlpha(Ui.accent(), Math.round(60 + 60 * h)));
            Render2D.roundedRect(ctx, x, y, w, ht, r, ColorUtil.lerp(Ui.accent(0xE0), Ui.accentLight(), h * 0.4f), Ui.accent(0xE0));
        } else {
            Render2D.roundedRect(ctx, x, y, w, ht, r, ColorUtil.lerp(0xB00E111A, 0xD0191E2C, h));
            Render2D.roundedOutline(ctx, x, y, w, ht, r, 1, ColorUtil.lerp(Ui.BORDER, Ui.accent(0xC0), h));
        }
        String label = getMessage().getString();
        int color = !active ? Ui.MUTED : primary ? 0xFFFFFFFF : ColorUtil.lerp(Ui.SUBTEXT, Ui.TEXT, 0.4f + 0.6f * h);
        UiFont.draw(ctx, label, x + (w - UiFont.width(label, UiFont.Size.BODY, primary)) / 2f, y + (ht - 8) / 2f, color, UiFont.Size.BODY, primary);
    }
}

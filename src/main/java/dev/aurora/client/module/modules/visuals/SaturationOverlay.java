package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

/**
 * Shows your hidden saturation on the hunger bar: each food icon gets a golden overlay for the saturation it holds
 * (2 points per icon, like food). Drawn right after vanilla's hunger bar (InGameHudMixin).
 */
public final class SaturationOverlay extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", "Overlay color", 0x99FFC107));
    private final BooleanSetting outline = add(new BooleanSetting("Outline", "Outline style instead of a filled overlay", false));
    private final BooleanSetting value = add(new BooleanSetting("Show Value", "Write the saturation number next to the bar", false));

    public SaturationOverlay() {
        super("Saturation Overlay", "Shows food saturation on the hunger bar.", Category.VISUALS);
    }

    /** Called after vanilla drew the food icons; icon i sits at (right - i * 8 - 9, top). */
    public void render(DrawContext ctx, PlayerEntity player, int top, int right) {
        float saturation = player.getHungerManager().getSaturationLevel();
        int c = color.getColor();
        for (int i = 0; i < 10; i++) {
            float f = fraction(saturation, i);
            if (f <= 0) continue;
            float x = right - i * 8 - 9;
            float w = 9 * f;
            if (outline.get()) {
                Render2D.rect(ctx, x + 9 - w, top, w, 1, c);
                Render2D.rect(ctx, x + 9 - w, top + 8, w, 1, c);
                Render2D.rect(ctx, x + 8, top, 1, 9, c);
                if (f >= 1) Render2D.rect(ctx, x, top, 1, 9, c);
            } else {
                // Fills from the right edge of the icon, like a half food icon.
                Render2D.rect(ctx, x + 9 - w, top + 1, w, 7, c);
            }
        }
        if (value.get() && saturation > 0) {
            String text = String.format(Locale.ROOT, "%.1f", saturation);
            MinecraftClient mc = MinecraftClient.getInstance();
            ctx.drawText(mc.textRenderer, text, right - 81 - mc.textRenderer.getWidth(text) - 3, top + 1, c | 0xFF000000, true);
        }
    }

    /** Saturation shown on food icon {@code index} (0 = rightmost): 0..1 for its 2 points. */
    public static float fraction(float saturation, int index) {
        return Math.max(0f, Math.min(1f, (saturation - index * 2f) / 2f));
    }
}

package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.module.Category;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import dev.aurora.client.util.CpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

/** WASD, mouse buttons (with CPS) and space bar overlay with press animations. */
public final class Keystrokes extends HudModule {
    private final NumberSetting size = add(new NumberSetting("Key Size", "Size of each key", 22, 14, 32, 1));
    private final NumberSetting gap = add(new NumberSetting("Gap", "Space between keys", 2, 0, 5, 1));
    private final BooleanSetting mouse = add(new BooleanSetting("Mouse Buttons", "Show left and right mouse buttons", true));
    private final BooleanSetting cps = add(new BooleanSetting("CPS", "Show clicks per second on mouse buttons", true))
        .visibleWhen(mouse::get);
    private final BooleanSetting space = add(new BooleanSetting("Space Bar", "Show the jump key", true));

    private final Animation[] presses = new Animation[7];

    public Keystrokes() {
        super("Keystrokes", "Shows movement keys, mouse buttons and CPS as you press them.", Category.VISUALS, 0.0, 0.62, false);
        for (int i = 0; i < presses.length; i++) presses[i] = new Animation(0, 22);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        GameOptions o = MinecraftClient.getInstance().options;
        int s = size.getInt();
        int g = gap.getInt();
        int full = s * 3 + g * 2;
        int y = 0;
        key(ctx, 0, o.forwardKey, s + g, y, s, s, null);
        y += s + g;
        key(ctx, 1, o.leftKey, 0, y, s, s, null);
        key(ctx, 2, o.backKey, s + g, y, s, s, null);
        key(ctx, 3, o.rightKey, (s + g) * 2, y, s, s, null);
        y += s + g;
        if (mouse.get()) {
            int half = (full - g) / 2;
            int mh = cps.get() ? s + 2 : s;
            key(ctx, 4, o.attackKey, 0, y, half, mh, cps.get() ? CpsTracker.INSTANCE.getLeftCps() + " CPS" : null);
            key(ctx, 5, o.useKey, half + g, y, full - half - g, mh, cps.get() ? CpsTracker.INSTANCE.getRightCps() + " CPS" : null);
            y += mh + g;
        }
        if (space.get()) {
            key(ctx, 6, o.jumpKey, 0, y, full, Math.max(10, s / 2), null);
            y += Math.max(10, s / 2) + g;
        }
        setSize(full, y - g);
    }

    private void key(DrawContext ctx, int index, KeyBinding binding, int x, int y, int w, int h, String sub) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Animation a = presses[index];
        a.setTarget(binding.isPressed() ? 1 : 0);
        float p = (float) a.update();
        int bg = ColorUtil.lerp(Theme.hudBackground(), ColorUtil.withAlpha(Theme.accent(), 200), p);
        Render2D.roundedRect(ctx, x, y, w, h, Theme.radius(), bg);
        String label = index == 4 ? "LMB" : index == 5 ? "RMB" : index == 6 ? "——" : binding.getBoundKeyLocalizedText().getString();
        if (label.length() > 4) label = label.substring(0, 4);
        int textColor = ColorUtil.lerp(Theme.TEXT, 0xFF101014, p * 0.8f);
        if (sub == null) {
            ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 7) / 2, textColor, false);
        } else {
            ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + h / 2 - 8, textColor, false);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x + w / 2f, y + h / 2f + 2, 0);
            ctx.getMatrices().scale(0.6f, 0.6f, 1);
            ctx.drawText(tr, sub, -tr.getWidth(sub) / 2, 0, textColor, false);
            ctx.getMatrices().pop();
        }
    }
}

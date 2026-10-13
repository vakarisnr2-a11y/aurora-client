package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import dev.aurora.client.util.CpsTracker;
import net.minecraft.client.MinecraftClient;
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
        Animation a = presses[index];
        a.setTarget(binding.isPressed() ? 1 : 0);
        float p = (float) a.update();
        int accent = HudStyle.accent();
        int bg = ColorUtil.lerp(HudStyle.cell(), ColorUtil.withAlpha(accent, 210), p);
        if (p > 0.01f && HudStyle.animations()) {
            // Pressed keys shrink slightly toward their centre.
            float inset = p * 0.8f;
            HudStyle.cell(ctx, x + inset, y + inset, w - inset * 2, h - inset * 2, bg);
        } else {
            HudStyle.cell(ctx, x, y, w, h, bg);
        }
        String label = index == 4 ? "LMB" : index == 5 ? "RMB" : index == 6 ? "——" : binding.getBoundKeyLocalizedText().getString();
        if (label.length() > 4) label = label.substring(0, 4);
        int textColor = ColorUtil.lerp(HudStyle.text(), 0xFF101014, p * 0.8f);
        boolean shadow = HudStyle.textShadow() && p < 0.5f; // no light shadow under dark text on a pressed key
        if (sub == null) {
            HudStyle.text(ctx, label, x + (w - HudStyle.width(label)) / 2f, y + (h - 7) / 2f, textColor, shadow);
        } else {
            HudStyle.text(ctx, label, x + (w - HudStyle.width(label)) / 2f, y + h / 2f - 8, textColor, shadow);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x + w / 2f, y + h / 2f + 2, 0);
            ctx.getMatrices().scale(0.6f, 0.6f, 1);
            HudStyle.text(ctx, sub, -HudStyle.width(sub) / 2f, 0, textColor, shadow);
            ctx.getMatrices().pop();
        }
    }
}

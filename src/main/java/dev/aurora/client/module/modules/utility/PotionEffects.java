package dev.aurora.client.module.modules.utility;

import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.hud.SmoothBox;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Active potion effects with icons, level and remaining time. Can hide the vanilla effect HUD. */
public final class PotionEffects extends HudModule {
    private static final String[] ROMAN = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
    private static final Comparator<StatusEffectInstance> LONGEST_FIRST =
        Comparator.comparingInt(StatusEffectInstance::getDuration).reversed();
    private static final int ROW = 20;
    private static final int ICON = 18;

    private final BooleanSetting hideVanilla = add(new BooleanSetting("Hide Vanilla", "Hide Minecraft's effect icons in the corner", true));
    private final BooleanSetting icons = add(new BooleanSetting("Icons", "Show effect icons", true));
    private final BooleanSetting typeColors = add(new BooleanSetting("Type Colors", "Green for beneficial, red for harmful effects", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private final List<StatusEffectInstance> effects = new ArrayList<>();
    private final SmoothBox box = new SmoothBox();

    public PotionEffects() {
        super("Potion Effects", "Lists your active effects with time remaining.", Category.UTILITY, 1.0, 0.45, true);
    }

    public boolean hidesVanilla() {
        return hideVanilla.get();
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        effects.clear();
        if (mc.player != null) effects.addAll(mc.player.getStatusEffects());
        effects.sort(LONGEST_FIRST);
        boolean right = isRightSide();
        int pad = HudStyle.padding();
        if (effects.isEmpty()) {
            if (editor) {
                String name = "Speed II";
                String time = "1:30";
                int w = tr.getWidth(name) + pad * 2;
                box.update(w, ROW + pad);
                if (background.get()) HudStyle.background(ctx, 0, 0, box.width(), box.height());
                HudStyle.text(ctx, name, pad, pad / 2f + 2, 0xFF86EFAC);
                HudStyle.text(ctx, time, pad, pad / 2f + 11, HudStyle.sub());
                setSize(box.intWidth(), box.intHeight());
            } else {
                setSize(80, ROW);
            }
            return;
        }
        int maxW = 0;
        for (StatusEffectInstance e : effects) maxW = Math.max(maxW, tr.getWidth(name(e)));
        int iconW = icons.get() ? ICON + 3 : 0;
        int width = maxW + iconW + pad * 2;
        int top = pad / 2;
        box.update(width, effects.size() * ROW + top * 2);
        if (background.get()) HudStyle.background(ctx, 0, 0, box.width(), box.height());
        float boxW = box.width();
        int sub = HudStyle.sub();
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance e = effects.get(i);
            int y = top + i * ROW;
            String name = name(e);
            if (icons.get()) {
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(e.getEffectType());
                int ix = right ? Math.round(boxW) - pad - ICON : pad;
                ctx.drawSprite(ix, y + 1, 0, ICON, ICON, sprite);
            }
            int color = !typeColors.get() ? HudStyle.text()
                : e.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL ? 0xFFF87171 : 0xFF86EFAC;
            float textX = right ? boxW - pad - iconW - tr.getWidth(name) : pad + iconW;
            HudStyle.text(ctx, name, textX, y + 2, color);
            String time = duration(e);
            boolean expiring = !e.isInfinite() && e.getDuration() < 200;
            int timeColor = expiring ? ColorUtil.lerp(sub, 0xFFF87171, (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 120.0))) : sub;
            float timeX = right ? boxW - pad - iconW - tr.getWidth(time) : pad + iconW;
            HudStyle.text(ctx, time, timeX, y + 11, timeColor);
        }
        setSize(box.intWidth(), box.intHeight());
    }

    private static String name(StatusEffectInstance e) {
        int level = e.getAmplifier() + 1;
        String roman = level < ROMAN.length ? ROMAN[level] : " " + level;
        return e.getEffectType().value().getName().getString() + roman;
    }

    private static String duration(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int seconds = e.getDuration() / 20;
        return seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
    }
}

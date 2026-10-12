package dev.aurora.client.module.modules.utility;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
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

/** Active potion effects with icons, level and remaining time. Can hide the vanilla effect HUD. */
public final class PotionEffects extends HudModule {
    private static final String[] ROMAN = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    private final BooleanSetting hideVanilla = add(new BooleanSetting("Hide Vanilla", "Hide Minecraft's effect icons in the corner", true));
    private final BooleanSetting icons = add(new BooleanSetting("Icons", "Show effect icons", true));
    private final BooleanSetting typeColors = add(new BooleanSetting("Type Colors", "Green for beneficial, red for harmful effects", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private final List<StatusEffectInstance> effects = new ArrayList<>();

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
        effects.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());
        boolean right = isRightSide();
        int lh = 20;
        if (effects.isEmpty()) {
            if (editor) {
                String sample = "Speed II  1:30";
                int w = tr.getWidth(sample) + HudStyle.PADDING * 2;
                if (background.get()) HudStyle.panel(ctx, 0, 0, w, lh);
                ctx.drawText(tr, sample, HudStyle.PADDING, 6, Theme.TEXT, true);
                setSize(w, lh);
            } else {
                setSize(80, lh);
            }
            return;
        }
        int maxW = 0;
        for (StatusEffectInstance e : effects) maxW = Math.max(maxW, tr.getWidth(name(e)));
        int iconW = icons.get() ? 20 : 0;
        int width = maxW + iconW + HudStyle.PADDING * 2;
        int height = effects.size() * lh;
        if (background.get()) HudStyle.panel(ctx, 0, 0, width, height);
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance e = effects.get(i);
            int y = i * lh;
            int textX = right ? width - HudStyle.PADDING - tr.getWidth(name(e)) - iconW : HudStyle.PADDING + iconW;
            if (icons.get()) {
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(e.getEffectType());
                int ix = right ? width - HudStyle.PADDING - 18 : HudStyle.PADDING;
                ctx.drawSprite(ix, y + 1, 0, 18, 18, sprite);
            }
            int color = !typeColors.get() ? Theme.TEXT
                : e.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL ? 0xFFF87171 : 0xFF86EFAC;
            ctx.drawText(tr, name(e), textX, y + 2, color, true);
            String time = duration(e);
            boolean expiring = !e.isInfinite() && e.getDuration() < 200;
            int timeColor = expiring ? ColorUtil.lerp(Theme.SUBTEXT, 0xFFF87171,
                (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 120.0))) : Theme.SUBTEXT;
            int timeX = right ? width - HudStyle.PADDING - iconW - tr.getWidth(time) : HudStyle.PADDING + iconW;
            ctx.drawText(tr, time, timeX, y + 11, timeColor, true);
        }
        setSize(width, height);
    }

    private static String name(StatusEffectInstance e) {
        int level = e.getAmplifier() + 1;
        String roman = level < ROMAN.length ? ROMAN[level] : " " + level;
        return e.getEffectType().value().getName().getString() + roman;
    }

    private static String duration(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int seconds = e.getDuration() / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}

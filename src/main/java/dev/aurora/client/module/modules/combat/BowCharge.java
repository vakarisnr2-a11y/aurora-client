package dev.aurora.client.module.modules.combat;

import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;

/** How far your bow (or crossbow) is drawn, as a bar and percentage. */
public final class BowCharge extends HudModule {
    private static final int READY_COLOR = 0xFF4ADE80;
    private static final int BAR_W = 60;
    private static final int BAR_H = 3;
    private static final int PAD = 4;

    private final BooleanSetting percent = add(new BooleanSetting("Percent", "Show the charge as a percentage", true));
    private final BooleanSetting hideIdle = add(new BooleanSetting("Hide When Idle", "Only show while drawing a bow or crossbow", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    public BowCharge() {
        super("Bow Charge", "Shows how far your bow is drawn.", Category.COMBAT, 0.5, 0.45, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float progress = charge(mc.player);
        if (progress < 0) {
            if (!editor && hideIdle.get()) {
                setSize(BAR_W + PAD * 2, 16);
                return;
            }
            progress = editor ? 0.65f : 0f;
        }
        TextRenderer tr = mc.textRenderer;
        boolean ready = progress >= 1f;
        String label = ready ? "Ready" : percent.get() ? Math.round(progress * 100) + "%" : "Bow";
        int width = BAR_W + PAD * 2;
        int height = PAD + tr.fontHeight + 2 + BAR_H + PAD;
        if (background.get()) HudStyle.background(ctx, 0, 0, width, height);
        int color = ready ? READY_COLOR : HudStyle.accent();
        HudStyle.text(ctx, label, (width - tr.getWidth(label)) / 2f, PAD, ready ? READY_COLOR : HudStyle.text());
        float barY = PAD + tr.fontHeight + 2;
        HudStyle.bar(ctx, PAD, barY, BAR_W, BAR_H, progress, color);
        setSize(width, height);
    }

    /** Draw progress 0..1 of the bow or crossbow being used, or -1 when none is being drawn. */
    private static float charge(ClientPlayerEntity p) {
        if (p == null || !p.isUsingItem()) return -1;
        ItemStack stack = p.getActiveItem();
        int ticks = p.getItemUseTime();
        if (stack.getItem() instanceof BowItem) return bowProgress(ticks);
        if (stack.getItem() instanceof CrossbowItem) return crossbowProgress(ticks, CrossbowItem.getPullTime(stack, p));
        return -1;
    }

    /** Same curve as BowItem.getPullProgress (checked with javap against 1.21.1): full after 20 ticks. */
    public static float bowProgress(int useTicks) {
        float f = useTicks / 20f;
        f = (f * f + f * 2f) / 3f;
        return Math.min(1f, f);
    }

    /** Same as CrossbowItem's private getPullProgress: linear over the pull time (Quick Charge shortens it). */
    public static float crossbowProgress(int useTicks, int pullTime) {
        return Math.min(1f, useTicks / (float) Math.max(1, pullTime));
    }
}

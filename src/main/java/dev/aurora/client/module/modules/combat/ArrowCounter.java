package dev.aurora.client.module.modules.combat;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;

/** Counts the arrows (normal, spectral and tipped) in your inventory. */
public final class ArrowCounter extends HudModule {
    private static final int LOW_COLOR = 0xFFFF5555;
    private static final int SAMPLE_COUNT = 24;
    private static final int ICON = 16;
    private static final int PAD = 2;

    private final BooleanSetting onlyWithBow = add(new BooleanSetting("Only With Bow", "Only show while holding a bow or crossbow", false));
    private final NumberSetting lowWarning = add(new NumberSetting("Low Warning", "Show the count in red at or below this many arrows (0 = off)", 8, 0, 64, 1));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private ItemStack icon;

    public ArrowCounter() {
        super("Arrow Counter", "Shows how many arrows you carry.", Category.COMBAT, 1.0, 0.62, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        boolean holding = p != null && (isRanged(p.getMainHandStack()) || isRanged(p.getOffHandStack()));
        if (!isShown(onlyWithBow.get(), holding, editor)) {
            setSize(40, ICON + PAD * 2);
            return;
        }
        int count = p == null ? SAMPLE_COUNT : countArrows(p.getInventory());
        if (icon == null) icon = new ItemStack(Items.ARROW);
        TextRenderer tr = mc.textRenderer;
        String text = String.valueOf(count);
        int width = PAD + ICON + 2 + tr.getWidth(text) + PAD + 1;
        int height = ICON + PAD * 2;
        if (background.get()) HudStyle.panel(ctx, 0, 0, width, height);
        ctx.drawItem(icon, PAD, PAD);
        ctx.drawText(tr, text, PAD + ICON + 2, PAD + 4, color(count, lowWarning.getInt()), true);
        setSize(width, height);
    }

    private static boolean isRanged(ItemStack stack) {
        return stack.getItem() instanceof RangedWeaponItem;
    }

    private static int countArrows(PlayerInventory inventory) {
        return inventory.count(Items.ARROW) + inventory.count(Items.SPECTRAL_ARROW) + inventory.count(Items.TIPPED_ARROW);
    }

    public static boolean isShown(boolean onlyWithBow, boolean holdingBow, boolean editor) {
        return editor || !onlyWithBow || holdingBow;
    }

    /** Red at or below the warning threshold (a threshold of 0 never warns). */
    public static int color(int count, int lowWarning) {
        return lowWarning > 0 && count <= lowWarning ? LOW_COLOR : Theme.TEXT;
    }
}

package dev.aurora.client.module.modules.utility;

import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Armor pieces (and optionally held items) with durability. */
public final class ArmorStatus extends HudModule {
    public enum Layout {
        VERTICAL, HORIZONTAL
    }

    public enum DurabilityMode {
        PERCENT, VALUE, BAR, NONE
    }

    private final EnumSetting<Layout> layout = add(new EnumSetting<>("Layout", "Stack direction", Layout.VERTICAL));
    private final EnumSetting<DurabilityMode> mode = add(new EnumSetting<>("Durability", "How durability is shown", DurabilityMode.PERCENT));
    private final BooleanSetting hands = add(new BooleanSetting("Held Items", "Also show main and off-hand items", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", false));

    private final ItemStack[] stacks = new ItemStack[6];
    private ItemStack[] samples;

    public ArmorStatus() {
        super("Armor Status", "Shows your armor and held items with durability.", Category.UTILITY, 1.0, 0.75, true);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        int count = 0;
        if (p != null) {
            for (int i = 3; i >= 0; i--) {
                ItemStack s = p.getInventory().getArmorStack(i);
                if (!s.isEmpty()) stacks[count++] = s;
            }
            if (hands.get()) {
                if (!p.getMainHandStack().isEmpty()) stacks[count++] = p.getMainHandStack();
                if (!p.getOffHandStack().isEmpty()) stacks[count++] = p.getOffHandStack();
            }
        }
        if (count == 0 && editor) {
            if (samples == null) {
                samples = new ItemStack[]{new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
                    new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_BOOTS)};
            }
            System.arraycopy(samples, 0, stacks, 0, samples.length);
            count = samples.length;
        }
        TextRenderer tr = mc.textRenderer;
        boolean vertical = layout.get() == Layout.VERTICAL;
        boolean right = isRightSide();
        int textW = mode.get() == DurabilityMode.PERCENT || mode.get() == DurabilityMode.VALUE ? 26 : 0;
        int cellW = vertical ? 18 + textW : 18;
        int cellH = vertical ? 18 : 18 + (textW > 0 ? 9 : 0);
        int width = vertical ? cellW + 4 : Math.max(1, count) * cellW + 4;
        int height = vertical ? Math.max(1, count) * cellH + 4 : cellH + 4;
        if (background.get() && count > 0) HudStyle.background(ctx, 0, 0, width, height);

        for (int i = 0; i < count; i++) {
            ItemStack s = stacks[i];
            int x = vertical ? (right ? width - 20 : 2) : 2 + i * cellW;
            int y = vertical ? 2 + i * cellH : 2;
            ctx.drawItem(s, x + 1, y + 1);
            if (!s.isDamageable()) {
                if (s.getCount() > 1) ctx.drawItemInSlot(tr, s, x + 1, y + 1);
                continue;
            }
            int remaining = s.getMaxDamage() - s.getDamage();
            float fraction = remaining / (float) s.getMaxDamage();
            int color = ColorUtil.healthColor(fraction);
            switch (mode.get()) {
                case PERCENT, VALUE -> {
                    String text = mode.get() == DurabilityMode.PERCENT ? Math.round(fraction * 100) + "%" : String.valueOf(remaining);
                    if (vertical) {
                        int tx = right ? x - 2 - tr.getWidth(text) : x + 20;
                        HudStyle.text(ctx, text, tx, y + 5, color);
                    } else {
                        HudStyle.text(ctx, text, x + 9 - tr.getWidth(text) / 2f, y + 18, color);
                    }
                }
                case BAR -> {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(0, 0, 200);
                    HudStyle.bar(ctx, x + 2, y + 15, 14, 2, fraction, color);
                    ctx.getMatrices().pop();
                }
                case NONE -> {
                }
            }
        }
        for (int i = 0; i < stacks.length; i++) stacks[i] = null;
        setSize(width, height);
    }

    public int textColor() {
        return HudStyle.text();
    }
}

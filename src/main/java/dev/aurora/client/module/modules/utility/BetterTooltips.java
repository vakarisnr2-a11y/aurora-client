package dev.aurora.client.module.modules.utility;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Item tooltip upgrades: exact durability ("Durability: 1203 / 1561", colored by how worn the item is) and enchantment
 * levels as numbers ("Sharpness 5" instead of "Sharpness V", also beyond X where vanilla shows a raw key).
 */
public final class BetterTooltips extends Module {
    private final BooleanSetting durability = add(new BooleanSetting("Durability", "Show exact durability on damageable items", true));
    private final BooleanSetting numbers = add(new BooleanSetting("Enchant Numbers", "Show enchantment levels as numbers", true));

    public BetterTooltips() {
        super("Better Tooltips", "Exact durability numbers and enchantment levels as numbers in item tooltips.", Category.UTILITY);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    public boolean numberedLevels() {
        return isEnabled() && numbers.get();
    }

    /** Called from Fabric's tooltip event. */
    public void onTooltip(ItemStack stack, TooltipType type, List<Text> lines) {
        if (!durability.get() || !stack.isDamageable() || lines.isEmpty()) return;
        // Vanilla already shows durability on advanced tooltips (F3+H) for damaged items.
        if (type.isAdvanced() && stack.isDamaged()) return;
        int max = stack.getMaxDamage();
        int left = max - stack.getDamage();
        int color = ColorUtil.healthColor(left / (float) max) & 0xFFFFFF;
        Text line = Text.literal("Durability: ").formatted(Formatting.GRAY)
            .append(Text.literal(left + " / " + max).styled(s -> s.withColor(TextColor.fromRgb(color))));
        lines.add(line);
    }

    /** "enchantment.level.5" → "5"; anything else is returned as null (left to vanilla). */
    public static String levelNumber(String translationKey) {
        String prefix = "enchantment.level.";
        if (translationKey == null || !translationKey.startsWith(prefix)) return null;
        String n = translationKey.substring(prefix.length());
        if (n.isEmpty()) return null;
        for (int i = 0; i < n.length(); i++) if (!Character.isDigit(n.charAt(i))) return null;
        return n;
    }
}

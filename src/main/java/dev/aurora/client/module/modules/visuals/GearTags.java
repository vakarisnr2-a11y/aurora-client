package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Appends the armor tier and held sword tier of other players to their nametags, e.g. "Steve ⛨D ⚔I".
 *
 * <p>Only gear the server shows to everyone is used: the worn armor pieces and the item in the
 * main hand, exactly what is drawn on the player model. Nothing is added for invisible players,
 * and enchantments are not read.</p>
 */
public final class GearTags extends Module {
    public enum Format {
        SHORT, FULL
    }

    /** Gear materials, weakest first within armor (leather..netherite) and within swords (wood..netherite). */
    public enum Tier {
        WOOD("W", "Wood", 0xB5895A),
        LEATHER("L", "Leather", 0xC0703C),
        GOLD("G", "Gold", 0xFFAA00),
        STONE("S", "Stone", 0x9E9E9E),
        CHAINMAIL("C", "Chain", 0xB4B4B4),
        IRON("I", "Iron", 0xFFFFFF),
        DIAMOND("D", "Diamond", 0x55FFFF),
        NETHERITE("N", "Netherite", 0xB08F8F);

        public final String code;
        public final String label;
        public final int color;

        Tier(String code, String label, int color) {
            this.code = code;
            this.label = label;
            this.color = color;
        }
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final BooleanSetting armor = add(new BooleanSetting("Armor", "Show the best armor tier worn", true));
    private final BooleanSetting sword = add(new BooleanSetting("Sword", "Show the tier of a sword held in the main hand", true));
    private final EnumSetting<Format> format = add(new EnumSetting<>("Format", "Short letters (D) or full names (Diamond)", Format.SHORT));
    private final BooleanSetting enemiesOnly = add(new BooleanSetting("Enemies Only", "Skip players on your scoreboard team", true));

    public GearTags() {
        super("Gear Tags", "Adds enemies' visible armor and sword tier to their nametags.", Category.VISUALS);
    }

    public Text decorate(Entity entity, Text text) {
        if (!(entity instanceof PlayerEntity player)) return text;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || player == mc.player || player.isInvisibleTo(mc.player)) return text;
        if (enemiesOnly.get() && player.isTeammate(mc.player)) return text;

        Tier armorTier = null;
        if (armor.get()) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                armorTier = better(armorTier, armorTier(itemPath(player.getEquippedStack(slot))));
            }
        }
        Tier swordTier = sword.get() ? swordTier(itemPath(player.getMainHandStack())) : null;
        if (armorTier == null && swordTier == null) return text;

        MutableText out = text.copy();
        if (armorTier != null) append(out, "⛨", armorTier);
        if (swordTier != null) append(out, "⚔", swordTier);
        return out;
    }

    private void append(MutableText out, String icon, Tier tier) {
        String name = format.get() == Format.FULL ? tier.label : tier.code;
        out.append(Text.literal(" " + icon + name).styled(s -> s.withColor(tier.color)));
    }

    private static String itemPath(ItemStack stack) {
        return stack.isEmpty() ? "" : Registries.ITEM.getId(stack.getItem()).getPath();
    }

    /** Tier of an armor item id path such as "diamond_leggings"; null for anything else (incl. turtle shells). */
    public static Tier armorTier(String itemPath) {
        for (String piece : new String[]{"_helmet", "_chestplate", "_leggings", "_boots"}) {
            if (itemPath.endsWith(piece)) return material(itemPath.substring(0, itemPath.length() - piece.length()));
        }
        return null;
    }

    /** Tier of a sword item id path such as "stone_sword"; null for anything else. */
    public static Tier swordTier(String itemPath) {
        return itemPath.endsWith("_sword") ? material(itemPath.substring(0, itemPath.length() - "_sword".length())) : null;
    }

    /** The stronger of two armor tiers (null counts as nothing worn). */
    public static Tier better(Tier a, Tier b) {
        if (a == null) return b;
        if (b == null) return a;
        return b.ordinal() > a.ordinal() ? b : a;
    }

    private static Tier material(String prefix) {
        return switch (prefix) {
            case "wooden" -> Tier.WOOD;
            case "leather" -> Tier.LEATHER;
            case "golden" -> Tier.GOLD;
            case "stone" -> Tier.STONE;
            case "chainmail" -> Tier.CHAINMAIL;
            case "iron" -> Tier.IRON;
            case "diamond" -> Tier.DIAMOND;
            case "netherite" -> Tier.NETHERITE;
            default -> null;
        };
    }
}

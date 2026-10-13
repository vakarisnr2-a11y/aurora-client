package dev.aurora.client.module.modules.utility;

import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Counts the Bedwars currencies (iron, gold, diamonds, emeralds) in your inventory, with item icons. */
public final class ResourceCounter extends HudModule {
    public enum Layout {
        VERTICAL, HORIZONTAL
    }

    public enum Resource {
        IRON(0xFFE5E7EB), GOLD(0xFFFBBF24), DIAMOND(0xFF55FFFF), EMERALD(0xFF4ADE80);

        public final int color;

        Resource(int color) {
            this.color = color;
        }

        /** Resolved on use: Items must not be touched while modules are constructed. */
        public Item item() {
            return switch (this) {
                case IRON -> Items.IRON_INGOT;
                case GOLD -> Items.GOLD_INGOT;
                case DIAMOND -> Items.DIAMOND;
                case EMERALD -> Items.EMERALD;
            };
        }
    }

    private static final Resource[] RESOURCES = Resource.values();
    private static final int[] SAMPLE_COUNTS = {48, 12, 3, 1};
    private static final int ICON = 16;
    private static final int ROW = 17;
    private static final int PAD = 2;
    private static final int GAP = 6;

    private final EnumSetting<Layout> layout = add(new EnumSetting<>("Layout", "Stack direction", Layout.VERTICAL));
    private final BooleanSetting iron = add(new BooleanSetting("Iron", "Count iron ingots", true));
    private final BooleanSetting gold = add(new BooleanSetting("Gold", "Count gold ingots", true));
    private final BooleanSetting diamond = add(new BooleanSetting("Diamonds", "Count diamonds", true));
    private final BooleanSetting emerald = add(new BooleanSetting("Emeralds", "Count emeralds", true));
    private final BooleanSetting hideEmpty = add(new BooleanSetting("Hide Empty", "Hide resources you have none of", false));
    private final BooleanSetting colored = add(new BooleanSetting("Colored", "Color each count like its resource", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private final int[] counts = new int[RESOURCES.length];
    private final int[] shown = new int[RESOURCES.length];
    private final String[] labels = new String[RESOURCES.length];
    private ItemStack[] icons;

    public ResourceCounter() {
        super("Resource Counter", "Shows how much iron, gold, diamonds and emeralds you carry.", Category.UTILITY, 0.0, 0.3, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (icons == null) {
            icons = new ItemStack[RESOURCES.length];
            for (int i = 0; i < RESOURCES.length; i++) icons[i] = new ItemStack(RESOURCES[i].item());
        }
        TextRenderer tr = mc.textRenderer;
        int n = 0;
        int textW = 0;
        for (int i = 0; i < RESOURCES.length; i++) {
            counts[i] = p == null ? (editor ? SAMPLE_COUNTS[i] : 0) : p.getInventory().count(RESOURCES[i].item());
            if (!isShown(enabled(RESOURCES[i]), counts[i], hideEmpty.get(), editor)) continue;
            labels[i] = String.valueOf(counts[i]);
            textW = Math.max(textW, tr.getWidth(labels[i]));
            shown[n++] = i;
        }
        if (n == 0) {
            setSize(40, ROW);
            return;
        }

        boolean vertical = layout.get() == Layout.VERTICAL;
        int width;
        int height;
        if (vertical) {
            width = PAD + ICON + 2 + textW + PAD + 1;
            height = PAD * 2 + n * ROW - 1;
        } else {
            width = PAD * 2 - GAP;
            for (int k = 0; k < n; k++) width += ICON + 2 + tr.getWidth(labels[shown[k]]) + GAP;
            height = PAD * 2 + ICON;
        }
        if (background.get()) HudStyle.background(ctx, 0, 0, width, height);

        boolean right = vertical && isRightSide();
        int x = PAD;
        for (int k = 0; k < n; k++) {
            int i = shown[k];
            int y = vertical ? PAD + k * ROW : PAD;
            int color = colored.get() ? RESOURCES[i].color : HudStyle.text();
            int labelW = tr.getWidth(labels[i]);
            if (right) {
                int iconX = width - PAD - ICON;
                ctx.drawItem(icons[i], iconX, y);
                HudStyle.text(ctx, labels[i], iconX - 2 - labelW, y + 4, color);
            } else {
                ctx.drawItem(icons[i], x, y);
                HudStyle.text(ctx, labels[i], x + ICON + 2, y + 4, color);
                if (!vertical) x += ICON + 2 + labelW + GAP;
            }
        }
        setSize(width, height);
    }

    private boolean enabled(Resource resource) {
        return switch (resource) {
            case IRON -> iron.get();
            case GOLD -> gold.get();
            case DIAMOND -> diamond.get();
            case EMERALD -> emerald.get();
        };
    }

    /** A resource is drawn when enabled and either held, not hiding empty ones, or shown in the HUD editor. */
    public static boolean isShown(boolean enabled, int count, boolean hideEmpty, boolean editor) {
        return enabled && (count > 0 || !hideEmpty || editor);
    }
}

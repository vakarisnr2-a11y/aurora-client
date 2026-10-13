package dev.aurora.client.module.modules.utility;

import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.hud.SmoothBox;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Locale;

public final class Coordinates extends HudModule {
    private static final int MAX_LINES = 4;

    private final NumberSetting decimals = add(new NumberSetting("Decimals", "Decimal places", 1, 0, 3, 1));
    private final BooleanSetting direction = add(new BooleanSetting("Direction", "Show facing direction", true));
    private final BooleanSetting otherDimension = add(new BooleanSetting("Nether/Overworld", "Show converted coordinates of the other dimension", true));
    private final BooleanSetting biome = add(new BooleanSetting("Biome", "Show the current biome", false));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private final String[] labels = new String[MAX_LINES];
    private final String[] values = new String[MAX_LINES];
    private final SmoothBox box = new SmoothBox();

    public Coordinates() {
        super("Coordinates", "Shows your position, facing and converted dimension coordinates.", Category.UTILITY, 0.0, 0.92, true);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        ClientPlayerEntity p = mc.player;
        int n = 0;
        String fmt = "%." + decimals.getInt() + "f";
        double x = p == null ? 0 : p.getX(), y = p == null ? 64 : p.getY(), z = p == null ? 0 : p.getZ();
        labels[n] = "XYZ";
        values[n++] = f(fmt, x) + " " + f(fmt, y) + " " + f(fmt, z);
        if (direction.get() && p != null) {
            Direction d = p.getHorizontalFacing();
            String axis = switch (d) {
                case NORTH -> "-Z";
                case SOUTH -> "+Z";
                case WEST -> "-X";
                default -> "+X";
            };
            labels[n] = "Facing";
            values[n++] = capitalize(d.asString()) + " (" + axis + ")";
        }
        if (otherDimension.get() && p != null && mc.world != null) {
            if (mc.world.getRegistryKey() == World.NETHER) {
                labels[n] = "Overworld";
                values[n++] = f(fmt, x * 8) + " " + f(fmt, z * 8);
            } else if (mc.world.getRegistryKey() == World.OVERWORLD) {
                labels[n] = "Nether";
                values[n++] = f(fmt, x / 8) + " " + f(fmt, z / 8);
            }
        }
        if (biome.get() && p != null && mc.world != null) {
            String id = mc.world.getBiome(p.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("unknown");
            labels[n] = "Biome";
            values[n++] = capitalize(id.replace('_', ' '));
        }
        int pad = HudStyle.padding();
        int labelW = 0;
        for (int i = 0; i < n; i++) labelW = Math.max(labelW, tr.getWidth(labels[i]));
        int valueW = 0;
        for (int i = 0; i < n; i++) valueW = Math.max(valueW, tr.getWidth(values[i]));
        int lh = tr.fontHeight + 2;
        box.update(labelW + 4 + valueW + pad * 2, n * lh + pad * 2 - 2);
        if (background.get()) HudStyle.background(ctx, 0, 0, box.width(), box.height());
        int label = HudStyle.label();
        int text = HudStyle.text();
        for (int i = 0; i < n; i++) {
            int ly = pad + i * lh;
            HudStyle.text(ctx, labels[i], pad, ly, label);
            HudStyle.text(ctx, values[i], pad + labelW + 4, ly, text);
        }
        setSize(box.intWidth(), box.intHeight());
    }

    private static String f(String fmt, double v) {
        return String.format(Locale.ROOT, fmt, v);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

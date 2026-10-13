package dev.aurora.client.module.modules.utility;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

/** Replaces the signal bars in the tab list with each player's latency in milliseconds. */
public final class TabPing extends Module {
    /** Width of the vanilla latency icon the text replaces. */
    private static final int ICON_WIDTH = 10;

    private final BooleanSetting colored = add(new BooleanSetting("Colored", "Color the number by latency", true));
    private final BooleanSetting unit = add(new BooleanSetting("Show Unit", "Append \"ms\" to the number", true));

    public TabPing() {
        super("Tab Ping", "Shows ping as a number in milliseconds in the tab list.", Category.UTILITY);
    }

    /** Extra column width needed for the widest ping text among the listed players (0 if it fits the icon). */
    public int extraColumnWidth() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return 0;
        int widest = 0;
        for (PlayerListEntry entry : handler.getListedPlayerListEntries()) {
            widest = Math.max(widest, mc.textRenderer.getWidth(format(entry.getLatency(), unit.get())));
        }
        return Math.max(0, widest - ICON_WIDTH);
    }

    /** Draws the latency right-aligned where vanilla puts the icon ({@code x + width - 11}, 10 px wide). */
    public void render(DrawContext ctx, int width, int x, int y, PlayerListEntry entry) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int latency = entry.getLatency();
        String text = format(latency, unit.get());
        ctx.drawTextWithShadow(tr, text, x + width - 1 - tr.getWidth(text), y, colored.get() ? color(latency) : 0xFFFFFFFF);
    }

    /** "42ms", or "?" while the server has not reported a latency yet (negative values). */
    public static String format(int latency, boolean withUnit) {
        if (latency < 0) return "?";
        return withUnit ? latency + "ms" : String.valueOf(latency);
    }

    /** Green up to 80 ms and yellow up to 160 ms like the Ping HUD, then orange up to 300 ms, then red. */
    public static int color(int latency) {
        if (latency < 0) return 0xFF9BA1B5;
        if (latency <= 80) return 0xFF4ADE80;
        if (latency <= 160) return 0xFFFBBF24;
        if (latency <= 300) return 0xFFFB923C;
        return 0xFFF87171;
    }
}

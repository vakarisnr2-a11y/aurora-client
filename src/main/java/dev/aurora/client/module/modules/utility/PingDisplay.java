package dev.aurora.client.module.modules.utility;

import dev.aurora.client.hud.HudLine;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.util.CachedText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

public final class PingDisplay extends HudModule {
    private final BooleanSetting colored = add(new BooleanSetting("Colored", "Color the value by latency", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw a background", true));

    private final HudLine line = new HudLine(true);
    private final CachedText text = new CachedText(" ms");

    public PingDisplay() {
        super("Ping", "Shows your latency to the server.", Category.UTILITY, 0.0, 0.13, true);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        int color = !colored.get() ? HudStyle.text() : ping <= 80 ? 0xFF4ADE80 : ping <= 160 ? 0xFFFBBF24 : 0xFFF87171;
        setSize(line.draw(ctx, "Ping", text.of(ping), color, background.get()), HudStyle.lineHeight());
    }
}

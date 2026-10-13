package dev.aurora.client.module.modules.world;

import dev.aurora.client.hud.HudLine;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.biome.Biome;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** In-game day, time and weather (plus optional real-world clock). */
public final class TimeWeather extends HudModule {
    private static final DateTimeFormatter REAL = DateTimeFormatter.ofPattern("HH:mm");

    private final BooleanSetting showDay = add(new BooleanSetting("Day", "Show the day counter", true));
    private final BooleanSetting twentyFour = add(new BooleanSetting("24h", "Use 24-hour time", true));
    private final BooleanSetting weather = add(new BooleanSetting("Weather", "Show the current weather", true));
    private final BooleanSetting realTime = add(new BooleanSetting("Real Time", "Also show your local time", false));

    private final HudLine line = new HudLine(false);

    public TimeWeather() {
        super("Time & Weather", "Shows in-game time, day and weather.", Category.WORLD, 1.0, 0.04, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        ClientWorld world = MinecraftClient.getInstance().world;
        long timeOfDay = world == null ? 6000 : world.getTimeOfDay();
        long ticks = Math.floorMod(timeOfDay, 24000L);
        int hours = (int) ((ticks / 1000 + 6) % 24);
        int minutes = (int) (ticks % 1000 * 60 / 1000);
        StringBuilder sb = new StringBuilder();
        if (showDay.get()) sb.append("Day ").append(timeOfDay / 24000L + 1).append("  ");
        if (twentyFour.get()) {
            sb.append(String.format(Locale.ROOT, "%02d:%02d", hours, minutes));
        } else {
            int h12 = hours % 12 == 0 ? 12 : hours % 12;
            sb.append(String.format(Locale.ROOT, "%d:%02d %s", h12, minutes, hours < 12 ? "AM" : "PM"));
        }
        if (weather.get()) sb.append(" §8|§r ").append(weather(world));
        if (realTime.get()) sb.append(" §8|§r ").append(LocalTime.now().format(REAL));
        setSize(line.draw(ctx, hours >= 6 && hours < 18 ? "☀" : "☽", sb.toString(), HudStyle.text(), true), HudStyle.lineHeight());
    }

    private static String weather(ClientWorld world) {
        if (world == null) return "Clear";
        MinecraftClient mc = MinecraftClient.getInstance();
        if (world.isThundering()) return "⚡ Thunder";
        if (world.isRaining()) {
            if (mc.player != null && world.getBiome(mc.player.getBlockPos()).value()
                .getPrecipitation(mc.player.getBlockPos()) == Biome.Precipitation.SNOW) return "❄ Snow";
            return "☂ Rain";
        }
        return "Clear";
    }
}

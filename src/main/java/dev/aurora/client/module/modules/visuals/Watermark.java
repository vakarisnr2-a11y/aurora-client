package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.module.Category;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.StringSetting;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Client name with optional version, FPS and clock. */
public final class Watermark extends HudModule {
    public enum Style {
        PILL, MINIMAL
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final StringSetting text = add(new StringSetting("Text", "Watermark text", "Aurora", 24));
    private final EnumSetting<Style> style = add(new EnumSetting<>("Style", "Watermark look", Style.PILL));
    private final BooleanSetting version = add(new BooleanSetting("Version", "Show the client version", true));
    private final BooleanSetting fps = add(new BooleanSetting("FPS", "Show frames per second", true));
    private final BooleanSetting clock = add(new BooleanSetting("Clock", "Show the local time", false));
    private final BooleanSetting gradient = add(new BooleanSetting("Gradient Title", "Animate the title with an accent gradient", true));

    public Watermark() {
        super("Watermark", "Customizable client watermark.", Category.VISUALS, 0.0, 0.0, true);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        String title = text.get().isEmpty() ? "Aurora" : text.get();
        StringBuilder info = new StringBuilder();
        if (version.get()) info.append(' ').append(AuroraClient.version());
        if (fps.get()) info.append(" §8|§r ").append(mc.getCurrentFps()).append(" fps");
        if (clock.get()) info.append(" §8|§r ").append(LocalTime.now().format(TIME));
        String rest = info.toString();

        int titleW = tr.getWidth(title);
        int width = titleW + tr.getWidth(rest) + (style.get() == Style.PILL ? 14 : 0);
        int height = tr.fontHeight + (style.get() == Style.PILL ? 8 : 0);
        int x = 0;
        int y = 0;
        if (style.get() == Style.PILL) {
            Render2D.shadow(ctx, 0, 0, width, height, Theme.radius() + 1, 3, 0x70000000);
            Render2D.roundedRect(ctx, 0, 0, width, height, Theme.radius() + 1, Theme.hudBackground());
            Render2D.roundedRect(ctx, 3, 3, 2, height - 6, 1, Theme.accent());
            x = 9;
            y = 4;
        }
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            int color = gradient.get()
                ? ColorUtil.lerp(Theme.accent(), ColorUtil.brighten(Theme.accent(), 0.6f),
                (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 400.0 - i * 0.6)))
                : Theme.accent();
            ctx.drawText(tr, ch, x, y, color, true);
            x += tr.getWidth(ch);
        }
        ctx.drawText(tr, rest, x, y, Theme.SUBTEXT, true);
        setSize(width, height);
    }
}

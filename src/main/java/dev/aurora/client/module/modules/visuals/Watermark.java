package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.hud.HudStyle;
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
    /** One-character strings for ASCII, so the per-letter gradient does not allocate a string per letter per frame. */
    private static final String[] CHARS = new String[128];

    static {
        for (int i = 32; i < 127; i++) CHARS[i] = String.valueOf((char) i);
    }

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

        int pad = HudStyle.padding();
        boolean pill = style.get() == Style.PILL;
        int titleW = tr.getWidth(title);
        int width = titleW + tr.getWidth(rest) + (pill ? pad * 2 + 6 : 0);
        int height = tr.fontHeight + (pill ? pad * 2 : 0);
        int x = 0;
        int y = 0;
        int accent = HudStyle.accent();
        if (pill) {
            HudStyle.background(ctx, 0, 0, width, height);
            Render2D.roundedRect(ctx, pad - 1, 3, 2, height - 6, 1, accent);
            x = pad + 5;
            y = pad;
        }
        boolean shadow = HudStyle.textShadow();
        for (int i = 0; i < title.length(); i++) {
            char ch = title.charAt(i);
            int color = gradient.get()
                ? ColorUtil.lerp(accent, ColorUtil.brighten(accent, 0.6f),
                (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 400.0 - i * 0.6)))
                : accent;
            String glyph = ch < 128 && CHARS[ch] != null ? CHARS[ch] : String.valueOf(ch);
            ctx.drawText(tr, glyph, x, y, color, shadow);
            x += tr.getWidth(glyph);
        }
        ctx.drawText(tr, rest, x, y, HudStyle.sub(), shadow);
        setSize(width, height);
    }
}

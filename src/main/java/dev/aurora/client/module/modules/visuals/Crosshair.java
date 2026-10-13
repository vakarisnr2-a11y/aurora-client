package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.event.events.Render2DEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.PixelPattern;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.StringSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

/** Replaces the vanilla crosshair (cancelled in InGameHudMixin) with a configurable one. */
public final class Crosshair extends Module {
    public enum Style {
        CROSS, T_SHAPE, DOT, CIRCLE, PIXEL
    }

    private final EnumSetting<Style> style = add(new EnumSetting<>("Style", "Crosshair shape", Style.CROSS));
    private final ColorSetting color = add(new ColorSetting("Color", "Crosshair color", 0xFFFFFFFF));
    private final NumberSetting length = add(new NumberSetting("Length", "Length of each arm", 4, 1, 12, 0.5))
        .visibleWhen(() -> style.get() == Style.CROSS || style.get() == Style.T_SHAPE);
    private final NumberSetting gap = add(new NumberSetting("Gap", "Space between the center and the arms", 2, 0, 10, 0.5))
        .visibleWhen(this::notPixel);
    private final NumberSetting thickness = add(new NumberSetting("Thickness", "Line thickness", 1, 0.5, 4, 0.5))
        .visibleWhen(this::notPixel);
    private final BooleanSetting dot = add(new BooleanSetting("Center Dot", "Draw a dot in the middle", false))
        .visibleWhen(() -> style.get() != Style.DOT && notPixel());
    private final StringSetting pixels = add(new StringSetting("Pixels", "Pixel crosshair pattern (edit it with the Crosshair Editor)",
        PixelPattern.DEFAULT, 64)).hide();
    private final NumberSetting pixelSize = add(new NumberSetting("Pixel Size", "Size of one pixel of the pixel crosshair", 1, 0.5, 4, 0.5))
        .visibleWhen(() -> style.get() == Style.PIXEL);
    private final BooleanSetting outline = add(new BooleanSetting("Outline", "Dark outline for visibility on bright blocks", true));
    private final ColorSetting outlineColor = add(new ColorSetting("Outline Color", "Outline color", 0xB0000000))
        .visibleWhen(outline::get);
    private final BooleanSetting dynamic = add(new BooleanSetting("Dynamic", "Gap widens while moving", true)).visibleWhen(this::notPixel);
    private final BooleanSetting attackIndicator = add(new BooleanSetting("Attack Cooldown", "Show the attack cooldown bar under the crosshair", true));
    private final BooleanSetting thirdPerson = add(new BooleanSetting("Third Person", "Also draw the crosshair in third person", false));

    private final Animation spread = new Animation(0, 14);
    private final float[] runs = new float[PixelPattern.maxRuns() * 4];
    private int runCount;
    private String runsFor;

    public Crosshair() {
        super("Crosshair", "Custom crosshair with styles, colors, outline and dynamic spread.", Category.VISUALS);
        on(Render2DEvent.class, this::render);
    }

    private void render(Render2DEvent e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || (!thirdPerson.get() && !mc.options.getPerspective().isFirstPerson())) return;
        DrawContext ctx = e.context;
        float cx = e.width / 2f;
        float cy = e.height / 2f;

        double speed = Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ);
        spread.setTarget(dynamic.get() ? Math.min(4, speed * 14) : 0);
        float g = gap.getFloat() + (float) spread.update();
        float l = length.getFloat();
        float t = thickness.getFloat();
        int c = color.getColor();

        if (outline.get()) drawShape(ctx, cx, cy, g, l, t, 0.5f, outlineColor.getColor());
        drawShape(ctx, cx, cy, g, l, t, 0, c);

        if (attackIndicator.get()) {
            float progress = p.getAttackCooldownProgress(0f);
            if (progress < 1f) {
                float y = style.get() == Style.PIXEL ? cy + (PixelPattern.CENTER + 1) * pixelSize.getFloat() + 3 : cy + g + Math.max(l, t) + 4;
                Render2D.rect(ctx, cx - 8, y, 16, 2, 0x90000000);
                Render2D.rect(ctx, cx - 8, y, 16 * progress, 2, c);
            }
        }
    }

    private void drawShape(DrawContext ctx, float cx, float cy, float g, float l, float t, float grow, int c) {
        float half = t / 2f + grow;
        switch (style.get()) {
            case CROSS, T_SHAPE -> {
                if (style.get() == Style.CROSS) Render2D.rect(ctx, cx - half, cy - g - l - grow, half * 2, l + grow * 2, c);
                Render2D.rect(ctx, cx - half, cy + g - grow, half * 2, l + grow * 2, c);
                Render2D.rect(ctx, cx - g - l - grow, cy - half, l + grow * 2, half * 2, c);
                Render2D.rect(ctx, cx + g - grow, cy - half, l + grow * 2, half * 2, c);
                if (dot.get()) Render2D.rect(ctx, cx - half, cy - half, half * 2, half * 2, c);
            }
            case DOT -> Render2D.circle(ctx, cx, cy, Math.max(1f, t) + grow, c);
            case CIRCLE -> {
                Render2D.ring(ctx, cx, cy, g + 3 + grow, t + grow * 2, c);
                if (dot.get()) Render2D.circle(ctx, cx, cy, Math.max(0.75f, t / 1.5f) + grow, c);
            }
            case PIXEL -> {
                updateRuns();
                float ps = pixelSize.getFloat();
                float origin = -(PixelPattern.CENTER + 0.5f) * ps; // the pattern's centre pixel sits on the screen centre
                Render2D.rects(ctx, cx + origin, cy + origin, ps, grow, runs, runCount, c);
            }
        }
    }

    private boolean notPixel() {
        return style.get() != Style.PIXEL;
    }

    /** Re-merges the pattern into rectangles only when it changed. */
    private void updateRuns() {
        String current = pixels.get();
        if (current.equals(runsFor)) return;
        boolean[] decoded = PixelPattern.decode(current);
        if (decoded == null) decoded = PixelPattern.decode(PixelPattern.DEFAULT);
        runCount = PixelPattern.runs(decoded, runs);
        runsFor = current;
    }

    public StringSetting getPixels() {
        return pixels;
    }

    public void usePixelStyle() {
        style.set(Style.PIXEL);
        setEnabled(true);
    }

    public float getPixelSize() {
        return pixelSize.getFloat();
    }

    /** Outline color, or 0 when the outline is off. */
    public int getOutlineColorValue() {
        return outline.get() ? outlineColor.getColor() : 0;
    }

    public boolean isPixelStyle() {
        return style.get() == Style.PIXEL;
    }

    @Override
    public String getDisplayInfo() {
        return style.getDisplay();
    }

    /** Exposed for the GUI preview. */
    public int getColorValue() {
        return ColorUtil.withAlpha(color.getColor(), 255);
    }
}

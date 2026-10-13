package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.event.events.Render2DEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/** A red glow along the screen edges while your health is at or below a threshold; stronger the lower it gets. */
public final class LowHealthWarning extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Health", "Show the glow at or below this many hearts", 4, 1, 10, 0.5));
    private final ColorSetting color = add(new ColorSetting("Color", "Glow color", 0xFFFF2A2A, false));
    private final NumberSetting intensity = add(new NumberSetting("Intensity", "Maximum strength of the glow", 0.65, 0.1, 1.0, 0.05));
    private final NumberSetting size = add(new NumberSetting("Size", "How far the glow reaches into the screen", 0.16, 0.05, 0.4, 0.01));
    private final BooleanSetting pulse = add(new BooleanSetting("Pulse", "Pulse like a heartbeat", true));

    private final Animation shown = new Animation(0, 6);

    public LowHealthWarning() {
        super("Low Health Warning", "Red screen-edge glow when your health is low.", Category.VISUALS);
        on(Render2DEvent.class, this::render);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    private void render(Render2DEvent e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        float target = p == null || p.isCreative() || p.isSpectator() || p.isDead() ? 0
            : strength(p.getHealth() + p.getAbsorptionAmount(), threshold.getFloat() * 2);
        shown.setTarget(target);
        float s = (float) shown.update();
        if (s < 0.01f) return;
        if (pulse.get()) s *= 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / (180.0 + 220.0 * (1 - s)));
        int edge = ColorUtil.withAlpha(color.get(), Math.round(255 * intensity.getFloat() * s));
        int clear = ColorUtil.withAlpha(color.get(), 0);
        float w = e.width, h = e.height;
        float d = Math.min(w, h) * size.getFloat();
        Render2D.gradientV(e.context, 0, 0, w, d, edge, clear);
        Render2D.gradientV(e.context, 0, h - d, w, d, clear, edge);
        Render2D.gradientH(e.context, 0, 0, d, h, edge, clear);
        Render2D.gradientH(e.context, w - d, 0, d, h, clear, edge);
    }

    /** 0 above the threshold (in health points), rising from 0.35 at the threshold to 1 at zero health. */
    public static float strength(float health, float threshold) {
        if (threshold <= 0 || health > threshold) return 0;
        float missing = 1f - Math.max(0f, health) / threshold;
        return 0.35f + 0.65f * missing;
    }
}

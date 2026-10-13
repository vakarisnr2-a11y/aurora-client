package dev.aurora.client.module.modules.world;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.TntEntity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * Shows the remaining fuse time above primed TNT.
 *
 * <p>The label is depth-tested like the TNT itself, so it is hidden behind blocks (unlike vanilla
 * nametags, which show through walls). Labels farther than vanilla's 64 block nametag range are skipped.</p>
 */
public final class TntTimer extends Module {
    /** Vanilla's default fuse; used as "full" for the green-to-red color. */
    private static final float DEFAULT_FUSE_TICKS = 80f;
    private static final double MAX_DISTANCE_SQ = 64 * 64;

    private final NumberSetting decimals = add(new NumberSetting("Decimals", "Digits after the decimal point", 1, 0, 2, 1));
    private final NumberSetting scale = add(new NumberSetting("Scale", "Label size", 1.0, 0.5, 3, 0.05));
    private final BooleanSetting colorByTime = add(new BooleanSetting("Color By Time", "Fade from green to red as the fuse runs out", true));
    private final BooleanSetting background = add(new BooleanSetting("Background", "Draw the vanilla nametag background", true));

    public TntTimer() {
        super("TNT Timer", "Shows the fuse countdown above primed TNT.", Category.WORLD);
    }

    /** Called from the TNT renderer with the matrices at the entity's feet. */
    public void renderLabel(TntEntity tnt, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers) {
        MinecraftClient mc = MinecraftClient.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        if (dispatcher.getSquaredDistanceToCamera(tnt) > MAX_DISTANCE_SQ) return;

        float ticks = ticksLeft(tnt.getFuse(), tickDelta);
        Text text = Text.literal(format(ticks, decimals.getInt()));
        int color = colorByTime.get() ? ColorUtil.healthColor(ticks / DEFAULT_FUSE_TICKS) : 0xFFFFFFFF;
        int bg = background.get() ? ColorUtil.withAlpha(0, Math.round(mc.options.getTextBackgroundOpacity(0.25f) * 255)) : 0;
        TextRenderer tr = mc.textRenderer;

        matrices.push();
        matrices.translate(0, tnt.getHeight() + 0.5, 0);
        matrices.multiply(dispatcher.getRotation());
        float s = 0.025f * scale.getFloat();
        matrices.scale(s, -s, s);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        tr.draw(text, -tr.getWidth(text) / 2f, 0, color, false, matrix, consumers, TextRenderer.TextLayerType.NORMAL, bg,
            LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();
    }

    /** Fuse ticks left at this frame; the client counts the fuse down once per tick. */
    public static float ticksLeft(int fuse, float tickDelta) {
        return Math.max(0f, fuse - tickDelta);
    }

    /** "2.4s" style label. With no decimals the seconds round up, so "1s" means less than a second left. */
    public static String format(float ticks, int decimals) {
        float seconds = Math.max(0f, ticks) / 20f;
        if (decimals <= 0) return (int) Math.ceil(seconds) + "s";
        return String.format(Locale.ROOT, "%." + decimals + "fs", seconds);
    }
}

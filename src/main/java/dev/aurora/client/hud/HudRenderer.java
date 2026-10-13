package dev.aurora.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws every visible {@link HudModule} at its position and scale. Elements fade and pop in when enabled and fade
 * out when disabled (see {@link HudModule#updateVisibility()}).
 */
public final class HudRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Aurora/HUD");
    /** Elements start at this size while fading in. */
    private static final float POP_FROM = 0.9f;
    private final List<HudModule> elements = new ArrayList<>();

    public HudRenderer(ModuleManager modules) {
        for (Module m : modules.getAll()) {
            if (m instanceof HudModule hud) elements.add(hud);
        }
    }

    public List<HudModule> getElements() {
        return elements;
    }

    public void render(DrawContext ctx, float tickDelta) {
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        for (int i = 0; i < elements.size(); i++) {
            HudModule hud = elements.get(i);
            float visibility = hud.updateVisibility();
            if (visibility > 0.01f) renderElement(ctx, hud, tickDelta, w, h, false, visibility);
        }
    }

    public static void renderElement(DrawContext ctx, HudModule hud, float tickDelta, int screenW, int screenH, boolean editor) {
        renderElement(ctx, hud, tickDelta, screenW, screenH, editor, 1f);
    }

    public static void renderElement(DrawContext ctx, HudModule hud, float tickDelta, int screenW, int screenH, boolean editor,
                                     float visibility) {
        boolean fading = visibility < 0.999f;
        ctx.getMatrices().push();
        try {
            ctx.getMatrices().translate(Math.round(hud.getX(screenW)), Math.round(hud.getY(screenH)), 0);
            if (fading) {
                // Pop from slightly smaller around the centre, fading everything (shapes, text, items) via the shader color.
                float pop = POP_FROM + (1 - POP_FROM) * visibility;
                float cx = hud.getScaledWidth() / 2f;
                float cy = hud.getScaledHeight() / 2f;
                ctx.getMatrices().translate(cx, cy, 0);
                ctx.getMatrices().scale(pop, pop, 1);
                ctx.getMatrices().translate(-cx, -cy, 0);
                ctx.draw();
                RenderSystem.setShaderColor(1, 1, 1, visibility);
            }
            ctx.getMatrices().scale(hud.getScale(), hud.getScale(), 1);
            hud.render(ctx, tickDelta, editor);
        } catch (Throwable t) {
            LOGGER.error("HUD element {} failed to render", hud.getName(), t);
            hud.setEnabled(false, false); // a broken element is disabled instead of spamming every frame
            hud.hideImmediately();
        } finally {
            ctx.getMatrices().pop();
            if (fading) {
                ctx.draw();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        }
    }
}

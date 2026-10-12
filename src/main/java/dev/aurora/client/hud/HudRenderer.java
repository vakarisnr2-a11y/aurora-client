package dev.aurora.client.hud;

import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/** Draws every enabled {@link HudModule} at its configured position and scale. */
public final class HudRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Aurora/HUD");
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
            if (hud.isEnabled()) renderElement(ctx, hud, tickDelta, w, h, false);
        }
    }

    public static void renderElement(DrawContext ctx, HudModule hud, float tickDelta, int screenW, int screenH, boolean editor) {
        ctx.getMatrices().push();
        try {
            ctx.getMatrices().translate(hud.getX(screenW), hud.getY(screenH), 0);
            ctx.getMatrices().scale(hud.getScale(), hud.getScale(), 1);
            hud.render(ctx, tickDelta, editor);
        } catch (Throwable t) {
            LOGGER.error("HUD element {} failed to render", hud.getName(), t);
            hud.setEnabled(false, false); // a broken element is disabled instead of spamming every frame
        } finally {
            ctx.getMatrices().pop();
        }
    }
}

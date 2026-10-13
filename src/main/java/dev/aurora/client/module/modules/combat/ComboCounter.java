package dev.aurora.client.module.modules.combat;

import dev.aurora.client.hud.HudLine;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.CachedText;
import dev.aurora.client.util.CombatTracker;
import net.minecraft.client.gui.DrawContext;

/** Counts consecutive hits you land (confirmed by the server) without taking damage. */
public final class ComboCounter extends HudModule {
    private final BooleanSetting hideZero = add(new BooleanSetting("Hide At Zero", "Hide the counter when there is no combo", false));
    private final Animation pop = new Animation(1, 10);
    private final HudLine line = new HudLine(true);
    private final CachedText text = new CachedText();
    private int lastCombo;

    public ComboCounter() {
        super("Combo Counter", "Counts consecutive landed hits without being hit back.", Category.COMBAT, 0.5, 0.56, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        int combo = CombatTracker.INSTANCE.getCombo();
        if (combo > lastCombo && HudStyle.animations()) pop.snap(1.3);
        lastCombo = combo;
        pop.setTarget(1);
        float s = (float) pop.update();
        if (combo == 0 && hideZero.get() && !editor) {
            setSize(40, HudStyle.lineHeight());
            return;
        }
        int h = HudStyle.lineHeight();
        float w = line.width();
        ctx.getMatrices().push();
        // Pop around the centre of the pill.
        ctx.getMatrices().translate(w / 2f, h / 2f, 0);
        ctx.getMatrices().scale(s, s, 1);
        ctx.getMatrices().translate(-w / 2f, -h / 2f, 0);
        int width = line.draw(ctx, "Combo", text.of(combo), combo >= 3 ? HudStyle.accent() : HudStyle.text(), true);
        ctx.getMatrices().pop();
        setSize(width, h);
    }
}

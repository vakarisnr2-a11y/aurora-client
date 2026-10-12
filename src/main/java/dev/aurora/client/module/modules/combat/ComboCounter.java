package dev.aurora.client.module.modules.combat;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.CombatTracker;
import net.minecraft.client.gui.DrawContext;

/** Counts consecutive hits you land (confirmed by the server) without taking damage. */
public final class ComboCounter extends HudModule {
    private final BooleanSetting hideZero = add(new BooleanSetting("Hide At Zero", "Hide the counter when there is no combo", false));
    private final Animation pop = new Animation(1, 10);
    private int lastCombo;

    public ComboCounter() {
        super("Combo Counter", "Counts consecutive landed hits without being hit back.", Category.COMBAT, 0.5, 0.56, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        int combo = CombatTracker.INSTANCE.getCombo();
        if (combo > lastCombo) pop.snap(1.35);
        lastCombo = combo;
        pop.setTarget(1);
        float s = (float) pop.update();
        if (combo == 0 && hideZero.get() && !editor) {
            setSize(40, 12);
            return;
        }
        ctx.getMatrices().push();
        ctx.getMatrices().scale(s, s, 1);
        int w = HudStyle.drawLabelValue(ctx, "Combo", String.valueOf(combo), combo >= 3 ? Theme.accent() : Theme.TEXT, true);
        ctx.getMatrices().pop();
        setSize(w, HudStyle.lineHeight());
    }
}

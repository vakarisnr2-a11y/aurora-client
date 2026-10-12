package dev.aurora.client.module.modules.combat;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.CombatTracker;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Distance from your eyes to the target's hitbox on your last attack. */
public final class ReachDisplay extends HudModule {
    private final NumberSetting decimals = add(new NumberSetting("Decimals", "Decimal places", 2, 0, 3, 1));
    private final NumberSetting fade = add(new NumberSetting("Reset After", "Seconds before the value resets", 4, 1, 15, 0.5));

    public ReachDisplay() {
        super("Reach Display", "Shows the distance of your last attack.", Category.COMBAT, 0.5, 0.52, false);
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        CombatTracker tracker = CombatTracker.INSTANCE;
        boolean recent = System.currentTimeMillis() - tracker.getLastAttackMs() < fade.get() * 1000;
        String value = recent ? String.format(Locale.ROOT, "%." + decimals.getInt() + "f m", tracker.getLastReach()) : "-";
        setSize(HudStyle.drawLabelValue(ctx, "Reach", value, Theme.TEXT, true), HudStyle.lineHeight());
    }
}

package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Improves vanilla nametags: larger scale, health readout and your own tag in third person.
 * Visibility rules (distance, sneaking, line of sight for names) stay vanilla.
 */
public final class Nametags extends Module {
    public enum HealthMode {
        HEARTS, NUMBER, PERCENT
    }

    private final NumberSetting scale = add(new NumberSetting("Scale", "Nametag size multiplier", 1.25, 0.5, 3, 0.05));
    private final BooleanSetting health = add(new BooleanSetting("Health", "Append health to nametags", true));
    private final EnumSetting<HealthMode> healthMode = add(new EnumSetting<>("Health Format", "How health is shown", HealthMode.HEARTS))
        .visibleWhen(health::get);
    private final BooleanSetting playersOnly = add(new BooleanSetting("Players Only", "Only add health to player nametags", true))
        .visibleWhen(health::get);
    private final BooleanSetting showOwn = add(new BooleanSetting("Show Own", "Show your own nametag in third person", false));

    public Nametags() {
        super("Nametags", "Bigger nametags with health and an optional own nametag.", Category.VISUALS);
    }

    public Text decorate(Entity entity, Text text) {
        if (!health.get() || !(entity instanceof LivingEntity living)) return text;
        if (playersOnly.get() && !(entity instanceof PlayerEntity)) return text;
        float hp = living.getHealth() + living.getAbsorptionAmount();
        float max = Math.max(1f, living.getMaxHealth());
        String value = switch (healthMode.get()) {
            case HEARTS -> String.format(Locale.ROOT, "%.1f❤", hp / 2f);
            case NUMBER -> String.format(Locale.ROOT, "%.1f", hp);
            case PERCENT -> Math.round(hp / max * 100) + "%";
        };
        int color = ColorUtil.healthColor(hp / max) & 0xFFFFFF;
        return text.copy().append(Text.literal(" " + value).styled(s -> s.withColor(color)));
    }

    public float getScale() {
        return scale.getFloat();
    }

    public boolean showOwn() {
        return showOwn.get();
    }
}

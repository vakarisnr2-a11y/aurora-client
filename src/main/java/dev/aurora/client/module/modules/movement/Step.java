package dev.aurora.client.module.modules.movement;

import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;

import java.util.Locale;

/**
 * Raises the player's step-height attribute (client side). Intended for singleplayer or servers
 * that allow it; servers that don't will correct your position. Restored to 0.6 on disable.
 */
public final class Step extends Module {
    private static final double VANILLA = 0.6;
    private final NumberSetting height = add(new NumberSetting("Height", "Maximum step height in blocks", 1.0, 0.6, 2.5, 0.1));

    public Step() {
        super("Step", "Walk up full blocks without jumping (where permitted).", Category.MOVEMENT);
        on(TickEvent.Pre.class, e -> apply(height.get()));
    }

    @Override
    protected void onDisable() {
        apply(VANILLA);
    }

    private static void apply(double value) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;
        EntityAttributeInstance attribute = p.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (attribute != null && attribute.getBaseValue() != value) attribute.setBaseValue(value);
    }

    @Override
    public String getDisplayInfo() {
        return String.format(Locale.ROOT, "%.1f", height.get());
    }
}

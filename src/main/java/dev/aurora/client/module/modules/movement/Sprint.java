package dev.aurora.client.module.modules.movement;

import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Sprint assistance: starts sprinting automatically when you walk forward, under exactly the
 * conditions vanilla allows sprinting (enough food, not sneaking/using items/blind, no wall ahead).
 */
public final class Sprint extends Module {
    private final BooleanSetting inWater = add(new BooleanSetting("Swim Sprint", "Also start sprint-swimming in water", true));

    public Sprint() {
        super("Sprint", "Automatically sprints while moving forward, following normal sprint rules.", Category.MOVEMENT);
        on(TickEvent.Pre.class, e -> tick());
    }

    private void tick() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null || p.isSprinting()) return;
        boolean enoughFood = p.hasVehicle() || p.getHungerManager().getFoodLevel() > 6 || p.getAbilities().allowFlying;
        boolean blocked = p.isSneaking() || p.isUsingItem() || p.horizontalCollision || p.isFallFlying()
            || p.hasStatusEffect(StatusEffects.BLINDNESS);
        boolean water = p.isTouchingWater();
        if (water && (!inWater.get() || !p.isSubmergedInWater())) return;
        if (p.input.hasForwardMovement() && enoughFood && !blocked) p.setSprinting(true);
    }
}

package dev.aurora.client.module.modules.movement;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;

/** Stops you from walking off block edges, using vanilla's sneak-edge logic (PlayerEntityMixin). */
public final class SafeWalk extends Module {
    private final BooleanSetting blocksOnly = add(new BooleanSetting("Blocks Only", "Only active while holding a block", false));

    public SafeWalk() {
        super("Safe Walk", "Prevents walking off edges as if sneaking, without slowing down.", Category.MOVEMENT);
    }

    public boolean shouldClip() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null || p.getAbilities().flying) return false;
        return !blocksOnly.get() || p.getMainHandStack().getItem() instanceof BlockItem
            || p.getOffHandStack().getItem() instanceof BlockItem;
    }
}

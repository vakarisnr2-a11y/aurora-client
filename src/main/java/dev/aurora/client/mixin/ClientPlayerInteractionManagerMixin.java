package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.player.ItemLock;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    /** Every inventory click goes through here before it is sent; a THROW (Q over a slot) of a locked slot is dropped. */
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void aurora$itemLock(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (actionType != SlotActionType.THROW) return;
        ItemLock lock = AuroraClient.module(ItemLock.class);
        if (lock == null || !lock.isEnabled() || player == null) return;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null || slotId < 0 || slotId >= handler.slots.size()) return;
        if (lock.blockThrow(handler.getSlot(slotId), player)) ci.cancel();
    }
}

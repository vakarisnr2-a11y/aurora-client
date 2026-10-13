package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.player.ItemLock;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    /** The drop key (Q / Ctrl+Q) in game. Returning false also skips the arm swing vanilla does after a drop. */
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void aurora$itemLock(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ItemLock lock = AuroraClient.module(ItemLock.class);
        if (lock != null && lock.isEnabled() && lock.blockDrop(((ClientPlayerEntity) (Object) this).getInventory())) {
            cir.setReturnValue(false);
        }
    }
}

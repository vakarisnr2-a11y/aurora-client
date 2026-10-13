package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.combat.TotemPops;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    /**
     * Status 35 is a totem pop. The handler first runs on the network thread, where forceMainThread throws to
     * reschedule it, so TAIL (the method's single return, checked with javap against 1.21.1) only runs on the client thread.
     */
    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void aurora$totemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;
        TotemPops totemPops = AuroraClient.module(TotemPops.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (totemPops == null || !totemPops.isEnabled() || mc.world == null) return;
        Entity entity = packet.getEntity(mc.world);
        if (entity != null) totemPops.onTotemPop(entity);
    }
}

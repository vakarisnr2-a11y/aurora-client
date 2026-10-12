package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.event.events.MouseButtonEvent;
import dev.aurora.client.event.events.MouseScrollEvent;
import dev.aurora.client.util.LookController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void aurora$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (AuroraClient.get() == null || window != mc.getWindow().getHandle()) return;
        MouseButtonEvent event = MouseButtonEvent.INSTANCE.set(button, action, mods, mc.currentScreen != null);
        if (AuroraClient.bus().post(event).isCancelled()) ci.cancel();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void aurora$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (AuroraClient.get() == null || window != mc.getWindow().getHandle() || mc.currentScreen != null) return;
        if (AuroraClient.bus().post(MouseScrollEvent.INSTANCE.set(horizontal, vertical)).isCancelled()) ci.cancel();
    }

    /** All per-frame look rotation passes through here (freelook, zoom sensitivity, smoothing). */
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void aurora$changeLookDirection(ClientPlayerEntity player, double dx, double dy) {
        LookController.changeLookDirection(player, dx, dy);
    }
}

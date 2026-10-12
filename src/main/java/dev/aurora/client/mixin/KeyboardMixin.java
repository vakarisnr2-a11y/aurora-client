package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.event.events.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void aurora$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (AuroraClient.get() == null || window != mc.getWindow().getHandle()) return;
        KeyEvent event = KeyEvent.INSTANCE.set(key, scancode, action, modifiers, mc.currentScreen != null);
        if (AuroraClient.bus().post(event).isCancelled()) ci.cancel();
    }
}

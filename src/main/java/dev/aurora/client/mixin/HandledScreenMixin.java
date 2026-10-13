package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.player.ItemLock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    /** After each slot is drawn (in screen-relative coordinates): Item Lock's padlock. */
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void aurora$lockedSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        ItemLock lock = AuroraClient.module(ItemLock.class);
        if (lock != null && lock.isEnabled()) lock.renderSlot(context, slot, MinecraftClient.getInstance().player);
    }
}

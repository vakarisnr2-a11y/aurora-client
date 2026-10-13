package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.utility.TabPing;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    /**
     * Vanilla reserves 13 px per column for the latency icon (the only 13 in render, checked with javap
     * against 1.21.1); widen the columns so the ping number fits next to the name and objective.
     */
    @ModifyConstant(method = "render", constant = @Constant(intValue = 13))
    private int aurora$widenForPing(int original) {
        TabPing tabPing = AuroraClient.module(TabPing.class);
        return tabPing != null && tabPing.isEnabled() ? original + tabPing.extraColumnWidth() : original;
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void aurora$pingNumber(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        TabPing tabPing = AuroraClient.module(TabPing.class);
        if (tabPing != null && tabPing.isEnabled()) {
            tabPing.render(context, width, x, y, entry);
            ci.cancel();
        }
    }
}

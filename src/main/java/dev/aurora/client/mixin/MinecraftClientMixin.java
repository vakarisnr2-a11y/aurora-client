package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.menu.AuroraTitleScreen;
import dev.aurora.client.module.modules.utility.UnfocusedFps;
import dev.aurora.client.module.modules.visuals.AuroraMenu;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    /** The frame limit passed to RenderSystem.limitDisplayFPS every frame (both returns, checked with javap). */
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void aurora$unfocusedFps(CallbackInfoReturnable<Integer> cir) {
        UnfocusedFps module = AuroraClient.module(UnfocusedFps.class);
        if (module != null && module.isEnabled()) cir.setReturnValue(module.limit(cir.getReturnValue()));
    }

    /** Swaps vanilla's title screen (exactly TitleScreen, not subclasses) for the Aurora menu. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen aurora$mainMenu(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class) {
            AuroraMenu menu = AuroraClient.module(AuroraMenu.class);
            if (menu != null && menu.replaceTitleScreen()) return new AuroraTitleScreen();
        }
        return screen;
    }
}

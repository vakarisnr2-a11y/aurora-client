package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.menu.AuroraBackground;
import dev.aurora.client.module.modules.visuals.AuroraMenu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The terrain loading screen (joining a server, changing dimension) draws its own background; the Aurora backdrop instead. */
@Mixin(DownloadingTerrainScreen.class)
public abstract class LoadingScreensMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void aurora$loadingBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AuroraMenu menu = AuroraClient.module(AuroraMenu.class);
        if (menu != null && menu.themedLoading()) {
            Screen screen = (Screen) (Object) this;
            AuroraBackground.render(context, screen.width, screen.height);
            ci.cancel();
        }
    }
}

package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.menu.AuroraBackground;
import dev.aurora.client.module.modules.visuals.AuroraMenu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow
    public int width;
    @Shadow
    public int height;

    /**
     * Every menu without a world, including world loading, connecting and progress screens, draws the panorama here
     * (then vanilla blurs and darkens it): the Aurora backdrop instead.
     */
    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void aurora$menuBackground(DrawContext context, float delta, CallbackInfo ci) {
        AuroraMenu menu = AuroraClient.module(AuroraMenu.class);
        if (menu != null && menu.themedBackground((Screen) (Object) this)) {
            AuroraBackground.render(context, width, height);
            ci.cancel();
        }
    }
}

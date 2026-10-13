package dev.aurora.client.mixin;

import dev.aurora.client.pack.BedwarsPack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The player inventory draws its own "Crafting" title: light on the bundled pack's transparent inventory. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @ModifyConstant(method = "drawForeground", constant = @Constant(intValue = BedwarsPack.VANILLA_TITLE))
    private int aurora$titleColor(int color) {
        return BedwarsPack.titleColor((Screen) (Object) this, color);
    }
}

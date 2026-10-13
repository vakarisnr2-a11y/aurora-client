package dev.aurora.client.mixin;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.utility.BetterTooltips;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
    /** The only Text.translatable in getName: "enchantment.level." + level (checked with javap against 1.21.1). */
    @Redirect(method = "getName", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;)Lnet/minecraft/text/MutableText;"))
    private static MutableText aurora$levelNumber(String key) {
        BetterTooltips tooltips = AuroraClient.module(BetterTooltips.class);
        if (tooltips != null && tooltips.numberedLevels()) {
            String number = BetterTooltips.levelNumber(key);
            if (number != null) return Text.literal(number);
        }
        return Text.translatable(key);
    }
}

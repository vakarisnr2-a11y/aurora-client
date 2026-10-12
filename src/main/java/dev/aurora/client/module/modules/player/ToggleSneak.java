package dev.aurora.client.module.modules.player;

import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public final class ToggleSneak extends ToggleKeyModule {
    private final BooleanSetting notWhileFlying = add(new BooleanSetting("Pause While Flying",
        "Don't hold sneak while flying (it would make you descend)", true));

    public ToggleSneak() {
        super("Toggle Sneak", "Press your sneak key once to keep sneaking; press again to stop.", 0.0, 0.54);
    }

    @Override
    protected KeyBinding binding() {
        return MinecraftClient.getInstance().options.sneakKey;
    }

    @Override
    protected boolean isActiveNow() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return !(notWhileFlying.get() && mc.player != null && mc.player.getAbilities().flying);
    }

    @Override
    protected String label() {
        return "Sneaking";
    }
}

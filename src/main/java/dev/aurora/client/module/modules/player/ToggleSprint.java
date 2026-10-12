package dev.aurora.client.module.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public final class ToggleSprint extends ToggleKeyModule {
    public ToggleSprint() {
        super("Toggle Sprint", "Press your sprint key once to keep sprinting; press again to stop.", 0.0, 0.5);
    }

    @Override
    protected KeyBinding binding() {
        return MinecraftClient.getInstance().options.sprintKey;
    }

    @Override
    protected String label() {
        return "Sprinting";
    }
}

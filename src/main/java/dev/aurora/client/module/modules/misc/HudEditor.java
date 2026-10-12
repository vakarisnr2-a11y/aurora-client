package dev.aurora.client.module.modules.misc;

import dev.aurora.client.gui.HudEditorScreen;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;

/** Opens the HUD editor where elements can be dragged, snapped and scaled. */
public final class HudEditor extends Module {
    public HudEditor() {
        super("HUD Editor", "Drag, snap and scale HUD elements. Scroll over an element to resize it.", Category.MISC);
    }

    @Override
    protected void onEnable() {
        net.minecraft.client.MinecraftClient mc = mc();
        if (mc != null) mc.send(() -> mc.setScreen(new HudEditorScreen(mc.currentScreen))); // next frame, see ClickGui
        setEnabled(false, false);
    }

    @Override
    public boolean savesEnabledState() {
        return false;
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }
}

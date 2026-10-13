package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.gui.CrosshairEditorScreen;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;

/** Opens the pixel crosshair editor (the pattern is used by the Crosshair module's Pixel style). */
public final class CrosshairEditor extends Module {
    public CrosshairEditor() {
        super("Crosshair Editor", "Paint your own pixel crosshair. Used by Crosshair with Style: Pixel.", Category.VISUALS);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    protected void onEnable() {
        net.minecraft.client.MinecraftClient mc = mc();
        if (mc != null) mc.send(() -> mc.setScreen(new CrosshairEditorScreen(mc.currentScreen))); // next frame, see ClickGui
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

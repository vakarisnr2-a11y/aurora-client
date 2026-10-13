package dev.aurora.client.module.modules.misc;

import dev.aurora.client.gui.HudEditorScreen;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;

/** Opens the HUD editor where elements can be dragged, snapped, anchored and scaled. Also stores the editor options. */
public final class HudEditor extends Module {
    private final BooleanSetting snapElements = add(new BooleanSetting("Snap To Elements",
        "Snap to the edges and centres of other elements (hold Shift to move freely)", true));
    private final BooleanSetting snapGrid = add(new BooleanSetting("Snap To Grid", "Snap to the grid when nothing else is close", false));
    private final NumberSetting gridSize = add(new NumberSetting("Grid Size", "Grid spacing in GUI pixels", 8, 4, 32, 1));
    private final BooleanSetting showGrid = add(new BooleanSetting("Show Grid", "Draw the grid in the editor", true));

    public HudEditor() {
        super("HUD Editor", "Drag, snap, anchor and scale HUD elements, and save or load layouts. Scroll over an element to resize it.",
            Category.MISC);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
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

    public BooleanSetting snapElements() {
        return snapElements;
    }

    public BooleanSetting snapGrid() {
        return snapGrid;
    }

    public int gridSize() {
        return gridSize.getInt();
    }

    public boolean showGrid() {
        return showGrid.get();
    }
}

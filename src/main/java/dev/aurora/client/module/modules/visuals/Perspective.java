package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

/** Third-person camera with adjustable distance and optional block clipping (CameraMixin). */
public final class Perspective extends Module {
    public enum View {
        BACK, FRONT
    }

    private final EnumSetting<View> view = add(new EnumSetting<>("View", "Which side the camera looks from", View.BACK));
    private final NumberSetting distance = add(new NumberSetting("Distance", "Camera distance in blocks", 4, 1, 20, 0.5));
    private final BooleanSetting noClip = add(new BooleanSetting("No Clip", "Let the camera pass through blocks instead of zooming in", false));

    private net.minecraft.client.option.Perspective previous;

    public Perspective() {
        super("Perspective", "Third-person camera with adjustable distance.", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = mc();
        if (mc == null || mc.options == null) return;
        previous = mc.options.getPerspective();
        applyView();
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = mc();
        if (previous != null && mc != null && mc.options != null) mc.options.setPerspective(previous);
        previous = null;
    }

    @Override
    protected void onSettingChanged(Setting<?> setting) {
        if (setting == view && isEnabled()) applyView();
    }

    private void applyView() {
        MinecraftClient mc = mc();
        if (mc == null || mc.options == null) return;
        mc.options.setPerspective(view.get() == View.BACK
            ? net.minecraft.client.option.Perspective.THIRD_PERSON_BACK
            : net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT);
    }

    public float getDistance() {
        return distance.getFloat();
    }

    public boolean isNoClip() {
        return noClip.get();
    }

    @Override
    public String getDisplayInfo() {
        return view.getDisplay();
    }
}

package dev.aurora.client.module.modules.movement;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.KeybindSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Look around without turning your player (hold Left Alt). Mouse deltas are captured in
 * LookController and the camera rotation is replaced in CameraMixin.
 */
public final class Freelook extends Module {
    private final BooleanSetting thirdPerson = add(new BooleanSetting("Third Person", "Switch to third person while freelooking", true));
    private final BooleanSetting invertPitch = add(new BooleanSetting("Invert Pitch", "Invert vertical mouse movement", false));

    private float yaw;
    private float pitch;
    private net.minecraft.client.option.Perspective previous;

    public Freelook() {
        super("Freelook", "Hold to look around freely without changing where you walk.", Category.MOVEMENT,
            GLFW.GLFW_KEY_LEFT_ALT, false);
    }

    @Override
    protected KeybindSetting.Mode defaultBindMode() {
        return KeybindSetting.Mode.HOLD;
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = mc();
        if (mc == null || mc.player == null) {
            yaw = 0;
            pitch = 0;
        } else {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
        }
        if (mc != null && mc.options != null && thirdPerson.get()) {
            previous = mc.options.getPerspective();
            mc.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = mc();
        if (previous != null && mc != null && mc.options != null) mc.options.setPerspective(previous);
        previous = null;
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }

    /** Same 0.15 degrees-per-unit scaling as Entity#changeLookDirection. */
    public void addRotation(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) (invertPitch.get() ? -dy : dy) * 0.15f, -90f, 90f);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}

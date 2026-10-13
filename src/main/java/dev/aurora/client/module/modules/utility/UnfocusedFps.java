package dev.aurora.client.module.modules.utility;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Caps the frame rate while the game window is not focused or is minimized, to save power and keep other programs
 * smooth. Applied in MinecraftClientMixin on top of vanilla's own limit (the lower one wins).
 */
public final class UnfocusedFps extends Module {
    private final NumberSetting unfocused = add(new NumberSetting("Unfocused FPS", "Frame limit while another window has focus", 15, 1, 60, 1));
    private final NumberSetting minimized = add(new NumberSetting("Minimized FPS", "Frame limit while the window is minimized", 5, 1, 30, 1));

    public UnfocusedFps() {
        super("Unfocused FPS", "Lowers the frame rate while the game window is in the background or minimized.", Category.UTILITY);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }

    /** Called every frame with vanilla's limit. */
    public int limit(int vanilla) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean focused = mc.isWindowFocused();
        boolean iconified = mc.getWindow() != null
            && GLFW.glfwGetWindowAttrib(mc.getWindow().getHandle(), GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
        return limit(vanilla, focused, iconified, unfocused.getInt(), minimized.getInt());
    }

    public static int limit(int vanilla, boolean focused, boolean iconified, int unfocusedFps, int minimizedFps) {
        if (iconified) return Math.min(vanilla, minimizedFps);
        if (!focused) return Math.min(vanilla, unfocusedFps);
        return vanilla;
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(unfocused.getInt());
    }
}

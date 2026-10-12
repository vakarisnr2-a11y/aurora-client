package dev.aurora.client.module.modules.player;

import dev.aurora.client.event.events.KeyEvent;
import dev.aurora.client.event.events.MouseButtonEvent;
import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Shared logic for Toggle Sprint / Toggle Sneak: pressing the vanilla key flips a latched state
 * that keeps the key held. When disabled the key is restored to its physical state, so it can
 * never get stuck down.
 */
public abstract class ToggleKeyModule extends HudModule {
    private final BooleanSetting indicator = add(new BooleanSetting("Indicator", "Show the toggle state on screen", true));
    private boolean latched;

    protected ToggleKeyModule(String name, String description, double x, double y) {
        super(name, description, Category.PLAYER, x, y, false);
        on(KeyEvent.class, e -> {
            if (!e.inScreen && e.action == GLFW.GLFW_PRESS && binding().matchesKey(e.key, e.scancode)) latched = !latched;
        });
        on(MouseButtonEvent.class, e -> {
            if (!e.inScreen && e.action == GLFW.GLFW_PRESS && binding().matchesMouse(e.button)) latched = !latched;
        });
        on(TickEvent.Pre.class, e -> {
            if (latched && isActiveNow()) binding().setPressed(true);
            else if (!isPhysicallyPressed(binding())) binding().setPressed(false);
        });
    }

    protected abstract KeyBinding binding();

    protected abstract String label();

    /** Extra conditions (e.g. not while flying). */
    protected boolean isActiveNow() {
        return true;
    }

    @Override
    protected void onEnable() {
        latched = true;
    }

    @Override
    protected void onDisable() {
        latched = false;
        if (mc() != null && mc().options != null) binding().setPressed(isPhysicallyPressed(binding()));
    }

    public boolean isLatched() {
        return latched;
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        if (!indicator.get() && !editor) {
            setSize(1, 1);
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        String state = latched ? "[" + label() + " (Toggled)]" : binding().isPressed() ? "[" + label() + " (Held)]" : "";
        if (state.isEmpty() && editor) state = "[" + label() + "]";
        ctx.drawText(mc.textRenderer, state, 0, 0, latched ? Theme.accent() : Theme.SUBTEXT, true);
        setSize(Math.max(10, mc.textRenderer.getWidth(state)), mc.textRenderer.fontHeight);
    }

    @Override
    public String getDisplayInfo() {
        return latched ? "On" : "Off";
    }

    static boolean isPhysicallyPressed(KeyBinding binding) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return false;
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        long handle = mc.getWindow().getHandle();
        if (key.getCode() == InputUtil.UNKNOWN_KEY.getCode()) return false;
        if (key.getCategory() == InputUtil.Type.MOUSE) return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        return InputUtil.isKeyPressed(handle, key.getCode());
    }
}

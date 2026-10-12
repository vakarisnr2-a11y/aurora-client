package dev.aurora.client.keybind;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.event.EventPriority;
import dev.aurora.client.event.events.KeyEvent;
import dev.aurora.client.event.events.MouseButtonEvent;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.setting.KeybindSetting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Dispatches key and mouse-button presses to module keybinds.
 *
 * <p>Presses only count while no screen is open (so typing in chat never toggles modules).
 * Releases are always processed so a HOLD-mode module never stays stuck on when a screen
 * opens while its key is held.</p>
 */
public final class KeybindManager {
    private final ModuleManager modules;

    public KeybindManager(ModuleManager modules, EventBus bus) {
        this.modules = modules;
        bus.subscribe(this, KeyEvent.class, this::onKey, EventPriority.HIGH);
        bus.subscribe(this, MouseButtonEvent.class, this::onMouse, EventPriority.HIGH);
    }

    private void onKey(KeyEvent e) {
        if (e.action == GLFW.GLFW_REPEAT || e.key == GLFW.GLFW_KEY_UNKNOWN) return;
        boolean pressed = e.action == GLFW.GLFW_PRESS;
        if (pressed && e.inScreen) return;
        List<Module> all = modules.getAll();
        for (int i = 0; i < all.size(); i++) {
            Module m = all.get(i);
            if (m.getKeybind().matchesKey(e.key)) handle(m, pressed);
        }
    }

    private void onMouse(MouseButtonEvent e) {
        boolean pressed = e.action == GLFW.GLFW_PRESS;
        if (pressed && e.inScreen) return;
        List<Module> all = modules.getAll();
        for (int i = 0; i < all.size(); i++) {
            Module m = all.get(i);
            if (m.getKeybind().matchesMouse(e.button)) handle(m, pressed);
        }
    }

    private static void handle(Module module, boolean pressed) {
        if (module.getKeybind().getMode() == KeybindSetting.Mode.HOLD) {
            module.setEnabled(pressed);
        } else if (pressed) {
            module.toggle();
        }
    }
}

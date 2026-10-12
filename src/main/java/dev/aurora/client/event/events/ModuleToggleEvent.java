package dev.aurora.client.event.events;

import dev.aurora.client.event.Event;
import dev.aurora.client.module.Module;

/** Fired after a module was enabled or disabled by the user (not during config loading). */
public final class ModuleToggleEvent extends Event {
    public static final ModuleToggleEvent INSTANCE = new ModuleToggleEvent();

    public Module module;
    public boolean enabled;

    public ModuleToggleEvent set(Module module, boolean enabled) {
        resetCancelled();
        this.module = module;
        this.enabled = enabled;
        return this;
    }
}

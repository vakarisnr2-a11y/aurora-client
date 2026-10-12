package dev.aurora.client.module;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.setting.Setting;

/** Callbacks from a module to whatever manages it (normally the {@link ModuleManager}). */
public interface ModuleHost {
    ModuleHost NONE = new ModuleHost() {
        @Override
        public EventBus bus() {
            return null;
        }

        @Override
        public void onToggled(Module module, boolean enabled, boolean userAction) {
        }

        @Override
        public void onSettingChanged(Module module, Setting<?> setting) {
        }
    };

    /** May be null when modules are used without a bus (unit tests). */
    EventBus bus();

    void onToggled(Module module, boolean enabled, boolean userAction);

    void onSettingChanged(Module module, Setting<?> setting);
}

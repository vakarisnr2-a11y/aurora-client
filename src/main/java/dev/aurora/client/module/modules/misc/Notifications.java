package dev.aurora.client.module.modules.misc;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.NumberSetting;

/** Settings for toast notifications. Warnings and errors are always shown. */
public final class Notifications extends Module {
    public enum Position {
        BOTTOM_RIGHT(true, true), TOP_RIGHT(true, false), BOTTOM_LEFT(false, true), TOP_LEFT(false, false);

        public final boolean right;
        public final boolean bottom;

        Position(boolean right, boolean bottom) {
            this.right = right;
            this.bottom = bottom;
        }
    }

    private final NumberSetting duration = add(new NumberSetting("Duration", "Seconds a notification stays visible", 2.5, 1, 10, 0.5));
    private final EnumSetting<Position> position = add(new EnumSetting<>("Position", "Screen corner for notifications", Position.BOTTOM_RIGHT));
    private final NumberSetting maxVisible = add(new NumberSetting("Max Visible", "Maximum notifications on screen", 5, 1, 10, 1));
    private final BooleanSetting toggles = add(new BooleanSetting("Module Toggles", "Notify when modules are enabled or disabled", true));

    public Notifications() {
        super("Notifications", "Animated toasts for module toggles and client messages.", Category.MISC, KeybindSetting.NONE, true);
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }

    public long durationMs() {
        return (long) (duration.get() * 1000);
    }

    public Position position() {
        return position.get();
    }

    public int maxVisible() {
        return maxVisible.getInt();
    }

    public boolean showToggles() {
        return isEnabled() && toggles.get();
    }
}

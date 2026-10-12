package dev.aurora.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A key or mouse-button binding.
 *
 * <p>Keyboard keys are stored as GLFW key codes; mouse buttons are stored as
 * {@link #MOUSE_OFFSET} + GLFW button index so both fit in one int.</p>
 */
public final class KeybindSetting extends Setting<Integer> {
    public static final int NONE = -1;
    public static final int MOUSE_OFFSET = 1000;
    private static final int GLFW_KEY_LAST = 348;
    private static final int GLFW_MOUSE_BUTTON_LAST = 7;

    public enum Mode {
        TOGGLE, HOLD
    }

    private final Mode defaultMode;
    private Mode mode;

    public KeybindSetting(String name, String description, int defaultKey) {
        this(name, description, defaultKey, Mode.TOGGLE);
    }

    public KeybindSetting(String name, String description, int defaultKey, Mode defaultMode) {
        super(name, description, defaultKey);
        this.defaultMode = defaultMode;
        this.mode = defaultMode;
    }

    @Override
    protected Integer sanitize(Integer candidate) {
        int code = candidate;
        boolean keyboard = code >= 0 && code <= GLFW_KEY_LAST;
        boolean mouse = code >= MOUSE_OFFSET && code <= MOUSE_OFFSET + GLFW_MOUSE_BUTTON_LAST;
        return keyboard || mouse ? code : NONE;
    }

    public boolean isBound() {
        return value != NONE;
    }

    public boolean isMouse() {
        return value >= MOUSE_OFFSET;
    }

    public boolean matchesKey(int glfwKey) {
        return value != NONE && !isMouse() && value == glfwKey;
    }

    public boolean matchesMouse(int glfwButton) {
        return isMouse() && value - MOUSE_OFFSET == glfwButton;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        if (mode != null && this.mode != mode) {
            this.mode = mode;
            fireChanged();
        }
    }

    public void cycleMode() {
        setMode(mode == Mode.TOGGLE ? Mode.HOLD : Mode.TOGGLE);
    }

    public void unbind() {
        set(NONE);
    }

    @Override
    public void reset() {
        setMode(defaultMode);
        super.reset();
    }

    @Override
    public boolean isDefault() {
        return mode == defaultMode && super.isDefault();
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", value);
        obj.addProperty("mode", mode.name());
        return obj;
    }

    @Override
    public boolean load(JsonElement json) {
        if (json == null) return false;
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            set(json.getAsInt());
            return true;
        }
        if (!json.isJsonObject()) return false;
        JsonObject obj = json.getAsJsonObject();
        boolean ok = true;
        JsonElement key = obj.get("key");
        if (key != null && key.isJsonPrimitive() && key.getAsJsonPrimitive().isNumber()) {
            set(key.getAsInt());
        } else {
            ok = false;
        }
        JsonElement m = obj.get("mode");
        if (m != null && m.isJsonPrimitive()) {
            try {
                setMode(Mode.valueOf(m.getAsString().trim().toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                ok = false;
            }
        }
        return ok;
    }
}

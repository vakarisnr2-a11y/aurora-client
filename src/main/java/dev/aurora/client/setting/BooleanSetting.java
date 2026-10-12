package dev.aurora.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public boolean isOn() {
        return value;
    }

    public void toggle() {
        set(!value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public boolean load(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
            set(json.getAsBoolean());
            return true;
        }
        return false;
    }
}

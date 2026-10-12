package dev.aurora.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A short single-line text value (control characters are stripped, length is capped). */
public final class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String name, String description, String defaultValue, int maxLength) {
        super(name, description, defaultValue);
        this.maxLength = Math.max(1, maxLength);
        this.value = sanitize(defaultValue);
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    protected String sanitize(String candidate) {
        StringBuilder sb = new StringBuilder(Math.min(candidate.length(), maxLength));
        for (int i = 0; i < candidate.length() && sb.length() < maxLength; i++) {
            char c = candidate.charAt(i);
            if (c >= 32 && c != 127) sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public boolean load(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            set(json.getAsString());
            return true;
        }
        return false;
    }
}

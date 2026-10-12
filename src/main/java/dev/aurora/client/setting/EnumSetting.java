package dev.aurora.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/** A dropdown / mode selector backed by an enum. */
public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] constants;

    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        this.constants = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] getConstants() {
        return constants;
    }

    public boolean is(E constant) {
        return value == constant;
    }

    public void setIndex(int index) {
        if (index >= 0 && index < constants.length) set(constants[index]);
    }

    public void cycle(int direction) {
        int n = constants.length;
        set(constants[((value.ordinal() + direction) % n + n) % n]);
    }

    public String getDisplay() {
        return displayName(value);
    }

    /** Uses an enum's overridden toString(), otherwise turns THIRD_PERSON into "Third Person". */
    public static String displayName(Enum<?> e) {
        String s = e.toString();
        if (!s.equals(e.name())) return s;
        StringBuilder out = new StringBuilder(s.length());
        boolean upper = true;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                out.append(' ');
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = false;
            }
        }
        return out.toString();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }

    @Override
    public boolean load(JsonElement json) {
        if (json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) return false;
        String raw = json.getAsString().trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (E c : constants) {
            if (c.name().equals(raw)) {
                set(c);
                return true;
            }
        }
        return false;
    }
}

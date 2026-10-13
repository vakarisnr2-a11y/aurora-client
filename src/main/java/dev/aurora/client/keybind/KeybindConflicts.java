package dev.aurora.client.keybind;

import dev.aurora.client.setting.KeybindSetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds keys used by more than one binding. Only conflicts that involve an Aurora binding are reported; two vanilla
 * controls sharing a key are vanilla's business (its own Controls screen already marks them).
 */
public final class KeybindConflicts {
    /**
     * One binding.
     *
     * @param label   display name, e.g. "Zoom" or "Screenshot › Capture Key"
     * @param code    Aurora key code (GLFW key, or {@link KeybindSetting#MOUSE_OFFSET} + mouse button)
     * @param vanilla true for Minecraft (and other mods') controls
     * @param source  the KeybindSetting or KeyBinding behind it
     */
    public record Binding(String label, int code, boolean vanilla, Object source) {
    }

    private KeybindConflicts() {
    }

    /** For every bound Aurora binding that shares its key with another binding, the bindings it conflicts with. */
    public static Map<Binding, List<Binding>> find(List<Binding> bindings) {
        Map<Binding, List<Binding>> out = new LinkedHashMap<>();
        for (Binding a : bindings) {
            if (a.vanilla() || a.code() == KeybindSetting.NONE) continue;
            List<Binding> others = null;
            for (Binding b : bindings) {
                if (b == a || b.code() != a.code()) continue;
                if (others == null) others = new ArrayList<>(2);
                others.add(b);
            }
            if (others != null) out.put(a, others);
        }
        return out;
    }

    /** Text of a conflict warning: "Also: A, B". */
    public static String describe(List<Binding> others) {
        StringBuilder sb = new StringBuilder("Also: ");
        for (int i = 0; i < others.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(others.get(i).label());
        }
        return sb.toString();
    }
}

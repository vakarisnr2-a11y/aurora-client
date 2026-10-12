package dev.aurora.client.util;

import dev.aurora.client.setting.KeybindSetting;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Human readable names for keybind codes. */
public final class KeyNames {
    private KeyNames() {
    }

    public static String of(int code) {
        if (code == KeybindSetting.NONE) return "None";
        if (code >= KeybindSetting.MOUSE_OFFSET) {
            int button = code - KeybindSetting.MOUSE_OFFSET;
            return switch (button) {
                case 0 -> "LMB";
                case 1 -> "RMB";
                case 2 -> "MMB";
                default -> "Mouse " + (button + 1);
            };
        }
        return switch (code) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-Shift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-Shift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R-Ctrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-Ctrl";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R-Alt";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L-Alt";
            default -> {
                try {
                    yield InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
                } catch (RuntimeException e) {
                    yield "Key " + code;
                }
            }
        };
    }
}

package dev.aurora.client.gui.component;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

/** Minimal single-line text editing (append, backspace, ctrl+backspace, paste) shared by the search bar and string settings. */
public final class TextInput {
    private final StringBuilder text = new StringBuilder();
    private final int maxLength;

    public TextInput(int maxLength) {
        this.maxLength = maxLength;
    }

    public String get() {
        return text.toString();
    }

    public void set(String value) {
        text.setLength(0);
        append(value);
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public void clear() {
        text.setLength(0);
    }

    /** @return true if the key was consumed */
    public boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (text.isEmpty()) return true;
            if (Screen.hasControlDown()) {
                int i = text.length() - 1;
                while (i > 0 && text.charAt(i - 1) != ' ') i--;
                text.setLength(Math.max(0, i));
            } else {
                text.setLength(text.length() - 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
            append(MinecraftClient.getInstance().keyboard.getClipboard());
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (chr < 32 || chr == 127 || chr == '§') return false;
        append(String.valueOf(chr));
        return true;
    }

    private void append(String s) {
        for (int i = 0; i < s.length() && text.length() < maxLength; i++) {
            char c = s.charAt(i);
            if (c >= 32 && c != 127 && c != '§') text.append(c);
        }
    }
}

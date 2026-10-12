package dev.aurora.client.event.events;

import dev.aurora.client.event.Event;

/** Raw keyboard input (GLFW codes). Fired for in-game and in-screen key presses. Cancelling blocks vanilla handling. */
public final class KeyEvent extends Event {
    public static final KeyEvent INSTANCE = new KeyEvent();

    public int key;
    public int scancode;
    /** GLFW_PRESS (1), GLFW_RELEASE (0) or GLFW_REPEAT (2). */
    public int action;
    public int modifiers;
    /** True when a screen (chat, inventory, GUI...) is open. */
    public boolean inScreen;

    public KeyEvent set(int key, int scancode, int action, int modifiers, boolean inScreen) {
        resetCancelled();
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.modifiers = modifiers;
        this.inScreen = inScreen;
        return this;
    }
}

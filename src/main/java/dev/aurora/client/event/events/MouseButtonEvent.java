package dev.aurora.client.event.events;

import dev.aurora.client.event.Event;

/** Raw mouse button input. Cancelling blocks vanilla handling. */
public final class MouseButtonEvent extends Event {
    public static final MouseButtonEvent INSTANCE = new MouseButtonEvent();

    public int button;
    public int action;
    public int modifiers;
    public boolean inScreen;

    public MouseButtonEvent set(int button, int action, int modifiers, boolean inScreen) {
        resetCancelled();
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
        this.inScreen = inScreen;
        return this;
    }
}

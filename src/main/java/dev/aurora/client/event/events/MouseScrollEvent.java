package dev.aurora.client.event.events;

import dev.aurora.client.event.Event;

/** Mouse wheel input while no screen is open. Cancelling prevents the hotbar from scrolling. */
public final class MouseScrollEvent extends Event {
    public static final MouseScrollEvent INSTANCE = new MouseScrollEvent();

    public double horizontal;
    public double vertical;

    public MouseScrollEvent set(double horizontal, double vertical) {
        resetCancelled();
        this.horizontal = horizontal;
        this.vertical = vertical;
        return this;
    }
}

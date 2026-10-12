package dev.aurora.client.event.events;

import dev.aurora.client.event.Event;

/** Client tick events (20 per second). {@link Pre} fires before the world/player tick, {@link Post} after. */
public final class TickEvent {
    private TickEvent() {
    }

    public static final class Pre extends Event {
        public static final Pre INSTANCE = new Pre();

        public Pre reset() {
            resetCancelled();
            return this;
        }
    }

    public static final class Post extends Event {
        public static final Post INSTANCE = new Post();

        public Post reset() {
            resetCancelled();
            return this;
        }
    }
}

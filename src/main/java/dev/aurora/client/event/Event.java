package dev.aurora.client.event;

/**
 * Base class for all events. Events are reused (one mutable instance per type) so that
 * per-frame posting does not allocate; listeners must not keep references to them.
 */
public abstract class Event {
    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }

    protected void resetCancelled() {
        cancelled = false;
    }
}

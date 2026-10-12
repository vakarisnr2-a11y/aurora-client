package dev.aurora.client.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Small typed event bus.
 *
 * <p>Listeners are stored per exact event class in copy-on-write arrays: subscribing is rare
 * (module toggles) while posting happens every tick/frame, so posting iterates a plain array
 * with no iterator or lambda allocation. A listener that throws is logged (rate-limited) and
 * skipped so one broken module can never crash the game loop.</p>
 */
public final class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger("Aurora/EventBus");
    private static final Listener[] EMPTY = new Listener[0];
    private static final Comparator<Listener> ORDER = Comparator.comparingInt(l -> l.priority);

    private final Map<Class<?>, Listener[]> listeners = new ConcurrentHashMap<>();

    private static final class Listener {
        final Object owner;
        final Consumer<Object> handler;
        final int priority;
        int failures;

        Listener(Object owner, Consumer<Object> handler, int priority) {
            this.owner = owner;
            this.handler = handler;
            this.priority = priority;
        }
    }

    public <E extends Event> void subscribe(Object owner, Class<E> type, Consumer<? super E> handler) {
        subscribe(owner, type, handler, EventPriority.NORMAL);
    }

    @SuppressWarnings("unchecked")
    public synchronized <E extends Event> void subscribe(Object owner, Class<E> type, Consumer<? super E> handler, EventPriority priority) {
        Listener[] current = listeners.getOrDefault(type, EMPTY);
        Listener[] next = Arrays.copyOf(current, current.length + 1);
        next[current.length] = new Listener(owner, (Consumer<Object>) handler, priority.ordinal());
        Arrays.sort(next, ORDER); // stable sort: equal priorities keep subscription order
        listeners.put(type, next);
    }

    /** Removes every listener registered by {@code owner}. */
    public synchronized void unsubscribeAll(Object owner) {
        for (Map.Entry<Class<?>, Listener[]> entry : listeners.entrySet()) {
            Listener[] current = entry.getValue();
            List<Listener> kept = new ArrayList<>(current.length);
            for (Listener l : current) {
                if (l.owner != owner) kept.add(l);
            }
            if (kept.size() != current.length) {
                entry.setValue(kept.toArray(EMPTY));
            }
        }
    }

    public <E extends Event> E post(E event) {
        Listener[] array = listeners.get(event.getClass());
        if (array == null) return event;
        for (Listener listener : array) {
            try {
                listener.handler.accept(event);
            } catch (Throwable t) {
                listener.failures++;
                // Log the first few failures with a stack trace, then only occasionally.
                if (listener.failures <= 3 || listener.failures % 1000 == 0) {
                    LOGGER.error("Listener of {} for {} failed ({} times)", listener.owner.getClass().getSimpleName(),
                        event.getClass().getSimpleName(), listener.failures, t);
                }
            }
        }
        return event;
    }

    public int listenerCount(Class<? extends Event> type) {
        Listener[] array = listeners.get(type);
        return array == null ? 0 : array.length;
    }
}

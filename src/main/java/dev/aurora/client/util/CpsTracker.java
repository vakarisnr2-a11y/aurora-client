package dev.aurora.client.util;

import dev.aurora.client.event.EventBus;
import dev.aurora.client.event.EventPriority;
import dev.aurora.client.event.events.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

/** Counts physical left/right clicks per second using fixed-size ring buffers (no allocation). */
public final class CpsTracker {
    public static final CpsTracker INSTANCE = new CpsTracker();
    private static final int CAPACITY = 64;

    private final long[] left = new long[CAPACITY];
    private final long[] right = new long[CAPACITY];
    private int leftIndex;
    private int rightIndex;

    private CpsTracker() {
    }

    public void register(EventBus bus) {
        bus.subscribe(this, MouseButtonEvent.class, e -> {
            if (e.inScreen || e.action != GLFW.GLFW_PRESS) return;
            long now = System.currentTimeMillis();
            if (e.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                left[leftIndex] = now;
                leftIndex = (leftIndex + 1) % CAPACITY;
            } else if (e.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                right[rightIndex] = now;
                rightIndex = (rightIndex + 1) % CAPACITY;
            }
        }, EventPriority.HIGHEST);
    }

    public int getLeftCps() {
        return count(left);
    }

    public int getRightCps() {
        return count(right);
    }

    private static int count(long[] buffer) {
        long cutoff = System.currentTimeMillis() - 1000;
        int n = 0;
        for (long t : buffer) if (t > cutoff) n++;
        return n;
    }
}

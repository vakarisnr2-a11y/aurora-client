package dev.aurora.client.event.events;

import dev.aurora.client.event.Event;
import net.minecraft.client.gui.DrawContext;

/** Fired after the vanilla HUD has been drawn, in scaled GUI coordinates. */
public final class Render2DEvent extends Event {
    public static final Render2DEvent INSTANCE = new Render2DEvent();

    public DrawContext context;
    public float tickDelta;
    public int width;
    public int height;

    public Render2DEvent set(DrawContext context, float tickDelta, int width, int height) {
        resetCancelled();
        this.context = context;
        this.tickDelta = tickDelta;
        this.width = width;
        this.height = height;
        return this;
    }
}

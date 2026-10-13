package dev.aurora.client.notification;

import dev.aurora.client.gui.Theme;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.modules.misc.Notifications;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Animated toast notifications (module toggles, config messages, warnings). */
public final class NotificationManager {
    public enum Type {
        INFO(0xFF60A5FA, "i"),
        SUCCESS(0xFF4ADE80, "✔"),
        DISABLED(0xFFF87171, "✖"),
        WARNING(0xFFFBBF24, "!"),
        ERROR(0xFFEF4444, "!");

        final int color;
        final String icon;

        Type(int color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    /** A clickable button on a toast. Toasts can only be clicked while a screen with a mouse cursor is open (e.g. chat). */
    public record Action(String label, Runnable run) {
    }

    private static final int WIDTH_MIN = 120;
    private static final int HEIGHT = 30;
    private static final int ACTION_HEIGHT = 16;
    private static final int GAP = 4;

    private static final class Toast {
        final String title;
        final String message;
        final Type type;
        final Object key;
        final Action[] actions;
        final long duration;
        /** Button rectangles from the last frame: x, y, w, h per action. */
        final float[] buttons;
        long created = System.currentTimeMillis();
        final Animation slide = new Animation(0, 14);
        final Animation offsetY = new Animation(-1, 16);

        Toast(String title, String message, Type type, Object key, long duration, Action[] actions) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.key = key;
            this.duration = duration;
            this.actions = actions;
            this.buttons = new float[actions.length * 4];
            slide.setTarget(1);
        }

        int height() {
            return actions.length == 0 ? HEIGHT : HEIGHT + ACTION_HEIGHT;
        }
    }

    private final List<Toast> toasts = new ArrayList<>();
    private final ModuleManager modules;

    public NotificationManager(ModuleManager modules) {
        this.modules = modules;
    }

    private Notifications settings() {
        return modules.get(Notifications.class);
    }

    public void post(String title, String message, Type type) {
        post(title, message, type, null);
    }

    /** Toasts with the same key replace each other (e.g. repeated toggles of one module). */
    public synchronized void post(String title, String message, Type type, Object key) {
        post(title, message, type, key, 0);
    }

    /**
     * A toast with buttons.
     *
     * @param durationMs how long it stays, or 0 for the configured duration
     */
    public synchronized void post(String title, String message, Type type, Object key, long durationMs, Action... actions) {
        boolean important = type == Type.WARNING || type == Type.ERROR || actions.length > 0;
        if (!important && !settings().isEnabled()) return;
        if (key != null) toasts.removeIf(t -> key.equals(t.key));
        toasts.add(new Toast(title, message, type, key, durationMs, actions));
        int max = settings().maxVisible();
        while (toasts.size() > max) toasts.remove(0);
    }

    public void onModuleToggled(Module module, boolean enabled) {
        if (!module.notifiesOnToggle() || !settings().showToggles()) return;
        post(module.getName(), enabled ? "Enabled" : "Disabled", enabled ? Type.SUCCESS : Type.DISABLED, module);
    }

    public synchronized void render(DrawContext ctx, int screenW, int screenH) {
        if (toasts.isEmpty()) return;
        Notifications cfg = settings();
        long now = System.currentTimeMillis();
        long defaultDuration = cfg.durationMs();
        boolean right = cfg.position().right;
        boolean bottom = cfg.position().bottom;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        boolean cursor = mc.currentScreen != null;
        double mx = cursor ? mc.mouse.getX() * screenW / Math.max(1, mc.getWindow().getWidth()) : -1;
        double my = cursor ? mc.mouse.getY() * screenH / Math.max(1, mc.getWindow().getHeight()) : -1;
        // Keep clear of the chat input bar while chat is open.
        float bottomInset = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen ? 14 : 0;

        float stack = 0;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            long duration = t.duration > 0 ? t.duration : defaultDuration;
            long age = now - t.created;
            if (age > duration) t.slide.setTarget(0);
            float s = (float) t.slide.update();
            if (age > duration && s <= 0.01f) {
                toasts.remove(i);
                continue;
            }
            if (t.offsetY.get() < 0) t.offsetY.snap(stack);
            t.offsetY.setTarget(stack);
            float slot = (float) t.offsetY.update();
            int height = t.height();

            int width = Math.max(WIDTH_MIN, Math.max(tr.getWidth(t.title), tr.getWidth(t.message)) + 34);
            float hidden = (1 - s) * (width + 12);
            float x = right ? screenW - width - 6 + hidden : 6 - hidden;
            float y = bottom ? screenH - 6 - bottomInset - height - slot : 6 + slot;
            float alpha = Math.min(1f, s * 1.5f);

            Render2D.shadow(ctx, x, y, width, height, 5, 4, ColorUtil.withAlpha(0, (int) (120 * alpha)));
            Render2D.roundedRect(ctx, x, y, width, height, 5, ColorUtil.fade(Theme.panel(), alpha));
            Render2D.roundedRect(ctx, x + 5, y + 7, 16, 16, 8, ColorUtil.fade(ColorUtil.withAlpha(t.type.color, 60), alpha));
            int textAlpha = Math.max(4, (int) (255 * alpha));
            ctx.drawText(tr, t.type.icon, (int) (x + 13 - tr.getWidth(t.type.icon) / 2f), (int) y + 11,
                ColorUtil.withAlpha(t.type.color, textAlpha), false);
            ctx.drawText(tr, t.title, (int) x + 27, (int) y + 6, ColorUtil.withAlpha(Theme.TEXT, textAlpha), false);
            ctx.drawText(tr, t.message, (int) x + 27, (int) y + 17, ColorUtil.withAlpha(t.type == Type.DISABLED || t.type == Type.SUCCESS
                ? t.type.color : Theme.SUBTEXT, textAlpha), false);
            if (t.actions.length > 0) drawActions(ctx, tr, t, x, y + HEIGHT - 3, alpha, cursor, mx, my);
            float progress = 1f - Math.min(1f, age / (float) duration);
            Render2D.rect(ctx, x + 5, y + height - 2, (width - 10) * progress, 1, ColorUtil.fade(t.type.color, alpha));
            stack += height + GAP;
        }
    }

    private static void drawActions(DrawContext ctx, TextRenderer tr, Toast t, float x, float y, float alpha, boolean cursor,
                                    double mx, double my) {
        float bx = x + 27;
        int textAlpha = Math.max(4, (int) (255 * alpha));
        for (int i = 0; i < t.actions.length; i++) {
            String label = t.actions[i].label();
            float bw = tr.getWidth(label) + 10;
            boolean hover = cursor && mx >= bx && mx < bx + bw && my >= y && my < y + 12;
            Render2D.roundedRect(ctx, bx, y, bw, 12, 3, ColorUtil.fade(hover ? ColorUtil.withAlpha(Theme.accent(), 220)
                : ColorUtil.withAlpha(Theme.accent(), 90), alpha));
            ctx.drawText(tr, label, (int) bx + 5, (int) y + 2, ColorUtil.withAlpha(Theme.TEXT, textAlpha), false);
            t.buttons[i * 4] = bx;
            t.buttons[i * 4 + 1] = y;
            t.buttons[i * 4 + 2] = bw;
            t.buttons[i * 4 + 3] = 12;
            bx += bw + 4;
        }
        if (!cursor) {
            ctx.drawText(tr, "open chat to click", (int) bx + 2, (int) y + 2, ColorUtil.withAlpha(Theme.MUTED, textAlpha), false);
        }
    }

    /** Handles a left click (scaled screen coordinates) while a screen is open. Returns true if a button was hit. */
    public synchronized boolean click(double mx, double my) {
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            for (int a = 0; a < t.actions.length; a++) {
                float bx = t.buttons[a * 4], by = t.buttons[a * 4 + 1], bw = t.buttons[a * 4 + 2], bh = t.buttons[a * 4 + 3];
                if (bw > 0 && mx >= bx && mx < bx + bw && my >= by && my < by + bh) {
                    t.actions[a].run().run();
                    return true;
                }
            }
        }
        return false;
    }
}

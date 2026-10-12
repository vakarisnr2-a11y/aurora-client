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

    private static final int WIDTH_MIN = 120;
    private static final int HEIGHT = 30;
    private static final int GAP = 4;

    private static final class Toast {
        final String title;
        final String message;
        final Type type;
        final Object key;
        long created = System.currentTimeMillis();
        final Animation slide = new Animation(0, 14);
        final Animation offsetY = new Animation(-1, 16);

        Toast(String title, String message, Type type, Object key) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.key = key;
            slide.setTarget(1);
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
        boolean important = type == Type.WARNING || type == Type.ERROR;
        if (!important && !settings().isEnabled()) return;
        if (key != null) toasts.removeIf(t -> key.equals(t.key));
        toasts.add(new Toast(title, message, type, key));
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
        long duration = cfg.durationMs();
        boolean right = cfg.position().right;
        boolean bottom = cfg.position().bottom;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        int index = 0;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            long age = now - t.created;
            if (age > duration) t.slide.setTarget(0);
            float s = (float) t.slide.update();
            if (age > duration && s <= 0.01f) {
                toasts.remove(i);
                continue;
            }
            if (t.offsetY.get() < 0) t.offsetY.snap(index);
            t.offsetY.setTarget(index);
            float slot = (float) t.offsetY.update();

            int width = Math.max(WIDTH_MIN, Math.max(tr.getWidth(t.title), tr.getWidth(t.message)) + 34);
            float hidden = (1 - s) * (width + 12);
            float x = right ? screenW - width - 6 + hidden : 6 - hidden;
            float y = bottom ? screenH - 6 - HEIGHT - slot * (HEIGHT + GAP) : 6 + slot * (HEIGHT + GAP);
            float alpha = Math.min(1f, s * 1.5f);

            Render2D.shadow(ctx, x, y, width, HEIGHT, 5, 4, ColorUtil.withAlpha(0, (int) (120 * alpha)));
            Render2D.roundedRect(ctx, x, y, width, HEIGHT, 5, ColorUtil.fade(Theme.panel(), alpha));
            Render2D.roundedRect(ctx, x + 5, y + 7, 16, 16, 8, ColorUtil.fade(ColorUtil.withAlpha(t.type.color, 60), alpha));
            int textAlpha = Math.max(4, (int) (255 * alpha));
            ctx.drawText(tr, t.type.icon, (int) (x + 13 - tr.getWidth(t.type.icon) / 2f), (int) y + 11,
                ColorUtil.withAlpha(t.type.color, textAlpha), false);
            ctx.drawText(tr, t.title, (int) x + 27, (int) y + 6, ColorUtil.withAlpha(Theme.TEXT, textAlpha), false);
            ctx.drawText(tr, t.message, (int) x + 27, (int) y + 17, ColorUtil.withAlpha(t.type == Type.DISABLED || t.type == Type.SUCCESS
                ? t.type.color : Theme.SUBTEXT, textAlpha), false);
            float progress = 1f - Math.min(1f, age / (float) duration);
            Render2D.rect(ctx, x + 5, y + HEIGHT - 2, (width - 10) * progress, 1, ColorUtil.fade(t.type.color, alpha));
            index++;
        }
    }
}

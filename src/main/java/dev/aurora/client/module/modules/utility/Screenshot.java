package dev.aurora.client.module.modules.utility;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.event.events.KeyEvent;
import dev.aurora.client.event.events.MouseButtonEvent;
import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.KeybindSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.glfw.GLFW;

/**
 * Screenshot shortcut on a configurable key (default F12), optionally hiding the HUD first.
 * Uses vanilla's ScreenshotRecorder, so files land in .minecraft/screenshots.
 */
public final class Screenshot extends Module {
    private final KeybindSetting captureKey = add(new KeybindSetting("Capture Key", "Key that takes a screenshot", GLFW.GLFW_KEY_F12));
    private final BooleanSetting hideHud = add(new BooleanSetting("Hide HUD", "Hide the HUD for the screenshot", false));
    private final BooleanSetting notify = add(new BooleanSetting("Notify", "Show a notification when saved", true));

    private int pendingTicks;
    private boolean restoreHud;

    public Screenshot() {
        super("Screenshot", "Takes screenshots with a configurable key, optionally without the HUD.", Category.UTILITY,
            KeybindSetting.NONE, true);
        on(KeyEvent.class, e -> {
            if (!e.inScreen && e.action == GLFW.GLFW_PRESS && captureKey.matchesKey(e.key)) request();
        });
        on(MouseButtonEvent.class, e -> {
            if (!e.inScreen && e.action == GLFW.GLFW_PRESS && captureKey.matchesMouse(e.button)) request();
        });
        on(TickEvent.Pre.class, e -> {
            if (pendingTicks > 0 && --pendingTicks == 0) capture();
        });
    }

    private void request() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (pendingTicks > 0) return;
        if (hideHud.get() && !mc.options.hudHidden) {
            mc.options.hudHidden = true;
            restoreHud = true;
            pendingTicks = 2; // let at least one frame render without the HUD
        } else {
            capture();
        }
    }

    private void capture() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(), message -> mc.execute(() -> {
                if (mc.inGameHud != null) mc.inGameHud.getChatHud().addMessage(message);
                if (notify.get()) AuroraClient.notifications().post("Screenshot", "Saved to screenshots folder",
                    NotificationManager.Type.INFO);
            }));
        } finally {
            if (restoreHud) {
                mc.options.hudHidden = false;
                restoreHud = false;
            }
        }
    }

    @Override
    protected void onDisable() {
        if (restoreHud) {
            MinecraftClient.getInstance().options.hudHidden = false;
            restoreHud = false;
        }
        pendingTicks = 0;
    }
}

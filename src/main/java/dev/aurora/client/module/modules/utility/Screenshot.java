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
import dev.aurora.client.util.ImageClipboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

/**
 * Screenshot shortcut on a configurable key (default F12), optionally hiding the HUD first. Saved with vanilla's
 * ScreenshotRecorder into .minecraft/screenshots; afterwards a notification and a chat line offer Open, Copy (the image
 * itself) and Folder.
 */
public final class Screenshot extends Module {
    private static final DateTimeFormatter NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final long TOAST_MS = 8000;

    private final KeybindSetting captureKey = add(new KeybindSetting("Capture Key", "Key that takes a screenshot", GLFW.GLFW_KEY_F12));
    private final BooleanSetting hideHud = add(new BooleanSetting("Hide HUD", "Hide the HUD for the screenshot", false));
    private final BooleanSetting notify = add(new BooleanSetting("Notify", "Show a notification with Open / Copy / Folder buttons", true));
    private final BooleanSetting chatButtons = add(new BooleanSetting("Chat Buttons", "Add clickable Open / Copy / Folder buttons in chat", true));

    private int pendingTicks;
    private boolean restoreHud;

    public Screenshot() {
        super("Screenshot", "Takes screenshots with a configurable key, then offers to open or copy them.", Category.UTILITY,
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
            File dir = new File(mc.runDirectory, "screenshots");
            String name = fileName(LocalDateTime.now(), n -> new File(dir, n).exists());
            Path file = dir.toPath().resolve(name);
            ScreenshotRecorder.saveScreenshot(mc.runDirectory, name, mc.getFramebuffer(), message -> mc.execute(() -> saved(message, file)));
        } finally {
            if (restoreHud) {
                mc.options.hudHidden = false;
                restoreHud = false;
            }
        }
    }

    /** Runs on the client thread once vanilla reported the result. */
    private void saved(Text message, Path file) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud != null) mc.inGameHud.getChatHud().addMessage(message);
        if (!Files.isRegularFile(file)) return; // vanilla's message already explains the failure
        String name = file.getFileName().toString();
        if (chatButtons.get() && mc.inGameHud != null) mc.inGameHud.getChatHud().addMessage(chatButtons(file));
        if (notify.get()) {
            AuroraClient.notifications().post("Screenshot saved", name, NotificationManager.Type.INFO, this, TOAST_MS,
                new NotificationManager.Action("Open", () -> Util.getOperatingSystem().open(file.toFile())),
                new NotificationManager.Action("Copy", () -> copyImage(file)),
                new NotificationManager.Action("Folder", () -> Util.getOperatingSystem().open(file.getParent().toFile())));
        }
    }

    private static Text chatButtons(Path file) {
        MutableText line = Text.literal("[Aurora] ").formatted(Formatting.LIGHT_PURPLE);
        line.append(button("Open", Formatting.GREEN, new ClickEvent(ClickEvent.Action.OPEN_FILE, file.toAbsolutePath().toString()), "Open the screenshot"));
        line.append(" ");
        line.append(button("Copy", Formatting.AQUA, new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            "/aurora screenshot copy " + file.getFileName()), "Copy the image to the clipboard"));
        line.append(" ");
        line.append(button("Folder", Formatting.YELLOW, new ClickEvent(ClickEvent.Action.OPEN_FILE,
            file.getParent().toAbsolutePath().toString()), "Open the screenshots folder"));
        return line;
    }

    private static MutableText button(String label, Formatting color, ClickEvent click, String hover) {
        return Text.literal("[" + label + "]").styled(s -> s.withColor(color).withClickEvent(click)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** Copies the image in the background and reports the result. */
    public static void copyImage(Path file) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Util.getIoWorkerExecutor().execute(() -> {
            boolean ok = ImageClipboard.copy(file);
            mc.execute(() -> {
                if (ok) {
                    AuroraClient.notifications().post("Screenshot copied", "The image is on your clipboard", NotificationManager.Type.SUCCESS);
                } else {
                    mc.keyboard.setClipboard(file.toAbsolutePath().toString());
                    AuroraClient.notifications().post("Image copy unavailable", "Copied the file path instead", NotificationManager.Type.WARNING);
                }
            });
        });
    }

    /** Vanilla's naming ("2024-05-01_14.03.22.png", then "_1", "_2"... when taken within the same second). */
    public static String fileName(LocalDateTime time, Predicate<String> exists) {
        String base = time.format(NAME);
        String name = base + ".png";
        for (int i = 1; exists.test(name); i++) name = base + "_" + i + ".png";
        return name;
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

package dev.aurora.client.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Copies a PNG file to the system clipboard as an image. Minecraft runs AWT headless and GLFW only handles text, so
 * this asks the operating system: PowerShell on Windows, osascript on macOS, wl-copy (Wayland) or xclip (X11) on
 * Linux. Blocking; call it off the render thread.
 */
public final class ImageClipboard {
    public enum Os {
        WINDOWS, MAC, LINUX, OTHER
    }

    private ImageClipboard() {
    }

    /** The command that copies {@code file}, or null when there is none for this OS. */
    public static List<String> command(Os os, Path file, boolean wayland) {
        String path = file.toAbsolutePath().toString();
        return switch (os) {
            case WINDOWS -> List.of("powershell", "-NoProfile", "-NonInteractive", "-STA", "-Command",
                "Add-Type -AssemblyName System.Windows.Forms; Add-Type -AssemblyName System.Drawing; "
                    + "$i = [System.Drawing.Image]::FromFile('" + path.replace("'", "''") + "'); "
                    + "[System.Windows.Forms.Clipboard]::SetImage($i); $i.Dispose()");
            case MAC -> List.of("osascript", "-e",
                "set the clipboard to (read (POSIX file \"" + path.replace("\\", "\\\\").replace("\"", "\\\"") + "\") as «class PNGf»)");
            case LINUX -> wayland ? List.of("wl-copy", "--type", "image/png") : List.of("xclip", "-selection", "clipboard", "-t", "image/png", "-i", path);
            case OTHER -> null;
        };
    }

    public static Os currentOs() {
        String name = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (name.contains("win")) return Os.WINDOWS;
        if (name.contains("mac") || name.contains("darwin")) return Os.MAC;
        if (name.contains("linux") || name.contains("bsd")) return Os.LINUX;
        return Os.OTHER;
    }

    /** Copies the image; returns true when the helper program finished successfully. */
    public static boolean copy(Path file) {
        if (!Files.isRegularFile(file)) return false;
        Map<String, String> env = System.getenv();
        boolean wayland = env.containsKey("WAYLAND_DISPLAY");
        List<String> cmd = command(currentOs(), file, wayland);
        if (cmd == null) return false;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD);
            boolean stdinImage = currentOs() == Os.LINUX && wayland; // wl-copy reads the image from stdin
            if (stdinImage) pb.redirectInput(file.toFile());
            Process p = pb.start();
            if (!stdinImage) p.getOutputStream().close();
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

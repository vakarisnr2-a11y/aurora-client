package dev.aurora.client.gui.clickgui;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.config.ConfigManager;
import dev.aurora.client.config.LoadReport;
import dev.aurora.client.config.ThemeIO;
import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.ui.Icons;
import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.module.modules.visuals.HudTheme;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.Animation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Profiles page: the profile list (click to load, right-click twice to delete) and profile/theme actions. */
public final class ProfilesView {
    private static final String[] ACTIONS = {"Save", "New", "Export", "Import", "Folder", "Reset", "Export Theme", "Import Theme"};
    private static final String[] TIPS = {
        "Save the active profile now (it also autosaves)",
        "Create a new profile from the current settings",
        "Write the profile to config/aurora/exports and copy it to the clipboard",
        "Import a profile from JSON in the clipboard",
        "Open the config folder",
        "Reset every module of this profile to defaults (click twice)",
        "Export the ClickGUI and HUD Theme look to config/aurora/themes and the clipboard",
        "Import a look (ClickGUI and HUD Theme) from JSON in the clipboard"};
    private static final float ROW = 30;
    private static final float BTN_H = 20;

    private final ClickGuiScreen screen;
    private final List<String> profiles = new ArrayList<>();
    private final Animation scroll = new Animation(0, 18);
    private double scrollTarget;
    private String armedDelete;
    private long armedUntil;
    private long resetArmedUntil;
    private float x, y, w, h;

    public ProfilesView(ClickGuiScreen screen) {
        this.screen = screen;
        refresh();
    }

    public void refresh() {
        profiles.clear();
        profiles.addAll(AuroraClient.config().listProfiles());
    }

    private static ConfigManager config() {
        return AuroraClient.config();
    }

    private static void toast(String title, String message, NotificationManager.Type type) {
        AuroraClient.notifications().post(title, message, type, ProfilesView.class);
    }

    private int columns() {
        return w > 380 ? 4 : 2;
    }

    private float buttonW() {
        int cols = columns();
        return (w - Ui.PAD * 2 - (cols - 1) * 6) / cols;
    }

    private float[] button(int i) {
        int cols = columns();
        float bw = buttonW();
        return new float[]{x + Ui.PAD + (i % cols) * (bw + 6), y + 48 + (i / cols) * (BTN_H + 6), bw, BTN_H};
    }

    private float listTop() {
        int rowsOfButtons = (ACTIONS.length + columns() - 1) / columns();
        return y + 48 + rowsOfButtons * (BTN_H + 6) + 12;
    }

    private int buttonAt(double mx, double my) {
        for (int i = 0; i < ACTIONS.length; i++) {
            float[] b = button(i);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) return i;
        }
        return -1;
    }

    private int rowAt(double mx, double my) {
        float top = listTop();
        if (mx < x + Ui.PAD || mx >= x + w - Ui.PAD || my < top || my >= y + h) return -1;
        int i = (int) ((my - top + scroll.get()) / ROW);
        return i >= 0 && i < profiles.size() ? i : -1;
    }

    public void render(DrawContext ctx, float x, float y, float w, float h, double mx, double my) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        UiFont.title(ctx, "Profiles", x + Ui.PAD, y + 12, Ui.TEXT);
        UiFont.draw(ctx, "Active: " + config().getActiveProfile() + "  ·  click a profile to load it, right-click twice to delete",
            x + Ui.PAD, y + 30, Ui.SUBTEXT);
        int hovered = buttonAt(mx, my);
        for (int i = 0; i < ACTIONS.length; i++) {
            float[] b = button(i);
            boolean armed = i == 5 && System.currentTimeMillis() < resetArmedUntil;
            if (armed) {
                Render2D.roundedRect(ctx, b[0], b[1], b[2], b[3], Ui.R_MEDIUM, 0xFF7F1D1D);
                UiFont.centered(ctx, "Sure? Click again", b[0] + b[2] / 2f, b[1] + 6, 0xFFFFFFFF, UiFont.Size.BODY, false);
            } else {
                Ui.button(ctx, b[0], b[1], b[2], b[3], ACTIONS[i], i == hovered ? 1 : 0, i == 0);
            }
        }
        float top = listTop();
        Ui.divider(ctx, x + Ui.PAD, top - 7, w - Ui.PAD * 2);
        double maxScroll = Math.max(0, profiles.size() * ROW - (y + h - top));
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll.setTarget(scrollTarget);
        float s = (float) scroll.update();
        ctx.enableScissor(screen.toScreenX(x), screen.toScreenY(top), screen.toScreenX(x + w), screen.toScreenY(y + h));
        int hoverRow = rowAt(mx, my);
        for (int i = 0; i < profiles.size(); i++) {
            float ry = top + i * ROW - s;
            if (ry + ROW < top || ry > y + h) continue;
            String name = profiles.get(i);
            boolean active = name.equals(config().getActiveProfile());
            boolean armed = name.equals(armedDelete) && System.currentTimeMillis() < armedUntil;
            Ui.card(ctx, x + Ui.PAD, ry, w - Ui.PAD * 2, ROW - 4, i == hoverRow ? 1 : 0, active ? 1 : 0);
            Icons.draw(ctx, Icons.Icon.PROFILES, x + Ui.PAD + 8, ry + 6, active ? Ui.accentLight() : Ui.MUTED);
            UiFont.bold(ctx, armed ? "Right-click again to delete " + name : name, x + Ui.PAD + 28, ry + 9, armed ? Ui.DANGER : Ui.TEXT);
            if (active) {
                float cw = UiFont.width("Active") + 10;
                Ui.chip(ctx, x + w - Ui.PAD - 8 - cw, ry + 6.5f, "Active", 0xFFFFFFFF, Ui.accent(0xC0));
            }
        }
        ctx.disableScissor();
    }

    public boolean mouseClicked(double mx, double my, int button) {
        int b = buttonAt(mx, my);
        if (b >= 0) {
            if (button == 0) action(b);
            return true;
        }
        int row = rowAt(mx, my);
        if (row < 0) return false;
        String name = profiles.get(row);
        boolean active = name.equals(config().getActiveProfile());
        if (button == 0 && !active) {
            LoadReport report = config().loadProfile(name);
            toast("Profile loaded", name + (report.isClean() ? "" : " (" + report.getWarnings().size() + " warnings)"),
                report.isClean() ? NotificationManager.Type.SUCCESS : NotificationManager.Type.WARNING);
            screen.onProfileChanged();
        } else if (button == 1 && !active) {
            if (name.equals(armedDelete) && System.currentTimeMillis() < armedUntil) {
                if (config().deleteProfile(name)) toast("Profile deleted", name, NotificationManager.Type.INFO);
                armedDelete = null;
                refresh();
            } else {
                armedDelete = name;
                armedUntil = System.currentTimeMillis() + 3000;
            }
        }
        return true;
    }

    private void action(int i) {
        ConfigManager cfg = config();
        MinecraftClient mc = MinecraftClient.getInstance();
        HudTheme hudTheme = AuroraClient.modules().get(HudTheme.class);
        switch (i) {
            case 0 -> toast("Profile saved", cfg.getActiveProfile(), cfg.save() ? NotificationManager.Type.SUCCESS : NotificationManager.Type.ERROR);
            case 1 -> {
                String name = freeName(cfg, "profile-");
                if (cfg.createProfile(name, true)) toast("Profile created", name, NotificationManager.Type.SUCCESS);
                refresh();
            }
            case 2 -> {
                try {
                    Path file = cfg.exportActive();
                    mc.keyboard.setClipboard(cfg.exportToString());
                    toast("Profile exported", "exports/" + file.getFileName() + " + clipboard", NotificationManager.Type.SUCCESS);
                } catch (IOException e) {
                    toast("Export failed", e.getMessage(), NotificationManager.Type.ERROR);
                }
            }
            case 3 -> {
                String name = freeName(cfg, "imported-");
                try {
                    LoadReport report = cfg.importProfile(name, mc.keyboard.getClipboard());
                    toast("Profile imported", name + (report.isClean() ? "" : " (" + report.getWarnings().size() + " values fixed)"),
                        NotificationManager.Type.SUCCESS);
                    screen.onProfileChanged();
                } catch (IllegalArgumentException | IOException e) {
                    toast("Import failed", "Clipboard does not hold a valid profile", NotificationManager.Type.ERROR);
                }
            }
            case 4 -> Util.getOperatingSystem().open(cfg.getRoot());
            case 5 -> {
                if (System.currentTimeMillis() < resetArmedUntil) {
                    AuroraClient.modules().getAll().forEach(m -> {
                        m.resetSettings();
                        if (m.savesEnabledState()) m.setEnabled(m.isEnabledByDefault(), false);
                    });
                    resetArmedUntil = 0;
                    toast("Profile reset", "All modules restored to defaults", NotificationManager.Type.INFO);
                    screen.onProfileChanged();
                } else {
                    resetArmedUntil = System.currentTimeMillis() + 3000;
                }
            }
            case 6 -> {
                try {
                    Path file = ThemeIO.exportTo(cfg.getRoot().resolve("themes"), screen.settings(), hudTheme);
                    mc.keyboard.setClipboard(ThemeIO.exportString(screen.settings(), hudTheme));
                    toast("Theme exported", "themes/" + file.getFileName() + " + clipboard", NotificationManager.Type.SUCCESS);
                } catch (IOException e) {
                    toast("Export failed", e.getMessage(), NotificationManager.Type.ERROR);
                }
            }
            case 7 -> {
                try {
                    int n = ThemeIO.importString(mc.keyboard.getClipboard(), screen.settings(), hudTheme);
                    toast("Theme imported", n + " values applied", NotificationManager.Type.SUCCESS);
                } catch (IllegalArgumentException e) {
                    toast("Import failed", "Clipboard does not hold an Aurora theme", NotificationManager.Type.ERROR);
                }
            }
            default -> {
            }
        }
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (my < listTop()) return false;
        scrollTarget -= amount * ROW;
        return true;
    }

    public String getTooltip(double mx, double my) {
        int b = buttonAt(mx, my);
        return b < 0 ? null : TIPS[b];
    }

    private static String freeName(ConfigManager cfg, String prefix) {
        List<String> existing = cfg.listProfiles();
        int n = 1;
        while (existing.contains(prefix + n)) n++;
        return prefix + n;
    }
}

package dev.aurora.client.gui.component;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.config.ConfigManager;
import dev.aurora.client.config.LoadReport;
import dev.aurora.client.config.ThemeIO;
import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.module.modules.misc.ClickGui;
import dev.aurora.client.module.modules.visuals.HudTheme;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Profile list (click to load, right-click twice to delete) plus save / new / export / import actions. */
public final class ProfilesPanel extends Panel {
    public ProfilesPanel(ClickGuiScreen screen) {
        super(screen, ClickGui.PROFILES_PANEL, "Profiles", "☰");
        refresh();
    }

    public void refresh() {
        children.clear();
        for (String name : AuroraClient.config().listProfiles()) children.add(new ProfileRow(name));
        children.add(new Actions());
    }

    private static ConfigManager config() {
        return AuroraClient.config();
    }

    private static void toast(String title, String message, NotificationManager.Type type) {
        AuroraClient.notifications().post(title, message, type, ProfilesPanel.class);
    }

    private final class ProfileRow extends Component {
        private final String name;
        private long armedUntil;

        ProfileRow(String name) {
            this.name = name;
        }

        @Override
        public float getHeight() {
            return 15;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            boolean active = name.equals(config().getActiveProfile());
            boolean hover = isHovered(mx, my) && screen.isMouseInBody();
            Render2D.rect(ctx, x, y, width, getHeight(), hover ? Theme.hover() : Theme.surface());
            if (active) Render2D.circle(ctx, x + 7, y + 7.5f, 2.5f, Theme.accent());
            boolean armed = System.currentTimeMillis() < armedUntil;
            String label = armed ? "Right-click again to delete" : name;
            ctx.drawText(tr, label, (int) x + 14, (int) y + 4, armed ? 0xFFF87171 : active ? Theme.TEXT : Theme.SUBTEXT, false);
            if (active && !armed) ctx.drawText(tr, "active", (int) (x + width - 5 - tr.getWidth("active")), (int) y + 4, Theme.MUTED, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!isHovered(mx, my)) return false;
            if (button == 0 && !name.equals(config().getActiveProfile())) {
                LoadReport report = config().loadProfile(name);
                toast("Profile loaded", name + (report.isClean() ? "" : " (" + report.getWarnings().size() + " warnings)"),
                    report.isClean() ? NotificationManager.Type.SUCCESS : NotificationManager.Type.WARNING);
                screen.onProfileChanged();
            } else if (button == 1 && !name.equals(config().getActiveProfile())) {
                if (System.currentTimeMillis() < armedUntil) {
                    if (config().deleteProfile(name)) toast("Profile deleted", name, NotificationManager.Type.INFO);
                    refresh();
                } else {
                    armedUntil = System.currentTimeMillis() + 3000;
                }
            }
            return true;
        }

        @Override
        public String getTooltip(double mouseX, double mouseY) {
            return "Left-click to load this profile. Right-click twice to delete it.";
        }
    }

    private final class Actions extends Component {
        private static final String[] LABELS = {"Save", "New", "Export", "Import", "Folder", "Reset", "Theme Out", "Theme In"};
        private static final String[] TIPS = {
            "Save the active profile now (it also autosaves)",
            "Create a new profile from the current settings",
            "Write the profile to config/aurora/exports and copy it to the clipboard",
            "Import a profile from JSON in the clipboard",
            "Open the config folder",
            "Reset every module of this profile to defaults (click twice)",
            "Export the ClickGUI and HUD Theme look to config/aurora/themes and the clipboard",
            "Import a look (ClickGUI and HUD Theme) from JSON in the clipboard"};
        private long resetArmedUntil;

        @Override
        public float getHeight() {
            return 52;
        }

        /** Two rows of three buttons, then a row of two (the theme buttons). */
        private int columns(int row) {
            return row < 2 ? 3 : 2;
        }

        private int indexAt(double mx, double my) {
            if (!isHovered(mx, my)) return -1;
            int row = (int) ((my - y - 2) / 16);
            if (row < 0 || row > 2) return -1;
            int col = (int) ((mx - x) / (width / columns(row)));
            int i = row * 3 + col;
            return i >= 0 && i < LABELS.length ? i : -1;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int hovered = screen.isMouseInBody() ? indexAt(mx, my) : -1;
            for (int i = 0; i < LABELS.length; i++) {
                int row = i / 3;
                float bw = width / columns(row);
                float bx = x + (i % 3) * bw + 1;
                float by = y + 2 + row * 16;
                boolean armed = i == 5 && System.currentTimeMillis() < resetArmedUntil;
                int bg = armed ? 0xFF7F1D1D : i == hovered ? ColorUtil.withAlpha(Theme.accent(), 110) : Theme.surface();
                Render2D.roundedRect(ctx, bx, by, bw - 2, 14, 3, bg);
                String label = armed ? "Sure?" : LABELS[i];
                ctx.drawText(tr, label, (int) (bx + (bw - 2 - tr.getWidth(label)) / 2), (int) by + 3, Theme.TEXT, false);
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            int i = indexAt(mx, my);
            if (i < 0 || button != 0) return i >= 0;
            ConfigManager cfg = config();
            MinecraftClient mc = MinecraftClient.getInstance();
            switch (i) {
                case 0 -> toast("Profile saved", cfg.getActiveProfile(),
                    cfg.save() ? NotificationManager.Type.SUCCESS : NotificationManager.Type.ERROR);
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
                case 6 -> {
                    try {
                        Path file = ThemeIO.exportTo(cfg.getRoot().resolve("themes"), screen.settings(), AuroraClient.modules().get(HudTheme.class));
                        mc.keyboard.setClipboard(ThemeIO.exportString(screen.settings(), AuroraClient.modules().get(HudTheme.class)));
                        toast("Theme exported", "themes/" + file.getFileName() + " + clipboard", NotificationManager.Type.SUCCESS);
                    } catch (IOException e) {
                        toast("Export failed", e.getMessage(), NotificationManager.Type.ERROR);
                    }
                }
                case 7 -> {
                    try {
                        int n = ThemeIO.importString(mc.keyboard.getClipboard(), screen.settings(), AuroraClient.modules().get(HudTheme.class));
                        toast("Theme imported", n + " values applied", NotificationManager.Type.SUCCESS);
                    } catch (IllegalArgumentException e) {
                        toast("Import failed", "Clipboard does not hold an Aurora theme", NotificationManager.Type.ERROR);
                    }
                }
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
                default -> {
                }
            }
            return true;
        }

        @Override
        public String getTooltip(double mx, double my) {
            int i = indexAt(mx, my);
            return i < 0 ? null : TIPS[i];
        }

        private String freeName(ConfigManager cfg, String prefix) {
            List<String> existing = cfg.listProfiles();
            int n = 1;
            while (existing.contains(prefix + n)) n++;
            return prefix + n;
        }
    }
}

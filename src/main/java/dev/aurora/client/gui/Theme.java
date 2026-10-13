package dev.aurora.client.gui;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.misc.ClickGui;
import dev.aurora.client.util.ColorUtil;

/** Central color palette derived from the ClickGUI module's theme settings. */
public final class Theme {
    public enum Preset {
        MIDNIGHT("Midnight", 0xFF0A0C13, 0xFF0D1018, 0xFF141824, 0xFF8B5CF6),
        OCEAN("Ocean", 0xFF080D15, 0xFF0B121C, 0xFF111B29, 0xFF38BDF8),
        SAKURA("Sakura", 0xFF110B10, 0xFF160F15, 0xFF21161F, 0xFFF472B6),
        EMERALD("Emerald", 0xFF080F0D, 0xFF0B1411, 0xFF12201B, 0xFF34D399),
        CRIMSON("Crimson", 0xFF100A0B, 0xFF150D0F, 0xFF201517, 0xFFF43F5E),
        GRAPHITE("Graphite", 0xFF0B0B0D, 0xFF101012, 0xFF19191C, 0xFFE5E7EB);

        private final String label;
        public final int background;
        public final int panel;
        public final int surface;
        public final int accent;

        Preset(String label, int background, int panel, int surface, int accent) {
            this.label = label;
            this.background = background;
            this.panel = panel;
            this.surface = surface;
            this.accent = accent;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final int TEXT = 0xFFF1F3F9;
    public static final int SUBTEXT = 0xFFA3AAC0;
    public static final int MUTED = 0xFF6B7287;

    private Theme() {
    }

    private static ClickGui gui() {
        AuroraClient client = AuroraClient.get();
        return client == null ? null : AuroraClient.modules().get(ClickGui.class);
    }

    private static Preset preset() {
        ClickGui g = gui();
        return g == null ? Preset.MIDNIGHT : g.getPreset();
    }

    private static float opacity() {
        ClickGui g = gui();
        return g == null ? 0.92f : g.getOpacity();
    }

    public static int accent() {
        ClickGui g = gui();
        return g == null ? Preset.MIDNIGHT.accent : g.getAccent();
    }

    public static int accent(long offsetMs) {
        ClickGui g = gui();
        return g == null ? Preset.MIDNIGHT.accent : g.getAccent(offsetMs);
    }

    public static int panel() {
        return ColorUtil.fade(preset().panel, opacity());
    }

    public static int header() {
        return ColorUtil.fade(preset().background, Math.min(1f, opacity() + 0.05f));
    }

    public static int surface() {
        return ColorUtil.fade(preset().surface, opacity());
    }

    public static int hover() {
        return ColorUtil.fade(ColorUtil.brighten(preset().surface, 0.12f), opacity());
    }

    public static int hudBackground() {
        return ColorUtil.withAlpha(preset().background, 150);
    }

    /** The preset's window, panel and card colors. */
    public static int windowColor() {
        return ColorUtil.fade(preset().panel, opacity());
    }

    public static int sidebarColor() {
        return ColorUtil.fade(preset().background, opacity());
    }

    public static int cardColor() {
        return ColorUtil.fade(preset().surface, opacity());
    }

    public static float radius() {
        ClickGui g = gui();
        return g == null ? 4f : g.getRadius();
    }
}

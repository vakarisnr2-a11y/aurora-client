package dev.aurora.client.gui;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.misc.ClickGui;
import dev.aurora.client.util.ColorUtil;

/** Central color palette derived from the ClickGUI module's theme settings. */
public final class Theme {
    public enum Preset {
        MIDNIGHT("Midnight", 0xFF0E1016, 0xFF151821, 0xFF1C2030, 0xFF8B5CF6),
        OCEAN("Ocean", 0xFF0A121C, 0xFF0F1A28, 0xFF15243A, 0xFF38BDF8),
        SAKURA("Sakura", 0xFF170F14, 0xFF21151C, 0xFF2D1C27, 0xFFF472B6),
        EMERALD("Emerald", 0xFF0B1411, 0xFF101C18, 0xFF162822, 0xFF34D399),
        CRIMSON("Crimson", 0xFF150D0E, 0xFF1F1214, 0xFF2B181B, 0xFFF43F5E),
        GRAPHITE("Graphite", 0xFF101010, 0xFF181818, 0xFF222222, 0xFFE5E7EB);

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

    public static final int TEXT = 0xFFECEEF4;
    public static final int SUBTEXT = 0xFF9BA1B5;
    public static final int MUTED = 0xFF6B7186;

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

    public static float radius() {
        ClickGui g = gui();
        return g == null ? 4f : g.getRadius();
    }
}

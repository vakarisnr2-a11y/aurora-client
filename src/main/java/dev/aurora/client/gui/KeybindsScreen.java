package dev.aurora.client.gui;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.keybind.KeybindConflicts;
import dev.aurora.client.keybind.KeybindConflicts.Binding;
import dev.aurora.client.module.Module;
import dev.aurora.client.gui.ui.Icons;
import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.KeyNames;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every Aurora keybind in one list, with warnings where a key is also used by another Aurora bind or a Minecraft
 * control. Click a key to rebind it (Backspace clears, Esc cancels), right-click to switch Toggle/Hold.
 */
public final class KeybindsScreen extends Screen {
    private enum Filter {
        ALL("All"), BOUND("Bound"), CONFLICTS("Conflicts");

        final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    private static final float ROW = 20;
    private static final float TOP = 74;

    private final Screen parent;
    private final List<Binding> aurora = new ArrayList<>();
    private final List<Binding> shown = new ArrayList<>();
    private Map<Binding, List<Binding>> conflicts = Map.of();
    private final Animation scroll = new Animation(0, 18);
    private double scrollTarget;
    private Filter filter = Filter.ALL;
    private KeybindSetting listening;

    public KeybindsScreen(Screen parent) {
        super(Text.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        List<Binding> all = new ArrayList<>();
        aurora.clear();
        for (Module m : AuroraClient.modules().getAll()) {
            for (Setting<?> s : m.getSettings()) {
                if (!(s instanceof KeybindSetting k) || k.isHidden()) continue;
                String label = k == m.getKeybind() ? m.getName() : m.getName() + " › " + k.getName();
                Binding b = new Binding(label, k.get(), false, k);
                aurora.add(b);
                all.add(b);
            }
        }
        if (client != null) {
            for (KeyBinding kb : client.options.allKeys) {
                int code = toCode(KeyBindingHelper.getBoundKeyOf(kb));
                if (code != KeybindSetting.NONE) all.add(new Binding(Text.translatable(kb.getTranslationKey()).getString(), code, true, kb));
            }
        }
        conflicts = KeybindConflicts.find(all);
        shown.clear();
        for (Binding b : aurora) {
            boolean keep = switch (filter) {
                case ALL -> true;
                case BOUND -> b.code() != KeybindSetting.NONE;
                case CONFLICTS -> conflicts.containsKey(b);
            };
            if (keep) shown.add(b);
        }
    }

    /** Converts a Minecraft key to Aurora's code (scancode-only keys cannot be compared and count as unbound). */
    static int toCode(InputUtil.Key key) {
        if (key == null || key.equals(InputUtil.UNKNOWN_KEY)) return KeybindSetting.NONE;
        return switch (key.getCategory()) {
            case KEYSYM -> key.getCode();
            case MOUSE -> KeybindSetting.MOUSE_OFFSET + key.getCode();
            default -> KeybindSetting.NONE;
        };
    }

    private float panelW() {
        return Math.min(520, width - 24);
    }

    private float panelX() {
        return (width - panelW()) / 2f;
    }

    private float panelY() {
        return 12;
    }

    private float listBottom() {
        return height - 40;
    }

    private float rowY(int i) {
        return TOP + i * ROW - scroll.getFloat();
    }

    private float keyX() {
        return panelX() + Math.max(150, panelW() * 0.40f);
    }

    private static final float KEY_W = 86;

    /** Filter tab rectangles, laid out from the left under the title. */
    private float tabX(Filter f) {
        float x = panelX() + 14;
        for (Filter g : Filter.values()) {
            if (g == f) return x;
            x += tabW(g) + 4;
        }
        return x;
    }

    private float tabW(Filter f) {
        return UiFont.width(tabLabel(f)) + 18;
    }

    private String tabLabel(Filter f) {
        return f.label + (f == Filter.CONFLICTS ? " (" + conflicts.size() + ")" : "");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        float px = panelX(), pw = panelW(), py = panelY();
        Ui.window(ctx, px, py, pw, height - py * 2, Ui.R_LARGE);
        Icons.draw(ctx, Icons.Icon.KEYBOARD, px + 14, py + 13, Ui.accentLight());
        UiFont.title(ctx, "Keybinds", px + 34, py + 12, Ui.TEXT);
        String summary = conflicts.isEmpty() ? "No conflicts" : conflicts.size() + " conflicting bind" + (conflicts.size() == 1 ? "" : "s");
        float sw = UiFont.width(summary) + 16;
        Render2D.roundedRect(ctx, px + pw - 14 - sw, py + 12, sw, 16, 8, conflicts.isEmpty() ? 0x2634D399 : 0x26FBBF24);
        UiFont.draw(ctx, summary, px + pw - 14 - sw + 8, py + 16, conflicts.isEmpty() ? Ui.SUCCESS : Ui.WARNING);
        for (Filter f : Filter.values()) {
            float tx = tabX(f), w = tabW(f);
            boolean hover = mouseX >= tx && mouseX < tx + w && mouseY >= py + 34 && mouseY < py + 52;
            boolean active = f == filter;
            Render2D.roundedRect(ctx, tx, py + 34, w, 18, 9, active ? Ui.accent(0xFF) : hover ? Ui.surfaceHover() : Ui.surface());
            UiFont.draw(ctx, tabLabel(f), tx + 9, py + 39, active ? 0xFFFFFFFF : Ui.SUBTEXT);
        }
        Ui.divider(ctx, px + 14, TOP - 6, pw - 28);

        double maxScroll = Math.max(0, shown.size() * ROW - (listBottom() - TOP));
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll.setTarget(scrollTarget);
        scroll.update();
        ctx.enableScissor((int) px, (int) TOP - 2, (int) (px + pw), (int) listBottom());
        String tooltip = null;
        for (int i = 0; i < shown.size(); i++) {
            float y = rowY(i);
            if (y + ROW < TOP || y > listBottom()) continue;
            Binding b = shown.get(i);
            KeybindSetting setting = (KeybindSetting) b.source();
            boolean hover = mouseY >= Math.max(TOP, y) && mouseY < Math.min(listBottom(), y + ROW) && mouseX >= px && mouseX < px + pw;
            if (hover) Render2D.roundedRect(ctx, px + 8, y, pw - 16, ROW, Ui.R_SMALL, 0x12FFFFFF);
            UiFont.draw(ctx, UiFont.trim(b.label(), (int) (keyX() - px - 26), UiFont.Size.BODY, false), px + 16, y + (ROW - 8) / 2f, Ui.TEXT);
            boolean isListening = listening == setting;
            String key = isListening ? "Press a key…" : KeyNames.of(setting.get()) + (setting.getMode() == KeybindSetting.Mode.HOLD ? " · Hold" : "");
            boolean overKey = hover && mouseX >= keyX() && mouseX < keyX() + KEY_W;
            Ui.field(ctx, keyX(), y + 2, KEY_W, ROW - 4, isListening ? 1 : overKey ? 0.5f : 0);
            int keyColor = isListening ? Ui.accentLight() : setting.isBound() ? Ui.TEXT : Ui.MUTED;
            String keyText = UiFont.trim(key, (int) KEY_W - 10, UiFont.Size.BODY, false);
            UiFont.draw(ctx, keyText, keyX() + (KEY_W - UiFont.width(keyText)) / 2f, y + (ROW - 8) / 2f, keyColor);
            List<Binding> others = conflicts.get(b);
            if (others != null) {
                String warn = KeybindConflicts.describe(others);
                float wx = keyX() + KEY_W + 10;
                Render2D.circle(ctx, wx + 3, y + ROW / 2f, 3, Ui.WARNING);
                int avail = (int) (px + pw - 16 - wx - 10);
                UiFont.draw(ctx, UiFont.trim(warn, avail, UiFont.Size.BODY, false), wx + 10, y + (ROW - 8) / 2f, Ui.WARNING);
                if (hover && mouseX >= wx) tooltip = warn;
            }
        }
        ctx.disableScissor();
        if (shown.isEmpty()) {
            String empty = filter == Filter.CONFLICTS ? "No conflicts" : "Nothing to show";
            UiFont.centered(ctx, empty, px + pw / 2, TOP + 14, Ui.MUTED, UiFont.Size.BODY, false);
        }
        String help = "Click a key to rebind  ·  Backspace clears  ·  Right-click: Toggle / Hold  ·  Esc: back";
        UiFont.draw(ctx, UiFont.trim(help, (int) pw - 28, UiFont.Size.BODY, false), px + 14, height - py - 20, Ui.MUTED);
        if (tooltip != null) ctx.drawTooltip(textRenderer, Text.literal(tooltip), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listening != null && button >= 2) {
            listening.set(KeybindSetting.MOUSE_OFFSET + button);
            listening = null;
            rebuild();
            return true;
        }
        for (Filter f : Filter.values()) {
            float tx = tabX(f), w = tabW(f);
            if (mouseX >= tx && mouseX < tx + w && mouseY >= panelY() + 34 && mouseY < panelY() + 52) {
                filter = f;
                scrollTarget = 0;
                rebuild();
                return true;
            }
        }
        if (mouseY >= TOP && mouseY < listBottom() && mouseX >= keyX() && mouseX < keyX() + KEY_W) {
            int i = (int) ((mouseY - TOP + scroll.get()) / ROW);
            if (i >= 0 && i < shown.size()) {
                KeybindSetting s = (KeybindSetting) shown.get(i).source();
                if (button == 0) listening = listening == s ? null : s;
                else if (button == 1) s.cycleMode();
                rebuild();
                return true;
            }
        }
        listening = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget -= verticalAmount * ROW * 2;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) listening.unbind();
            else if (keyCode != GLFW.GLFW_KEY_ESCAPE) listening.set(keyCode);
            listening = null;
            rebuild();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

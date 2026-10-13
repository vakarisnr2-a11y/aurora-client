package dev.aurora.client.gui;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.keybind.KeybindConflicts;
import dev.aurora.client.keybind.KeybindConflicts.Binding;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
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

    private static final float ROW = 16;
    private static final float TOP = 46;

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
        return Math.min(460, width - 20);
    }

    private float panelX() {
        return (width - panelW()) / 2f;
    }

    private float listBottom() {
        return height - 30;
    }

    private float rowY(int i) {
        return TOP + i * ROW - scroll.getFloat();
    }

    private float keyX() {
        return panelX() + panelW() * 0.42f;
    }

    private static final float KEY_W = 74;

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        float px = panelX(), pw = panelW();
        Render2D.shadow(ctx, px, 8, pw, height - 16, 6, 6, 0x90000000);
        Render2D.roundedRect(ctx, px, 8, pw, height - 16, 6, Theme.panel());
        ctx.drawText(textRenderer, "⌨ Keybinds", (int) px + 10, 15, Theme.accent(), false);
        String summary = conflicts.isEmpty() ? "No conflicts" : conflicts.size() + " conflicting bind" + (conflicts.size() == 1 ? "" : "s");
        ctx.drawText(textRenderer, summary, (int) (px + pw - 10 - textRenderer.getWidth(summary)), 15,
            conflicts.isEmpty() ? 0xFF4ADE80 : 0xFFFBBF24, false);
        // Filter tabs
        float tx = px + 10;
        for (Filter f : Filter.values()) {
            String label = f.label + (f == Filter.CONFLICTS ? " (" + conflicts.size() + ")" : "");
            float w = textRenderer.getWidth(label) + 12;
            boolean hover = mouseX >= tx && mouseX < tx + w && mouseY >= 27 && mouseY < 41;
            Render2D.roundedRect(ctx, tx, 27, w, 14, 4, f == filter ? ColorUtil.withAlpha(Theme.accent(), 150) : hover ? Theme.hover() : Theme.surface());
            ctx.drawText(textRenderer, label, (int) tx + 6, 30, Theme.TEXT, false);
            tx += w + 4;
        }

        double maxScroll = Math.max(0, shown.size() * ROW - (listBottom() - TOP));
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll.setTarget(scrollTarget);
        scroll.update();
        ctx.enableScissor((int) px, (int) TOP, (int) (px + pw), (int) listBottom());
        String tooltip = null;
        for (int i = 0; i < shown.size(); i++) {
            float y = rowY(i);
            if (y + ROW < TOP || y > listBottom()) continue;
            Binding b = shown.get(i);
            KeybindSetting setting = (KeybindSetting) b.source();
            boolean hover = mouseY >= Math.max(TOP, y) && mouseY < Math.min(listBottom(), y + ROW) && mouseX >= px && mouseX < px + pw;
            if (hover) Render2D.rect(ctx, px + 4, y, pw - 8, ROW, 0x18FFFFFF);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(b.label(), (int) (keyX() - px - 16)), (int) px + 10, (int) y + 4, Theme.TEXT, false);
            boolean isListening = listening == setting;
            String key = isListening ? "Press a key…" : KeyNames.of(setting.get()) + (setting.getMode() == KeybindSetting.Mode.HOLD ? " · Hold" : "");
            boolean overKey = hover && mouseX >= keyX() && mouseX < keyX() + KEY_W;
            Render2D.roundedRect(ctx, keyX(), y + 2, KEY_W, ROW - 4, 3,
                isListening ? ColorUtil.withAlpha(Theme.accent(), 120) : overKey ? Theme.hover() : Theme.surface());
            int keyColor = setting.isBound() || isListening ? Theme.TEXT : Theme.MUTED;
            ctx.drawText(textRenderer, textRenderer.trimToWidth(key, (int) KEY_W - 8), (int) (keyX() + 4), (int) y + 4, keyColor, false);
            List<Binding> others = conflicts.get(b);
            if (others != null) {
                String warn = "⚠ " + KeybindConflicts.describe(others);
                float wx = keyX() + KEY_W + 8;
                int avail = (int) (px + pw - 10 - wx);
                ctx.drawText(textRenderer, textRenderer.trimToWidth(warn, avail), (int) wx, (int) y + 4, 0xFFFBBF24, false);
                if (hover && mouseX >= wx) tooltip = warn;
            }
        }
        ctx.disableScissor();
        if (shown.isEmpty()) {
            String empty = filter == Filter.CONFLICTS ? "No conflicts" : "Nothing to show";
            ctx.drawText(textRenderer, empty, (int) (px + pw / 2 - textRenderer.getWidth(empty) / 2f), (int) TOP + 10, Theme.MUTED, false);
        }
        String help = "Click a key to rebind · Backspace clears · Right-click: Toggle/Hold · Esc: back";
        ctx.drawText(textRenderer, textRenderer.trimToWidth(help, (int) pw - 20), (int) px + 10, height - 22, Theme.SUBTEXT, false);
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
        float tx = panelX() + 10;
        for (Filter f : Filter.values()) {
            String label = f.label + (f == Filter.CONFLICTS ? " (" + conflicts.size() + ")" : "");
            float w = textRenderer.getWidth(label) + 12;
            if (mouseX >= tx && mouseX < tx + w && mouseY >= 27 && mouseY < 41) {
                filter = f;
                scrollTarget = 0;
                rebuild();
                return true;
            }
            tx += w + 4;
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

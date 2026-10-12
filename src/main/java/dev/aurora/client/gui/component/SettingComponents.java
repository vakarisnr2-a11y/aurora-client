package dev.aurora.client.gui.component;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.setting.StringSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import dev.aurora.client.util.KeyNames;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** Factory and implementations of the setting widgets (toggle, slider, dropdown, color picker, keybind, text). */
public final class SettingComponents {
    private SettingComponents() {
    }

    public static SettingComponent create(ClickGuiScreen screen, Setting<?> setting) {
        if (setting instanceof BooleanSetting s) return new Toggle(screen, s);
        if (setting instanceof NumberSetting s) return new Slider(screen, s);
        if (setting instanceof EnumSetting<?> s) return new Dropdown(screen, s);
        if (setting instanceof ColorSetting s) return new ColorPicker(screen, s);
        if (setting instanceof KeybindSetting s) return new Keybind(screen, s);
        if (setting instanceof StringSetting s) return new TextField(screen, s);
        throw new IllegalArgumentException("No component for " + setting.getClass().getSimpleName());
    }

    // ------------------------------------------------------------------ toggle switch

    static final class Toggle extends SettingComponent {
        private final BooleanSetting s;
        private final Animation knob;

        Toggle(ClickGuiScreen screen, BooleanSetting s) {
            super(screen, s);
            this.s = s;
            this.knob = new Animation(s.get() ? 1 : 0, 16);
        }

        @Override
        protected float fullHeight() {
            return 14;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            drawLabel(ctx, s.getName());
            knob.setTarget(s.get() ? 1 : 0);
            float k = (float) knob.update();
            float sw = 16, sh = 8;
            float sx = x + width - sw - 4, sy = y + 3;
            Render2D.roundedRect(ctx, sx, sy, sw, sh, 4, ColorUtil.lerp(0xFF3A3F52, Theme.accent(), k));
            Render2D.circle(ctx, sx + 4 + k * (sw - 8), sy + 4, 3, 0xFFF4F4F8);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && isHovered(mx, my)) {
                s.toggle();
                return true;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ slider

    static final class Slider extends SettingComponent {
        private final NumberSetting s;
        private final Animation fill;
        private boolean dragging;

        Slider(ClickGuiScreen screen, NumberSetting s) {
            super(screen, s);
            this.s = s;
            this.fill = new Animation(s.getProgress(), 18);
        }

        @Override
        protected float fullHeight() {
            return 20;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            if (dragging) s.setProgress((mx - (x + 4)) / (width - 8));
            drawLabel(ctx, s.getName());
            TextRenderer tr = font();
            String value = s.format();
            ctx.drawText(tr, value, (int) (x + width - 4 - tr.getWidth(value)), (int) y + 3, Theme.TEXT, false);
            fill.setTarget(s.getProgress());
            float p = (float) fill.update();
            float tx = x + 4, ty = y + 14, tw = width - 8;
            Render2D.roundedRect(ctx, tx, ty, tw, 3, 1.5f, 0xFF2C3040);
            Render2D.roundedRect(ctx, tx, ty, Math.max(3, tw * p), 3, 1.5f, ColorUtil.darken(Theme.accent(), 0.25f), Theme.accent());
            Render2D.circle(ctx, tx + tw * p, ty + 1.5f, dragging || isHovered(mx, my) ? 3.5f : 2.5f, 0xFFF4F4F8);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && isHovered(mx, my)) {
                dragging = true;
                s.setProgress((mx - (x + 4)) / (width - 8));
                return true;
            }
            return false;
        }

        @Override
        public void mouseReleased(double mx, double my, int button) {
            dragging = false;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double amount) {
            if (!isHovered(mx, my)) return false;
            s.increment(amount > 0 ? 1 : -1);
            return true;
        }
    }

    // ------------------------------------------------------------------ dropdown

    static final class Dropdown extends SettingComponent {
        private static final float OPTION_H = 12;
        private final EnumSetting<?> s;
        private final Animation open = new Animation(0, 16);
        private boolean expanded;

        Dropdown(ClickGuiScreen screen, EnumSetting<?> s) {
            super(screen, s);
            this.s = s;
        }

        @Override
        protected float fullHeight() {
            return 14 + s.getConstants().length * OPTION_H * open.getFloat();
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            open.setTarget(expanded ? 1 : 0);
            float o = (float) open.update();
            drawLabel(ctx, s.getName());
            TextRenderer tr = font();
            String value = s.getDisplay() + (expanded ? " ▴" : " ▾");
            ctx.drawText(tr, value, (int) (x + width - 4 - tr.getWidth(value)), (int) y + 3, Theme.accent(), false);
            if (o < 0.02f) return;
            Enum<?>[] options = s.getConstants();
            float listY = y + 14;
            Render2D.roundedRect(ctx, x + 4, listY, width - 8, options.length * OPTION_H * o, 3, ColorUtil.fade(0xFF0D0F15, 0.8f));
            ctx.enableScissor(screen.toScreen(x), screen.toScreen(listY), screen.toScreen(x + width), screen.toScreen(listY + options.length * OPTION_H * o));
            for (int i = 0; i < options.length; i++) {
                float oy = listY + i * OPTION_H;
                boolean selected = options[i] == s.get();
                boolean hover = mx >= x + 4 && mx < x + width - 4 && my >= oy && my < oy + OPTION_H;
                if (hover) Render2D.rect(ctx, x + 4, oy, width - 8, OPTION_H, 0x22FFFFFF);
                ctx.drawText(tr, EnumSetting.displayName(options[i]), (int) x + 9, (int) oy + 2,
                    selected ? Theme.accent() : Theme.SUBTEXT, false);
            }
            ctx.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!isHovered(mx, my)) return false;
            if (my < y + 14) {
                if (button == 0) expanded = !expanded;
                else if (button == 1) s.cycle(1);
                return true;
            }
            if (expanded) {
                int index = (int) ((my - (y + 14)) / OPTION_H);
                s.setIndex(index);
                expanded = false;
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ color picker

    static final class ColorPicker extends SettingComponent {
        private static final float PICKER_H = 72;
        private final ColorSetting s;
        private final Animation open = new Animation(0, 16);
        private final float[] hsb = new float[3];
        private boolean expanded;
        private int dragging; // 0 none, 1 square, 2 hue, 3 alpha

        ColorPicker(ClickGuiScreen screen, ColorSetting s) {
            super(screen, s);
            this.s = s;
            ColorUtil.toHsb(s.get(), hsb);
        }

        @Override
        protected float fullHeight() {
            return 14 + PICKER_H * open.getFloat();
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            open.setTarget(expanded ? 1 : 0);
            float o = (float) open.update();
            drawLabel(ctx, s.getName());
            float swX = x + width - 18;
            Render2D.roundedRect(ctx, swX - 0.5f, y + 2.5f, 14, 9, 2.5f, 0xFF000000);
            Render2D.roundedRect(ctx, swX, y + 3, 13, 8, 2, s.getColor());
            if (o < 0.02f) return;

            if (dragging != 0) drag(mx, my);
            else if (s.get() != hsbColor()) ColorUtil.toHsb(s.get(), hsb);

            ctx.enableScissor(screen.toScreen(x), screen.toScreen(y + 14), screen.toScreen(x + width), screen.toScreen(y + 14 + PICKER_H * o));
            float sqX = x + 4, sqY = y + 16, sqW = width - 26, sqH = 44;
            int hue = ColorUtil.hsb(hsb[0], 1, 1);
            // Saturation/brightness square: white-to-hue horizontally, then transparent-to-black vertically.
            Render2D.gradientH(ctx, sqX, sqY, sqW, sqH, 0xFFFFFFFF, hue);
            Render2D.gradientV(ctx, sqX, sqY, sqW, sqH, 0x00000000, 0xFF000000);
            Render2D.circle(ctx, sqX + hsb[1] * sqW, sqY + (1 - hsb[2]) * sqH, 2.5f, 0xFFFFFFFF);
            Render2D.circle(ctx, sqX + hsb[1] * sqW, sqY + (1 - hsb[2]) * sqH, 1.5f, s.get() | 0xFF000000);
            // Hue bar: six gradient segments.
            float hx = x + width - 18, hw = 14;
            for (int i = 0; i < 6; i++) {
                Render2D.gradientV(ctx, hx, sqY + sqH * i / 6f, hw, sqH / 6f, ColorUtil.hsb(i / 6f, 1, 1), ColorUtil.hsb((i + 1) / 6f, 1, 1));
            }
            Render2D.rect(ctx, hx - 1, sqY + hsb[0] * sqH - 1, hw + 2, 2, 0xFFFFFFFF);
            float rowY = sqY + sqH + 4;
            if (s.allowsAlpha()) {
                float aw = sqW;
                for (int i = 0; i < aw / 4; i++) {
                    Render2D.rect(ctx, sqX + i * 4, rowY, 4, 4, (i % 2 == 0) ? 0xFF8A8A8A : 0xFFCFCFCF);
                    Render2D.rect(ctx, sqX + i * 4, rowY + 4, 4, 4, (i % 2 == 0) ? 0xFFCFCFCF : 0xFF8A8A8A);
                }
                Render2D.gradientH(ctx, sqX, rowY, aw, 8, s.get() & 0x00FFFFFF, s.get() | 0xFF000000);
                Render2D.rect(ctx, sqX + ColorUtil.alpha(s.get()) / 255f * aw - 1, rowY - 1, 2, 10, 0xFFFFFFFF);
            }
            TextRenderer tr = font();
            String rb = (s.isRainbow() ? "■ " : "□ ") + "Rainbow";
            ctx.drawText(tr, rb, (int) (x + width - 4 - tr.getWidth(rb)), (int) rowY, s.isRainbow() ? Theme.accent() : Theme.SUBTEXT, false);
            ctx.disableScissor();
        }

        private int hsbColor() {
            return ColorUtil.withAlpha(ColorUtil.hsb(hsb[0], hsb[1], hsb[2]), ColorUtil.alpha(s.get()));
        }

        private void drag(double mx, double my) {
            float sqX = x + 4, sqY = y + 16, sqW = width - 26, sqH = 44;
            switch (dragging) {
                case 1 -> {
                    hsb[1] = (float) Math.max(0, Math.min(1, (mx - sqX) / sqW));
                    hsb[2] = (float) Math.max(0, Math.min(1, 1 - (my - sqY) / sqH));
                    s.set(hsbColor());
                }
                case 2 -> {
                    hsb[0] = (float) Math.max(0, Math.min(0.999, (my - sqY) / sqH));
                    s.set(hsbColor());
                }
                case 3 -> {
                    int alpha = (int) Math.round(Math.max(0, Math.min(1, (mx - sqX) / sqW)) * 255);
                    s.set(ColorUtil.withAlpha(s.get(), alpha));
                }
                default -> {
                }
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!isHovered(mx, my)) return false;
            if (my < y + 14) {
                expanded = !expanded;
                return true;
            }
            if (!isFullyShown() || open.get() < 0.9) return true;
            float sqX = x + 4, sqY = y + 16, sqW = width - 26, sqH = 44;
            float rowY = sqY + sqH + 4;
            if (mx >= sqX && mx <= sqX + sqW && my >= sqY && my <= sqY + sqH) dragging = 1;
            else if (mx >= x + width - 18 && mx <= x + width - 4 && my >= sqY && my <= sqY + sqH) dragging = 2;
            else if (s.allowsAlpha() && mx >= sqX && mx <= sqX + sqW && my >= rowY && my <= rowY + 8) dragging = 3;
            else if (my >= rowY && my <= rowY + 9 && mx > x + width / 2f) s.setRainbow(!s.isRainbow());
            if (dragging != 0) drag(mx, my);
            return true;
        }

        @Override
        public void mouseReleased(double mx, double my, int button) {
            dragging = 0;
        }
    }

    // ------------------------------------------------------------------ keybind

    static final class Keybind extends SettingComponent {
        private final KeybindSetting s;

        Keybind(ClickGuiScreen screen, KeybindSetting s) {
            super(screen, s);
            this.s = s;
        }

        @Override
        protected float fullHeight() {
            return 14;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            drawLabel(ctx, s.getName());
            boolean listening = screen.getListening() == this;
            String text = listening ? "Press a key…" : KeyNames.of(s.get())
                + (s.getMode() == KeybindSetting.Mode.HOLD ? " · Hold" : "");
            TextRenderer tr = font();
            int tw = tr.getWidth(text);
            float bx = x + width - tw - 10;
            Render2D.roundedRect(ctx, bx, y + 1.5f, tw + 6, 11, 3, listening ? ColorUtil.withAlpha(Theme.accent(), 90) : 0xFF262A38);
            ctx.drawText(tr, text, (int) bx + 3, (int) y + 3, listening ? Theme.TEXT : Theme.accent(), false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (screen.getListening() == this) {
                // Mouse buttons other than left/right can be bound while listening.
                if (button >= 2) {
                    s.set(KeybindSetting.MOUSE_OFFSET + button);
                    screen.setListening(null);
                    return true;
                }
            }
            if (!isHovered(mx, my)) return false;
            if (button == 0) screen.setListening(screen.getListening() == this ? null : this);
            else if (button == 1) s.cycleMode();
            return true;
        }

        @Override
        public boolean keyPressed(int key, int scancode, int modifiers) {
            if (screen.getListening() != this) return false;
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) s.unbind();
            else if (key != GLFW.GLFW_KEY_ESCAPE) s.set(key);
            screen.setListening(null);
            return true;
        }

        @Override
        public String getTooltip(double mouseX, double mouseY) {
            return s.getDescription() + " Backspace clears it.";
        }
    }

    // ------------------------------------------------------------------ text field

    static final class TextField extends SettingComponent {
        private final StringSetting s;
        private final TextInput input;

        TextField(ClickGuiScreen screen, StringSetting s) {
            super(screen, s);
            this.s = s;
            this.input = new TextInput(s.getMaxLength());
        }

        @Override
        protected float fullHeight() {
            return 28;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            drawLabel(ctx, s.getName());
            boolean focused = screen.getListening() == this;
            if (!focused) input.set(s.get());
            float fx = x + 4, fy = y + 13, fw = width - 8;
            Render2D.roundedRect(ctx, fx, fy, fw, 12, 3, 0xFF0D0F15);
            if (focused) Render2D.roundedOutline(ctx, fx, fy, fw, 12, 3, 1, Theme.accent());
            TextRenderer tr = font();
            String shown = tr.trimToWidth(input.get(), (int) fw - 8, true);
            boolean caret = focused && (System.currentTimeMillis() / 500) % 2 == 0;
            ctx.drawText(tr, shown + (caret ? "_" : ""), (int) fx + 4, (int) fy + 2, Theme.TEXT, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (isHovered(mx, my)) {
                screen.setListening(this);
                return true;
            }
            if (screen.getListening() == this) screen.setListening(null);
            return false;
        }

        @Override
        public boolean keyPressed(int key, int scancode, int modifiers) {
            if (screen.getListening() != this) return false;
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_TAB) {
                screen.setListening(null);
                return true;
            }
            if (input.keyPressed(key)) s.set(input.get());
            return true;
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (screen.getListening() != this) return false;
            if (input.charTyped(chr)) s.set(input.get());
            return true;
        }
    }
}

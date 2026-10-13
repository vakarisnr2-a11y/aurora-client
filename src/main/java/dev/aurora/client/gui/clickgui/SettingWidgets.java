package dev.aurora.client.gui.clickgui;

import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.component.TextInput;
import dev.aurora.client.gui.ui.Icons;
import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.StringSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import dev.aurora.client.util.KeyNames;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** The setting controls: switch, slider, segmented choice / dropdown, color picker, keybind and text field. */
public final class SettingWidgets {
    private static final float ROW = 26;

    private SettingWidgets() {
    }

    // ------------------------------------------------------------------ switch

    static final class Toggle extends SettingRow {
        private final BooleanSetting s;
        private final Animation knob;

        Toggle(ClickGuiScreen screen, BooleanSetting s) {
            super(screen, s);
            this.s = s;
            this.knob = new Animation(s.get() ? 1 : 0, 16);
        }

        @Override
        protected float fullHeight() {
            return ROW;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            drawLabel(ctx, mx, my, ROW);
            knob.setSpeed(16 * screen.animationSpeed());
            knob.setTarget(s.get() ? 1 : 0);
            Ui.toggle(ctx, x + width - Ui.SWITCH_W - 4, y + (ROW - Ui.SWITCH_H) / 2f, (float) knob.update());
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

    static final class Slider extends SettingRow {
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
            return 34;
        }

        private float trackX() {
            return x + 4;
        }

        private float trackW() {
            return width - 8;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            if (dragging) s.setProgress((mx - trackX()) / trackW());
            drawLabel(ctx, mx, my, 22);
            String value = s.format();
            float vw = UiFont.width(value) + 10;
            Render2D.roundedRect(ctx, x + width - 4 - vw, y + 5, vw, 13, Ui.R_SMALL, Ui.accent(0x30));
            UiFont.draw(ctx, value, x + width - 4 - vw + 5, y + 7.5f, Ui.accentLight());
            fill.setSpeed(18 * screen.animationSpeed());
            fill.setTarget(s.getProgress());
            Ui.slider(ctx, trackX(), y + 25, trackW(), (float) fill.update(), dragging || isHovered(mx, my) ? 1 : 0);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && isHovered(mx, my)) {
                dragging = true;
                s.setProgress((mx - trackX()) / trackW());
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

    // ------------------------------------------------------------------ segmented control / dropdown

    static final class Choice extends SettingRow {
        private static final float OPTION_H = 18;
        private final EnumSetting<?> s;
        private final Animation open = new Animation(0, 16);
        private final Animation selection = new Animation(-1, 18);
        private boolean expanded;

        Choice(ClickGuiScreen screen, EnumSetting<?> s) {
            super(screen, s);
            this.s = s;
        }

        /** Few short options are shown side by side; otherwise a dropdown. */
        private boolean segmented() {
            Enum<?>[] options = s.getConstants();
            if (options.length > 3) return false;
            float total = 0;
            for (Enum<?> o : options) total += UiFont.width(EnumSetting.displayName(o)) + 14;
            return total <= width * 0.6f;
        }

        @Override
        protected float fullHeight() {
            return ROW + (segmented() ? 0 : s.getConstants().length * OPTION_H * open.getFloat() + (open.get() > 0.01 ? 4 : 0));
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            open.setSpeed(16 * screen.animationSpeed());
            open.setTarget(expanded && !segmented() ? 1 : 0);
            open.update();
            drawLabel(ctx, mx, my, ROW);
            if (segmented()) renderSegments(ctx, mx, my);
            else renderDropdown(ctx, mx, my);
        }

        private float segmentsX() {
            float total = 0;
            for (Enum<?> o : s.getConstants()) total += UiFont.width(EnumSetting.displayName(o)) + 14;
            return x + width - 4 - total;
        }

        private void renderSegments(DrawContext ctx, double mx, double my) {
            Enum<?>[] options = s.getConstants();
            float sx = segmentsX();
            float total = x + width - 4 - sx;
            float sy = y + 5;
            Render2D.roundedRect(ctx, sx, sy, total, 16, 8, Ui.field());
            float cx = sx;
            float selX = sx, selW = 0;
            for (Enum<?> o : options) {
                float w = UiFont.width(EnumSetting.displayName(o)) + 14;
                if (o == s.get()) {
                    selX = cx;
                    selW = w;
                }
                cx += w;
            }
            if (selection.get() < 0) selection.snap(selX);
            selection.setSpeed(18 * screen.animationSpeed());
            selection.setTarget(selX);
            float animX = (float) selection.update();
            Render2D.roundedRect(ctx, animX + 1, sy + 1, selW - 2, 14, 7, Ui.accent(0xFF));
            cx = sx;
            for (Enum<?> o : options) {
                String label = EnumSetting.displayName(o);
                float w = UiFont.width(label) + 14;
                boolean hover = mx >= cx && mx < cx + w && my >= sy && my < sy + 16;
                int color = o == s.get() ? 0xFFFFFFFF : hover ? Ui.TEXT : Ui.SUBTEXT;
                UiFont.draw(ctx, label, cx + 7, sy + 4, color);
                cx += w;
            }
        }

        private void renderDropdown(DrawContext ctx, double mx, double my) {
            String value = s.getDisplay();
            float fw = Math.max(70, UiFont.width(value) + 28);
            float fx = x + width - 4 - fw;
            float fy = y + 4;
            boolean hover = mx >= fx && mx < fx + fw && my >= fy && my < fy + 18;
            Ui.field(ctx, fx, fy, fw, 18, expanded ? 1 : hover ? 0.5f : 0);
            UiFont.draw(ctx, value, fx + 8, fy + 5, Ui.TEXT);
            float chevron = fx + fw - 14;
            float flip = open.getFloat();
            Render2D.line(ctx, chevron, fy + 7 + flip * 3, chevron + 3, fy + 10 - flip * 3, 1.3f, Ui.SUBTEXT);
            Render2D.line(ctx, chevron + 3, fy + 10 - flip * 3, chevron + 6, fy + 7 + flip * 3, 1.3f, Ui.SUBTEXT);
            float o = open.getFloat();
            if (o < 0.02f) return;
            Enum<?>[] options = s.getConstants();
            float listY = y + ROW;
            float listH = options.length * OPTION_H * o;
            Render2D.roundedRect(ctx, x + 4, listY, width - 8, listH + 2, Ui.R_MEDIUM, Ui.field());
            ctx.enableScissor(screen.toScreenX(x), screen.toScreenY(listY), screen.toScreenX(x + width), screen.toScreenY(listY + listH + 2));
            for (int i = 0; i < options.length; i++) {
                float oy = listY + 1 + i * OPTION_H;
                boolean selected = options[i] == s.get();
                boolean optHover = mx >= x + 4 && mx < x + width - 4 && my >= oy && my < oy + OPTION_H;
                if (optHover || selected) {
                    Render2D.roundedRect(ctx, x + 6, oy + 1, width - 12, OPTION_H - 2, Ui.R_SMALL,
                        selected ? Ui.accent(0x40) : 0x14FFFFFF);
                }
                UiFont.draw(ctx, EnumSetting.displayName(options[i]), x + 12, oy + 5, selected ? Ui.accentLight() : Ui.SUBTEXT);
                if (selected) Icons.draw(ctx, Icons.Icon.CHECK, x + width - 24, oy + 2, Ui.accentLight());
            }
            ctx.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!isHovered(mx, my)) {
                if (expanded && button >= 0) expanded = false; // click outside closes the list
                return false;
            }
            if (segmented()) {
                float cx = segmentsX();
                for (Enum<?> o : s.getConstants()) {
                    float w = UiFont.width(EnumSetting.displayName(o)) + 14;
                    if (mx >= cx && mx < cx + w && my >= y + 5 && my < y + 21) {
                        s.setIndex(o.ordinal());
                        return true;
                    }
                    cx += w;
                }
                if (button == 1) s.cycle(1);
                return true;
            }
            if (my < y + ROW) {
                if (button == 0) expanded = !expanded;
                else if (button == 1) s.cycle(1);
                return true;
            }
            if (expanded) {
                int index = (int) ((my - (y + ROW + 1)) / OPTION_H);
                s.setIndex(index);
                expanded = false;
            }
            return true;
        }
    }

    // ------------------------------------------------------------------ color picker

    static final class ColorPicker extends SettingRow {
        private static final float PICKER_H = 92;
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
            return ROW + PICKER_H * open.getFloat();
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            open.setSpeed(16 * screen.animationSpeed());
            open.setTarget(expanded ? 1 : 0);
            float o = (float) open.update();
            drawLabel(ctx, mx, my, ROW);
            String hex = ColorSetting.toHex(s.get()).substring(s.allowsAlpha() ? 0 : 3);
            if (!s.allowsAlpha()) hex = "#" + hex;
            float swX = x + width - 4 - 22;
            UiFont.draw(ctx, s.isRainbow() ? "Rainbow" : hex, swX - 6 - UiFont.width(s.isRainbow() ? "Rainbow" : hex), y + 9, Ui.SUBTEXT);
            Render2D.roundedRect(ctx, swX, y + 6, 22, 14, Ui.R_SMALL, 0xFF000000);
            Render2D.roundedRect(ctx, swX + 1, y + 7, 20, 12, Ui.R_SMALL - 1, s.getColor());
            Render2D.roundedOutline(ctx, swX, y + 6, 22, 14, Ui.R_SMALL, 1, Ui.BORDER_STRONG);
            if (o < 0.02f) return;

            if (dragging != 0) drag(mx, my);
            else if (s.get() != hsbColor()) ColorUtil.toHsb(s.get(), hsb);

            ctx.enableScissor(screen.toScreenX(x), screen.toScreenY(y + ROW), screen.toScreenX(x + width), screen.toScreenY(y + ROW + PICKER_H * o));
            float sqX = x + 4, sqY = y + ROW + 2, sqW = width - 36, sqH = 60;
            int hue = ColorUtil.hsb(hsb[0], 1, 1);
            Render2D.gradientH(ctx, sqX, sqY, sqW, sqH, 0xFFFFFFFF, hue);
            Render2D.gradientV(ctx, sqX, sqY, sqW, sqH, 0x00000000, 0xFF000000);
            Render2D.roundedOutline(ctx, sqX, sqY, sqW, sqH, 2, 1, Ui.BORDER);
            float px = sqX + hsb[1] * sqW, py = sqY + (1 - hsb[2]) * sqH;
            Render2D.circle(ctx, px, py, 3.5f, 0xFFFFFFFF);
            Render2D.circle(ctx, px, py, 2.5f, s.get() | 0xFF000000);
            float hx = x + width - 26, hw = 22;
            for (int i = 0; i < 6; i++) {
                Render2D.gradientV(ctx, hx, sqY + sqH * i / 6f, hw, sqH / 6f, ColorUtil.hsb(i / 6f, 1, 1), ColorUtil.hsb((i + 1) / 6f, 1, 1));
            }
            Render2D.roundedOutline(ctx, hx - 1, sqY + hsb[0] * sqH - 2, hw + 2, 4, 1.5f, 1, 0xFFFFFFFF);
            float rowY = sqY + sqH + 8;
            if (s.allowsAlpha()) {
                float aw = sqW;
                for (int i = 0; i < aw / 4; i++) {
                    Render2D.rect(ctx, sqX + i * 4, rowY, 4, 4, (i % 2 == 0) ? 0xFF8A8A8A : 0xFFCFCFCF);
                    Render2D.rect(ctx, sqX + i * 4, rowY + 4, 4, 4, (i % 2 == 0) ? 0xFFCFCFCF : 0xFF8A8A8A);
                }
                Render2D.gradientH(ctx, sqX, rowY, aw, 8, s.get() & 0x00FFFFFF, s.get() | 0xFF000000);
                Render2D.roundedOutline(ctx, sqX + ColorUtil.alpha(s.get()) / 255f * aw - 2, rowY - 2, 4, 12, 1.5f, 1, 0xFFFFFFFF);
            }
            float rbX = x + width - 26;
            boolean rb = s.isRainbow();
            Render2D.roundedRect(ctx, rbX, rowY - 2, 22, 12, 6, rb ? Ui.accent(0xFF) : 0xFF2A3042);
            Render2D.circle(ctx, rbX + 6 + (rb ? 10 : 0), rowY + 4, 4, 0xFFF7F8FC);
            ctx.disableScissor();
        }

        private int hsbColor() {
            return ColorUtil.withAlpha(ColorUtil.hsb(hsb[0], hsb[1], hsb[2]), ColorUtil.alpha(s.get()));
        }

        private void drag(double mx, double my) {
            float sqX = x + 4, sqY = y + ROW + 2, sqW = width - 36, sqH = 60;
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
                case 3 -> s.set(ColorUtil.withAlpha(s.get(), (int) Math.round(Math.max(0, Math.min(1, (mx - sqX) / sqW)) * 255)));
                default -> {
                }
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!isHovered(mx, my)) return false;
            if (my < y + ROW) {
                expanded = !expanded;
                return true;
            }
            if (!isFullyShown() || open.get() < 0.9) return true;
            float sqX = x + 4, sqY = y + ROW + 2, sqW = width - 36, sqH = 60;
            float rowY = sqY + sqH + 8;
            if (mx >= sqX && mx <= sqX + sqW && my >= sqY && my <= sqY + sqH) dragging = 1;
            else if (mx >= x + width - 26 && mx <= x + width - 4 && my >= sqY && my <= sqY + sqH) dragging = 2;
            else if (s.allowsAlpha() && mx >= sqX && mx <= sqX + sqW && my >= rowY && my <= rowY + 8) dragging = 3;
            else if (my >= rowY - 2 && my <= rowY + 10 && mx >= x + width - 26) s.setRainbow(!s.isRainbow());
            if (dragging != 0) drag(mx, my);
            return true;
        }

        @Override
        public void mouseReleased(double mx, double my, int button) {
            dragging = 0;
        }

        @Override
        public String getTooltip(double mouseX, double mouseY) {
            return expanded ? s.getDescription() + " (switch on the right: rainbow)" : super.getTooltip(mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ keybind

    static final class Keybind extends SettingRow {
        private final KeybindSetting s;

        Keybind(ClickGuiScreen screen, KeybindSetting s) {
            super(screen, s);
            this.s = s;
        }

        @Override
        protected float fullHeight() {
            return ROW;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            drawLabel(ctx, mx, my, ROW);
            boolean listening = screen.getListening() == this;
            String key = listening ? "Press a key…" : KeyNames.of(s.get());
            String mode = s.getMode() == KeybindSetting.Mode.HOLD ? "Hold" : "Toggle";
            float modeW = UiFont.width(mode) + 12;
            float keyW = Math.max(54, UiFont.width(key) + 16);
            float mxPos = x + width - 4 - modeW;
            float kx = mxPos - 4 - keyW;
            boolean keyHover = mx >= kx && mx < kx + keyW && my >= y + 4 && my < y + 22;
            Ui.field(ctx, kx, y + 4, keyW, 18, listening ? 1 : keyHover ? 0.5f : 0);
            UiFont.draw(ctx, key, kx + (keyW - UiFont.width(key)) / 2f, y + 9, listening ? Ui.accentLight() : s.isBound() ? Ui.TEXT : Ui.MUTED);
            Render2D.roundedRect(ctx, mxPos, y + 4, modeW, 18, Ui.R_MEDIUM, Ui.accent(s.getMode() == KeybindSetting.Mode.HOLD ? 0x50 : 0x22));
            UiFont.draw(ctx, mode, mxPos + 6, y + 9, Ui.SUBTEXT);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (screen.getListening() == this && button >= 2) {
                s.set(KeybindSetting.MOUSE_OFFSET + button);
                screen.setListening(null);
                return true;
            }
            if (!isHovered(mx, my)) return false;
            String mode = s.getMode() == KeybindSetting.Mode.HOLD ? "Hold" : "Toggle";
            float modeX = x + width - 4 - (UiFont.width(mode) + 12);
            if (button == 1 || mx >= modeX) s.cycleMode();
            else if (button == 0) screen.setListening(screen.getListening() == this ? null : this);
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

    static final class TextField extends SettingRow {
        private final StringSetting s;
        private final TextInput input;

        TextField(ClickGuiScreen screen, StringSetting s) {
            super(screen, s);
            this.s = s;
            this.input = new TextInput(s.getMaxLength());
        }

        @Override
        protected float fullHeight() {
            return 44;
        }

        @Override
        public void render(DrawContext ctx, double mx, double my, float delta) {
            drawLabel(ctx, mx, my, 20);
            boolean focused = screen.getListening() == this;
            if (!focused) input.set(s.get());
            float fx = x + 4, fy = y + 21, fw = width - 8;
            Ui.field(ctx, fx, fy, fw, 18, focused ? 1 : 0);
            String text = input.get();
            String shown = text;
            while (UiFont.width(shown) > fw - 14 && !shown.isEmpty()) shown = shown.substring(1);
            boolean caret = focused && (System.currentTimeMillis() / 500) % 2 == 0;
            if (shown.isEmpty() && !focused) UiFont.draw(ctx, "Click to type…", fx + 7, fy + 5, Ui.MUTED);
            else UiFont.draw(ctx, shown + (caret ? "|" : ""), fx + 7, fy + 5, Ui.TEXT);
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

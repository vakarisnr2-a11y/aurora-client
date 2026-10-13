package dev.aurora.client.gui;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.component.TextInput;
import dev.aurora.client.hud.HudLayouts;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudRenderer;
import dev.aurora.client.hud.HudSnapper;
import dev.aurora.client.module.modules.misc.HudEditor;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.List;

/**
 * Drag HUD elements with the mouse. Elements snap to screen edges, centre lines, other elements and (optionally) a
 * grid, with alignment guides; hold Shift to move freely. A dropped element is anchored to the part of the screen it
 * is in, so it keeps its distance to that edge when the window is resized. The toolbar toggles snapping, shows or
 * hides elements, saves and loads layouts and resets everything.
 */
public final class HudEditorScreen extends Screen {
    private static final float SNAP = 5;
    private static final float BAR_Y = 6;
    private static final float BTN_H = 14;
    private static final float MENU_W = 140;
    private static final float ROW_H = 12;
    private static final int B_GRID = 0, B_SNAP = 1, B_ELEMENTS = 2, B_LAYOUTS = 3, B_RESET = 4, B_DONE = 5;

    private enum Menu {
        NONE, ELEMENTS, LAYOUTS
    }

    private final Screen parent;
    private final HudEditor options = AuroraClient.modules().get(HudEditor.class);
    private final HudLayouts layouts = new HudLayouts(AuroraClient.config().getRoot().resolve("layouts"));
    private final HudSnapper.Result snap = new HudSnapper.Result();
    private final String[] buttonLabels = new String[6];
    private final float[] buttonX = new float[6];
    private final float[] buttonW = new float[6];
    private final TextInput nameInput = new TextInput(32);
    private float[] others = new float[64];
    private HudModule dragging;
    private HudModule selected;
    private double offsetX;
    private double offsetY;
    private float guideX = Float.NaN;
    private float guideY = Float.NaN;
    private Menu menu = Menu.NONE;
    private List<String> layoutNames = List.of();
    private boolean naming;
    private String armedDelete;
    private long armedUntil;
    private long resetArmedUntil;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    private static List<HudModule> elements() {
        return AuroraClient.hud().getElements();
    }

    private HudModule elementAt(double mx, double my) {
        List<HudModule> list = elements();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule h = list.get(i);
            if (!h.isEnabled()) continue;
            float x = h.getX(width), y = h.getY(height);
            if (mx >= x - 2 && mx <= x + h.getScaledWidth() + 2 && my >= y - 2 && my <= y + h.getScaledHeight() + 2) return h;
        }
        return null;
    }

    private double mouseX() {
        return client == null ? 0 : client.mouse.getX() * width / client.getWindow().getWidth();
    }

    private double mouseY() {
        return client == null ? 0 : client.mouse.getY() * height / client.getWindow().getHeight();
    }

    private static void toast(String title, String message, NotificationManager.Type type) {
        AuroraClient.notifications().post(title, message, type, HudEditorScreen.class);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x55000000);
        if (options.showGrid()) {
            int grid = options.gridSize();
            int color = options.snapGrid().get() ? 0x16FFFFFF : 0x0CFFFFFF;
            for (int x = 0; x < width; x += grid) ctx.fill(x, 0, x + 1, height, color);
            for (int y = 0; y < height; y += grid) ctx.fill(0, y, width, y + 1, color);
        }
        ctx.fill(width / 2, 0, width / 2 + 1, height, 0x22FFFFFF);
        ctx.fill(0, height / 2, width, height / 2 + 1, 0x22FFFFFF);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        if (dragging != null) drag(mouseX, mouseY);
        boolean overUi = isOverToolbar(mouseX, mouseY) || isOverMenu(mouseX, mouseY);
        HudModule hovered = dragging != null ? dragging : overUi ? null : elementAt(mouseX, mouseY);
        HudModule focus = hovered != null ? hovered : selected;
        for (HudModule h : elements()) {
            if (!h.isEnabled()) continue;
            HudRenderer.renderElement(ctx, h, delta, width, height, true);
            float x = h.getX(width), y = h.getY(height);
            float w = h.getScaledWidth(), hh = h.getScaledHeight();
            int color = h == hovered ? Theme.accent() : h == selected ? ColorUtil.withAlpha(Theme.accent(), 140) : 0x50FFFFFF;
            Render2D.roundedOutline(ctx, x - 2, y - 2, w + 4, hh + 4, 3, 1, color);
        }
        if (focus != null && focus.isEnabled()) {
            drawAnchor(ctx, focus);
            drawLabel(ctx, focus);
        }
        int guide = ColorUtil.withAlpha(Theme.accent(), 190);
        if (!Float.isNaN(guideX)) Render2D.rect(ctx, guideX - 0.5f, 0, 1, height, guide);
        if (!Float.isNaN(guideY)) Render2D.rect(ctx, 0, guideY - 0.5f, width, 1, guide);

        renderToolbar(ctx, mouseX, mouseY);
        renderMenu(ctx, mouseX, mouseY);

        String help = "Drag: move · Shift: free · Scroll/+/-: size · Arrows: nudge · R: reset · Right-click: hide · G: grid · Esc: done";
        if (textRenderer.getWidth(help) + 16 > width) help = "Drag · Scroll: size · R: reset · Right-click: hide";
        int hw = textRenderer.getWidth(help) + 16;
        Render2D.roundedRect(ctx, width / 2f - hw / 2f, height - 22, hw, 16, 8, Theme.panel());
        ctx.drawText(textRenderer, help, width / 2 - hw / 2 + 8, height - 18, Theme.SUBTEXT, false);
    }

    private void drawLabel(DrawContext ctx, HudModule h) {
        float x = h.getX(width), y = h.getY(height);
        String label = h.getName() + " · " + Math.round(h.getScale() * 100) + "% · " + h.getAnchor();
        int lw = textRenderer.getWidth(label);
        float lx = Math.max(0, Math.min(width - lw - 8, x - 2));
        float ly = y - 14 < BAR_Y + BTN_H + 4 ? y + h.getScaledHeight() + 4 : y - 14;
        Render2D.roundedRect(ctx, lx, ly, lw + 8, 11, 3, ColorUtil.withAlpha(Theme.accent(), 210));
        ctx.drawText(textRenderer, label, (int) lx + 4, (int) ly + 2, 0xFF0E0F14, false);
    }

    /** Dashed lines from the element to the edges (or centre lines) it is anchored to. */
    private void drawAnchor(DrawContext ctx, HudModule h) {
        HudModule.Anchor a = h.getAnchor();
        if (a == HudModule.Anchor.AUTO) return;
        float x = h.getX(width), y = h.getY(height), w = h.getScaledWidth(), hh = h.getScaledHeight();
        float cx = x + w / 2, cy = y + hh / 2;
        int color = ColorUtil.withAlpha(Theme.accent(), 150);
        switch (a.h) {
            case 0 -> dashH(ctx, 0, x - 2, cy, color);
            case 2 -> dashH(ctx, x + w + 2, width, cy, color);
            default -> dashH(ctx, Math.min(cx, width / 2f), Math.max(cx, width / 2f), cy, color);
        }
        switch (a.v) {
            case 0 -> dashV(ctx, cx, 0, y - 2, color);
            case 2 -> dashV(ctx, cx, y + hh + 2, height, color);
            default -> dashV(ctx, cx, Math.min(cy, height / 2f), Math.max(cy, height / 2f), color);
        }
    }

    private static void dashH(DrawContext ctx, float from, float to, float y, int color) {
        for (float x = from; x < to; x += 6) Render2D.rect(ctx, x, y - 0.5f, Math.min(3, to - x), 1, color);
    }

    private static void dashV(DrawContext ctx, float x, float from, float to, int color) {
        for (float y = from; y < to; y += 6) Render2D.rect(ctx, x - 0.5f, y, 1, Math.min(3, to - y), color);
    }

    private void layoutToolbar() {
        buttonLabels[B_GRID] = "Grid: " + (options.snapGrid().get() ? "On" : "Off");
        buttonLabels[B_SNAP] = "Snap: " + (options.snapElements().get() ? "On" : "Off");
        buttonLabels[B_ELEMENTS] = "Elements " + (menu == Menu.ELEMENTS ? "▴" : "▾");
        buttonLabels[B_LAYOUTS] = "Layouts " + (menu == Menu.LAYOUTS ? "▴" : "▾");
        buttonLabels[B_RESET] = System.currentTimeMillis() < resetArmedUntil ? "Sure?" : "Reset All";
        buttonLabels[B_DONE] = "Done";
        float total = 0;
        for (int i = 0; i < buttonLabels.length; i++) {
            buttonW[i] = textRenderer.getWidth(buttonLabels[i]) + 12;
            total += buttonW[i] + 4;
        }
        float x = width / 2f - (total - 4) / 2f;
        for (int i = 0; i < buttonLabels.length; i++) {
            buttonX[i] = x;
            x += buttonW[i] + 4;
        }
    }

    private int buttonAt(double mx, double my) {
        if (my < BAR_Y || my >= BAR_Y + BTN_H) return -1;
        for (int i = 0; i < buttonLabels.length; i++) {
            if (mx >= buttonX[i] && mx < buttonX[i] + buttonW[i]) return i;
        }
        return -1;
    }

    private boolean isOverToolbar(double mx, double my) {
        layoutToolbar();
        return buttonAt(mx, my) >= 0;
    }

    private void renderToolbar(DrawContext ctx, int mx, int my) {
        layoutToolbar();
        int hovered = buttonAt(mx, my);
        for (int i = 0; i < buttonLabels.length; i++) {
            boolean active = (i == B_GRID && options.snapGrid().get()) || (i == B_SNAP && options.snapElements().get())
                || (i == B_ELEMENTS && menu == Menu.ELEMENTS) || (i == B_LAYOUTS && menu == Menu.LAYOUTS);
            boolean armed = i == B_RESET && System.currentTimeMillis() < resetArmedUntil;
            int bg = armed ? 0xFF7F1D1D : i == hovered ? ColorUtil.withAlpha(Theme.accent(), 200)
                : active ? ColorUtil.withAlpha(Theme.accent(), 110) : Theme.panel();
            Render2D.shadow(ctx, buttonX[i], BAR_Y, buttonW[i], BTN_H, 4, 3, 0x60000000);
            Render2D.roundedRect(ctx, buttonX[i], BAR_Y, buttonW[i], BTN_H, 4, bg);
            ctx.drawText(textRenderer, buttonLabels[i], (int) (buttonX[i] + 6), (int) BAR_Y + 3, Theme.TEXT, false);
        }
    }

    // ------------------------------------------------------------------ dropdown menus

    private float menuX() {
        return menu == Menu.ELEMENTS ? buttonX[B_ELEMENTS] : buttonX[B_LAYOUTS];
    }

    private float menuY() {
        return BAR_Y + BTN_H + 3;
    }

    private int menuRows() {
        return switch (menu) {
            case ELEMENTS -> elements().size();
            case LAYOUTS -> layoutNames.size() + 2; // save row, layouts, folder row
            case NONE -> 0;
        };
    }

    private boolean isOverMenu(double mx, double my) {
        if (menu == Menu.NONE) return false;
        float x = Math.min(menuX(), width - MENU_W - 4);
        return mx >= x && mx < x + MENU_W && my >= menuY() && my < menuY() + menuRows() * ROW_H + 4;
    }

    private int menuRowAt(double mx, double my) {
        if (!isOverMenu(mx, my)) return -1;
        int row = (int) ((my - menuY() - 2) / ROW_H);
        return row >= 0 && row < menuRows() ? row : -1;
    }

    private void renderMenu(DrawContext ctx, int mx, int my) {
        if (menu == Menu.NONE) return;
        float x = Math.min(menuX(), width - MENU_W - 4);
        float y = menuY();
        int rows = menuRows();
        Render2D.shadow(ctx, x, y, MENU_W, rows * ROW_H + 4, 4, 4, 0x80000000);
        Render2D.roundedRect(ctx, x, y, MENU_W, rows * ROW_H + 4, 4, Theme.panel());
        int hovered = menuRowAt(mx, my);
        for (int i = 0; i < rows; i++) {
            float ry = y + 2 + i * ROW_H;
            if (i == hovered) Render2D.rect(ctx, x + 2, ry, MENU_W - 4, ROW_H, 0x22FFFFFF);
            String text;
            int color = Theme.SUBTEXT;
            if (menu == Menu.ELEMENTS) {
                HudModule h = elements().get(i);
                text = (h.isEnabled() ? "■ " : "□ ") + h.getName();
                color = h.isEnabled() ? Theme.TEXT : Theme.MUTED;
            } else if (i == 0) {
                boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
                text = naming ? "Name: " + nameInput.get() + (caret ? "_" : "") : "+ Save current layout";
                color = Theme.accent();
            } else if (i == rows - 1) {
                text = "Open layouts folder";
                color = Theme.MUTED;
            } else {
                String name = layoutNames.get(i - 1);
                boolean armed = name.equals(armedDelete) && System.currentTimeMillis() < armedUntil;
                text = armed ? "Right-click again to delete" : name;
                color = armed ? 0xFFF87171 : Theme.TEXT;
            }
            ctx.drawText(textRenderer, textRenderer.trimToWidth(text, (int) MENU_W - 10), (int) x + 6, (int) ry + 2, color, false);
        }
    }

    private void openMenu(Menu m) {
        menu = menu == m ? Menu.NONE : m;
        naming = false;
        if (menu == Menu.LAYOUTS) layoutNames = layouts.list();
    }

    private void clickMenu(int row, int button) {
        if (menu == Menu.ELEMENTS) {
            if (button == 0) elements().get(row).toggle();
            return;
        }
        int rows = menuRows();
        if (row == 0) {
            if (button == 0 && !naming) {
                naming = true;
                nameInput.set(freeLayoutName());
            }
        } else if (row == rows - 1) {
            if (button == 0) {
                try {
                    java.nio.file.Files.createDirectories(layouts.getDir());
                } catch (IOException ignored) {
                    // opening a missing folder simply does nothing
                }
                Util.getOperatingSystem().open(layouts.getDir());
            }
        } else {
            String name = layoutNames.get(row - 1);
            if (button == 0) {
                try {
                    int n = layouts.load(name, elements());
                    toast("Layout loaded", name + " (" + n + " elements)", NotificationManager.Type.SUCCESS);
                } catch (IOException | IllegalArgumentException e) {
                    toast("Layout not loaded", name + " is not a valid layout", NotificationManager.Type.ERROR);
                }
            } else if (button == 1) {
                if (name.equals(armedDelete) && System.currentTimeMillis() < armedUntil) {
                    if (layouts.delete(name)) toast("Layout deleted", name, NotificationManager.Type.INFO);
                    armedDelete = null;
                    layoutNames = layouts.list();
                } else {
                    armedDelete = name;
                    armedUntil = System.currentTimeMillis() + 3000;
                }
            }
        }
    }

    private String freeLayoutName() {
        List<String> existing = layouts.list();
        int n = 1;
        while (existing.contains("layout-" + n)) n++;
        return "layout-" + n;
    }

    private void saveLayout() {
        String name = nameInput.get().trim();
        if (!HudLayouts.isValidName(name)) {
            toast("Invalid name", "Use letters, digits, - and _ (up to 32)", NotificationManager.Type.WARNING);
            return;
        }
        try {
            layouts.save(name, elements());
            toast("Layout saved", name, NotificationManager.Type.SUCCESS);
            naming = false;
            layoutNames = layouts.list();
        } catch (IOException e) {
            toast("Layout not saved", e.getMessage(), NotificationManager.Type.ERROR);
        }
    }

    // ------------------------------------------------------------------ dragging

    private void drag(double mouseX, double mouseY) {
        float w = dragging.getScaledWidth();
        float h = dragging.getScaledHeight();
        float x = (float) (mouseX - offsetX);
        float y = (float) (mouseY - offsetY);
        guideX = Float.NaN;
        guideY = Float.NaN;
        if (!hasShiftDown()) {
            int n = 0;
            for (HudModule o : elements()) {
                if (o == dragging || !o.isEnabled()) continue;
                if (others.length < (n + 1) * 4) others = java.util.Arrays.copyOf(others, others.length * 2);
                others[n * 4] = o.getX(width);
                others[n * 4 + 1] = o.getY(height);
                others[n * 4 + 2] = o.getScaledWidth();
                others[n * 4 + 3] = o.getScaledHeight();
                n++;
            }
            HudSnapper.snap(x, y, w, h, width, height, others, n, SNAP,
                options.snapGrid().get() ? options.gridSize() : 0, options.snapElements().get(), snap);
            x = snap.x;
            y = snap.y;
            guideX = snap.guideX;
            guideY = snap.guideY;
        } else {
            x = Math.max(0, Math.min(width - w, x));
            y = Math.max(0, Math.min(height - h, y));
        }
        dragging.setPosition(x, y, width, height);
    }

    private void nudge(HudModule h, int dx, int dy) {
        float x = Math.max(0, Math.min(width - h.getScaledWidth(), h.getX(width) + dx));
        float y = Math.max(0, Math.min(height - h.getScaledHeight(), h.getY(height) + dy));
        h.setPosition(x, y, width, height);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layoutToolbar();
        int row = menuRowAt(mouseX, mouseY);
        if (row >= 0) {
            clickMenu(row, button);
            return true;
        }
        int b = buttonAt(mouseX, mouseY);
        if (b >= 0) {
            if (button == 0) clickButton(b);
            return true;
        }
        if (menu != Menu.NONE) {
            menu = Menu.NONE; // a click outside closes the menu
            naming = false;
            return true;
        }
        HudModule h = elementAt(mouseX, mouseY);
        if (h == null) {
            selected = null;
            return false;
        }
        if (button == 0) {
            dragging = h;
            selected = h;
            offsetX = mouseX - h.getX(width);
            offsetY = mouseY - h.getY(height);
        } else if (button == 1) {
            h.setEnabled(false);
            if (selected == h) selected = null;
        }
        return true;
    }

    private void clickButton(int b) {
        switch (b) {
            case B_GRID -> options.snapGrid().toggle();
            case B_SNAP -> options.snapElements().toggle();
            case B_ELEMENTS -> openMenu(Menu.ELEMENTS);
            case B_LAYOUTS -> openMenu(Menu.LAYOUTS);
            case B_RESET -> {
                if (System.currentTimeMillis() < resetArmedUntil) {
                    for (HudModule h : elements()) {
                        h.resetPosition();
                        h.getScaleSetting().reset();
                    }
                    resetArmedUntil = 0;
                    toast("HUD reset", "All elements are back at their default positions", NotificationManager.Type.INFO);
                } else {
                    resetArmedUntil = System.currentTimeMillis() + 3000;
                }
            }
            case B_DONE -> close();
            default -> {
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        guideX = Float.NaN;
        guideY = Float.NaN;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isOverMenu(mouseX, mouseY)) return true;
        HudModule h = elementAt(mouseX, mouseY);
        if (h == null) return false;
        h.getScaleSetting().increment(verticalAmount > 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (naming) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> saveLayout();
                case GLFW.GLFW_KEY_ESCAPE -> naming = false;
                default -> nameInput.keyPressed(keyCode);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && menu != Menu.NONE) {
            menu = Menu.NONE;
            return true;
        }
        HudModule hovered = elementAt(mouseX(), mouseY());
        HudModule target = hovered != null ? hovered : selected;
        int step = hasShiftDown() ? 10 : 1;
        switch (keyCode) {
            case GLFW.GLFW_KEY_R -> {
                if (target != null) {
                    target.resetPosition();
                    target.getScaleSetting().reset();
                }
                return true;
            }
            case GLFW.GLFW_KEY_G -> {
                options.snapGrid().toggle();
                return true;
            }
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                if (target != null) target.getScaleSetting().increment(1);
                return true;
            }
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                if (target != null) target.getScaleSetting().increment(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (target != null) target.setEnabled(false);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                if (selected != null) {
                    int dx = keyCode == GLFW.GLFW_KEY_LEFT ? -step : keyCode == GLFW.GLFW_KEY_RIGHT ? step : 0;
                    int dy = keyCode == GLFW.GLFW_KEY_UP ? -step : keyCode == GLFW.GLFW_KEY_DOWN ? step : 0;
                    nudge(selected, dx, dy);
                }
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (naming) {
            nameInput.charTyped(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
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

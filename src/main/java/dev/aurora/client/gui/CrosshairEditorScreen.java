package dev.aurora.client.gui;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.modules.visuals.Crosshair;
import dev.aurora.client.render.PixelPattern;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Paints the 15x15 pixel crosshair: left-click/drag paints, right-click/drag erases, with optional mirroring.
 * Every change is written to the Crosshair module's pattern at once (and saved with the profile).
 */
public final class CrosshairEditorScreen extends Screen {
    private static final String[] PRESET_LABELS = {"Cross", "Gap", "Dot", "Circle", "X", "T"};
    private static final PixelPattern.Preset[] PRESETS = {PixelPattern.Preset.CROSS, PixelPattern.Preset.GAP_CROSS,
        PixelPattern.Preset.DOT, PixelPattern.Preset.CIRCLE, PixelPattern.Preset.X, PixelPattern.Preset.T_SHAPE};
    private static final float BTN_H = 14;

    private final Screen parent;
    private final Crosshair crosshair = AuroraClient.modules().get(Crosshair.class);
    private final float[] runs = new float[PixelPattern.maxRuns() * 4];
    private final float[] evenCells = new float[PixelPattern.SIZE * PixelPattern.SIZE * 4];
    private final float[] oddCells = new float[PixelPattern.SIZE * PixelPattern.SIZE * 4];
    private final float[] onCells = new float[PixelPattern.SIZE * PixelPattern.SIZE * 4];
    private boolean[] pixels;
    private boolean mirrorX = true;
    private boolean mirrorY = true;
    private int paint = -1; // 1 paint, 0 erase, -1 not dragging

    public CrosshairEditorScreen(Screen parent) {
        super(Text.literal("Crosshair Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boolean[] decoded = PixelPattern.decode(crosshair.getPixels().get());
        pixels = decoded != null ? decoded : PixelPattern.decode(PixelPattern.DEFAULT);
    }

    private float cell() {
        return Math.max(6, Math.min(12, (height - 80) / (float) PixelPattern.SIZE));
    }

    private float gridSize() {
        return cell() * PixelPattern.SIZE;
    }

    private float panelW() {
        return gridSize() + 150;
    }

    private float panelH() {
        return gridSize() + 44;
    }

    private float panelX() {
        return (width - panelW()) / 2f;
    }

    private float panelY() {
        return (height - panelH()) / 2f;
    }

    private float gridX() {
        return panelX() + 12;
    }

    private float gridY() {
        return panelY() + 26;
    }

    private float sideX() {
        return gridX() + gridSize() + 12;
    }

    private static final float SIDE_W = 114;

    private void save() {
        crosshair.getPixels().set(PixelPattern.encode(pixels));
    }

    // ------------------------------------------------------------------ buttons (laid out top to bottom in the side column)

    private float previewY() {
        return gridY();
    }

    private float buttonsY() {
        return previewY() + 64 + 6;
    }

    /** Button rectangles: index 0 mirror X, 1 mirror Y, 2 clear, 3 invert, 4-9 presets, 10 use pixel style, 11 done. */
    private float[] button(int i) {
        float x = sideX();
        float y = buttonsY();
        float half = (SIDE_W - 4) / 2f;
        float third = (SIDE_W - 8) / 3f;
        return switch (i) {
            case 0 -> new float[]{x, y, half, BTN_H};
            case 1 -> new float[]{x + half + 4, y, half, BTN_H};
            case 2 -> new float[]{x, y + 18, half, BTN_H};
            case 3 -> new float[]{x + half + 4, y + 18, half, BTN_H};
            case 10 -> new float[]{x, y + 18 * 4, SIDE_W, BTN_H};
            case 11 -> new float[]{x, y + 18 * 5, SIDE_W, BTN_H};
            default -> {
                int p = i - 4;
                yield new float[]{x + (p % 3) * (third + 4), y + 36 + (p / 3) * 18, third, BTN_H};
            }
        };
    }

    private String buttonLabel(int i) {
        return switch (i) {
            case 0 -> "Mirror ↔ " + (mirrorX ? "On" : "Off");
            case 1 -> "Mirror ↕ " + (mirrorY ? "On" : "Off");
            case 2 -> "Clear";
            case 3 -> "Invert";
            case 10 -> crosshair.isEnabled() && crosshair.isPixelStyle() ? "Pixel style active" : "Use pixel style";
            case 11 -> "Done";
            default -> PRESET_LABELS[i - 4];
        };
    }

    private int buttonAt(double mx, double my) {
        for (int i = 0; i < 12; i++) {
            float[] b = button(i);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        float px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        Render2D.shadow(ctx, px, py, pw, ph, 6, 6, 0x90000000);
        Render2D.roundedRect(ctx, px, py, pw, ph, 6, Theme.panel());
        ctx.drawText(textRenderer, "✚ Crosshair Editor", (int) px + 12, (int) py + 9, Theme.accent(), false);
        String count = PixelPattern.count(pixels) + " px";
        ctx.drawText(textRenderer, count, (int) (gridX() + gridSize() - textRenderer.getWidth(count)), (int) py + 9, Theme.MUTED, false);

        float c = cell();
        float gx = gridX(), gy = gridY();
        int color = crosshair.getColorValue();
        int hx = (int) Math.floor((mouseX - gx) / c);
        int hy = (int) Math.floor((mouseY - gy) / c);
        boolean hoverGrid = hx >= 0 && hy >= 0 && hx < PixelPattern.SIZE && hy < PixelPattern.SIZE;
        // Three batched draws: the two checker shades and the painted cells.
        int even = 0, odd = 0, on = 0;
        for (int y = 0; y < PixelPattern.SIZE; y++) {
            for (int x = 0; x < PixelPattern.SIZE; x++) {
                float[] target = pixels[PixelPattern.index(x, y)] ? onCells : ((x + y) & 1) == 0 ? evenCells : oddCells;
                int n = target == onCells ? on++ : target == evenCells ? even++ : odd++;
                target[n * 4] = x;
                target[n * 4 + 1] = y;
                target[n * 4 + 2] = 1;
                target[n * 4 + 3] = 1;
            }
        }
        Render2D.rects(ctx, gx, gy, c, 0, evenCells, even, 0xFF1A1D27);
        Render2D.rects(ctx, gx, gy, c, 0, oddCells, odd, 0xFF20232E);
        Render2D.rects(ctx, gx, gy, c, 0, onCells, on, color);
        float center = PixelPattern.CENTER * c;
        Render2D.roundedOutline(ctx, gx + center, gy + center, c, c, 0, 1, ColorUtil.withAlpha(Theme.accent(), 160));
        if (hoverGrid) {
            for (int idx : PixelPattern.mirrored(hx, hy, mirrorX, mirrorY)) {
                int x = idx % PixelPattern.SIZE, y = idx / PixelPattern.SIZE;
                Render2D.rect(ctx, gx + x * c, gy + y * c, c, c, 0x40FFFFFF);
            }
        }
        Render2D.roundedOutline(ctx, gx - 1, gy - 1, gridSize() + 2, gridSize() + 2, 2, 1, 0x40FFFFFF);

        renderPreview(ctx, color);
        int hovered = buttonAt(mouseX, mouseY);
        for (int i = 0; i < 12; i++) {
            float[] b = button(i);
            boolean active = (i == 0 && mirrorX) || (i == 1 && mirrorY) || (i == 10 && crosshair.isEnabled() && crosshair.isPixelStyle());
            int bg = i == hovered ? ColorUtil.withAlpha(Theme.accent(), 200) : active ? ColorUtil.withAlpha(Theme.accent(), 100) : Theme.surface();
            Render2D.roundedRect(ctx, b[0], b[1], b[2], b[3], 4, bg);
            String label = textRenderer.trimToWidth(buttonLabel(i), (int) b[2] - 6);
            ctx.drawText(textRenderer, label, (int) (b[0] + (b[2] - textRenderer.getWidth(label)) / 2), (int) b[1] + 3, Theme.TEXT, false);
        }
        String help = "Left: paint · Right: erase · Drag to draw · Esc: done";
        ctx.drawText(textRenderer, help, (int) (px + pw / 2 - textRenderer.getWidth(help) / 2f), (int) (py + ph - 12), Theme.SUBTEXT, false);
    }

    /** The crosshair at its real in-game size on a sky-to-ground backdrop. */
    private void renderPreview(DrawContext ctx, int color) {
        float x = sideX(), y = previewY(), w = SIDE_W, h = 64;
        Render2D.roundedRect(ctx, x, y, w, h, 4, 0xFF6FA8DC, 0xFF3D5A3A);
        ctx.drawText(textRenderer, "Preview", (int) x + 4, (int) y + 3, 0xC0FFFFFF, false);
        int n = PixelPattern.runs(pixels, runs);
        float ps = crosshair.getPixelSize();
        float origin = -(PixelPattern.CENTER + 0.5f) * ps;
        float cx = x + w / 2f, cy = y + h / 2f + 4;
        int outline = crosshair.getOutlineColorValue();
        if (outline != 0) Render2D.rects(ctx, cx + origin, cy + origin, ps, 0.5f, runs, n, outline);
        Render2D.rects(ctx, cx + origin, cy + origin, ps, 0, runs, n, color);
    }

    // ------------------------------------------------------------------ input

    private boolean paintAt(double mx, double my) {
        float c = cell();
        int x = (int) Math.floor((mx - gridX()) / c);
        int y = (int) Math.floor((my - gridY()) / c);
        if (x < 0 || y < 0 || x >= PixelPattern.SIZE || y >= PixelPattern.SIZE) return false;
        boolean value = paint == 1;
        boolean changed = false;
        for (int idx : PixelPattern.mirrored(x, y, mirrorX, mirrorY)) {
            if (pixels[idx] != value) {
                pixels[idx] = value;
                changed = true;
            }
        }
        if (changed) save();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int b = buttonAt(mouseX, mouseY);
        if (b >= 0 && button == 0) {
            switch (b) {
                case 0 -> mirrorX = !mirrorX;
                case 1 -> mirrorY = !mirrorY;
                case 2 -> pixels = PixelPattern.empty();
                case 3 -> pixels = PixelPattern.invert(pixels);
                case 10 -> crosshair.usePixelStyle();
                case 11 -> close();
                default -> pixels = PixelPattern.preset(PRESETS[b - 4]);
            }
            if (b >= 2 && b <= 9) save();
            return true;
        }
        if (button == 0 || button == 1) {
            paint = button == 0 ? 1 : 0;
            if (paintAt(mouseX, mouseY)) return true;
            paint = -1;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (paint >= 0) {
            paintAt(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        paint = -1;
        return super.mouseReleased(mouseX, mouseY, button);
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

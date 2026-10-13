package dev.aurora.client.module.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * Restyles the vanilla hotbar: a rounded translucent bar and a selection highlight that slides between slots.
 *
 * <p>InGameHudMixin hands every hotbar sprite to {@link #draw}; the bar, the selection frame and the offhand slot are
 * replaced and everything else (items, attack indicator) is drawn by vanilla as usual.</p>
 */
public final class HotbarStyle extends Module {
    public enum Part {
        BAR, SELECTION, OFFHAND_LEFT, OFFHAND_RIGHT, OTHER;

        /** Maps a vanilla GUI sprite path to the part it draws. */
        public static Part of(String path) {
            return switch (path) {
                case "hud/hotbar" -> BAR;
                case "hud/hotbar_selection" -> SELECTION;
                case "hud/hotbar_offhand_left" -> OFFHAND_LEFT;
                case "hud/hotbar_offhand_right" -> OFFHAND_RIGHT;
                default -> OTHER;
            };
        }
    }

    /** Vanilla slot pitch and bar width. */
    private static final int SLOT = 20;
    private static final int BAR_WIDTH = 182;

    private final ColorSetting background = add(new ColorSetting("Background", "Bar color", 0x90101018));
    private final BooleanSetting themeAccent = add(new BooleanSetting("Theme Accent", "Color the selection with the ClickGUI accent", true));
    private final ColorSetting selection = add(new ColorSetting("Selection", "Selected slot color", 0x60FFFFFF)).visibleWhen(() -> !themeAccent.get());
    private final BooleanSetting outline = add(new BooleanSetting("Outline", "Outline the selected slot", true));
    private final NumberSetting radius = add(new NumberSetting("Radius", "Corner rounding", 4, 0, 8, 0.5));
    private final BooleanSetting smooth = add(new BooleanSetting("Smooth", "Slide the selection between slots", true));

    private final Animation slide = new Animation(0, 18);
    private boolean slideStarted;

    public HotbarStyle() {
        super("Hotbar Style", "Rounded, translucent hotbar with a sliding selection.", Category.VISUALS);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    protected void onEnable() {
        slideStarted = false;
    }

    /**
     * Draws the styled replacement for one vanilla hotbar sprite.
     *
     * @return false when vanilla should draw the sprite itself
     */
    public boolean draw(DrawContext ctx, String path, int x, int y, int width, int height) {
        Part part = Part.of(path);
        if (part == Part.OTHER) return false;
        float r = radius.getFloat();
        switch (part) {
            case BAR -> Render2D.roundedRect(ctx, x, y, width, height, r, background.get());
            case SELECTION -> drawSelection(ctx, x, y, r);
            // The offhand sprites are 29x24 with the 22x22 slot at the left (left hand) or right (right hand) edge.
            case OFFHAND_LEFT -> Render2D.roundedRect(ctx, x, y + 1, 22, 22, r, background.get());
            case OFFHAND_RIGHT -> Render2D.roundedRect(ctx, x + width - 22, y + 1, 22, 22, r, background.get());
            default -> {
            }
        }
        RenderSystem.enableBlend(); // vanilla keeps blending on for the remaining hotbar sprites
        return true;
    }

    /** Vanilla's selection sprite is 24x23 at {@code barLeft - 1 + slot * 20}, one pixel above the bar. */
    private void drawSelection(DrawContext ctx, int x, int y, float r) {
        int barLeft = ctx.getScaledWindowWidth() / 2 - BAR_WIDTH / 2;
        int slot = slotAt(x, barLeft);
        if (!smooth.get() || !slideStarted) {
            slide.snap(slot);
            slideStarted = true;
        }
        slide.setTarget(slot);
        float s = (float) slide.update();
        float sx = barLeft + 1 + s * SLOT;
        float sy = y + 2;
        int accent = Theme.accent();
        int fill = themeAccent.get() ? ColorUtil.withAlpha(accent, 0x55) : selection.get();
        Render2D.roundedRect(ctx, sx, sy, SLOT, SLOT, r, fill);
        if (outline.get()) {
            int line = themeAccent.get() ? accent : ColorUtil.withAlpha(selection.get(), 0xFF);
            Render2D.roundedOutline(ctx, sx, sy, SLOT, SLOT, r, 1f, line);
        }
    }

    /** Selected slot from the x vanilla passes for its selection sprite. */
    public static int slotAt(int selectionX, int barLeft) {
        return Math.max(0, Math.min(8, Math.round((selectionX - barLeft + 1) / (float) SLOT)));
    }
}

package dev.aurora.client.hud;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * A module that draws a movable HUD element.
 *
 * <p>Positions are stored as fractions (0..1) of the free space on screen, so elements stay in
 * the same relative place (e.g. hugging the right edge) at any resolution or GUI scale.
 * Subclasses draw at local (0,0) and report their size through {@link #setSize}.</p>
 */
public abstract class HudModule extends Module {
    private final NumberSetting posX;
    private final NumberSetting posY;
    protected final NumberSetting scale;
    private int width = 40;
    private int height = 10;

    protected HudModule(String name, String description, Category category, double defaultX, double defaultY, boolean enabledByDefault) {
        super(name, description, category, -1, enabledByDefault);
        posX = add(new NumberSetting("X", "Horizontal position", defaultX, 0, 1, 0.0001)).hide();
        posY = add(new NumberSetting("Y", "Vertical position", defaultY, 0, 1, 0.0001)).hide();
        scale = add(new NumberSetting("Scale", "Size of this HUD element", 1.0, 0.5, 2.5, 0.05));
    }

    /**
     * Draws the element at local coordinates (0,0).
     *
     * @param editor true inside the HUD editor: draw sample content when there is nothing to show
     */
    public abstract void render(DrawContext ctx, float tickDelta, boolean editor);

    protected final void setSize(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getScale() {
        return scale.getFloat();
    }

    public NumberSetting getScaleSetting() {
        return scale;
    }

    public float getScaledWidth() {
        return width * getScale();
    }

    public float getScaledHeight() {
        return height * getScale();
    }

    public float getX(int screenWidth) {
        return (float) (posX.get() * Math.max(0, screenWidth - getScaledWidth()));
    }

    public float getY(int screenHeight) {
        return (float) (posY.get() * Math.max(0, screenHeight - getScaledHeight()));
    }

    public void setPosition(float x, float y, int screenWidth, int screenHeight) {
        float freeX = Math.max(1, screenWidth - getScaledWidth());
        float freeY = Math.max(1, screenHeight - getScaledHeight());
        posX.set((double) Math.max(0, Math.min(1, x / freeX)));
        posY.set((double) Math.max(0, Math.min(1, y / freeY)));
    }

    /** Elements on the right half align their content to the right edge. */
    public boolean isRightSide() {
        return posX.get() > 0.5;
    }

    public void resetPosition() {
        posX.reset();
        posY.reset();
    }
}

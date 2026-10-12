package dev.aurora.client.gui.component;

import net.minecraft.client.gui.DrawContext;

/**
 * Base for ClickGUI widgets. Coordinates and mouse positions are in GUI-local units (already
 * divided by the ClickGUI scale), so components never deal with scaling themselves.
 */
public abstract class Component {
    protected float x;
    protected float y;
    protected float width;

    public void setBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public abstract float getHeight();

    public abstract void render(DrawContext ctx, double mouseX, double mouseY, float delta);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight();
    }

    /** Description shown after hovering for a moment, or null. */
    public String getTooltip(double mouseX, double mouseY) {
        return null;
    }
}

package dev.aurora.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import dev.aurora.client.util.Animation;
import net.minecraft.client.gui.DrawContext;

/**
 * A module that draws a movable HUD element.
 *
 * <p>An element is either placed by fractions of the free screen space ({@link Anchor#AUTO}: the default positions
 * and profiles from before 1.1) or anchored to a screen edge, corner or the centre with a pixel offset. The HUD
 * editor anchors an element to the part of the screen it is dropped in, so it keeps its distance to that edge when
 * the window is resized. Subclasses draw at local (0,0) and report their size through {@link #setSize}.</p>
 */
public abstract class HudModule extends Module {
    /** Where an element is attached. {@code h}/{@code v}: 0 = left/top, 1 = centre, 2 = right/bottom. */
    public enum Anchor {
        AUTO(-1, -1, "Auto"),
        TOP_LEFT(0, 0, "Top Left"), TOP_CENTER(1, 0, "Top"), TOP_RIGHT(2, 0, "Top Right"),
        MIDDLE_LEFT(0, 1, "Left"), CENTER(1, 1, "Center"), MIDDLE_RIGHT(2, 1, "Right"),
        BOTTOM_LEFT(0, 2, "Bottom Left"), BOTTOM_CENTER(1, 2, "Bottom"), BOTTOM_RIGHT(2, 2, "Bottom Right");

        public final int h;
        public final int v;
        private final String label;

        Anchor(int h, int v, String label) {
            this.h = h;
            this.v = v;
            this.label = label;
        }

        public static Anchor of(int h, int v) {
            for (Anchor a : values()) if (a.h == h && a.v == v) return a;
            return AUTO;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final double MAX_OFFSET = 10000;

    private final NumberSetting posX;
    private final NumberSetting posY;
    private final EnumSetting<Anchor> anchor;
    private final NumberSetting offsetX;
    private final NumberSetting offsetY;
    protected final NumberSetting scale;
    private final Animation visibility = new Animation(0, 12);
    private int width = 40;
    private int height = 10;

    protected HudModule(String name, String description, Category category, double defaultX, double defaultY, boolean enabledByDefault) {
        super(name, description, category, -1, enabledByDefault);
        posX = add(new NumberSetting("X", "Horizontal position", defaultX, 0, 1, 0.0001)).hide();
        posY = add(new NumberSetting("Y", "Vertical position", defaultY, 0, 1, 0.0001)).hide();
        anchor = add(new EnumSetting<>("Anchor", "Screen edge the element is attached to", Anchor.AUTO)).hide();
        offsetX = add(new NumberSetting("Offset X", "Distance from the anchored edge", 0, -MAX_OFFSET, MAX_OFFSET, 0.5)).hide();
        offsetY = add(new NumberSetting("Offset Y", "Distance from the anchored edge", 0, -MAX_OFFSET, MAX_OFFSET, 0.5)).hide();
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

    public Anchor getAnchor() {
        return anchor.get();
    }

    public float getX(int screenWidth) {
        float w = getScaledWidth();
        Anchor a = anchor.get();
        if (a == Anchor.AUTO) return (float) (posX.get() * Math.max(0, screenWidth - w));
        return clamp(resolve(a.h, offsetX.getFloat(), screenWidth, w), screenWidth, w);
    }

    public float getY(int screenHeight) {
        float h = getScaledHeight();
        Anchor a = anchor.get();
        if (a == Anchor.AUTO) return (float) (posY.get() * Math.max(0, screenHeight - h));
        return clamp(resolve(a.v, offsetY.getFloat(), screenHeight, h), screenHeight, h);
    }

    /** Moves the element so its top-left corner is at (x, y), anchoring it to the part of the screen it is in. */
    public void setPosition(float x, float y, int screenWidth, int screenHeight) {
        float w = getScaledWidth();
        float h = getScaledHeight();
        setPosition(x, y, screenWidth, screenHeight, anchorFor(x, y, w, h, screenWidth, screenHeight));
    }

    /** Moves the element with an explicit anchor ({@link Anchor#AUTO} stores screen fractions like before 1.1). */
    public void setPosition(float x, float y, int screenWidth, int screenHeight, Anchor a) {
        float w = getScaledWidth();
        float h = getScaledHeight();
        float freeX = Math.max(1, screenWidth - w);
        float freeY = Math.max(1, screenHeight - h);
        posX.set((double) Math.max(0, Math.min(1, x / freeX)));
        posY.set((double) Math.max(0, Math.min(1, y / freeY)));
        anchor.set(a);
        if (a != Anchor.AUTO) {
            offsetX.set((double) offsetFor(a.h, x, screenWidth, w));
            offsetY.set((double) offsetFor(a.v, y, screenHeight, h));
        }
    }

    /** Elements on the right half align their content to the right edge. */
    public boolean isRightSide() {
        Anchor a = anchor.get();
        if (a == Anchor.AUTO) return posX.get() > 0.5;
        return a.h == 2 || (a.h == 1 && offsetX.get() > 0);
    }

    public void resetPosition() {
        posX.reset();
        posY.reset();
        anchor.reset();
        offsetX.reset();
        offsetY.reset();
    }

    /**
     * Advances the fade animation toward the enabled state and returns the visibility (0 hidden .. 1 shown).
     * Called every frame for every element, enabled or not.
     */
    public float updateVisibility() {
        float target = isEnabled() ? 1 : 0;
        if (!HudStyle.animations()) {
            visibility.snap(target);
            return target;
        }
        visibility.setSpeed(12 * HudStyle.animationSpeed());
        visibility.setTarget(target);
        return (float) visibility.update();
    }

    /** Skips the fade-out (used when an element is disabled because it failed to render). */
    void hideImmediately() {
        visibility.snap(0);
    }

    // ------------------------------------------------------------------ layouts

    private Setting<?>[] layoutSettings() {
        return new Setting<?>[]{posX, posY, anchor, offsetX, offsetY, scale};
    }

    /** Position, scale and enabled state, as stored in HUD layout files. */
    public JsonObject layoutToJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", isEnabled());
        for (Setting<?> s : layoutSettings()) obj.add(s.getName(), s.toJson());
        return obj;
    }

    /** Applies a {@link #layoutToJson()} object; missing or invalid values keep their current value. */
    public void applyLayout(JsonObject obj) {
        for (Setting<?> s : layoutSettings()) {
            JsonElement value = obj.get(s.getName());
            if (value != null) s.load(value);
        }
        JsonElement enabled = obj.get("enabled");
        if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
            setEnabled(enabled.getAsBoolean());
        }
    }

    // ------------------------------------------------------------------ anchor math (pure, unit-tested)

    /** Screen position of an element of {@code size} with {@code offset} from the edge given by {@code align}. */
    public static float resolve(int align, float offset, float screen, float size) {
        return switch (align) {
            case 0 -> offset;
            case 1 -> (screen - size) / 2f + offset;
            default -> screen - size - offset;
        };
    }

    /** Inverse of {@link #resolve}: the offset that puts the element at {@code pos}. */
    public static float offsetFor(int align, float pos, float screen, float size) {
        return switch (align) {
            case 0 -> pos;
            case 1 -> pos - (screen - size) / 2f;
            default -> screen - size - pos;
        };
    }

    /** The anchor for an element by which third of the screen its centre is in. */
    public static Anchor anchorFor(float x, float y, float w, float h, float screenW, float screenH) {
        return Anchor.of(third(x + w / 2f, screenW), third(y + h / 2f, screenH));
    }

    private static int third(float center, float screen) {
        if (center < screen / 3f) return 0;
        if (center > screen * 2f / 3f) return 2;
        return 1;
    }

    private static float clamp(float pos, float screen, float size) {
        return Math.max(0, Math.min(Math.max(0, screen - size), pos));
    }
}

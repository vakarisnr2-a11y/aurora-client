package dev.aurora.client.module.modules.world;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;

/**
 * Dropped items lie flat on the ground instead of floating, bobbing and spinning.
 *
 * <p>ItemEntityRendererMixin removes vanilla's bob and spin and calls {@link #transform} right before the item
 * stack is drawn. Flat items (most items) are laid face up; 3D models (blocks) rest on their bottom face. Each item
 * keeps a fixed random yaw on the ground. Rendering only: item positions and pickup are unchanged.</p>
 */
public final class ItemPhysics extends Module {
    /** Vanilla spacing between stacked copies of a flat item (ItemEntityRenderer.renderStack). */
    private static final float FLAT_SPACING = 0.09375f;
    /** Gap above the ground so the item does not flicker into the block below. */
    private static final float GROUND_GAP = 0.005f;

    private final BooleanSetting spinInAir = add(new BooleanSetting("Spin In Air", "Items keep spinning while falling or floating and lie still once they land", true));

    public ItemPhysics() {
        super("Item Physics", "Dropped items lie flat on the ground instead of floating and spinning.", Category.WORLD);
    }

    /** Called with the matrices at the item entity's feet, before vanilla draws the stack with its ground transform. */
    public void transform(ItemEntity entity, float tickDelta, BakedModel model, boolean depth, int count, MatrixStack matrices) {
        Transformation ground = model.getTransformation().ground;
        boolean airborne = !entity.isOnGround();
        float yaw = spinInAir.get() && airborne ? entity.getRotation(tickDelta) : entity.uniqueOffset;
        if (depth) standBlock(matrices, yaw, ground);
        else layFlat(matrices, renderedCopies(count), yaw, ground);
    }

    /** A flat item face up, its lowest copy just above the ground. */
    public static void layFlat(MatrixStack matrices, int copies, float yaw, Transformation ground) {
        matrices.translate(0, flatLift(copies, ground.scale.z()), 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
        // Face up: the item's +Z (its front, and the direction copies are stacked in) now points up.
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
        // Cancel the ground transform's upward offset, which would now push the item sideways.
        matrices.translate(0, -ground.translation.y(), 0);
    }

    /** A 3D model standing on its bottom face. */
    public static void standBlock(MatrixStack matrices, float yaw, Transformation ground) {
        matrices.translate(0, blockLift(ground.scale.y(), ground.translation.y()), 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
    }

    /** Copies vanilla draws for a stack of {@code count} (ItemEntityRenderer.getRenderedAmount, checked with javap). */
    public static int renderedCopies(int count) {
        if (count <= 1) return 1;
        if (count <= 16) return 2;
        if (count <= 32) return 3;
        if (count <= 48) return 4;
        return 5;
    }

    /**
     * Height that puts the lowest copy of a face-up flat item on the ground. Copies are centered around the origin
     * along the item's depth, {@link #FLAT_SPACING} times the ground scale apart, and each is 1/16 thick before scaling.
     */
    public static float flatLift(int copies, float scaleZ) {
        return FLAT_SPACING * scaleZ * (copies - 1) / 2f + scaleZ / 32f + GROUND_GAP;
    }

    /** Height that puts the bottom of a full-block model (centered after its ground transform) on the ground. */
    public static float blockLift(float scaleY, float translationY) {
        return 0.5f * scaleY - translationY + GROUND_GAP;
    }
}

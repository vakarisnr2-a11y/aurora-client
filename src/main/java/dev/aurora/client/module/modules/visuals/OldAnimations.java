package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

/**
 * 1.7-style first-person animations. Rendering only: no input is changed and nothing is sent to the server.
 *
 * <p><b>Old Swing</b>: since 1.9 the held item dips after every attack while the attack cooldown recharges.
 * HeldItemRendererMixin gives the equip animation a full cooldown instead, so the item stays up like in 1.7/1.8.</p>
 *
 * <p><b>Sword Block</b>: holding the use key with a sword shows the 1.7/1.8 blocking pose. Modern swords cannot
 * block, so this is only a pose: it does not reduce damage, and right-click still does whatever it normally does.</p>
 */
public final class OldAnimations extends Module {
    /**
     * 1.8 drew the blocking sword with {@code transformFirstPersonItem} (a 45 degree Y turn, scale 0.4) followed by
     * {@code translate(-0.5, 0.2, 0), rotate(30, Y), rotate(-80, X), rotate(60, Y)}. Modern item models already contain
     * the 0.4 scale and the 45 degree turn, so for the right hand that becomes this translation followed by Y 75, X -80,
     * Y 15 (checked in OldAnimationsTest against the 1.8 matrices). The left hand mirrors X and the Y angles.
     */
    public static final float BLOCK_X = -0.14142136f;
    public static final float BLOCK_Y = 0.08f;
    public static final float BLOCK_Z = 0.14142136f;
    public static final float BLOCK_YAW_1 = 75f;
    public static final float BLOCK_PITCH = -80f;
    public static final float BLOCK_YAW_2 = 15f;

    private final BooleanSetting oldSwing = add(new BooleanSetting("Old Swing",
        "Keep the held item up after attacks instead of the 1.9+ cooldown dip", true));
    private final BooleanSetting swordBlock = add(new BooleanSetting("Sword Block",
        "Show the 1.7 blocking pose while holding use with a sword (looks only, does not block damage)", true));
    private final BooleanSetting blockHit = add(new BooleanSetting("Block Hit",
        "Keep swinging while blocking like 1.7 (off: the sword stays still like 1.8)", true)).visibleWhen(swordBlock::get);

    public OldAnimations() {
        super("Old Animations", "1.7-style swing and sword-block animations (visual only).", Category.VISUALS);
    }

    public boolean oldSwing() {
        return oldSwing.get();
    }

    public boolean blockHit() {
        return blockHit.get();
    }

    /** True when the first-person item in {@code hand} should be drawn in the blocking pose. */
    public boolean showsSwordBlock(PlayerEntity player, Hand hand, ItemStack stack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return swordBlockPose(swordBlock.get(), hand == Hand.MAIN_HAND, player == mc.player && mc.currentScreen == null
            && mc.options.useKey.isPressed(), player.isUsingItem(), stack.getItem() instanceof SwordItem);
    }

    /** The pose is shown for a sword in the main hand while use is held and no real item use (eating, offhand shield...) is running. */
    public static boolean swordBlockPose(boolean enabled, boolean mainHand, boolean useHeld, boolean usingItem, boolean sword) {
        return enabled && mainHand && useHeld && !usingItem && sword;
    }

    /** Applies the blocking pose after vanilla's equip (and optional swing) offset. */
    public static void applyBlockTransform(MatrixStack matrices, boolean rightArm) {
        float side = rightArm ? 1f : -1f;
        matrices.translate(side * BLOCK_X, BLOCK_Y, BLOCK_Z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * BLOCK_YAW_1));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(BLOCK_PITCH));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * BLOCK_YAW_2));
    }
}

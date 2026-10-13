package dev.aurora.client.module.modules.player;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.event.events.KeyEvent;
import dev.aurora.client.event.events.Render2DEvent;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.mixin.HandledScreenAccessor;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

/**
 * Locks inventory slots so their items cannot be dropped: Q / Ctrl+Q in game and throwing from an inventory screen
 * are blocked for locked slots. Press the Lock Key in game to lock the selected hotbar slot, or while hovering a slot
 * in your inventory. Locked slots get a small padlock.
 *
 * <p>Only dropping is blocked; locked items can still be moved, used and swapped. Creative mode's inventory drops
 * items through its own code and is not covered.</p>
 */
public final class ItemLock extends Module {
    /** Player inventory indices: 0-8 hotbar, 9-35 main, 36-39 armor, 40 offhand. */
    public static final int SLOTS = 41;

    private final KeybindSetting lockKey = add(new KeybindSetting("Lock Key",
        "Locks or unlocks the selected hotbar slot, or the slot under the mouse in your inventory", GLFW.GLFW_KEY_K));
    private final BooleanSetting overlay = add(new BooleanSetting("Show Locks", "Draw a padlock on locked slots", true));
    private final BooleanSetting notify = add(new BooleanSetting("Notify", "Show a notification when a drop is blocked", true));
    private final StringSetting locked = add(new StringSetting("Locked Slots", "Locked inventory slot numbers", "", 160)).hide();

    private long mask = -1;
    private String maskFor;

    public ItemLock() {
        super("Item Lock", "Locks slots so their items can't be dropped by accident. Press the Lock Key on a slot.", Category.PLAYER);
        on(KeyEvent.class, this::onKey);
        on(Render2DEvent.class, this::renderHotbarLocks);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    private long mask() {
        String raw = locked.get();
        if (!raw.equals(maskFor)) {
            mask = parse(raw);
            maskFor = raw;
        }
        return mask;
    }

    public boolean isLocked(int inventoryIndex) {
        return isEnabled() && isSet(mask(), inventoryIndex);
    }

    /** Whether a screen slot shows a locked slot of the player's own inventory. */
    public boolean isLocked(Slot slot, PlayerEntity player) {
        return slot != null && player != null && slot.inventory == player.getInventory() && isLocked(slot.getIndex());
    }

    private void toggle(int index) {
        long m = mask() ^ (1L << index);
        locked.set(format(m));
        boolean now = isSet(m, index);
        AuroraClient.notifications().post("Item Lock", (now ? "Locked " : "Unlocked ") + slotName(index),
            now ? NotificationManager.Type.SUCCESS : NotificationManager.Type.INFO, this);
    }

    private void onKey(KeyEvent e) {
        if (e.action != GLFW.GLFW_PRESS || !lockKey.matchesKey(e.key)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!e.inScreen) {
            toggle(mc.player.getInventory().selectedSlot);
            return;
        }
        if (mc.currentScreen instanceof HandledScreen<?> screen && !(screen.getFocused() instanceof TextFieldWidget)) {
            Slot slot = ((HandledScreenAccessor) screen).aurora$getFocusedSlot();
            if (slot != null && slot.inventory == mc.player.getInventory() && slot.getIndex() >= 0 && slot.getIndex() < SLOTS) {
                toggle(slot.getIndex());
                e.cancel();
            }
        }
    }

    /** Called when the game is about to drop the selected stack (Q). Returns true to block it. */
    public boolean blockDrop(PlayerInventory inventory) {
        if (!isLocked(inventory.selectedSlot)) return false;
        warn(inventory.selectedSlot);
        return true;
    }

    /** Called for a "throw" click on a screen slot. Returns true to block it. */
    public boolean blockThrow(Slot slot, PlayerEntity player) {
        if (!isLocked(slot, player)) return false;
        warn(slot.getIndex());
        return true;
    }

    private void warn(int index) {
        if (notify.get()) {
            AuroraClient.notifications().post("Item Lock", slotName(index) + " is locked", NotificationManager.Type.WARNING, this);
        }
    }

    // ------------------------------------------------------------------ padlocks

    private void renderHotbarLocks(Render2DEvent e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!overlay.get() || mc.player == null || mc.options.hudHidden || mask() == 0) return;
        int left = e.width / 2 - 91;
        for (int i = 0; i < 9; i++) {
            if (isSet(mask(), i)) padlock(e.context, left + i * 20 + 14, e.height - 21);
        }
    }

    /** Padlock overlay for a slot drawn in an inventory screen (slot coordinates, called by HandledScreenMixin). */
    public void renderSlot(DrawContext ctx, Slot slot, PlayerEntity player) {
        if (!overlay.get() || !isLocked(slot, player)) return;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 300); // above the item and its count
        Render2D.roundedOutline(ctx, slot.x - 0.5f, slot.y - 0.5f, 17, 17, 1.5f, 1, HudStyle.accent());
        padlock(ctx, slot.x + 11, slot.y);
        ctx.getMatrices().pop();
    }

    /** A 5x6 padlock: shackle and body. */
    private static void padlock(DrawContext ctx, float x, float y) {
        int color = HudStyle.accent();
        Render2D.rect(ctx, x - 0.5f, y + 1.5f, 6, 5, 0xC0000000);
        Render2D.rect(ctx, x + 1, y, 3, 1, color);
        Render2D.rect(ctx, x, y + 1, 1, 2, color);
        Render2D.rect(ctx, x + 4, y + 1, 1, 2, color);
        Render2D.rect(ctx, x, y + 3, 5, 3, color);
    }

    // ------------------------------------------------------------------ pure helpers

    public static String slotName(int index) {
        if (index < 9) return "Hotbar slot " + (index + 1);
        if (index < 36) return "Inventory slot " + (index - 8);
        if (index < 40) return "Armor slot " + (index - 35);
        return "Offhand slot";
    }

    public static boolean isSet(long mask, int index) {
        return index >= 0 && index < SLOTS && (mask & (1L << index)) != 0;
    }

    /** Parses "0,3,40" into a bit mask, ignoring anything that is not a valid slot number. */
    public static long parse(String raw) {
        long m = 0;
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            try {
                int i = Integer.parseInt(p);
                if (i >= 0 && i < SLOTS) m |= 1L << i;
            } catch (NumberFormatException ignored) {
                // skip garbage from hand-edited configs
            }
        }
        return m;
    }

    public static String format(long mask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SLOTS; i++) {
            if ((mask & (1L << i)) == 0) continue;
            if (!sb.isEmpty()) sb.append(',');
            sb.append(i);
        }
        return sb.toString();
    }
}

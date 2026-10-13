package dev.aurora.client.module.modules.player;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.event.events.KeyEvent;
import dev.aurora.client.event.events.MouseButtonEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.setting.KeybindSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts the open inventory or chest with real slot clicks (the same packets as clicking by hand),
 * so it works on any server that allows normal inventory interaction.
 *
 * <p>Phase 1 merges partial stacks of the same item. Phase 2 is a selection sort that swaps
 * slots with PICKUP clicks; slots holding the same item type are never swapped, which avoids
 * accidental merges.</p>
 */
public final class InventorySorter extends Module {
    public enum SortBy {
        NAME, ID, COUNT
    }

    private static final int MAX_CLICKS = 600;

    private final KeybindSetting sortKey = add(new KeybindSetting("Sort Key", "Key/button that sorts the open inventory",
        KeybindSetting.MOUSE_OFFSET + GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
    private final EnumSetting<SortBy> sortBy = add(new EnumSetting<>("Sort By", "Sorting order", SortBy.NAME));
    private final BooleanSetting hotbar = add(new BooleanSetting("Include Hotbar", "Also sort the hotbar in your inventory", false));
    private final BooleanSetting containers = add(new BooleanSetting("Containers", "Also sort chests, barrels and shulker boxes", true));

    private int clicks;

    public InventorySorter() {
        super("Inventory Sorter", "Press the sort key (middle mouse) in an inventory or chest to sort it.", Category.PLAYER,
            KeybindSetting.NONE, true);
        on(KeyEvent.class, e -> {
            if (e.inScreen && e.action == GLFW.GLFW_PRESS && sortKey.matchesKey(e.key) && trySort()) e.cancel();
        });
        on(MouseButtonEvent.class, e -> {
            if (e.inScreen && e.action == GLFW.GLFW_PRESS && sortKey.matchesMouse(e.button) && trySort()) e.cancel();
        });
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    private boolean trySort() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || !(mc.currentScreen instanceof HandledScreen<?> screen)) return false;
        ScreenHandler handler = screen.getScreenHandler();
        if (!handler.getCursorStack().isEmpty()) return false;
        List<Slot> slots = new ArrayList<>();
        if (screen instanceof InventoryScreen) {
            for (Slot s : handler.slots) {
                if (!(s.inventory instanceof PlayerInventory)) continue;
                int index = s.getIndex();
                if ((index >= 9 && index < 36) || (hotbar.get() && index >= 0 && index < 9)) slots.add(s);
            }
        } else if (containers.get() && (screen instanceof GenericContainerScreen || screen instanceof ShulkerBoxScreen)) {
            for (Slot s : handler.slots) if (!(s.inventory instanceof PlayerInventory)) slots.add(s);
        } else {
            return false;
        }
        if (slots.size() < 2) return false;
        // Keep hotbar slots after the main inventory so "first" positions are the main grid.
        slots.sort(Comparator.comparingInt(s -> s.inventory instanceof PlayerInventory && s.getIndex() < 9 ? s.getIndex() + 100 : s.getIndex()));
        clicks = 0;
        merge(mc, handler, slots);
        selectionSort(mc, handler, slots);
        AuroraClient.notifications().post("Inventory Sorter", "Sorted " + slots.size() + " slots (" + clicks + " clicks)",
            NotificationManager.Type.INFO, this);
        return true;
    }

    private void merge(MinecraftClient mc, ScreenHandler handler, List<Slot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ItemStack a = slots.get(i).getStack();
                ItemStack b = slots.get(j).getStack();
                if (a.isEmpty() || b.isEmpty() || a.getCount() >= a.getMaxCount() || !ItemStack.areItemsAndComponentsEqual(a, b)) continue;
                click(mc, handler, slots.get(j));
                click(mc, handler, slots.get(i));
                if (!handler.getCursorStack().isEmpty()) click(mc, handler, slots.get(j));
                if (clicks > MAX_CLICKS) return;
            }
        }
    }

    private void selectionSort(MinecraftClient mc, ScreenHandler handler, List<Slot> slots) {
        Comparator<ItemStack> order = comparator();
        for (int i = 0; i < slots.size() && clicks < MAX_CLICKS; i++) {
            int best = i;
            for (int j = i + 1; j < slots.size(); j++) {
                if (order.compare(slots.get(j).getStack(), slots.get(best).getStack()) < 0) best = j;
            }
            if (best == i) continue;
            ItemStack target = slots.get(i).getStack();
            ItemStack source = slots.get(best).getStack();
            if (!target.isEmpty() && ItemStack.areItemsAndComponentsEqual(target, source)) continue;
            click(mc, handler, slots.get(best));
            click(mc, handler, slots.get(i));
            if (!handler.getCursorStack().isEmpty()) click(mc, handler, slots.get(best));
        }
    }

    private Comparator<ItemStack> comparator() {
        Comparator<ItemStack> emptyLast = Comparator.comparing(ItemStack::isEmpty);
        Comparator<ItemStack> byName = Comparator.comparing(s -> s.getName().getString(), String.CASE_INSENSITIVE_ORDER);
        return switch (sortBy.get()) {
            case NAME -> emptyLast.thenComparing(byName);
            case ID -> emptyLast.thenComparing(s -> Registries.ITEM.getId(s.getItem()).toString()).thenComparing(byName);
            case COUNT -> emptyLast.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed()).thenComparing(byName);
        };
    }

    private void click(MinecraftClient mc, ScreenHandler handler, Slot slot) {
        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
        clicks++;
    }
}

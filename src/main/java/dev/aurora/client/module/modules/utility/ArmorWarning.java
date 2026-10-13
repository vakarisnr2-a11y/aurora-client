package dev.aurora.client.module.modules.utility;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

/**
 * A notification (and optional sound) when a worn armor piece drops to a low durability. Each piece warns once and
 * warns again only after it was repaired above the threshold or swapped.
 */
public final class ArmorWarning extends Module {
    private static final String[] PIECES = {"Boots", "Leggings", "Chestplate", "Helmet"};

    private final NumberSetting threshold = add(new NumberSetting("Warn At", "Warn when a piece drops to this durability percentage", 15, 1, 50, 1));
    private final BooleanSetting sound = add(new BooleanSetting("Sound", "Play a sound with the warning", true));

    private final boolean[] warned = new boolean[4];
    private final ItemStack[] tracked = new ItemStack[4];

    public ArmorWarning() {
        super("Armor Warning", "Warns you when your armor is about to break.", Category.UTILITY);
        on(TickEvent.Post.class, e -> tick());
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    protected void onEnable() {
        for (int i = 0; i < 4; i++) {
            warned[i] = false;
            tracked[i] = null;
        }
    }

    private void tick() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = p.getInventory().getArmorStack(slot);
            if (stack != tracked[slot]) {
                tracked[slot] = stack;
                warned[slot] = false;
            }
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            float pct = percent(stack.getMaxDamage() - stack.getDamage(), stack.getMaxDamage());
            boolean low = pct <= threshold.get();
            if (shouldWarn(warned[slot], low)) {
                AuroraClient.notifications().post("Armor breaking", PIECES[slot] + " at " + Math.round(pct) + "% ("
                    + (stack.getMaxDamage() - stack.getDamage()) + " left)", NotificationManager.Type.WARNING, PIECES[slot]);
                if (sound.get()) {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_SHIELD_BREAK, 1.4f, 0.8f));
                }
            }
            warned[slot] = low;
        }
    }

    public static float percent(int remaining, int max) {
        return max <= 0 ? 100 : 100f * remaining / max;
    }

    /** Warn when a piece becomes low; stay quiet while it stays low. */
    public static boolean shouldWarn(boolean alreadyWarned, boolean low) {
        return low && !alreadyWarned;
    }
}

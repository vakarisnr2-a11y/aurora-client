package dev.aurora.client.module.modules.player;

import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Optional;

/** Switches to the fastest suitable hotbar tool while mining, and back afterwards. */
public final class AutoTool extends Module {
    private final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", "Return to the previous slot when you stop mining", true));
    private final BooleanSetting saveTools = add(new BooleanSetting("Save Tools", "Never pick tools that are about to break", true));
    private final NumberSetting minDurability = add(new NumberSetting("Min Durability", "Durability percentage protected by Save Tools", 5, 1, 50, 1))
        .visibleWhen(saveTools::get);

    private int previousSlot = -1;

    public AutoTool() {
        super("Auto Tool", "Selects the best hotbar tool for the block you are mining.", Category.PLAYER);
        on(TickEvent.Pre.class, e -> tick());
    }

    private void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) return;
        boolean mining = mc.interactionManager.isBreakingBlock() && mc.crosshairTarget != null
            && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
        if (!mining) {
            if (previousSlot >= 0 && switchBack.get()) p.getInventory().selectedSlot = previousSlot;
            previousSlot = -1;
            return;
        }
        BlockState state = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos());
        if (state.isAir()) return;
        Optional<RegistryEntry.Reference<Enchantment>> efficiency = mc.world.getRegistryManager()
            .get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.EFFICIENCY);

        int current = p.getInventory().selectedSlot;
        int best = current;
        float bestScore = score(p.getInventory().getStack(current), state, efficiency);
        for (int slot = 0; slot < 9; slot++) {
            float s = score(p.getInventory().getStack(slot), state, efficiency);
            if (s > bestScore + 0.01f) {
                bestScore = s;
                best = slot;
            }
        }
        if (best != current) {
            if (previousSlot < 0) previousSlot = current;
            p.getInventory().selectedSlot = best;
        }
    }

    private float score(ItemStack stack, BlockState state, Optional<RegistryEntry.Reference<Enchantment>> efficiency) {
        if (saveTools.get() && stack.isDamageable()) {
            float pct = 100f * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage();
            if (pct <= minDurability.get()) return -1;
        }
        float speed = stack.getMiningSpeedMultiplier(state);
        if (speed > 1f && efficiency.isPresent()) {
            int level = EnchantmentHelper.getLevel(efficiency.get(), stack);
            if (level > 0) speed += level * level + 1; // vanilla efficiency bonus
        }
        if (state.isToolRequired() && stack.isSuitableFor(state)) speed += 0.5f; // prefer tools that make the block drop
        return speed;
    }

    @Override
    protected void onDisable() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null && previousSlot >= 0 && switchBack.get()) p.getInventory().selectedSlot = previousSlot;
        previousSlot = -1;
    }
}

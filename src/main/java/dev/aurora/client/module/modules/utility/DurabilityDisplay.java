package dev.aurora.client.module.modules.utility;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Durability of the held item with a bar and a low-durability warning. */
public final class DurabilityDisplay extends HudModule {
    private final NumberSetting warnAt = add(new NumberSetting("Warn At", "Warn when durability drops below this percentage", 10, 1, 50, 1));
    private final BooleanSetting warn = add(new BooleanSetting("Warning", "Show a notification when durability is low", true));
    private final BooleanSetting offhand = add(new BooleanSetting("Off-hand", "Also show the off-hand item", true));

    private ItemStack lastWarned; // not ItemStack.EMPTY: constructors must not initialize Minecraft registries
    private ItemStack sample;

    public DurabilityDisplay() {
        super("Durability", "Shows held item durability and warns before it breaks.", Category.UTILITY, 0.5, 0.82, false);
        on(TickEvent.Post.class, e -> checkWarning());
    }

    private void checkWarning() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null || !warn.get()) return;
        ItemStack held = p.getMainHandStack();
        if (!held.isDamageable() || held == lastWarned) return;
        float pct = 100f * (held.getMaxDamage() - held.getDamage()) / held.getMaxDamage();
        if (pct <= warnAt.get()) {
            lastWarned = held;
            AuroraClient.notifications().post("Low durability", held.getName().getString() + " is at " + Math.round(pct) + "%",
                NotificationManager.Type.WARNING, this);
        }
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        ClientPlayerEntity p = mc.player;
        ItemStack main = p == null ? ItemStack.EMPTY : p.getMainHandStack();
        ItemStack off = p == null || !offhand.get() ? ItemStack.EMPTY : p.getOffHandStack();
        if (editor && !main.isDamageable() && !off.isDamageable()) {
            if (sample == null) sample = new ItemStack(Items.DIAMOND_PICKAXE);
            main = sample;
        }
        int y = 0;
        int width = 90;
        y = row(ctx, tr, main, y, width);
        y = row(ctx, tr, off, y, width);
        setSize(width, Math.max(10, y));
    }

    private int row(DrawContext ctx, TextRenderer tr, ItemStack stack, int y, int width) {
        if (!stack.isDamageable()) return y;
        int remaining = stack.getMaxDamage() - stack.getDamage();
        float fraction = remaining / (float) stack.getMaxDamage();
        int color = ColorUtil.healthColor(fraction);
        HudStyle.panel(ctx, 0, y, width, 22);
        ctx.drawItem(stack, 3, y + 3);
        String text = remaining + " / " + stack.getMaxDamage();
        ctx.drawText(tr, text, 23, y + 3, Theme.TEXT, true);
        Render2D.roundedRect(ctx, 23, y + 14, width - 28, 3, 1.5f, 0x60000000);
        Render2D.roundedRect(ctx, 23, y + 14, (width - 28) * fraction, 3, 1.5f, color);
        return y + 24;
    }
}

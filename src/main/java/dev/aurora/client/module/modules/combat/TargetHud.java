package dev.aurora.client.module.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.module.Category;
import dev.aurora.client.render.Render2D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.util.Animation;
import dev.aurora.client.util.ColorUtil;
import dev.aurora.client.util.CombatTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;

import java.util.Locale;

/** Panel with the current target's head, name, health bar, distance and armor. */
public final class TargetHud extends HudModule {
    private final NumberSetting timeout = add(new NumberSetting("Timeout", "Seconds the panel stays after your last hit", 3, 1, 10, 0.5));
    private final BooleanSetting crosshairTarget = add(new BooleanSetting("Crosshair Target", "Also show the entity you are looking at", true));
    private final BooleanSetting armor = add(new BooleanSetting("Armor", "Show the target's armor", true));

    private final Animation health = new Animation(1, 8);
    private final Animation visibility = new Animation(0, 12);
    private LivingEntity shown;
    private ItemStack eggIcon;
    private Object eggFor;

    public TargetHud() {
        super("Target HUD", "Shows information about the entity you are fighting.", Category.COMBAT, 0.75, 0.55, false);
    }

    private LivingEntity currentTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        CombatTracker tracker = CombatTracker.INSTANCE;
        LivingEntity t = tracker.getTarget();
        if (t != null && System.currentTimeMillis() - tracker.getLastAttackMs() < timeout.get() * 1000 && t.isAlive()) return t;
        if (crosshairTarget.get() && mc.targetedEntity instanceof LivingEntity living && living.isAlive()) return living;
        return null;
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        LivingEntity target = currentTarget();
        if (target == null && editor) target = mc.player;
        visibility.setTarget(target != null ? 1 : 0);
        if (!HudStyle.animations()) visibility.snap(target != null ? 1 : 0);
        float v = (float) visibility.update();
        if (target != null) shown = target;
        int w = 140;
        int h = 46;
        setSize(w, h);
        if (shown == null || v < 0.02f) return;
        LivingEntity t = shown;
        TextRenderer tr = mc.textRenderer;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(w / 2f, h / 2f, 0);
        float scale = 0.9f + 0.1f * v;
        ctx.getMatrices().scale(scale, scale, 1);
        ctx.getMatrices().translate(-w / 2f, -h / 2f, 0);
        // Fade on top of the HUD renderer's own fade by multiplying the shader alpha.
        ctx.draw();
        float outerAlpha = RenderSystem.getShaderColor()[3];
        RenderSystem.setShaderColor(1, 1, 1, outerAlpha * v);
        try {
            draw(ctx, mc, tr, t, w, h);
        } finally {
            ctx.draw();
            RenderSystem.setShaderColor(1, 1, 1, outerAlpha);
            ctx.getMatrices().pop();
        }
    }

    private void draw(DrawContext ctx, MinecraftClient mc, TextRenderer tr, LivingEntity t, int w, int h) {
        HudStyle.background(ctx, 0, 0, w, h);
        int pad = 6;
        int head = h - pad * 2;
        // Head: player skin face, or the mob's spawn egg as an icon.
        if (t instanceof AbstractClientPlayerEntity player) {
            PlayerSkinDrawer.draw(ctx, player.getSkinTextures(), pad, pad, head);
        } else {
            if (eggFor != t.getType()) {
                SpawnEggItem egg = SpawnEggItem.forEntity(t.getType());
                eggIcon = egg == null ? ItemStack.EMPTY : new ItemStack(egg);
                eggFor = t.getType();
            }
            HudStyle.cell(ctx, pad, pad, head, head, HudStyle.cell());
            if (!eggIcon.isEmpty()) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(pad + 3, pad + 3, 0);
                ctx.getMatrices().scale(1.75f, 1.75f, 1);
                ctx.drawItem(eggIcon, 0, 0);
                ctx.getMatrices().pop();
            }
        }
        if (t.hurtTime > 0) {
            Render2D.roundedRect(ctx, pad, pad, head, head, Math.min(4, HudStyle.radius()), ColorUtil.withAlpha(0xFF3030, t.hurtTime * 12));
        }

        int textX = pad + head + 6;
        int barW = w - textX - pad;
        String name = t.getName().getString();
        name = HudStyle.trim(name, barW);
        HudStyle.text(ctx, name, textX, 7, HudStyle.text());

        float max = Math.max(1, t.getMaxHealth());
        float hp = t.getHealth() + t.getAbsorptionAmount();
        health.setTarget(Math.min(1, hp / max));
        float shownHp = (float) health.update();
        HudStyle.bar(ctx, textX, 19, barW, 5, shownHp, ColorUtil.healthColor(shownHp));
        String info = String.format(Locale.ROOT, "%.1f ❤", hp / 2f);
        if (mc.player != null && t != mc.player) {
            info += String.format(Locale.ROOT, "  %.1fm", mc.player.distanceTo(t));
        }
        HudStyle.text(ctx, info, textX, 28, HudStyle.sub());

        if (armor.get()) {
            int x = w - pad;
            for (ItemStack piece : t.getArmorItems()) {
                if (piece.isEmpty()) continue;
                x -= 11;
                ctx.getMatrices().push();
                ctx.getMatrices().translate(x, 34, 0);
                ctx.getMatrices().scale(0.6f, 0.6f, 1);
                ctx.drawItem(piece, 0, 0);
                ctx.getMatrices().pop();
            }
        }
    }
}

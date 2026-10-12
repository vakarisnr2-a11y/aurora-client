package dev.aurora.client.module.modules.combat;

import dev.aurora.client.gui.Theme;
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
        super("Target HUD", "Shows information about the entity you are fighting.", Category.COMBAT, 0.5, 0.6, false);
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

        Render2D.shadow(ctx, 0, 0, w, h, 6, 4, ColorUtil.withAlpha(0, (int) (110 * v)));
        Render2D.roundedRect(ctx, 0, 0, w, h, 6, ColorUtil.fade(Theme.panel(), v));

        // Head: player skin face, or the mob's spawn egg as an icon.
        if (t instanceof AbstractClientPlayerEntity player) {
            PlayerSkinDrawer.draw(ctx, player.getSkinTextures(), 6, 6, 34);
        } else {
            if (eggFor != t.getType()) {
                SpawnEggItem egg = SpawnEggItem.forEntity(t.getType());
                eggIcon = egg == null ? ItemStack.EMPTY : new ItemStack(egg);
                eggFor = t.getType();
            }
            Render2D.roundedRect(ctx, 6, 6, 34, 34, 4, ColorUtil.fade(Theme.surface(), v));
            if (!eggIcon.isEmpty()) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(9, 9, 0);
                ctx.getMatrices().scale(1.75f, 1.75f, 1);
                ctx.drawItem(eggIcon, 0, 0);
                ctx.getMatrices().pop();
            }
        }
        if (t.hurtTime > 0) {
            Render2D.roundedRect(ctx, 6, 6, 34, 34, 4, ColorUtil.withAlpha(0xFF3030, (int) (t.hurtTime * 12 * v)));
        }

        int alpha = Math.max(4, (int) (255 * v));
        String name = t.getName().getString();
        if (tr.getWidth(name) > 88) name = tr.trimToWidth(name, 84) + "…";
        ctx.drawText(tr, name, 46, 7, ColorUtil.withAlpha(Theme.TEXT, alpha), false);

        float max = Math.max(1, t.getMaxHealth());
        float hp = t.getHealth() + t.getAbsorptionAmount();
        health.setTarget(Math.min(1, hp / max));
        float shownHp = (float) health.update();
        Render2D.roundedRect(ctx, 46, 19, 88, 5, 2.5f, ColorUtil.fade(0xFF000000, 0.45f * v));
        Render2D.roundedRect(ctx, 46, 19, 88 * shownHp, 5, 2.5f,
            ColorUtil.fade(ColorUtil.healthColor(shownHp), v), ColorUtil.fade(ColorUtil.darken(ColorUtil.healthColor(shownHp), 0.25f), v));
        String info = String.format(Locale.ROOT, "%.1f ❤", hp / 2f);
        if (mc.player != null && t != mc.player) {
            info += String.format(Locale.ROOT, "  %.1fm", mc.player.distanceTo(t));
        }
        ctx.drawText(tr, info, 46, 28, ColorUtil.withAlpha(Theme.SUBTEXT, alpha), false);

        if (armor.get()) {
            int x = 134;
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
        ctx.getMatrices().pop();
    }
}

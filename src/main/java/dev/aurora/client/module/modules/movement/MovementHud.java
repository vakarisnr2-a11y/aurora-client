package dev.aurora.client.module.modules.movement;

import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.gui.Theme;
import dev.aurora.client.hud.HudModule;
import dev.aurora.client.hud.HudStyle;
import dev.aurora.client.module.Category;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.EnumSetting;
import dev.aurora.client.util.Animation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Locale;

/** Speed and movement state readout. */
public final class MovementHud extends HudModule {
    public enum Unit {
        BLOCKS_PER_SECOND("b/s", 1), KILOMETERS_PER_HOUR("km/h", 3.6);

        final String suffix;
        final double multiplier;

        Unit(String suffix, double multiplier) {
            this.suffix = suffix;
            this.multiplier = multiplier;
        }
    }

    private final EnumSetting<Unit> unit = add(new EnumSetting<>("Unit", "Speed unit", Unit.BLOCKS_PER_SECOND));
    private final BooleanSetting state = add(new BooleanSetting("State", "Show sprinting / sneaking / flying state", true));
    private final BooleanSetting vertical = add(new BooleanSetting("Vertical Speed", "Show vertical speed", false));

    private final Animation speed = new Animation(0, 10);
    private double verticalSpeed;

    public MovementHud() {
        super("Movement HUD", "Shows your speed and movement state.", Category.MOVEMENT, 0.5, 0.7, false);
        on(TickEvent.Post.class, e -> {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p == null) return;
            speed.setTarget(Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ) * 20);
            verticalSpeed = (p.getY() - p.prevY) * 20;
        });
    }

    @Override
    public void render(DrawContext ctx, float tickDelta, boolean editor) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%.2f %s", speed.update() * unit.get().multiplier, unit.get().suffix));
        if (vertical.get()) sb.append(String.format(Locale.ROOT, " §8|§r %+.1f", verticalSpeed * unit.get().multiplier));
        if (state.get()) sb.append(" §8|§r ").append(stateOf(p));
        setSize(HudStyle.drawLabelValue(ctx, "Speed", sb.toString(), Theme.TEXT, true), HudStyle.lineHeight());
    }

    private static String stateOf(ClientPlayerEntity p) {
        if (p == null) return "Idle";
        if (p.getAbilities().flying) return "Flying";
        if (p.isFallFlying()) return "Gliding";
        if (p.isSwimming()) return "Swimming";
        if (p.isSneaking()) return "Sneaking";
        if (p.isSprinting()) return "Sprinting";
        if (!p.isOnGround()) return "Airborne";
        return p.getX() != p.prevX || p.getZ() != p.prevZ ? "Walking" : "Idle";
    }
}

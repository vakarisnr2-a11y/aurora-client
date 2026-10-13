package dev.aurora.client.module.modules.world;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Stops drawing entities beyond a set distance from the camera (a hard cap on top of vanilla's entity distance
 * option). Players are only limited when "Limit Players" is on. Only rendering is skipped: the entities still exist.
 */
public final class EntityDistance extends Module {
    private final NumberSetting distance = add(new NumberSetting("Distance", "Blocks beyond which mobs, items and other entities are hidden", 48, 8, 256, 4));
    private final BooleanSetting players = add(new BooleanSetting("Limit Players", "Also hide players beyond Player Distance", false));
    private final NumberSetting playerDistance = add(new NumberSetting("Player Distance", "Blocks beyond which players are hidden", 96, 8, 256, 4))
        .visibleWhen(players::get);

    public EntityDistance() {
        super("Entity Distance", "Hides entities beyond a set distance for better FPS. Players are optional.", Category.WORLD);
    }

    /** Called for every entity before it is drawn; (x, y, z) is the camera position. */
    public boolean shouldHide(Entity entity, double x, double y, double z) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity == mc.getCameraEntity() || entity == mc.player || entity.hasPassenger(mc.player) || entity.hasVehicle() && entity.getVehicle() == mc.player) {
            return false;
        }
        boolean isPlayer = entity instanceof PlayerEntity;
        if (isPlayer && !players.get()) return false;
        double max = isPlayer ? playerDistance.get() : distance.get();
        return beyond(entity.getX() - x, entity.getY() - y, entity.getZ() - z, max);
    }

    public static boolean beyond(double dx, double dy, double dz, double max) {
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    @Override
    public String getDisplayInfo() {
        return distance.getInt() + "m";
    }
}

package dev.aurora.client.module.modules.combat;

import dev.aurora.client.event.events.Render3DEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render3D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Entity hitboxes (like F3+B) with a custom color, line width and entity filter.
 *
 * <p>Follows vanilla's rules: boxes are depth-tested (never visible through walls), invisible entities get none,
 * and nothing is drawn when the server enables reduced debug info. While enabled, vanilla's own F3+B boxes are
 * hidden (EntityRenderDispatcherMixin) so they are not drawn twice.</p>
 */
public final class Hitboxes extends Module {
    public enum Kind {
        PLAYER, MOB, ITEM, PROJECTILE, OTHER
    }

    private static final int EYE_COLOR = 0xFFFF0000;
    private static final int LOOK_COLOR = 0xFF0000FF;

    private final ColorSetting color = add(new ColorSetting("Color", "Hitbox color", 0xFFFFFFFF));
    private final ColorSetting targetColor = add(new ColorSetting("Target Color", "Color of the entity under your crosshair", 0xFFFF5555));
    private final NumberSetting width = add(new NumberSetting("Width", "Line thickness", 2, 1, 6, 0.5));
    private final BooleanSetting eyeLine = add(new BooleanSetting("Eye Line", "Red line at eye height, like F3+B", false));
    private final BooleanSetting lookVector = add(new BooleanSetting("Look Vector", "Blue line showing where entities look, like F3+B", false));
    private final BooleanSetting players = add(new BooleanSetting("Players", "Show player hitboxes", true));
    private final BooleanSetting mobs = add(new BooleanSetting("Mobs", "Show mob and animal hitboxes", true));
    private final BooleanSetting items = add(new BooleanSetting("Items", "Show dropped item and XP orb hitboxes", false));
    private final BooleanSetting projectiles = add(new BooleanSetting("Projectiles", "Show arrow, fireball and other projectile hitboxes", true));
    private final BooleanSetting others = add(new BooleanSetting("Others", "Show all other entities (TNT, minecarts, armor stands...)", false));
    private final BooleanSetting self = add(new BooleanSetting("Self", "Show your own hitbox in third person", false));

    public Hitboxes() {
        super("Hitboxes", "Entity hitboxes with custom color and width.", Category.COMBAT);
        on(Render3DEvent.class, this::render);
    }

    private void render(Render3DEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.hasReducedDebugInfo()) return;
        Vec3d cam = event.cameraPos;
        Entity cameraEntity = mc.getCameraEntity();
        boolean thirdPerson = event.camera.isThirdPerson();
        Render3D.begin(event, true);
        try {
            BufferBuilder lines = Render3D.lines(width.getFloat());
            for (Entity entity : mc.world.getEntities()) {
                if (entity.isInvisible() || !shows(kind(entity))) continue;
                if (entity == cameraEntity && !(thirdPerson && self.get())) continue;
                if (!entity.shouldRender(cam.x, cam.y, cam.z)) continue;
                Vec3d pos = entity.getLerpedPos(event.tickDelta);
                float ox = (float) (pos.x - cam.x);
                float oy = (float) (pos.y - cam.y);
                float oz = (float) (pos.z - cam.z);
                Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
                int c = entity == mc.targetedEntity ? targetColor.getColor() : color.getColor();
                Render3D.boxEdges(lines, ox + (float) box.minX, oy + (float) box.minY, oz + (float) box.minZ,
                    ox + (float) box.maxX, oy + (float) box.maxY, oz + (float) box.maxZ, c);
                float eye = oy + entity.getStandingEyeHeight();
                if (eyeLine.get() && entity instanceof LivingEntity) {
                    Render3D.boxEdges(lines, ox + (float) box.minX, eye - 0.01f, oz + (float) box.minZ,
                        ox + (float) box.maxX, eye + 0.01f, oz + (float) box.maxZ, EYE_COLOR);
                }
                if (lookVector.get()) {
                    Vec3d look = entity.getRotationVec(event.tickDelta);
                    Render3D.line(lines, ox, eye, oz, ox + (float) look.x * 2, eye + (float) look.y * 2, oz + (float) look.z * 2, LOOK_COLOR);
                }
            }
            Render3D.draw(lines);
        } finally {
            Render3D.end();
        }
    }

    private boolean shows(Kind kind) {
        return switch (kind) {
            case PLAYER -> players.get();
            case MOB -> mobs.get();
            case ITEM -> items.get();
            case PROJECTILE -> projectiles.get();
            case OTHER -> others.get();
        };
    }

    private static Kind kind(Entity entity) {
        return kind(entity instanceof PlayerEntity, entity instanceof LivingEntity && !(entity instanceof ArmorStandEntity),
            entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity, entity instanceof ProjectileEntity);
    }

    /** Armor stands are living entities but count as "other"; players are checked before mobs. */
    public static Kind kind(boolean player, boolean mob, boolean item, boolean projectile) {
        if (player) return Kind.PLAYER;
        if (mob) return Kind.MOB;
        if (item) return Kind.ITEM;
        if (projectile) return Kind.PROJECTILE;
        return Kind.OTHER;
    }
}

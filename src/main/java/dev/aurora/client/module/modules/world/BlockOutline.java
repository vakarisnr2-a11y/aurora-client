package dev.aurora.client.module.modules.world;

import dev.aurora.client.event.events.Render3DEvent;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.render.Render3D;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.ColorSetting;
import dev.aurora.client.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

/**
 * Replaces the block selection outline with a configurable color, width and fill.
 * Vanilla's outline is cancelled through Fabric's BLOCK_OUTLINE event; the block is drawn in the
 * same frame from the Render3D event (with depth testing, so it is never visible through walls).
 */
public final class BlockOutline extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", "Outline color", 0xFFA78BFA));
    private final NumberSetting width = add(new NumberSetting("Width", "Outline thickness", 2.5, 1, 6, 0.5));
    private final BooleanSetting fill = add(new BooleanSetting("Fill", "Fill the selected block faces", true));
    private final ColorSetting fillColor = add(new ColorSetting("Fill Color", "Fill color", 0x268B5CF6)).visibleWhen(fill::get);

    private BlockPos pos;
    private BlockState state;

    public BlockOutline() {
        super("Block Outline", "Custom block selection outline color, width and fill.", Category.WORLD);
        on(Render3DEvent.class, this::render);
    }

    /** Called from the BLOCK_OUTLINE event. Returns false to cancel the vanilla outline. */
    public boolean handleVanillaOutline(WorldRenderContext.BlockOutlineContext context) {
        if (!isEnabled()) return true;
        pos = context.blockPos().toImmutable();
        state = context.blockState();
        return false;
    }

    private void render(Render3DEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (pos == null || state == null || mc.world == null || mc.cameraEntity == null) return;
        VoxelShape shape = state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.cameraEntity));
        Vec3d cam = event.cameraPos;
        float ox = (float) (pos.getX() - cam.x);
        float oy = (float) (pos.getY() - cam.y);
        float oz = (float) (pos.getZ() - cam.z);
        pos = null; // only draw blocks reported this frame

        if (shape.isEmpty()) return;
        Render3D.begin(event, true);
        try {
            if (fill.get()) {
                BufferBuilder quads = Render3D.quads();
                int c = fillColor.getColor();
                shape.forEachBox((x1, y1, z1, x2, y2, z2) -> Render3D.boxFaces(quads,
                    ox + (float) x1 - 0.002f, oy + (float) y1 - 0.002f, oz + (float) z1 - 0.002f,
                    ox + (float) x2 + 0.002f, oy + (float) y2 + 0.002f, oz + (float) z2 + 0.002f, c));
                Render3D.draw(quads);
            }
            BufferBuilder lines = Render3D.lines(width.getFloat());
            int c = color.getColor();
            shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> Render3D.line(lines,
                ox + (float) x1, oy + (float) y1, oz + (float) z1, ox + (float) x2, oy + (float) y2, oz + (float) z2, c));
            Render3D.draw(lines);
        } finally {
            Render3D.end();
        }
    }
}

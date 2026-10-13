package dev.aurora.client;

import dev.aurora.client.module.modules.combat.BowCharge;
import dev.aurora.client.module.modules.world.ItemPhysics;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.item.BowItem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Compares Aurora's copies of vanilla formulas with the real 1.21.1 methods (both are pure, no game state needed). */
class VanillaParityTest {
    @Test
    void bowCurveMatchesBowItem() {
        for (int ticks = 0; ticks <= 40; ticks++) {
            assertEquals(BowItem.getPullProgress(ticks), BowCharge.bowProgress(ticks), 0f, "ticks " + ticks);
        }
    }

    @Test
    void renderedCopiesMatchItemEntityRenderer() throws ReflectiveOperationException {
        Method vanilla = ItemEntityRenderer.class.getDeclaredMethod("getRenderedAmount", int.class);
        vanilla.setAccessible(true);
        for (int count = 0; count <= 99; count++) {
            assertEquals(vanilla.invoke(null, count), ItemPhysics.renderedCopies(count), "count " + count);
        }
    }
}

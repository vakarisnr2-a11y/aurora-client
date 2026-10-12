package dev.aurora.client.util;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.modules.movement.CameraSmoothing;
import dev.aurora.client.module.modules.movement.Freelook;
import dev.aurora.client.module.modules.utility.Zoom;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Routes mouse-look deltas (called once per frame from {@code Mouse#updateMouse}).
 *
 * <ol>
 *   <li>Freelook captures the deltas to orbit the camera without turning the player.</li>
 *   <li>Zoom scales sensitivity down with the zoom factor so aiming stays controllable.</li>
 *   <li>Camera Smoothing low-pass filters the remaining rotation.</li>
 * </ol>
 */
public final class LookController {
    private LookController() {
    }

    public static void changeLookDirection(ClientPlayerEntity player, double dx, double dy) {
        AuroraClient client = AuroraClient.get();
        if (client == null) {
            player.changeLookDirection(dx, dy);
            return;
        }
        ModuleManager modules = AuroraClient.modules();
        Freelook freelook = modules.get(Freelook.class);
        if (freelook.isEnabled()) {
            freelook.addRotation(dx, dy);
            return;
        }
        double sensitivity = modules.get(Zoom.class).getSensitivityFactor();
        dx *= sensitivity;
        dy *= sensitivity;
        CameraSmoothing smoothing = modules.get(CameraSmoothing.class);
        if (smoothing.isEnabled()) {
            smoothing.apply(player, dx, dy);
        } else {
            player.changeLookDirection(dx, dy);
        }
    }
}

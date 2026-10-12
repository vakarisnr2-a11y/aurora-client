package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;

/** Removes the camera shake when taking damage (GameRendererMixin cancels tiltViewWhenHurt). */
public final class NoHurtCam extends Module {
    public NoHurtCam() {
        super("No Hurt Cam", "Removes the camera tilt when you take damage.", Category.VISUALS);
    }
}

package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.pack.BedwarsPack;

/** Switches the bundled Bedwars resource pack (3Supay 16x) on and off (triggers a resource reload). */
public final class BedwarsPackModule extends Module {
    public BedwarsPackModule() {
        super("Bedwars Pack", "Enables the bundled 3Supay 16x Bedwars texture pack (short swords, clean blocks, clear GUI).",
            Category.VISUALS);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    protected void onEnable() {
        if (mc() == null || mc().options == null) return;
        if (!BedwarsPack.setEnabled(true)) {
            AuroraClient.notifications().post("Bedwars Pack", "Pack not found - is the jar complete?", NotificationManager.Type.ERROR);
        }
    }

    @Override
    protected void onDisable() {
        if (mc() != null && mc().options != null) BedwarsPack.setEnabled(false);
    }
}

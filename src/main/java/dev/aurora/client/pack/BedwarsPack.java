package dev.aurora.client.pack;

import dev.aurora.client.AuroraClient;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Registers the bundled "Aurora Bedwars" resource pack (src/main/resources/resourcepacks/aurora_bedwars).
 * It appears in Options > Resource Packs and can also be switched with the Bedwars Pack module.
 */
public final class BedwarsPack {
    public static final Identifier ID = Identifier.of(AuroraClient.MOD_ID, "aurora_bedwars");
    /** Vanilla's container title color, and the light color used on the pack's dark container screens. */
    public static final int VANILLA_TITLE = 0x404040, THEMED_TITLE = 0xE6E8F0;

    private static String profileId;

    private BedwarsPack() {
    }

    public static void register() {
        FabricLoader.getInstance().getModContainer(AuroraClient.MOD_ID).ifPresent(container -> {
            boolean ok = ResourceManagerHelper.registerBuiltinResourcePack(ID, container,
                Text.literal("Aurora Bedwars"), ResourcePackActivationType.NORMAL);
            if (!ok) AuroraClient.LOGGER.warn("Could not register the built-in Bedwars resource pack");
        });
    }

    /** The pack's profile id in the resource pack manager, or null if it is not available. */
    public static String findProfileId() {
        ResourcePackManager manager = MinecraftClient.getInstance().getResourcePackManager();
        for (ResourcePackProfile profile : manager.getProfiles()) {
            if (profile.getId().contains("aurora_bedwars")) return profile.getId();
        }
        return null;
    }

    /** Whether the pack is currently enabled in the resource pack list. */
    public static boolean isActive() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        if (profileId == null) profileId = findProfileId();
        return profileId != null && mc.getResourcePackManager().getEnabledIds().contains(profileId);
    }

    /**
     * Title color for a container screen: the pack draws the inventory and chest screens dark, so their titles
     * switch to a light color while it is active. Other screens keep their vanilla look and color.
     */
    public static int titleColor(Screen screen, int color) {
        if (color != VANILLA_TITLE) return color;
        boolean themed = screen instanceof GenericContainerScreen || screen instanceof InventoryScreen;
        return themed && isActive() ? THEMED_TITLE : color;
    }

    /** Enables or disables the pack and reloads resources if the selection changed. */
    public static boolean setEnabled(boolean enabled) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String id = findProfileId();
        if (id == null || mc.options == null) return false;
        ResourcePackManager manager = mc.getResourcePackManager();
        boolean changed = enabled ? manager.enable(id) : manager.disable(id);
        if (changed) mc.options.refreshResourcePacks(manager); // saves options and reloads resources
        return true;
    }
}

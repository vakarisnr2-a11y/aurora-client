package dev.aurora.client.pack;

import dev.aurora.client.AuroraClient;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
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

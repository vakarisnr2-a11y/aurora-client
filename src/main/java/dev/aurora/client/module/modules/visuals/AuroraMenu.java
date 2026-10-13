package dev.aurora.client.module.modules.visuals;

import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.KeybindSetting;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

/**
 * Aurora's own main menu, menu backgrounds and loading screens (on by default). Each part can be switched off.
 * "Vanilla menu" on the Aurora main menu opens the normal title screen once.
 */
public final class AuroraMenu extends Module {
    private final BooleanSetting mainMenu = add(new BooleanSetting("Main Menu", "Replace the title screen with the Aurora menu", true));
    private final BooleanSetting backgrounds = add(new BooleanSetting("Menu Backgrounds", "Aurora backdrop instead of the panorama in menus", true));
    private final BooleanSetting loading = add(new BooleanSetting("Loading Screens", "Aurora backdrop on world loading and connecting screens", true));
    private final BooleanSetting splash = add(new BooleanSetting("Splash Screen", "Aurora look for the resource loading overlay", true));

    private static boolean vanillaOnce;

    public AuroraMenu() {
        super("Aurora Menu", "Themed Aurora main menu, menu backgrounds and loading screens with the Aurora logo.", Category.VISUALS,
            KeybindSetting.NONE, true);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    public boolean notifiesOnToggle() {
        return false;
    }

    /** The next title screen shown is the vanilla one. */
    public static void showVanillaOnce() {
        vanillaOnce = true;
    }

    /** Whether to swap this vanilla title screen for the Aurora menu (consumes the "vanilla once" flag). */
    public boolean replaceTitleScreen() {
        if (!isEnabled() || !mainMenu.get()) return false;
        if (vanillaOnce) {
            vanillaOnce = false;
            return false;
        }
        return true;
    }

    /** Menus get the backdrop with Menu Backgrounds; loading-type screens also with Loading Screens alone. */
    public boolean themedBackground(Screen screen) {
        if (!isEnabled()) return false;
        return backgrounds.get() || (loading.get() && isLoadingScreen(screen));
    }

    private static boolean isLoadingScreen(Screen screen) {
        return screen instanceof LevelLoadingScreen || screen instanceof ConnectScreen || screen instanceof MessageScreen
            || screen instanceof ProgressScreen || screen instanceof DownloadingTerrainScreen;
    }

    public boolean themedLoading() {
        return isEnabled() && loading.get();
    }

    public boolean themedSplash() {
        return isEnabled() && splash.get();
    }
}

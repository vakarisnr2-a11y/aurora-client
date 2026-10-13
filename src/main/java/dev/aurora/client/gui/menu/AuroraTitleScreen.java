package dev.aurora.client.gui.menu;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.gui.ui.Ui;
import dev.aurora.client.gui.ui.UiFont;
import dev.aurora.client.util.ColorUtil;
import dev.aurora.client.module.modules.visuals.AuroraMenu;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Aurora's main menu: animated aurora backdrop, the logo and a column of rounded buttons. "Vanilla menu" opens the
 * normal title screen once (also the way to reach buttons other mods add there).
 */
public class AuroraTitleScreen extends Screen {
    private static final int BUTTON_W = 210;
    private static final int BUTTON_H = 22;
    private static final long FADE_MS = 700;
    private static long firstShown = -1;

    public AuroraTitleScreen() {
        super(Text.literal("Aurora"));
    }

    @Override
    protected void init() {
        if (firstShown < 0) firstShown = Util.getMeasuringTimeMs();
        int x = width / 2 - BUTTON_W / 2;
        int y = buttonsTop();
        int step = BUTTON_H + 6;
        addDrawableChild(new AuroraButton(x, y, BUTTON_W, BUTTON_H, Text.translatable("menu.singleplayer"),
            b -> client.setScreen(new SelectWorldScreen(this)), true));
        addDrawableChild(new AuroraButton(x, y + step, BUTTON_W, BUTTON_H, Text.translatable("menu.multiplayer"),
            b -> client.setScreen(new MultiplayerScreen(this))));
        int half = (BUTTON_W - 6) / 2;
        addDrawableChild(new AuroraButton(x, y + step * 2, half, BUTTON_H, Text.translatable("menu.online"),
            b -> client.setScreen(new RealmsMainScreen(this))));
        Screen mods = modsScreen();
        AuroraButton modsButton = addDrawableChild(new AuroraButton(x + half + 6, y + step * 2, half, BUTTON_H, Text.literal("Mods"),
            b -> {
                Screen screen = modsScreen();
                if (screen != null) client.setScreen(screen);
            }));
        modsButton.active = mods != null;
        addDrawableChild(new AuroraButton(x, y + step * 3, half, BUTTON_H, Text.translatable("menu.options"),
            b -> client.setScreen(new OptionsScreen(this, client.options))));
        addDrawableChild(new AuroraButton(x + half + 6, y + step * 3, half, BUTTON_H, Text.translatable("menu.quit"),
            b -> client.scheduleStop()));
        addDrawableChild(new AuroraButton(8, height - 24, 96, 16, Text.literal("Vanilla menu"), b -> {
            AuroraMenu.showVanillaOnce();
            client.setScreen(new TitleScreen());
        }));
    }

    /** ModMenu's mod list, when ModMenu is installed (opened by reflection, there is no compile-time dependency). */
    private Screen modsScreen() {
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) return null;
        try {
            return (Screen) Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(Screen.class).newInstance(this);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private int logoSize() {
        return height < 300 ? 48 : 64;
    }

    /** Logo (emblem + wordmark + subtitle) above the buttons, centred as a group. */
    private int logoTop() {
        int group = logoSize() + 4 + 26 + 16 + (BUTTON_H + 6) * 4;
        return Math.max(8, (height - group) / 2 - 6);
    }

    private int buttonsTop() {
        return logoTop() + logoSize() + 4 + 26 + 18;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        AuroraBackground.render(ctx, width, height);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        float fade = Math.min(1f, (Util.getMeasuringTimeMs() - firstShown) / (float) FADE_MS);
        int logoY = logoTop();
        int logoH = AuroraLogo.draw(ctx, width / 2, logoY, logoSize(), fade);
        String sub = "Client  ·  Minecraft " + SharedConstants.getGameVersion().getName();
        UiFont.centered(ctx, sub, width / 2f, logoY + logoH + 2, ColorUtil.withAlpha(Ui.SUBTEXT, Math.max(4, Math.round(255 * fade))), UiFont.Size.BODY, false);
        String version = "Aurora " + AuroraClient.version();
        UiFont.draw(ctx, version, width - 8 - UiFont.width(version), height - 19, Ui.MUTED);
        if (fade < 1f) ctx.fill(0, 0, width, height, Math.round(255 * (1 - fade)) << 24);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}

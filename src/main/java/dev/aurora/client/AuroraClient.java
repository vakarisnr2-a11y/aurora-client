package dev.aurora.client;

import dev.aurora.client.command.AuroraCommands;
import dev.aurora.client.config.ConfigManager;
import dev.aurora.client.config.LoadReport;
import dev.aurora.client.event.EventBus;
import dev.aurora.client.event.events.ChatReceiveEvent;
import dev.aurora.client.event.EventPriority;
import dev.aurora.client.event.events.ModuleToggleEvent;
import dev.aurora.client.event.events.MouseButtonEvent;
import dev.aurora.client.event.events.Render2DEvent;
import dev.aurora.client.event.events.Render3DEvent;
import dev.aurora.client.event.events.TickEvent;
import dev.aurora.client.gui.HudEditorScreen;
import dev.aurora.client.hud.HudRenderer;
import dev.aurora.client.keybind.KeybindManager;
import dev.aurora.client.module.ModuleManager;
import dev.aurora.client.module.Modules;
import dev.aurora.client.module.modules.utility.BetterTooltips;
import dev.aurora.client.module.modules.world.BlockOutline;
import dev.aurora.client.notification.NotificationManager;
import dev.aurora.client.pack.BedwarsPack;
import dev.aurora.client.util.CombatTracker;
import dev.aurora.client.util.CpsTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint. Wires the event bus, modules, configuration, HUD and input systems to
 * Fabric API callbacks. Mixins only forward raw input and a few render hooks into these systems.
 */
public final class AuroraClient implements ClientModInitializer {
    public static final String MOD_ID = "aurora";
    public static final String NAME = "Aurora";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static AuroraClient instance;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private NotificationManager notificationManager;
    private KeybindManager keybindManager;
    private HudRenderer hudRenderer;
    private String version = "dev";
    private boolean configLoaded;
    private final Quaternionf viewRotation = new Quaternionf();

    @Override
    public void onInitializeClient() {
        instance = this;
        long start = System.currentTimeMillis();
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");

        eventBus = new EventBus();
        moduleManager = new ModuleManager(eventBus);
        moduleManager.registerAll(Modules.createAll());
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID), moduleManager);
        notificationManager = new NotificationManager(moduleManager);
        keybindManager = new KeybindManager(moduleManager, eventBus);
        hudRenderer = new HudRenderer(moduleManager);
        CpsTracker.INSTANCE.register(eventBus);
        CombatTracker.INSTANCE.register(eventBus);
        eventBus.subscribe(this, ModuleToggleEvent.class, e -> notificationManager.onModuleToggled(e.module, e.enabled));
        // Toast buttons (e.g. the screenshot's Open / Copy) can be clicked while a screen with a cursor is open.
        eventBus.subscribe(this, MouseButtonEvent.class, e -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!e.inScreen || e.action != GLFW.GLFW_PRESS || e.button != GLFW.GLFW_MOUSE_BUTTON_LEFT || mc.getWindow() == null) return;
            double x = mc.mouse.getX() * mc.getWindow().getScaledWidth() / Math.max(1, mc.getWindow().getWidth());
            double y = mc.mouse.getY() * mc.getWindow().getScaledHeight() / Math.max(1, mc.getWindow().getHeight());
            if (notificationManager.click(x, y)) e.cancel();
        }, EventPriority.HIGHEST);

        registerFabricHooks();
        AuroraCommands.register();
        BedwarsPack.register();

        LOGGER.info("{} {} initialized: {} modules registered in {} ms", NAME, version,
            moduleManager.getAll().size(), System.currentTimeMillis() - start);
    }

    private void registerFabricHooks() {
        // Game options do not exist yet during onInitializeClient, so profiles load once the client has started.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> loadConfig());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (configLoaded) configManager.save();
            moduleManager.getEnabled().forEach(m -> {
                if (!m.savesEnabledState()) m.setEnabled(false, false);
            });
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> eventBus.post(TickEvent.Pre.INSTANCE.reset()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            eventBus.post(TickEvent.Post.INSTANCE.reset());
            if (configLoaded) configManager.tick();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            float delta = tickCounter.getTickDelta(true);
            int w = context.getScaledWindowWidth();
            int h = context.getScaledWindowHeight();
            if (!(mc.currentScreen instanceof HudEditorScreen)) hudRenderer.render(context, delta);
            eventBus.post(Render2DEvent.INSTANCE.set(context, delta, w, h));
            notificationManager.render(context, w, h);
        });

        WorldRenderEvents.LAST.register(context -> {
            Render3DEvent event = Render3DEvent.INSTANCE.set(context.tickCounter().getTickDelta(true), context.camera());
            // Same view matrix vanilla builds in GameRenderer#renderWorld: the inverse camera rotation.
            event.view.rotation(context.camera().getRotation().conjugate(viewRotation));
            event.projection.set(RenderSystem.getProjectionMatrix());
            eventBus.post(event);
        });

        WorldRenderEvents.BLOCK_OUTLINE.register((worldContext, outlineContext) ->
            moduleManager.get(BlockOutline.class).handleVanillaOutline(outlineContext));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) CombatTracker.INSTANCE.onAttack(player, entity);
            return ActionResult.PASS;
        });

        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            BetterTooltips tooltips = moduleManager.get(BetterTooltips.class);
            if (tooltips.isEnabled()) tooltips.onTooltip(stack, tooltipType, lines);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
            eventBus.post(ChatReceiveEvent.INSTANCE.set(message, sender == null ? null : sender.getId(), false)));
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
            eventBus.post(ChatReceiveEvent.INSTANCE.set(message, null, overlay)));
    }

    private void loadConfig() {
        LoadReport report = configManager.loadActive();
        configLoaded = true;
        if (report.isRecovered()) {
            notificationManager.post("Config recovered", "Malformed profile backed up as " + report.getBackupFile(),
                NotificationManager.Type.WARNING);
        } else if (!report.getWarnings().isEmpty()) {
            notificationManager.post("Config warnings", report.getWarnings().size() + " invalid value(s) were reset (see log)",
                NotificationManager.Type.WARNING);
        }
        LOGGER.info("Loaded profile '{}' ({} modules, {} warnings)", report.getProfile(), report.getModulesLoaded(),
            report.getWarnings().size());
    }

    public static AuroraClient get() {
        return instance;
    }

    public static EventBus bus() {
        return instance.eventBus;
    }

    public static ModuleManager modules() {
        return instance.moduleManager;
    }

    public static ConfigManager config() {
        return instance.configManager;
    }

    public static NotificationManager notifications() {
        return instance.notificationManager;
    }

    public static KeybindManager keybinds() {
        return instance.keybindManager;
    }

    /** Module lookup for mixins; returns null before the client is initialized. */
    public static <T extends dev.aurora.client.module.Module> T module(Class<T> type) {
        return instance == null || instance.moduleManager == null ? null : instance.moduleManager.get(type);
    }

    public static HudRenderer hud() {
        return instance.hudRenderer;
    }

    public static String version() {
        return instance == null ? "dev" : instance.version;
    }
}

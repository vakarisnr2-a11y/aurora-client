package dev.aurora.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.aurora.client.AuroraClient;
import dev.aurora.client.config.ConfigManager;
import dev.aurora.client.config.LoadReport;
import dev.aurora.client.config.ThemeIO;
import dev.aurora.client.gui.CrosshairEditorScreen;
import dev.aurora.client.gui.KeybindsScreen;
import dev.aurora.client.hud.HudLayouts;
import dev.aurora.client.gui.ClickGuiScreen;
import dev.aurora.client.gui.HudEditorScreen;
import dev.aurora.client.module.Module;
import dev.aurora.client.module.modules.misc.ClickGui;
import dev.aurora.client.module.modules.utility.Screenshot;
import dev.aurora.client.module.modules.visuals.HudTheme;
import dev.aurora.client.setting.KeybindSetting;
import dev.aurora.client.util.KeyNames;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Client-side commands: {@code /aurora toggle|bind|unbind|reset|list|gui|hud|profile ...}.
 */
public final class AuroraCommands {
    private static final SuggestionProvider<FabricClientCommandSource> MODULES = (ctx, builder) -> {
        for (Module m : AuroraClient.modules().getAll()) builder.suggest(m.getId());
        return builder.buildFuture();
    };
    private static final SuggestionProvider<FabricClientCommandSource> PROFILES = (ctx, builder) -> {
        for (String p : AuroraClient.config().listProfiles()) builder.suggest(p);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> LAYOUTS = (ctx, builder) -> {
        for (String l : layouts().list()) builder.suggest(l);
        return builder.buildFuture();
    };

    private AuroraCommands() {
    }

    private static HudLayouts layouts() {
        return new HudLayouts(AuroraClient.config().getRoot().resolve("layouts"));
    }

    private static ClickGui clickGui() {
        return AuroraClient.modules().get(ClickGui.class);
    }

    private static HudTheme hudTheme() {
        return AuroraClient.modules().get(HudTheme.class);
    }

    private static int importTheme(FabricClientCommandSource source, String json, String from) {
        try {
            int n = ThemeIO.importString(json, clickGui(), hudTheme());
            return ok(source, "Theme imported from " + from + " (" + n + " values)");
        } catch (IllegalArgumentException e) {
            return error(source, "The " + from + " does not hold an Aurora theme");
        }
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("aurora")
            .executes(ctx -> help(ctx.getSource()))
            .then(literal("toggle").then(argument("module", StringArgumentType.word()).suggests(MODULES).executes(ctx -> {
                Module m = module(ctx);
                if (m == null) return 0;
                m.toggle();
                return ok(ctx.getSource(), m.getName() + (m.isEnabled() ? " enabled" : " disabled"));
            })))
            .then(literal("bind").then(argument("module", StringArgumentType.word()).suggests(MODULES)
                .then(argument("key", StringArgumentType.word()).executes(ctx -> {
                    Module m = module(ctx);
                    if (m == null) return 0;
                    Integer code = parseKey(StringArgumentType.getString(ctx, "key"));
                    if (code == null) return error(ctx.getSource(), "Unknown key. Examples: r, f6, left.shift, mouse4, none");
                    m.getKeybind().set(code);
                    return ok(ctx.getSource(), m.getName() + " bound to " + KeyNames.of(m.getKeybind().get()));
                }))))
            .then(literal("unbind").then(argument("module", StringArgumentType.word()).suggests(MODULES).executes(ctx -> {
                Module m = module(ctx);
                if (m == null) return 0;
                m.getKeybind().unbind();
                return ok(ctx.getSource(), m.getName() + " unbound");
            })))
            .then(literal("reset").then(argument("module", StringArgumentType.word()).suggests(MODULES).executes(ctx -> {
                Module m = module(ctx);
                if (m == null) return 0;
                m.resetSettings();
                return ok(ctx.getSource(), m.getName() + " settings reset");
            })))
            .then(literal("list").executes(ctx -> {
                StringBuilder sb = new StringBuilder();
                for (Module m : AuroraClient.modules().getAll()) {
                    sb.append(m.isEnabled() ? "§a" : "§7").append(m.getId()).append("§r ");
                }
                ctx.getSource().sendFeedback(Text.literal(sb.toString().trim()));
                return 1;
            }))
            .then(literal("gui").executes(ctx -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                // Deferred: the chat screen closes itself after the command runs.
                mc.send(() -> mc.setScreen(new ClickGuiScreen(AuroraClient.modules().get(ClickGui.class))));
                return 1;
            }))
            .then(literal("hud").executes(ctx -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.send(() -> mc.setScreen(new HudEditorScreen(null)));
                return 1;
            }))
            .then(literal("keybinds").executes(ctx -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.send(() -> mc.setScreen(new KeybindsScreen(null)));
                return 1;
            }))
            .then(literal("crosshair").executes(ctx -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.send(() -> mc.setScreen(new CrosshairEditorScreen(null)));
                return 1;
            }))
            .then(literal("theme")
                .then(literal("export").executes(ctx -> {
                    try {
                        Path file = ThemeIO.exportTo(AuroraClient.config().getRoot().resolve("themes"), clickGui(), hudTheme());
                        MinecraftClient.getInstance().keyboard.setClipboard(ThemeIO.exportString(clickGui(), hudTheme()));
                        return ok(ctx.getSource(), "Theme exported to " + file.toAbsolutePath() + " and copied to the clipboard");
                    } catch (IOException e) {
                        return error(ctx.getSource(), "Export failed: " + e.getMessage());
                    }
                }))
                .then(literal("import")
                    .executes(ctx -> importTheme(ctx.getSource(), MinecraftClient.getInstance().keyboard.getClipboard(), "clipboard"))
                    .then(argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                        Path dir = AuroraClient.config().getRoot().resolve("themes");
                        Path file = dir.resolve(StringArgumentType.getString(ctx, "file")).normalize();
                        if (!file.startsWith(dir) || !Files.isRegularFile(file)) return error(ctx.getSource(), "File not found in " + dir);
                        try {
                            return importTheme(ctx.getSource(), Files.readString(file), file.getFileName().toString());
                        } catch (IOException e) {
                            return error(ctx.getSource(), "Could not read " + file.getFileName());
                        }
                    }))))
            .then(literal("layout")
                .then(literal("list").executes(ctx -> ok(ctx.getSource(), "Layouts: " + String.join(", ", layouts().list()))))
                .then(literal("save").then(argument("name", StringArgumentType.word()).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    if (!HudLayouts.isValidName(name)) return error(ctx.getSource(), "Invalid name (letters, digits, - and _)");
                    try {
                        layouts().save(name, AuroraClient.hud().getElements());
                        return ok(ctx.getSource(), "Saved HUD layout " + name);
                    } catch (IOException e) {
                        return error(ctx.getSource(), "Saving failed: " + e.getMessage());
                    }
                })))
                .then(literal("load").then(argument("name", StringArgumentType.word()).suggests(LAYOUTS).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    try {
                        int n = layouts().load(name, AuroraClient.hud().getElements());
                        return ok(ctx.getSource(), "Loaded HUD layout " + name + " (" + n + " elements)");
                    } catch (IOException | IllegalArgumentException e) {
                        return error(ctx.getSource(), "No valid layout named " + name);
                    }
                }))))
            .then(literal("screenshot").then(literal("copy").then(argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("screenshots");
                Path file = dir.resolve(StringArgumentType.getString(ctx, "file")).normalize();
                if (!file.startsWith(dir) || !Files.isRegularFile(file)) return error(ctx.getSource(), "Screenshot not found");
                Screenshot.copyImage(file);
                return 1;
            }))))
            .then(literal("profile")
                .then(literal("list").executes(ctx -> {
                    ConfigManager cfg = AuroraClient.config();
                    return ok(ctx.getSource(), "Profiles: " + String.join(", ", cfg.listProfiles()) + " (active: " + cfg.getActiveProfile() + ")");
                }))
                .then(literal("save").executes(ctx -> AuroraClient.config().save()
                    ? ok(ctx.getSource(), "Saved profile " + AuroraClient.config().getActiveProfile())
                    : error(ctx.getSource(), "Saving failed, see the log")))
                .then(literal("load").then(argument("name", StringArgumentType.word()).suggests(PROFILES).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    if (!ConfigManager.isValidProfileName(name)) return error(ctx.getSource(), "Invalid name (letters, digits, - and _)");
                    LoadReport r = AuroraClient.config().loadProfile(name);
                    return ok(ctx.getSource(), (r.isCreated() ? "Created" : "Loaded") + " profile " + name
                        + (r.isClean() ? "" : " with " + r.getWarnings().size() + " warning(s)"));
                })))
                .then(literal("create").then(argument("name", StringArgumentType.word()).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    return AuroraClient.config().createProfile(name, true)
                        ? ok(ctx.getSource(), "Created and switched to profile " + name)
                        : error(ctx.getSource(), "Invalid name or profile already exists");
                })))
                .then(literal("delete").then(argument("name", StringArgumentType.word()).suggests(PROFILES).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    return AuroraClient.config().deleteProfile(name)
                        ? ok(ctx.getSource(), "Deleted profile " + name)
                        : error(ctx.getSource(), "Cannot delete (missing or currently active)");
                })))
                .then(literal("export").executes(ctx -> {
                    try {
                        Path file = AuroraClient.config().exportActive();
                        return ok(ctx.getSource(), "Exported to " + file.toAbsolutePath());
                    } catch (IOException e) {
                        return error(ctx.getSource(), "Export failed: " + e.getMessage());
                    }
                }))
                .then(literal("import").then(argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                    ConfigManager cfg = AuroraClient.config();
                    String raw = StringArgumentType.getString(ctx, "file");
                    Path file = cfg.getExportsDir().resolve(raw).normalize();
                    if (!file.startsWith(cfg.getExportsDir()) || !Files.isRegularFile(file)) {
                        return error(ctx.getSource(), "File not found in " + cfg.getExportsDir());
                    }
                    String base = file.getFileName().toString().replaceAll("\\.json$", "").replaceAll("[^A-Za-z0-9_-]", "_");
                    String name = base.length() > 32 ? base.substring(0, 32) : base;
                    try {
                        LoadReport r = cfg.importFile(file, name);
                        return ok(ctx.getSource(), "Imported as profile " + name + (r.isClean() ? "" : " (" + r.getWarnings().size() + " values fixed)"));
                    } catch (IOException | IllegalArgumentException e) {
                        return error(ctx.getSource(), "Import failed: " + e.getMessage());
                    }
                }))))));
    }

    private static Module module(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "module");
        Optional<Module> m = AuroraClient.modules().find(id);
        if (m.isEmpty()) error(ctx.getSource(), "Unknown module '" + id + "'. Use /aurora list");
        return m.orElse(null);
    }

    /** Accepts GLFW key names as in options.txt ("r", "f6", "left.shift"), "mouse1".."mouse8" and "none". */
    static Integer parseKey(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        if (s.equals("none")) return KeybindSetting.NONE;
        if (s.startsWith("mouse")) {
            try {
                int n = Integer.parseInt(s.substring(5));
                return n >= 1 && n <= 8 ? KeybindSetting.MOUSE_OFFSET + n - 1 : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        InputUtil.Key key = InputUtil.fromTranslationKey("key.keyboard." + s);
        return key == InputUtil.UNKNOWN_KEY ? null : key.getCode();
    }

    private static int help(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("Aurora commands:").formatted(Formatting.LIGHT_PURPLE));
        source.sendFeedback(Text.literal(" /aurora toggle|bind|unbind|reset <module> [key]"));
        source.sendFeedback(Text.literal(" /aurora list | gui | hud | keybinds | crosshair"));
        source.sendFeedback(Text.literal(" /aurora theme export|import [file] · /aurora layout list|save|load <name>"));
        source.sendFeedback(Text.literal(" /aurora profile list|save|load|create|delete|export|import"));
        return 1;
    }

    private static int ok(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal("[Aurora] ").formatted(Formatting.LIGHT_PURPLE).append(Text.literal(message).formatted(Formatting.WHITE)));
        return 1;
    }

    private static int error(FabricClientCommandSource source, String message) {
        source.sendError(Text.literal("[Aurora] " + message));
        return 0;
    }
}

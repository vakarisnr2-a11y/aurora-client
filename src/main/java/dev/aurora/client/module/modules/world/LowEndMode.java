package dev.aurora.client.module.modules.world;

import dev.aurora.client.AuroraClient;
import dev.aurora.client.module.Category;
import dev.aurora.client.module.Module;
import dev.aurora.client.setting.BooleanSetting;
import dev.aurora.client.setting.NumberSetting;
import dev.aurora.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Switches Minecraft's video settings to cheap values in one click and puts your own values back when turned off.
 *
 * <p>The original values are kept in {@code config/aurora/lowend-originals.txt} while the module is on, so they are
 * restored even after the game was closed with the module enabled or a profile switch. Changing one of these video
 * settings by hand while the module is on is undone when it is turned off.</p>
 */
public final class LowEndMode extends Module {
    private final BooleanSetting particles = add(new BooleanSetting("Minimal Particles", "Particles: Minimal", true));
    private final BooleanSetting clouds = add(new BooleanSetting("No Clouds", "Clouds: Off", true));
    private final BooleanSetting shadows = add(new BooleanSetting("No Entity Shadows", "Entity Shadows: Off", true));
    private final BooleanSetting fast = add(new BooleanSetting("Fast Graphics", "Graphics: Fast", true));
    private final BooleanSetting smoothLighting = add(new BooleanSetting("No Smooth Lighting", "Smooth Lighting: Off", true));
    private final BooleanSetting biomeBlend = add(new BooleanSetting("No Biome Blend", "Biome Blend: Off", true));
    private final BooleanSetting entityDistance = add(new BooleanSetting("Entity Distance 50%", "Entity Distance: 50%", true));
    private final NumberSetting renderDistance = add(new NumberSetting("Max Render Distance",
        "Lower the render distance to at most this many chunks (0 = leave it)", 8, 0, 32, 1));

    private Map<String, String> originals;

    public LowEndMode() {
        super("Low-End Mode", "Cheap video settings in one toggle (particles, clouds, shadows, graphics, lighting, distance). "
            + "Your own settings come back when it is turned off.", Category.WORLD);
    }

    @Override
    protected boolean shownInListByDefault() {
        return false;
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = mc();
        if (mc == null || mc.options == null) return;
        if (originals == null) originals = readBackup();
        if (originals == null) {
            originals = capture(mc.options);
            writeBackup(originals);
        }
        apply(mc.options);
        mc.options.write();
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = mc();
        if (mc == null || mc.options == null || originals == null) return;
        restore(mc.options, originals);
        mc.options.write();
        originals = null;
        deleteBackup();
    }

    @Override
    protected void onSettingChanged(Setting<?> setting) {
        MinecraftClient mc = mc();
        if (!isEnabled() || originals == null || mc == null || mc.options == null) return;
        restore(mc.options, originals);
        apply(mc.options);
    }

    private void apply(GameOptions o) {
        if (particles.get()) o.getParticles().setValue(ParticlesMode.MINIMAL);
        if (clouds.get()) o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        if (shadows.get()) o.getEntityShadows().setValue(false);
        if (fast.get() && o.getGraphicsMode().getValue() != GraphicsMode.FAST) o.getGraphicsMode().setValue(GraphicsMode.FAST);
        if (smoothLighting.get() && o.getAo().getValue()) o.getAo().setValue(false);
        if (biomeBlend.get() && o.getBiomeBlendRadius().getValue() != 0) o.getBiomeBlendRadius().setValue(0);
        if (entityDistance.get()) o.getEntityDistanceScaling().setValue(0.5);
        int max = renderDistance.getInt();
        if (max >= 2 && o.getViewDistance().getValue() > max) o.getViewDistance().setValue(max);
    }

    static Map<String, String> capture(GameOptions o) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("particles", o.getParticles().getValue().name());
        m.put("clouds", o.getCloudRenderMode().getValue().name());
        m.put("shadows", String.valueOf(o.getEntityShadows().getValue()));
        m.put("graphics", o.getGraphicsMode().getValue().name());
        m.put("ao", String.valueOf(o.getAo().getValue()));
        m.put("biomeBlend", String.valueOf(o.getBiomeBlendRadius().getValue()));
        m.put("entityDistance", String.valueOf(o.getEntityDistanceScaling().getValue()));
        m.put("viewDistance", String.valueOf(o.getViewDistance().getValue()));
        return m;
    }

    /** Puts stored values back, only touching options that differ (graphics and lighting changes reload chunks). */
    static void restore(GameOptions o, Map<String, String> m) {
        try {
            ParticlesMode p = ParticlesMode.valueOf(m.getOrDefault("particles", o.getParticles().getValue().name()));
            if (o.getParticles().getValue() != p) o.getParticles().setValue(p);
            CloudRenderMode c = CloudRenderMode.valueOf(m.getOrDefault("clouds", o.getCloudRenderMode().getValue().name()));
            if (o.getCloudRenderMode().getValue() != c) o.getCloudRenderMode().setValue(c);
            if (m.containsKey("shadows")) o.getEntityShadows().setValue(Boolean.parseBoolean(m.get("shadows")));
            GraphicsMode g = GraphicsMode.valueOf(m.getOrDefault("graphics", o.getGraphicsMode().getValue().name()));
            if (o.getGraphicsMode().getValue() != g) o.getGraphicsMode().setValue(g);
            if (m.containsKey("ao")) {
                boolean ao = Boolean.parseBoolean(m.get("ao"));
                if (o.getAo().getValue() != ao) o.getAo().setValue(ao);
            }
            if (m.containsKey("biomeBlend")) {
                int blend = Integer.parseInt(m.get("biomeBlend"));
                if (o.getBiomeBlendRadius().getValue() != blend) o.getBiomeBlendRadius().setValue(blend);
            }
            if (m.containsKey("entityDistance")) o.getEntityDistanceScaling().setValue(Double.parseDouble(m.get("entityDistance")));
            if (m.containsKey("viewDistance")) {
                int view = Integer.parseInt(m.get("viewDistance"));
                if (o.getViewDistance().getValue() != view) o.getViewDistance().setValue(view);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Low-End Mode could not restore some video settings: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ backup file

    private static Path backupFile() {
        return AuroraClient.get() == null ? null : AuroraClient.config().getRoot().resolve("lowend-originals.txt");
    }

    private static Map<String, String> readBackup() {
        Path file = backupFile();
        if (file == null || !Files.isRegularFile(file)) return null;
        try {
            Map<String, String> m = decode(Files.readString(file, StandardCharsets.UTF_8));
            return m.isEmpty() ? null : m;
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeBackup(Map<String, String> values) {
        Path file = backupFile();
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, encode(values), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Could not save the original video settings", e);
        }
    }

    private static void deleteBackup() {
        Path file = backupFile();
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // a stale backup is only used if the module is enabled again
        }
    }

    /** "key=value" lines. */
    public static String encode(Map<String, String> values) {
        StringBuilder sb = new StringBuilder();
        values.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
        return sb.toString();
    }

    public static Map<String, String> decode(String text) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String line : text.split("\\R")) {
            int eq = line.indexOf('=');
            if (eq > 0) m.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }
        return m;
    }
}

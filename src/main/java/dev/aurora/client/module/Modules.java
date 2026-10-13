package dev.aurora.client.module;

import dev.aurora.client.module.modules.combat.ComboCounter;
import dev.aurora.client.module.modules.combat.ReachDisplay;
import dev.aurora.client.module.modules.combat.TargetHud;
import dev.aurora.client.module.modules.misc.ChatAlerts;
import dev.aurora.client.module.modules.misc.ClickGui;
import dev.aurora.client.module.modules.misc.HudEditor;
import dev.aurora.client.module.modules.misc.Notifications;
import dev.aurora.client.module.modules.movement.CameraSmoothing;
import dev.aurora.client.module.modules.movement.Freelook;
import dev.aurora.client.module.modules.movement.MovementHud;
import dev.aurora.client.module.modules.movement.SafeWalk;
import dev.aurora.client.module.modules.movement.Sprint;
import dev.aurora.client.module.modules.movement.Step;
import dev.aurora.client.module.modules.player.AutoTool;
import dev.aurora.client.module.modules.player.InventorySorter;
import dev.aurora.client.module.modules.player.ToggleSneak;
import dev.aurora.client.module.modules.player.ToggleSprint;
import dev.aurora.client.module.modules.utility.ArmorStatus;
import dev.aurora.client.module.modules.utility.Coordinates;
import dev.aurora.client.module.modules.utility.CpsDisplay;
import dev.aurora.client.module.modules.utility.DurabilityDisplay;
import dev.aurora.client.module.modules.utility.FpsDisplay;
import dev.aurora.client.module.modules.utility.PingDisplay;
import dev.aurora.client.module.modules.utility.PotionEffects;
import dev.aurora.client.module.modules.utility.ResourceCounter;
import dev.aurora.client.module.modules.utility.Screenshot;
import dev.aurora.client.module.modules.utility.TabPing;
import dev.aurora.client.module.modules.utility.Zoom;
import dev.aurora.client.module.modules.visuals.BedwarsPackModule;
import dev.aurora.client.module.modules.visuals.Crosshair;
import dev.aurora.client.module.modules.visuals.Fullbright;
import dev.aurora.client.module.modules.visuals.GearTags;
import dev.aurora.client.module.modules.visuals.HitColor;
import dev.aurora.client.module.modules.visuals.Keystrokes;
import dev.aurora.client.module.modules.visuals.LowFire;
import dev.aurora.client.module.modules.visuals.ModuleList;
import dev.aurora.client.module.modules.visuals.Nametags;
import dev.aurora.client.module.modules.visuals.NoHurtCam;
import dev.aurora.client.module.modules.visuals.Perspective;
import dev.aurora.client.module.modules.visuals.Watermark;
import dev.aurora.client.module.modules.world.BlockOutline;
import dev.aurora.client.module.modules.world.TimeWeather;
import dev.aurora.client.module.modules.world.TntTimer;
import dev.aurora.client.module.modules.world.WorldColor;

import java.util.List;

/** The full list of built-in modules, in GUI order within each category. */
public final class Modules {
    private Modules() {
    }

    public static List<Module> createAll() {
        return List.of(
            // Combat
            new TargetHud(), new ComboCounter(), new ReachDisplay(),
            // Visuals
            new Fullbright(), new Crosshair(), new Nametags(), new GearTags(), new HitColor(), new NoHurtCam(), new Perspective(),
            new LowFire(), new BedwarsPackModule(), new ModuleList(), new Watermark(), new Keystrokes(),
            // Movement
            new Sprint(), new SafeWalk(), new Step(), new Freelook(), new CameraSmoothing(), new MovementHud(),
            // Player
            new AutoTool(), new InventorySorter(), new ToggleSprint(), new ToggleSneak(),
            // World
            new WorldColor(), new BlockOutline(), new TimeWeather(), new TntTimer(),
            // Utility
            new FpsDisplay(), new Coordinates(), new PingDisplay(), new TabPing(), new CpsDisplay(), new ArmorStatus(),
            new PotionEffects(), new DurabilityDisplay(), new ResourceCounter(), new Zoom(), new Screenshot(),
            // Misc
            new ClickGui(), new HudEditor(), new Notifications(), new ChatAlerts()
        );
    }
}

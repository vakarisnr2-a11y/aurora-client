# Aurora Client (Fabric, Minecraft 1.21.1)

A modular HUD / quality-of-life client mod with a ClickGUI, HUD editor, profiles and a built-in Bedwars texture pack.

## Install (Modrinth App or any launcher)
1. Create an instance: **Minecraft 1.21.1**, loader **Fabric**.
2. Add **Fabric API** to the instance (search it on Modrinth).
3. Drop `aurora-client-1.0.0.jar` into the instance's Mods (or `mods/` folder).
4. In game press **Right Shift** to open the ClickGUI. Enable the texture pack with the **Bedwars Pack** module
   (Visuals) or in Options > Resource Packs ("Aurora Bedwars").

## Build
Requires JDK 21. On this PC the Minecraft launcher's JDK works:
```
set JAVA_HOME=%APPDATA%\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta
gradlew build        (jar -> build\libs\aurora-client-1.0.0.jar, ~30 s after the first run)
gradlew runClient    (dev client; the first run downloads game assets)
```
Regenerate the texture pack: `java tools/BedwarsPackGenerator.java src/main/resources/resourcepacks/aurora_bedwars src/main/resources/assets/aurora/icon.png`

## Modules (54)
- **Combat:** Target HUD, Combo Counter, Reach Display, Damage Numbers, Hit Particles (crit/sharpness multiplier), Hitboxes,
  Arrow Counter, Bow Charge, Totem Pops
- **Visuals:** Fullbright, Crosshair, Nametags, Gear Tags (enemy armor/sword tier on nametags), Hit Color, No Hurt Cam, Perspective, Low Fire, Bedwars Pack, Module List, Watermark, Keystrokes,
  Old Animations (1.7 swing and sword-block look), Hotbar Style
- **Movement:** Sprint, Safe Walk, Step, Freelook (hold L-Alt), Camera Smoothing, Movement HUD
- **Player:** Auto Tool, Inventory Sorter (middle-click in inventory/chest), Toggle Sprint, Toggle Sneak
- **World:** World Color, Block Outline, Time & Weather, TNT Timer (fuse countdown above primed TNT), Item Physics, Particle Limiter
- **Utility:** FPS, Coordinates, Ping, Tab Ping (ping in ms in the tab list), CPS, Armor Status, Potion Effects, Durability,
  Resource Counter (iron/gold/diamonds/emeralds carried), Zoom (hold C), Screenshot (F12)
- **Misc:** ClickGUI (R-Shift), HUD Editor, Notifications, Chat Alerts (sound on your chat keywords; none set by default)

## Commands
`/aurora toggle|bind|unbind|reset <module> [key]`, `/aurora list|gui|hud`, `/aurora profile list|save|load|create|delete|export|import`

Config: `.minecraft/config/aurora/` (profiles/*.json, exports/). Malformed files are backed up and replaced with defaults.

## Known limitations
- Compiled and unit-tested; not yet play-tested in a running game.
- Step gives a movement advantage; servers may correct it, and public listings (e.g. Modrinth) may reject it.
- ESP and autoclicker were intentionally left out.

# Aurora Client (Fabric, Minecraft 1.21.1)

A modular HUD / quality-of-life client mod with a modern ClickGUI, HUD editor, profiles, an Aurora main menu and a
built-in Bedwars texture pack.

## Install (Modrinth App or any launcher)
1. Create an instance: **Minecraft 1.21.1**, loader **Fabric**.
2. Add **Fabric API** to the instance (search it on Modrinth).
3. Drop `aurora-client-1.1.0.jar` into the instance's Mods (or `mods/` folder).
4. In game press **Right Shift** to open the ClickGUI (type to search, click a card to toggle, right-click for
   settings). Enable the texture pack with the **Bedwars Pack** module (Visuals) or in Options > Resource Packs
   ("Aurora Bedwars").

## Build
Requires JDK 21. On this PC the Minecraft launcher's JDK works:
```
set JAVA_HOME=%APPDATA%\.minecraft\runtime\java-runtime-delta\windows\java-runtime-delta
gradlew build        (jar -> build\libs\aurora-client-1.1.0.jar, ~30 s after the first run)
gradlew runClient    (dev client; the first run downloads game assets)
```
Regenerate the texture pack (reads the vanilla textures from your Minecraft 1.21.1 jar, found automatically or
passed as the second argument): `java tools/BedwarsPackGenerator.java src/main/resources/resourcepacks/aurora_bedwars`

Regenerate the logo and mod icon: `java -Djava.awt.headless=true tools/LogoGenerator.java src/main/resources/assets/aurora`

## Modules (72)
- **Combat:** Target HUD, Combo Counter, Reach Display, Damage Numbers, Hit Particles (crit/sharpness multiplier), Hitboxes,
  Arrow Counter, Bow Charge, Totem Pops, Hit Sounds (custom hit sound, optional vanilla mute)
- **Visuals:** Fullbright, Crosshair (incl. Pixel style), Nametags, Gear Tags (enemy armor/sword tier on nametags), Hit Color,
  No Hurt Cam, Perspective, Low Fire, Bedwars Pack (Aurora pack: short swords, own blocks, dark GUI), Module List,
  Watermark, Keystrokes, Old Animations (1.7 swing and sword-block look), Hotbar Style, HUD Theme (shared HUD look,
  on by default; Aurora/Dark/Glass/Minimal/Neon presets, Aurora or Minecraft font), Scoreboard (movable sidebar, hide
  red numbers), Crosshair Editor (pixel crosshair painter), Saturation Overlay, Low Health Warning, Aurora Menu (on by
  default: main menu, menu/loading backgrounds, splash)
- **Movement:** Sprint, Safe Walk, Step, Freelook (hold L-Alt), Camera Smoothing, Movement HUD
- **Player:** Auto Tool, Inventory Sorter (middle-click in inventory/chest), Toggle Sprint, Toggle Sneak, Item Lock (K: no dropping locked slots)
- **World:** World Color, Block Outline, Time & Weather, TNT Timer (fuse countdown above primed TNT), Item Physics,
  Particle Limiter, Entity Distance, Low-End Mode
- **Utility:** FPS, Coordinates, Ping, Tab Ping (ping in ms in the tab list), CPS, Armor Status, Potion Effects, Durability,
  Resource Counter (iron/gold/diamonds/emeralds carried), Zoom (hold C, scroll to adjust), Screenshot (F12, open/copy
  buttons), Unfocused FPS, Better Tab (heads, sorting, own team first), Better Tooltips (durability, enchant numbers),
  Armor Warning
- **Misc:** ClickGUI (R-Shift; middle-click a module to pin it to Favorites), HUD Editor (snapping, anchoring, layouts),
  Notifications, Chat Alerts (sound on your chat keywords; none set by default), Better Chat (timestamps, stacked
  repeats, longer history), Chat Mentions, Chat Copy, Chat Search

Screens: the ClickGUI sidebar has the module categories, Favorites, **Profiles** (also exports and imports
**themes**), **Edit HUD** (the HUD editor) and **Keybinds** (overview with conflict warnings). Interface text uses
Poppins (SIL OFL 1.1, see `src/main/resources/licenses/`).

## Commands
`/aurora toggle|bind|unbind|reset <module> [key]`, `/aurora list|gui|hud|keybinds|crosshair`,
`/aurora profile list|save|load|create|delete|export|import`, `/aurora theme export|import [file]`,
`/aurora layout list|save|load <name>`, `/aurora screenshot copy <file>`

Config: `.minecraft/config/aurora/` (profiles/*.json, exports/, layouts/, themes/). Malformed files are backed up and replaced with defaults.

## Known limitations
- Compiled and unit-tested, and the screens were checked in a headless dev client (including a singleplayer
  world); not yet play-tested or tried on a server.
- Step gives a movement advantage; servers may correct it, and public listings (e.g. Modrinth) may reject it.
- ESP, autoclicker and other cheats were intentionally left out.
- No Discord status: no small, permissively licensed pure-Java IPC library was available (see CHANGES.md).

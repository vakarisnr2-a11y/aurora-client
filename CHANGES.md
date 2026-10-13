# Changes

## Batch A: Bedwars features

> **Not tested in-game.** These modules compile, their game-independent logic is unit-tested and
> the mixin targets were checked against the mapped 1.21.1 jar with `javap`. The dev client
> (`gradlew runClient`, headless) starts and reaches the title screen with all 44 modules registered
> and no mixin errors. No world or server was joined, so none of the features were exercised.
> Rendering, positions, colors and behavior on real servers (e.g. Hypixel) are unverified.

Five new modules, all **off by default**:

| Module | Category | What it does |
| --- | --- | --- |
| **Resource Counter** | Utility (HUD) | Shows the iron ingots, gold ingots, diamonds and emeralds in your inventory with their item icons. Settings: layout (vertical/horizontal), per-resource toggles, Hide Empty, Colored, Background, Scale. Movable in the HUD editor. |
| **TNT Timer** | World | Draws the remaining fuse time above primed TNT (e.g. `2.4s`), fading green to red. Settings: Decimals (0–2; with 0 the seconds round up), Scale, Color By Time, Background. The label is depth-tested, so it does **not** show through walls, and it is skipped beyond 64 blocks like vanilla nametags. |
| **Chat Alerts** | Misc | Plays a sound when a chat message contains one of your keywords. **Keywords are empty by default** (comma-separated, e.g. `bed, final kill`). Settings: Sound (Pling, Bell, Chime, Harp, Bit, Experience, Level Up, Anvil), Volume, Pitch, Case Sensitive, Whole Words, Ignore Own (skips your own messages), Cooldown. Changing the sound in the ClickGUI plays a preview. Action bar messages are ignored. |
| **Gear Tags** | Visuals | Appends other players' armor tier and held sword tier to their nametags, e.g. `Steve ⛨D ⚔I` (Format: short letters or full names). Uses only visible gear: worn armor and the main-hand item, as drawn on the player model. Enchantments are not read and invisible players get nothing. Settings: Armor, Sword, Format, Enemies Only (skips your scoreboard team, on by default). Works with or without the Nametags module. |
| **Tab Ping** | Utility | Replaces the signal-bar icon in the tab list with the latency in ms (e.g. `42ms`, `?` when unknown), colored green/yellow/orange/red. Columns widen to fit the number. Settings: Colored, Show Unit. |

### Implementation notes
- New event `ChatReceiveEvent`, posted from Fabric API's `ClientReceiveMessageEvents` (chat and game messages).
- New mixins: `TntEntityRendererMixin` (draws the fuse label after the TNT renders) and `PlayerListHudMixin`
  (widens the tab list's 13 px latency column and draws the number instead of the icon).
  `EntityRendererMixin` now adds Gear Tags after the Nametags health text.
- Tests: `BedwarsFeaturesTest` covers keyword parsing and matching, own-message detection, color-code stripping,
  armor/sword tier detection, fuse formatting, ping formatting and colors, resource visibility and registration.

### Known limitations
- Chat Alerts recognises your own messages on servers that send chat as system messages (Hypixel and most others)
  with a name heuristic (`<Name> text` or `... Name: text`). An unusual chat format can still trigger an alert for your own message.
- Gear Tags' Enemies Only uses scoreboard teams. On a server that puts each player in their own team (some do this for
  name prefixes), teammates will show gear too.
- Gear Tags shows the best armor piece worn. In Bedwars that is the leggings/boots upgrade, because helmets and chestplates stay leather.
- Tab Ping replaces the vanilla icon, so another tab-list mod that changes the same spot may conflict.

## Batch B: combat and visuals (visual only)

> **Not tested in-game.** These modules compile, their game-independent logic is unit-tested and every mixin
> target was checked against the mapped 1.21.1 jar with `javap`. The dev client (`gradlew runClient`, headless)
> reaches the title screen with all 54 modules registered and the default profile loaded. A temporary, uncommitted
> hook then force-loaded every mixin target class (including `ClientPlayNetworkHandler`, which is otherwise only
> loaded when joining a world) and all mixins applied without errors. No world or server was joined, so none of the
> features were exercised: animations, positions, colors, particle counts and behavior on real servers (e.g. Hypixel)
> are unverified.

Ten new modules, all **off by default**. None of them sends packets or changes input; they only change what is drawn.

| Module | Category | What it does |
| --- | --- | --- |
| **Old Animations** | Visuals | 1.7-style first-person animations. **Old Swing**: the held item no longer dips after each attack (the 1.9+ cooldown animation). **Sword Block**: holding use with a sword in the main hand shows the 1.7/1.8 blocking pose. **Block Hit**: keep swinging while in that pose like 1.7 (off: the sword stays still like 1.8). The pose is only a look: it does not reduce damage and right-click still does what it normally does. |
| **Damage Numbers** | Combat | Floating numbers above entities that lose health, rising and fading out. Settings: Unit (health points or hearts), Decimals, Duration, Scale, Color, Healing (+ Heal Color), Players Only, Self (third person), Shadow. Depth-tested, so hidden behind blocks; only within 48 blocks; at most 64 on screen. |
| **Hit Particles** | Combat | Crit and Sharpness (enchanted hit) particle multipliers (1–10, default 2). **Always Crit** / **Always Sharpness** add those particles to every hit you land. |
| **Hitboxes** | Combat | Entity hitboxes like F3+B with custom Color, Target Color (entity under your crosshair) and Width. Optional Eye Line and Look Vector. Filters: Players, Mobs, Items, Projectiles, Others, Self. Toggle with the module keybind. |
| **Arrow Counter** | Combat (HUD) | Arrow icon with the number of arrows carried (normal, spectral and tipped). Settings: Only With Bow, Low Warning (red at or below N, 0 = off), Background. |
| **Bow Charge** | Combat (HUD) | Bar and percentage while drawing a bow or crossbow; shows "Ready" at full charge. Settings: Percent, Hide When Idle, Background. |
| **Totem Pops** | Combat (HUD) | Lists players who popped a totem of undying with their count, most recent first. Settings: Self, Timeout, Max Rows, Chat Message (pops and "died after popping N totems" in your own chat only), Background. |
| **Item Physics** | World | Dropped items lie flat on the ground (blocks stand on their bottom face) with a fixed random rotation instead of floating, bobbing and spinning. **Spin In Air**: items keep spinning while falling or floating. Only the drawing changes; item positions and pickup are vanilla. |
| **Particle Limiter** | World | Max Particles (cap on particles alive, default 4000), Density (percentage of new particles spawned, spread evenly), Keep Combat (crit/sharpness ignore Density), Explosions and Block Breaking toggles. Shows the density in the module list. |
| **Hotbar Style** | Visuals | Rounded translucent hotbar and offhand slot, with a selection highlight that slides between slots. Settings: Background color, Theme Accent (use the ClickGUI accent) or a custom Selection color, Outline, Radius, Smooth. |

The three HUDs are movable in the HUD editor (default positions: Arrow Counter right edge, Bow Charge above the
crosshair, Totem Pops top left of center).

### Implementation notes
- New mixins (all checked with `javap` against the 1.21.1 Yarn-mapped jar):
  - `HeldItemRendererMixin`: redirects the only `getAttackCooldownProgress` call in `updateHeldItems` (Old Swing) and
    draws the blocking sword itself at the head of `renderFirstPersonItem`, using vanilla's private `applyEquipOffset`
    / `applySwingOffset` and `renderItem`. Vanilla 1.21.1 has no sword-block transform any more, so the pose was
    derived from the 1.8 code (`transformFirstPersonItem` + `doBlockTransformations`) and the difference between the
    1.8 and current first-person item models.
  - `ParticleManagerMixin`: extra emitters after `addEmitter(Entity, ParticleEffect)` (the server's crit/enchanted-hit
    emitters), the particle cap in `addParticle(Particle)`, and the block break toggles.
  - `WorldRendererMixin`: Density and Explosions at the head of `spawnParticle`, where vanilla already returns null
    for distant particles and every caller checks for it. `ParticleManager.addParticle(effect, ...)` is deliberately
    not filtered: vanilla's firework code casts its result and would crash on null.
  - `EntityRenderDispatcherMixin`: hides vanilla's F3+B box while Hitboxes is enabled, so boxes are not drawn twice.
  - `ItemEntityRendererMixin`: removes the bob (the only `translate`) and spin (the only `multiply`) in
    `ItemEntityRenderer.render` and positions the stack before `renderStack`.
  - `ClientPlayNetworkHandlerMixin`: entity status 35 (totem pop) at the tail of `onEntityStatus`, which only runs on
    the client thread.
  - `InGameHudMixin` now also redirects the 5-argument `drawGuiTexture` calls in `renderHotbar` (bar, selection,
    offhand slot); the attack indicator and items are still drawn by vanilla.
- Hitboxes and Damage Numbers draw from the existing `Render3DEvent`; `Render3D` gained `boxEdges`.
- Tests:
  - `CombatVisualsTest`: registration (all off by default, the three HUDs are HUD elements), the sword-block pose
    against the 1.8 matrix chain and its left-hand mirror, damage number formatting/detection/animation, the hit
    particle multiplier, the even particle thinning and cap, hitbox entity kinds, arrow/bow/totem HUD logic, hotbar
    sprite and slot mapping, and item physics geometry (vanilla's own ground `Transformation` is applied to check that
    flat items lie face up just above the ground for 1–5 stacked copies and that blocks stand on the ground).
  - `VanillaParityTest`: the bow charge curve equals `BowItem.getPullProgress` and the stacked-copy count equals
    `ItemEntityRenderer.getRenderedAmount` for every input tried.

### Known limitations
- Old Animations changes your first-person view only. Your character in third person and other players still use
  the vanilla poses. Block Hit only shows a swing when the game swings (attacking works normally while use is held).
- Damage Numbers reads the health the server syncs. Servers that hide or fake other players' health show wrong or no
  numbers. A health drop only counts while the entity shows the red hurt flash, so damage without a flash is missed,
  and healing during the flash can shrink a number. Absorption hearts count as health.
- Hit Particles multiplies every crit and sharpness hit you can see, not only yours: the client is not told who hit.
  The "Always" options use the Combo Counter's hit confirmation (the target flashes red within 4 ticks), so hits on a
  target that is still flashing do not get extra particles.
- Hitboxes are not drawn for invisible entities or when the server enables reduced debug info, same as vanilla.
  While the module is on, F3+B has no effect.
- Particle Limiter's Density and Explosions apply to world particles (packets, emitters, explosions). Particles added
  straight to the particle manager (fireworks, block breaking when shown) only count toward Max Particles.
- Item Physics: with Spin In Air, an item snaps to its resting rotation when it lands. Extra copies of a stacked block
  use vanilla's random offsets and can sink partly into the ground. Custom item models with a rotated ground
  transform may not sit exactly on the ground.
- Hotbar Style replaces the hotbar sprites while enabled (resource packs' hotbar textures are not shown) and may
  conflict with another mod that changes the same hotbar drawing.
- Totem Pops only sees pops and deaths of players near you. A count resets when the player's health reaches 0 on your
  client; servers that respawn players without a death keep counting until the Timeout.

## 1.1.0

Version 1.1.0 (`mod_version=1.1.0`) contains Parts 1–3 below: the HUD overhaul, Batch C and Batch D, 18 new modules
(72 in total) and upgrades to Zoom, Screenshot, Crosshair, the ClickGUI and the HUD editor. **Nothing in 1.1.0 was
tested in-game**: it compiles, 85 unit tests pass, all mixin targets were checked with `javap` and a headless dev
client applied every mixin and drew the new screens, but no world or server was joined.

## 1.1.0 Part 1: HUD overhaul

> **Not tested in-game.** Compiles and the game-independent logic (theme presets, anchoring, snapping, layouts,
> value transitions) is unit-tested. No world was joined, so the look and the animations are unverified.

- **HUD Theme** (Visuals, off by default): one shared style for every HUD element. Settings: Preset (Dark, Glass,
  Minimal, Neon; picking one sets all values), Background, Gradient, Corner Radius, Shadow (+ Size, Accent Glow),
  Outline (+ Accent Outline / Outline Color), Accent Bar (none/bottom/left/top), Custom Accent (otherwise the ClickGUI
  accent), Text Color, Accent Labels, Text Shadow, Padding, Animations, Animation Speed. While the module is off,
  HUDs use the default Dark look.
- **Animations**: elements fade and pop in when enabled and fade out when disabled; backgrounds ease to the new size
  when text changes; changed values (FPS, Ping, CPS, Reach, Combo, toggle state) slide in while the old value fades out.
- **HUD editor**: snap to screen edges/centre and to other elements' edges and centres with alignment guides, optional
  grid snap (G, grid size setting), Shift to move freely, arrow keys to nudge (Shift: 10 px), scroll or +/- to scale,
  R to reset an element, Delete or right-click to hide. Toolbar: Grid, Snap, Elements (show/hide any element),
  Layouts (save the current layout under a name, load, right-click twice to delete, open folder), Reset All.
  Dropped elements are anchored to the screen third they are in (corner, edge or centre) and keep their distance to
  that edge when the window is resized; the editor shows the anchor. Layouts are stored in `config/aurora/layouts/`.
  Old profiles keep their positions (elements stay on the fraction-based "Auto" anchor until moved).
- Every HUD element now draws through the shared style: FPS, Ping, CPS, Coordinates, Potion Effects, Armor Status,
  Durability, Resource Counter, Keystrokes, Target HUD, Combo Counter, Reach Display, Arrow Counter, Bow Charge,
  Totem Pops, Movement HUD, Time & Weather, Module List, Watermark and the Toggle Sprint/Sneak indicators.

## 1.1.0 Part 2: Batch C

> **Not tested in-game.** Compiles, the game-independent logic is unit-tested (`BatchCTest`) and every new mixin
> target was checked with `javap` against the mapped 1.21.1 jar. The dev client was started headless with a
> temporary (uncommitted) hook that ran Mixin's audit, which force-loads every remaining mixin target: 62 modules
> registered and all mixins applied without errors. No world or server was joined.

New modules (all **off by default**):

| Module | Category | What it does |
| --- | --- | --- |
| **Better Chat** | Misc | Timestamps (`[14:05]`, `[14:05:09]` or `[2:05 PM]`, own color), stacks a message that repeats the previous one into one line with a counter (`... [x3]`), and keeps a longer history (History Size 100–2000, default 500; also the sent-message history). A repeat only replaces the previous copy while it is still the newest message. |
| **Scoreboard** | Visuals (HUD) | The sidebar as a HUD element: move and scale it in the HUD editor, Hide Numbers (the red scores, on by default), Style (Vanilla or HUD Theme), Title, Text Shadow. Same lines and order as vanilla (its own comparator, 15 lines, team-color objective first); vanilla's sidebar is not drawn while it is on. |
| **Crosshair Editor** | Visuals | Opens a 15x15 pixel editor (also `/aurora crosshair`): left-drag paints, right-drag erases, mirror left/right and top/bottom, Clear, Invert, presets (Cross, Gap, Dot, Circle, X, T), a live preview at real size and "Use pixel style". The Crosshair module gained the **Pixel** style and a Pixel Size setting; the pattern is stored in the profile. |
| **Unfocused FPS** | Utility | Caps the frame rate while the window is in the background (default 15) or minimized (default 5). Never raises vanilla's own limit. |
| **Entity Distance** | World | Stops drawing entities beyond a distance from the camera (default 48 blocks). Players are only limited with Limit Players (Player Distance, default 96). You, your camera entity, your vehicle and passengers are never hidden. |
| **Low-End Mode** | World | One toggle for cheap video settings: minimal particles, no clouds, no entity shadows, fast graphics, no smooth lighting, no biome blend, entity distance 50 %, render distance capped at 8 (each switchable). Your own values come back when it is turned off; they are kept in `config/aurora/lowend-originals.txt` while it is on, so a restart or profile switch does not lose them. |
| **Item Lock** | Player | Lock Key (default K) locks the selected hotbar slot in game, or the hovered slot of your inventory in any container screen. Q / Ctrl+Q and throwing from an inventory screen are blocked for locked slots (with a notification), and locked slots get a padlock in the hotbar and inventory. |

Other additions:
- **Keybind overview** (ClickGUI footer "⌨ Keybinds" or `/aurora keybinds`): every Aurora keybind (module keys and
  extra keys such as Screenshot's Capture Key) with All / Bound / Conflicts filters. Warns when a key is also used by
  another Aurora bind or a Minecraft control (e.g. Zoom on C shares the key with "Save Hotbar Activator"). Click a
  key to rebind (Backspace clears), right-click to switch Toggle/Hold.
- **Favorites**: middle-click (or Shift+click) a module in the ClickGUI to pin it. Pinned modules appear in a new
  Favorites panel and get a ★. Stored in the profile.
- **Theme import/export**: "Theme Out" / "Theme In" in the Profiles panel and `/aurora theme export|import [file]`.
  A theme holds the ClickGUI look (preset, accent, opacity, corner radius, blur, animation speed) and every HUD Theme
  setting including whether it is on; panel positions and GUI scale are not part of it. Exports go to
  `config/aurora/themes/` and the clipboard; import reads the clipboard or a file from that folder.
- `/aurora layout list|save|load <name>` for the HUD layouts from Part 1.

Mixins added: `ChatHudMixin` (head of `addMessage(Text, MessageSignatureData, MessageIndicator)` — every chat line —
and the three `100` history caps), `ChatHudAccessor`, `InGameHudAccessor` (vanilla's sidebar comparator) and a head
cancel of the outer `renderScoreboardSidebar` in `InGameHudMixin`, `MinecraftClientMixin` (`getFramerateLimit`),
`EntityRenderDispatcherMixin.shouldRender`, `ClientPlayerEntityMixin.dropSelectedItem`,
`ClientPlayerInteractionManagerMixin.clickSlot` (THROW only), `HandledScreenMixin.drawSlot` and
`HandledScreenAccessor` (hovered slot).

Skipped:
- **Discord status**: skipped. The only pure-Java IPC client is jagrosh's DiscordIPC (Apache-2.0). It is not on
  Maven Central (it was published to JCenter, which is gone, so it would need JitPack), it uses junixsocket with
  native libraries for the Unix socket on Linux/macOS, and its JSON dependency (org.json) has had non-OSI licensing.
  The other options wrap native code too: java-discord-rpc (JNA plus the deprecated discord-rpc binaries) and the
  Discord Game SDK (proprietary binary). It could also not be tested here: there is no Discord client in the build
  container, and a status needs an application ID from the Discord Developer Portal, which only you can create.

Known limitations:
- Better Chat stacks by plain text, so two different players sending the same text in a row are stacked too.
- Item Lock does not cover creative mode's inventory (it drops items through its own code), dragging an item out of
  the window, or items moved out of a locked slot before dropping.
- Low-End Mode undoes video settings you change by hand while it is on when you turn it off.
- Entity Distance only hides entities; their sounds and particles still play.

## 1.1.0 Part 3: Batch D

> **Not tested in-game.** Compiles, the game-independent logic is unit-tested (`BatchDTest`) and every new mixin
> target was checked with `javap`. The dev client was started headless (Xvfb, no sound device) with a temporary,
> uncommitted hook: Mixin's audit force-loaded every target and all mixins applied without errors (72 modules), and the
> hook took screenshots of the Aurora main menu, ClickGUI, keybind overview, HUD editor and crosshair editor, which
> led to two layout fixes (HUD editor overlays drawn in front of HUD text, crosshair editor buttons). No world or
> server was joined, so chat, tab list, tooltips, sounds and overlays were not seen working.

New modules (all **off by default**):

| Module | Category | What it does |
| --- | --- | --- |
| **Chat Mentions** | Misc | Messages that mention your account name (or Extra Names) get the name recolored (and bold), a colored bar on the left (hover: "Mentions you") and an optional sound (Sound Type, Volume; previews in the ClickGUI). Whole-word and case-insensitive; your own messages are skipped. Tint Message also colors the rest of the message. |
| **Chat Copy** | Misc | With the chat open, right-click a message (or Ctrl+click / middle-click) to copy its text. Better Chat's timestamp and counter are left out (Remove Timestamps). |
| **Chat Search** | Misc | A search field in the chat screen (bottom right, Ctrl+F; Enter/Tab returns to the chat input). While it holds text, the chat shows only matching messages; closing the chat clears the search. Nothing is deleted. |
| **Better Tab** | Utility | Player heads on every server (vanilla hides them without online-mode encryption; offline servers show default skins), Sort: Vanilla / Name / Ping, and Own Team First. |
| **Saturation Overlay** | Visuals | A golden overlay on the hunger bar for the hidden saturation (2 points per icon), filled or outline, optional number. |
| **Better Tooltips** | Utility | "Durability: 1203 / 1561" (colored by wear) on damageable items, and enchantment levels as numbers ("Sharpness 5"; vanilla shows a raw key above X). |
| **Low Health Warning** | Visuals | A red glow on the screen edges at or below a health threshold (default 4 hearts), stronger the lower your health, optional heartbeat pulse. Color, Intensity, Size. Off in creative/spectator. |
| **Armor Warning** | Utility | A warning notification (and optional shield-break sound) when a worn armor piece drops to the threshold (default 15 %). Once per piece until it is repaired or swapped. |
| **Hit Sounds** | Combat | Plays one of 15 sounds (ding, click, pling, bell...) when you hit an entity, by default only for hits the server confirms. Volume, Pitch, Mute Vanilla (silences vanilla's player attack sounds). |
| **Aurora Menu** | Visuals | Aurora main menu (animated aurora backdrop, logo, rounded buttons: Singleplayer, Multiplayer, Realms, Mods if ModMenu is installed, Options, Quit, and "Vanilla menu" for the normal title screen), the aurora backdrop behind every menu and on world loading/connecting screens, and the resource loading overlay in Aurora colors with the Aurora icon and an accent progress bar. Each part can be switched off. |

Upgrades to existing modules:
- **Zoom**: Zoom Speed for the smooth zoom, Scroll Step, Min/Max Zoom for scrolling, Remember Level, Show Level (a
  "4.0x" readout under the crosshair while scrolling) and Cinematic Camera while zoomed.
- **Screenshot**: after saving, a notification with **Open**, **Copy** and **Folder** buttons (clickable while a
  screen with a cursor is open, e.g. chat) and the same buttons as a chat line. Copy puts the image itself on the
  clipboard via the OS (PowerShell on Windows, osascript on macOS, wl-copy/xclip on Linux) and falls back to copying
  the file path. Also `/aurora screenshot copy <file>`.
- Notifications can now carry buttons and keep clear of the chat input while chat is open.

Mixins added: `ChatScreenMixin` (search field, Ctrl+F, click to copy), a `ChatHudMixin.addVisibleMessage` filter for
search, `PlayerListHudMixin` (the single `isEncrypted` call in `render`, `collectPlayerEntries` return),
`InGameHudMixin.renderFood` (tail), `EnchantmentMixin` (the only `Text.translatable` in `getName`),
`SoundManagerMixin` (both `play` overloads), `MinecraftClientMixin.setScreen` (title screen swap), `ScreenMixin`
(`renderPanoramaBackground`), `LoadingScreensMixin` (`DownloadingTerrainScreen.renderBackground`) and
`SplashOverlayMixin` (brand color, logo and progress bar). Tooltips use Fabric API's `ItemTooltipCallback`.

Known limitations:
- Chat Mentions, Chat Copy and Chat Search work on what the chat shows; a server that sends your name split by
  formatting in unusual ways may not be detected. Copying while a search is active rebuilds the text from the wrapped
  lines, so spacing at line breaks can differ.
- Better Tab only re-orders the 80 entries vanilla keeps.
- Hit Sounds plays at the next client tick after the hit is seen (up to 50 ms later); Mute Vanilla also mutes other
  players' attack sounds near you.
- The Saturation Overlay does not follow the hunger bar's shake when you have the Hunger effect.
- The Aurora splash replaces the Mojang logo only after your profile is loaded, so the very first loading screen at
  startup stays vanilla; later reloads (resource packs, F3+T) use the Aurora look. The Aurora main menu does not show
  buttons that other mods add to the vanilla title screen (use "Vanilla menu").
- Image copy needs PowerShell (Windows), osascript (macOS) or wl-copy/xclip (Linux).

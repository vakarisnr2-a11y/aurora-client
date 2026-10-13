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

Version 1.1.0 (`mod_version=1.1.0`) contains Parts 1–4 below: the HUD overhaul, Batch C, Batch D and the UI
redesign, 18 new modules (72 in total) and upgrades to Zoom, Screenshot, Crosshair, the ClickGUI and the HUD editor.
**Nothing in 1.1.0 was play-tested**: it compiles, 92 unit tests pass, all mixin targets were checked with `javap` and
a headless dev client applied every mixin, drew the new screens and joined a generated singleplayer world for
screenshots (Part 4). No server was joined and nobody played with it.

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

## 1.1.0 Part 4: UI redesign

> **Not play-tested.** Compiles and the new game-independent logic is unit-tested (`UiRedesignTest`: profile
> migration, font selection, defaults). A headless dev client (Xvfb, no sound device) with a temporary, uncommitted
> hook took screenshots of the main menu, ClickGUI (module grid and a settings page), keybind overview, HUD editor,
> crosshair editor, and, in a generated flat singleplayer world, the HUD, chat, notifications, the ClickGUI over the
> world and the HUD editor. Those screenshots led to the layout fixes listed below. No server was joined; hovering,
> dragging and typing were only exercised as far as the screenshots show.

### The new look
One design system for every Aurora screen (`gui/ui/Ui`, `UiFont`, `Icons`): a dark night palette (window, sidebar,
raised cards, recessed fields) tinted by the ClickGUI theme preset (Midnight by default; Ocean, Sakura, Emerald,
Crimson and Graphite tint everything the same way), one accent color, hairline borders, soft shadows, rounded corners
in three sizes (4/6/10) and 8/12 px spacing.

- **Font**: Poppins Medium and SemiBold (SIL Open Font License 1.1, bundled with its license in `licenses/`) in two
  sizes: body text about the height of Minecraft's font and 13 px titles, in regular and bold. Minecraft draws
  TrueType glyphs from a fixed-resolution atlas, so each size is defined at 1x/2x/3x/4x/6x oversampling and the one
  matching the current GUI scale (and any scaling such as the ClickGUI open animation) is used. Text stays sharp at
  every GUI scale. Characters Poppins does not have (arrows, symbols, non-Latin scripts) fall back to Minecraft's font.
- **ClickGUI**, completely rebuilt (`gui/ClickGuiScreen` and `gui/clickgui/*`; the old floating category panels
  are gone):
  - A centered window over a blurred (Blur setting) and dimmed world. It scales and fades in on open and out on
    close.
  - **Sidebar** with the logo, version and drawn category icons (Combat, Visuals, Movement, Player, World, Utility,
    Misc) plus Favorites. Each category shows how many of its modules are on, and an accent indicator slides to the
    selected one. Under "Client": Profiles, Edit HUD and Keybinds. The active profile is shown at the bottom.
  - **Search bar** in the header: type anywhere, or Ctrl+F, to search every module by name or description.
    Right-click clears it, Esc clears and then closes.
  - **Module cards** in a two-column grid (one column in small windows). Each card has the name, a
    one-line description, an animated **toggle switch**, the keybind as a chip, a ★ for favorites and a gear.
    Click toggles, right-click or the gear opens settings, and middle-click or Shift+click pins to Favorites. Cards
    lift and light up on hover; enabled cards get an accent border. Descriptions show as a tooltip after a short
    hover (Tooltips setting). The grid scrolls smoothly.
  - **Settings page** slides in over the grid with a back button, the module's description and its on/off switch.
    Controls:
    - switches;
    - sliders with an accent fill and value badge;
    - segmented buttons for short choices and an animated dropdown for longer ones;
    - a color picker with a saturation/brightness square, hue bar, alpha bar (where allowed), hex value and Rainbow;
    - keybind buttons (click, then press a key or mouse button; Backspace clears; the Toggle/Hold chip switches
      the mode);
    - text fields.

    Settings that don't apply right now (e.g. Shadow Size with Shadow off) are hidden.
  - **Profiles page**: the profile list (click to load, right-click twice to delete) and Save, New, Export, Import,
    Folder, Reset, Export Theme, Import Theme as buttons.
- **HUD Theme is on by default** with the new **Aurora** preset: a translucent night background, 5 px corners, a
  soft shadow, a hairline outline, no accent bar and 4 px padding. Dark, Glass, Minimal and Neon are still there.
  New **Font** setting: Aurora (Poppins, same as the menus, the default) or Minecraft's pixel font. All HUD elements
  measure and draw text through the HUD style, so layouts fit either font. Turning HUD Theme off still gives the
  Aurora defaults.
- **HUD editor**: toolbar of rounded pill buttons (Grid, Snap, Elements ▾, Layouts ▾, Reset All, Done) with
  dropdowns in the new style. The selected element gets an accent outline and a label chip (name · scale · anchor).
  A hint bar at the bottom lists the shortcuts.
- **Keybind overview**: one window with All / Bound / Conflicts (n) tabs, rows that light up on hover, key buttons,
  conflicts in amber with a "n conflicting binds" badge, and a hint footer.
- **Crosshair editor**: same window style. Pixel grid, mirror and preset buttons and the live preview are laid out
  so nothing overflows at GUI scale 2 on 1600x900.
- **Notifications**: dark cards with a drawn type icon (info, success, warning, error) in a tinted circle, a
  bold title, the message, rounded action buttons (Open / Copy / Folder for screenshots) and a progress line in the
  type color. They slide in from the screen edge and stay above the chat input and Chat Search's field while chat is
  open.
- **Main menu and loading screens** (Aurora Menu, now **on by default**): the animated aurora backdrop with stars
  and the new logo, "AURORA" in spaced letters with a gradient, rounded buttons with Singleplayer as the primary
  accent button, and Realms/Mods and Options/Quit side by side. The world loading/connecting screens use the same
  backdrop. The resource loading overlay shows the new emblem and an accent progress bar.
- **New logo** (`tools/LogoGenerator.java`, Java2D, no dependencies): a sharp "A" peak crossed by a sweeping aurora
  ribbon in a teal → sky → violet → pink gradient, a small four-pointed star and a soft glow. Writes the
  256x256 emblem `assets/aurora/textures/gui/logo.png` (menus, ClickGUI, splash) and the 128x128 mod icon
  `assets/aurora/icon.png` (emblem on a dark rounded square). Regenerate with
  `java -Djava.awt.headless=true tools/LogoGenerator.java src/main/resources/assets/aurora`.

### Defaults and migration
- The module list only shows modules you'd want to see while playing. 25 modules that are client settings or
  background helpers (ClickGUI, HUD Editor, HUD Theme, Notifications, chat modules, Aurora Menu, Bedwars Pack,
  Hotbar Style, Low-End Mode, Unfocused FPS, Item Lock, ...) default to Show in List off. The module list also hides
  HUD elements by default (new **Hide HUD Elements** setting), since they're visible on screen anyway.
- Profile format 2. When a format 1 profile is loaded:
  - Show in List values for those 25 modules are not loaded, so upgraded profiles get the new defaults.
  - HUD Theme and Aurora Menu get the new defaults (on, Aurora preset) if the profile had them off, which was the
    old default. A profile that had turned them on keeps its values.
- New default positions so default HUDs don't overlap each other, the chat or the notifications:
  - Coordinates: top left under FPS/Ping/CPS, no longer under the chat.
  - Potion Effects and Armor Status: higher on the right.
  - Arrow Counter: right side.
  - Resource Counter: left, under Coordinates.
  - Target HUD: right of the crosshair instead of on top of Reach/Combo.

  Positions already saved in a profile are unchanged.
- Removed the hidden ClickGUI panel position settings of the old floating panels (old values in profiles are ignored).

### Fixed after looking at the screenshots
- HUD text was drawn over the HUD editor's toolbar and selection overlay; editor overlays are now drawn in front.
- The settings header's switch overlapped the close button. The search bar is hidden on settings pages.
- Notifications overlapped Chat Search's field while chat was open.
- Poppins has no ↔ ↕ ▾ glyphs; the editor draws its chevrons and labels avoid those characters.
- Crosshair editor buttons ran off the window at small sizes.

Known limitations:
- The blur is Minecraft's own menu blur and follows the game's Menu Background Blurriness option. The world is
  always dimmed behind the window, with or without blur.
- Text at fractional pixel scales (e.g. ClickGUI scale 1.15) uses the nearest oversampled font and is very slightly
  soft.
- Minecraft's own screens (options, world list, server list, inventory) keep the vanilla look apart from the
  Aurora backdrop.

## 1.1.0 Part 5: Bedwars pack at 32x32

The built-in **Aurora Bedwars** pack (enabled with the Bedwars Pack module or in Options > Resource Packs) is being
redrawn at 32x32. Every texture is generated by `tools/BedwarsPackGenerator.java` (plain Java, no dependencies):
`java tools/BedwarsPackGenerator.java src/main/resources/resourcepacks/aurora_bedwars`.

How it is drawn: shapes are filled with a material (a six-step color ramp from outline to highlight). The same
top-left bevel is then applied to every material: edges facing up and left are lit, edges facing down and right are
shaded. A 1 px outline in the material's darkest color goes around each sprite. So all textures share one light
direction and the same crisp edges. Materials are tuned so tiers are easy to tell apart: wood brown, stone mid
grey, iron near-white, gold yellow, diamond cyan, netherite dark violet-grey.

### Weapons and tools
- **Swords** (wood, stone, iron, gold, diamond, netherite) are **short**: the blade ends well before the corner, so
  the sword covers less of the screen in first person. Each blade has a bright cutting edge on top, a shaded lower
  edge, a fuller groove, a wrapped leather grip and a pommel. The guard changes a little per tier (straight for
  wood/stone/iron, winged for gold and diamond, spiked for netherite), so tiers differ by shape as well as color.
- **Pickaxes** and **axes** for all six tiers on the same dark handle. The axe has a bearded blade with a bright
  sharpened edge.
- **Shears**: two tapered blades with a rivet and red ring handles.
- **Bow**, standby and all three pull stages. The string draws back further each stage, with the arrow nocked.
- **Crossbow**: standby, three pull stages, loaded with an arrow, loaded with a firework.
- **Arrow**, **fishing rod** (with line and hook) and **fishing rod cast**, and **stick**.

### Armor
- **Armor icons** (helmet, chestplate, leggings, boots) for leather, chainmail, iron, gold, diamond and netherite,
  with one shared silhouette per piece. Helmets have a rim and crest, chestplates a center seam and chest plate
  line, leggings a belt with buckle and knee plates, boots cuffs and soles. Chainmail always has a ring pattern so
  it can't be mistaken for iron or stone at a glance.
- **Worn armor** (`models/armor/*_layer_1` and `_layer_2`, 128x64) for all six materials. They use the vanilla
  biped layout and vanilla's armor coverage (open face with eye slits, shoulder pads, boots on the lower legs,
  leggings with a waist band). Each box face is beveled on its own, with a crest on the helmet, a seam on the
  chestplate, a belt and buckle on the leggings, and soles and cuffs on the boots.
- **Leather** is drawn in light grey so the game's dye tint shows true team colors (undyed leather stays brown).
  The laces, brass rivets, buckle and soles are in the untinted `_overlay` textures, for both the items and the worn
  layers.

### Resources and Bedwars items
- **Resources** with different shapes as well as colors: diagonal iron, gold and netherite ingots with a lit top
  face, a brilliant-cut diamond with facets and a glint, and a tall step-cut emerald.
- **Fire charge** (glowing cracks between dark crust plates), **ender pearl** (teal orb with a dark core),
  **golden apple** (also used by the enchanted golden apple), **snowball** (Bedbug), **egg** (also the Bridge
  Egg, which is an egg item), **bucket**, **water bucket**, **milk bucket** (Magic Milk) and **lava bucket**.
- **Compass**: all 32 needle frames, red north half on a dark dial with an iron rim.
- **Potions**: the liquid (`potion_overlay`) is drawn in light greys, so Speed, Jump Boost, Invisibility and every
  other effect show their own color through the game's tint. The **potion**, **splash potion** (bent neck),
  **lingering potion** (wider lip) and **glass bottle** are clear glass with a cork and a shine.
- **TNT** side (dynamite sticks with a "TNT" label), top (stick ends and fuse) and bottom. Primed TNT uses the
  block's textures.
- **Sponge** and **wet sponge** (tileable, with round pores; the wet one darker, with droplets), and the **ladder**.

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

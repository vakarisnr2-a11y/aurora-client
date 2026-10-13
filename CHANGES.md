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

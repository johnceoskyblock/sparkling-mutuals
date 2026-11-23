# Configuration defaults and storage

## Controls and defaults

Open `/sm` for settings and `/sm gui` to move or resize HUDs. The main and contest settings files load independently. Fresh installs use the bundled defaults. Existing settings are loaded as saved, with fallbacks for missing or invalid values and documented one-time migrations. [Full defaults](../src/main/resources/defaults/sparkling-mutuals.properties) · [Standard defaults](../src/standard/resources/defaults/sparkling-mutuals.properties) · [Contest defaults](../src/main/resources/defaults/sparkling-mutuals-contest.properties)

| Setting | Fresh behavior |
| --- | --- |
| Safari mode | Unique. Use `/unique`, `/fc`, or `/sparkle` to change it. |
| Hotspot assigning | Off. Toggle it with `/hotspot`. |
| Safari party sync | Active for eligible parties. |
| Starting quest-item announcements | Off. |
| Critter outlines and boxes | Glow on and boxes off. |
| Standard Highlights | Line-of-sight critters with built-in biome limits, one Scrappy color, and generic floor-drop tiles. Boxes appear when their own toggle is on. |
| Full ESP | Separate glow and box controls, critter biome limits, named floor-drop controls, and customizable nameplates. |
| Capsule trajectory preview | On. Width defaults to 3, with a range of 1–5. The option to show the path through terrain defaults on. |
| Sparkle Warping | Both Full editions only. Warping and leeching default off. Reparty defaults to 22 seconds and warp defaults to 27 seconds. |
| Shard sale guidance | On. Adjust it separately from the Hunting Box value HUD. |
| In-game update downloads | Full only. Automatic downloads default off. |

Standard’s box setting and Scrappy color have their own saved values. Full’s biome limits and Warp List stay available when switching back. **Accept Hideyho Quest** uses Minecraft’s keybinding settings and offers setup at startup in Standard when unbound. It can also be set under **Safari → Helpers → Capture tools**.

## HUD appearance and limits

Fresh Safari HUDs use a black background with 20% transparency and borders off. HUDs retain normalized positions, reference dimensions, and expansion settings when the player changes GUI scale. Change expansion under a HUD’s Appearance settings or open its settings directly by right-clicking it in `/sm gui`.

**HUDs → Overview → HUD font customization** contains the resource pack font, text shadow, and pixel-aligned scaling controls. Resource pack fonts and shadows default on. Pixel-aligned scaling defaults off. Transparency runs from 0% solid to 100% invisible.

The progress HUD can show a run timer without progress rows. **Biome Objectives** has individual display rows and optional sparkling-check controls. Optional checks can affect completion requirements. [Safari logic](../SAFARI_LOGIC.md) · [Settings navigation](NATIVE_UI.md)

## Lifecycle and persistence

All editions use the active Minecraft instance’s `config/sparkling-mutuals` folder.

| Files in the folder | Contents |
| --- | --- |
| `sparkling-mutuals.properties` | Main settings, HUD layouts, and edition-specific What’s New history |
| `sparkling-mutuals-contest.properties` | Miria contest settings and state |
| `sparkling-mutuals-pbs.properties` | Personal bests |
| `sparkling-mutuals-honeyhives.properties` | Personal Honeyhive refill state |
| `sparkling-mutuals-*.json` | Run and sparkling history, achievements, playtime, market data and watches, and Ubik’s Cube cooldowns |
| `backups/legacy` | Archived copies of legacy files when the current store already exists |

Recognized files in the older root `config/` layout are migrated before stores open. Missing files are imported once from `config/sparkling-mutuals-legit`. Existing files in the shared folder take precedence. Fresh installs bundle preferences and layouts. Safari records, player identities, API keys, and personal timers are created by the player’s own instance.

**Reset to Defaults** restores the current edition’s editable preferences. **Disable All Features** turns off configurable switches. Both routes preserve Safari records and saved preferences used by the other edition. Commands and always-active systems retain their own controls. The local welcome message appears when entering Hypixel SkyBlock. Open `/sm` after an upgrade or use **What’s New** in settings to read release highlights for the installed edition.

### Personal best storage and migration

The legacy PB keys and old separate PB file migrate into `sparkling-mutuals-pbs.properties`. The migrator merges values, retaining faster times for matching timed records, and writes the destination before cleaning up the older storage. [Personal bests](PERSONAL_BESTS.md)

## Source and validation references

- [ConfigStorage](../src/main/kotlin/net/johnceo/sparklingmutuals/config/ConfigStorage.kt) owns file migration.
- [Full ConfigManager](../src/main/kotlin/net/johnceo/sparklingmutuals/config/ConfigManager.kt) and [Standard ConfigManager](../src/standard/kotlin/net/johnceo/sparklingmutuals/config/ConfigManager.kt) own values, migration flags, edition preservation, and reset behavior.
- [InstalledVersionHistory](../src/main/kotlin/net/johnceo/sparklingmutuals/config/InstalledVersionHistory.kt) owns the version records for What’s New.

## Last updated

2026-11-23 (America/New_York) — Reconciled storage and defaults with the consolidated editions and 4.4.4 settings.

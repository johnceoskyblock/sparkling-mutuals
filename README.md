# Sparkling Mutuals

## Purpose and audience

A Fabric client mod for Hypixel SkyBlock Safari, Sparkling discoveries, party coordination, Miria contests, history and achievements. This overview covers setup and entry points; the [documentation index](docs/README.md) leads to the current implementation reference.

## Categorized links

### Install

1. Use Java **25**, Fabric Loader **0.19.5+**, the Fabric API matching your Minecraft version, and Fabric Language Kotlin. Supported targets are **26.1.2**, **26.2** and **26.3**; the 26.1 line supports exactly 26.1.2.
2. Download an appropriate published build from [Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases), or `jar-mc<version>` from a successful [Actions run](https://github.com/johnceoskyblock/sparkling-mutuals/actions).
3. Install **one** matching Sparkling Mutuals JAR in `mods`, replacing the older mod JAR. Do not install source/test/development JARs. [Exact dependencies and filenames](docs/MULTI_VERSION_BUILDS.md#prerequisites).

Current release: **[v3.4.0](https://github.com/johnceoskyblock/sparkling-mutuals/releases/tag/v3.4.0)**, with `sparkling-mutuals-3.4.0+mc26.1.2.jar`, `sparkling-mutuals-3.4.0+mc26.2.jar` and `sparkling-mutuals-3.4.0+mc26.3.jar`. Install only the file matching your Minecraft version and remove the older Sparkling Mutuals JAR before updating.

HM API and MoulConfig are bundled. Player profile lookups use a Hypixel API key or the optional Shiny Hunter shared API. Keep keys and local authentication files private. Release notes and the availability of published assets belong to their [release descriptions](https://github.com/johnceoskyblock/sparkling-mutuals/releases).

### Play

| Feature | Start here |
| --- | --- |
| Modes | `/sparkle`, `/unique`, `/fc`; [mode goals and separate completion outcomes](SAFARI_LOGIC.md) |
| Species and resources | [All 37 critters](docs/critters/README.md), [captures/checks/reports](docs/critters/TRACKING.md), [floor drops](docs/critters/FLOOR_DROPS.md) |
| Progress and helpers | [Missing/Unchecked panels and highlights](docs/MISSING_PANEL.md), [Forest birds](docs/BIRD_COMPLETION.md), [Sparkling detection](docs/SPARKLING_DETECTION.md) |
| Capsule aiming guide | One **Capture projections** switch, terrain rings and possible-critter diamonds; [controls, limits and optional recorder](docs/CAPTURE_SHOT_RECORDING.md) |
| Settings | `/sm`; [defaults, mode presets and saved-setting caveats](docs/CONFIG_DEFAULTS.md) |
| HUD layout | `/sm gui`; drag and scroll to move/resize supported HUDs. Projection status text has been removed. |
| Capture review | `/captures` for current/last-run captures |
| History and achievements | `/sm history`, `/sm history import`, `/sm achievements`; [history/import/profit rules](docs/HISTORY_IMPORT.md) |
| Party streaks | `!since sparkling`, `!since unique`; [credit and participation](docs/SINCE_COMMAND.md) |
| Contest and readiness | [Miria/downtime](docs/CONTEST_DOWNTIME.md), [Safari entry guard and Shift override](docs/SAFARI_ENTRY_GUARD.md) |
| Canyon and warp | [Honeyhives](docs/HONEYHIVES.md), [warp reminders and automatic warp](docs/WARP_ALERTS.md) |

Starting food/coin quantities are shared once at the HOTSPOT message + two seconds, independently of mode and display settings. Later unused-resource reminders are local titles only; [resource accounting and mixed-client limits](docs/critters/TRACKING.md#early-party-starting-resource-synchronization).

The guide is a visual estimate, not guaranteed flight, impact or capture physics. HUD/ESP controls and underlying tracking have separate gates. Read each owner for exceptions rather than interpreting a green row or a highlight as proof of every completion outcome.

### Party commands

| Command | Purpose |
| --- | --- |
| `!mutual` / `!mutuals`, `!missing <IGN>` | Shared time-saving discoveries or missing discoveries |
| `!tickets <IGN>` | Safari ticket counts |
| `!m [biome]` | Current-run remaining work |
| `!dt [reason]`, `r` | Request downtime or release your own readiness hold |
| `!pb <biome>`, `!pb doom`, `!pb wumpa` | Active-mode biome or boss bests; supported duo/player variants are in command help |
| `!commands` | Available party commands in the active mode |
| `/sparkling [IGN\|party\|IGNs]` | Player cards and discovery comparison |
| `/alert`, `/alertdelay <seconds>` | Optional warp reminder controls |
| `/sm shots`, `/sm debug` | Explicit local recording controls or tracking diagnostics; review logs/exports before sharing |

### Contribute and verify

[Contributing](CONTRIBUTING.md) covers setup, focused changes and pull requests. [Multi-version builds](docs/MULTI_VERSION_BUILDS.md), [source synchronization](docs/SOURCE_SYNC.md), [manual scenarios](docs/IMPLEMENTATION_TEST_CHECKLIST.md), and the [implementation audit](docs/IMPLEMENTATION_AUDIT.md) distinguish static checks from live Minecraft evidence.

## Rule ownership and navigation

[Common vocabulary and ownership](docs/DOCUMENTATION_GUIDE.md) define the shared terms. Species pages describe exceptions and link to authoritative rule owners. Proposals and pending work live in [GitHub Issues](https://github.com/johnceoskyblock/sparkling-mutuals/issues); superseded implementation plans remain in Git history.

### Credits

- JavaBetter contributed player cards, cloud lookups, party helpers, honeyhives, history, achievements and profit tracking; GamingLegend123 contributed the Shiny Hunter icon.
- Adapted work: [CritterMod v0.9.0](https://github.com/MrCloudy2/critterMod/tree/v0.9.0), [ShinyHunter](https://github.com/javabetter/ShinyHunter), [Nebulune](https://github.com/Gaeritag/Nebulune), and [SkyHanni 9.1.0](https://github.com/hannibal002/SkyHanni/tree/9.1.0).
- Settings: [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig).
- Licensing and attribution: [LICENSE](LICENSE) and [third-party notices](licenses).

## Last updated

2026-10-19 (America/New_York) — Updated early starting-resource synchronization, verified coverage and local-only reminders for issue #68 while preserving the merged documentation template. Resource runtime reviewed at `76dd746`; prior audit provenance remains scoped to its stated revision. Automated all-target and offline native checks are separate from pending live Hypixel validation.

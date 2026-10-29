# Sparkling Mutuals

## Purpose and audience

A Fabric client mod for Hypixel SkyBlock Safari, Sparkling discoveries, party coordination, Miria contests, history and achievements. This overview covers setup and entry points; the [documentation index](docs/README.md) leads to the current implementation reference.

## Categorized links

### Install

1. Use Java **25**, Fabric Loader **0.19.5+**, the Fabric API matching your Minecraft version, and Fabric Language Kotlin. Supported targets are **26.1.2**, **26.2** and **26.3**; the 26.1 line supports exactly 26.1.2.
2. Download an appropriate published build from [Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases), or `jar-mc<version>` from a successful [Actions run](https://github.com/johnceoskyblock/sparkling-mutuals/actions).
3. Install **one** matching Sparkling Mutuals JAR in `mods`, replacing the older mod JAR. Do not install source/test/development JARs. [Exact dependencies and filenames](docs/MULTI_VERSION_BUILDS.md#prerequisites).

Current release: **[v3.5.0](https://github.com/johnceoskyblock/sparkling-mutuals/releases/tag/v3.5.0)**, with `sparkling-mutuals-3.5.0+mc26.1.2.jar`, `sparkling-mutuals-3.5.0+mc26.2.jar` and `sparkling-mutuals-3.5.0+mc26.3.jar`. Install only the file matching your Minecraft version and remove the older Sparkling Mutuals JAR before updating.

HM API and MoulConfig are bundled. Player profile lookups use a Hypixel API key or the optional Shiny Hunter shared API. Keep keys and local authentication files private. Release notes and the availability of published assets belong to their [release descriptions](https://github.com/johnceoskyblock/sparkling-mutuals/releases).

### Play

| Feature | Start here |
| --- | --- |
| Modes | `/sparkle`, `/unique`, `/fc`; [mode goals and separate completion outcomes](SAFARI_LOGIC.md) |
| Species and resources | [All 37 critters](docs/critters/README.md), [captures/checks/reports](docs/critters/TRACKING.md), [floor drops](docs/critters/FLOOR_DROPS.md) |
| Progress and helpers | [Missing/Unchecked panels and highlights](docs/MISSING_PANEL.md), [Forest birds](docs/BIRD_COMPLETION.md), [Sparkling detection](docs/SPARKLING_DETECTION.md) |
| Capsule aiming guide | One **Capture projections** switch, terrain rings and possible-critter diamonds; [controls and behavior](docs/CAPTURE_PROJECTIONS.md) |
| Gemzie gem selection | Select the matching whole hotbar gem on a fresh podium click; [helper controls and limits](docs/critters/gemzie.md#whole-gem-selection-helper) |
| Settings | `/sm`; [defaults, mode presets and saved-setting caveats](docs/CONFIG_DEFAULTS.md) |
| HUD layout | `/sm gui`; drag and scroll to move/resize supported HUDs. Projection status text has been removed. |
| Capture review | `/captures` for current/last-run captures |
| History and achievements | `/sm history`, `/sm history import`, `/sm achievements`; [history/import/profit rules](docs/HISTORY_IMPORT.md) |
| Sparkling rates | `/rates`; [cached-party checks and rate estimates](docs/SPARKLING_RATES.md) |
| Profitable critters | `/profit <ticket>` or `/profits <ticket>`; [shard + essence ranking](docs/PROFITABLE_CRITTERS.md) |
| Party streaks | `!since`, `!since unique`; [credit and participation](docs/SINCE_COMMAND.md) |
| Contest and readiness | [Miria/downtime](docs/CONTEST_DOWNTIME.md), [Safari entry guard and Shift override](docs/SAFARI_ENTRY_GUARD.md) |
| Canyon and warp | [Honeyhives](docs/HONEYHIVES.md), [warp reminders and automatic warp](docs/WARP_ALERTS.md) |

The guide is a visual estimate, not guaranteed flight, impact or capture physics. HUD/ESP controls and underlying tracking have separate gates. Read each owner for exceptions rather than interpreting a green row or a highlight as proof of every completion outcome.

### Party commands

| Command | Purpose |
| --- | --- |
| `!mutual` / `!mutuals`, `!missing [IGN]` | Shared time-saving discoveries or missing discoveries; omitted IGN means the sender |
| `!ticket [IGN]` / `!tickets [IGN]` | Safari ticket counts; omitted IGN means the sender |
| `!m [biome]` | Current-run remaining work |
| `!dt [reason]`, `r` | Request downtime or release your own readiness hold |
| `!pb <biome>`, `!pb doom`, `!pb wumpa` | Active-mode biome or boss bests; [solo/duo and observed unique variants](docs/PERSONAL_BESTS.md) are in command help |
| `!commands` | Available party commands in the active mode |
| `/sm help` | Local and party command list for the active mode |
| `/sparkling [IGN\|party\|IGNs]` | Player cards and discovery comparison |
| `/warping`, `/warpingdelay <seconds>`, `/autoparty` | Sparkle Warping and optional Warp List invitations |
| `/sm debug` | Local tracking diagnostics; review logs before sharing |

Press Tab to complete current-party IGNs in `/sparkling`, including successive comparison names, and name arguments for `!missing`, `!ticket`/`!tickets` and `!pb doom`/`!pb wumpa` and biome PB lookups. Party-chat forms such as `/pc !missing Jo` and `/p chat !tickets Jo` are supported. Matching ignores case; suggestions resolve verified party names even for members absent from the current tab list, without fetching SkyBlock discovery data.

### Contribute and verify

[Contributing](CONTRIBUTING.md) covers setup, focused changes and pull requests. [Multi-version builds](docs/MULTI_VERSION_BUILDS.md), [source synchronization](docs/SOURCE_SYNC.md), [manual scenarios](docs/IMPLEMENTATION_TEST_CHECKLIST.md), and the [implementation audit](docs/IMPLEMENTATION_AUDIT.md) distinguish static checks from live Minecraft evidence.

## Rule ownership and navigation

[Common vocabulary and ownership](docs/DOCUMENTATION_GUIDE.md) define the shared terms. Species pages describe exceptions and link to authoritative rule owners. Proposals and pending work live in [GitHub Issues](https://github.com/johnceoskyblock/sparkling-mutuals/issues); superseded implementation plans remain in Git history.

### Credits

- JavaBetter contributed player cards, cloud lookups, party helpers, honeyhives, history, achievements and profit tracking; GamingLegend123 contributed the Shiny Hunter icon.
- Adapted work: [CritterMod v0.9.0](https://github.com/MrCloudy2/critterMod/tree/v0.9.0), [ShinyHunter](https://github.com/javabetter/ShinyHunter), [Nebulune](https://github.com/Gaeritag/Nebulune), and [SkyHanni 9.1.0](https://github.com/hannibal002/SkyHanni/tree/9.1.0).
- Settings: [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig).
- Phantomleaf Solver: adapted from [SkyHanni](https://github.com/SkyHanniStudios/SkyHanni/blob/b3543887dfb80d95ff9880b3f610167ac2c104f2/src/main/java/at/hannibal2/skyhanni/features/garden/greenhouse/PhantomleafSolver.kt) under LGPL-2.1; open `/sm` → Misc Endgame Features. See [feature guide](docs/PHANTOMLEAF_SOLVER.md).
- Licensing and attribution: [LICENSE](LICENSE) and [third-party notices](licenses).

## Last updated

2026-10-29 - Updated Sparkle Warping commands and retired development shot recording.

2026-10-27 — Added ticket-specific `/profit` and `/profits` shard + essence ranking; `!missing` and `!ticket(s)` now default to the sender when no IGN is supplied.

2026-10-25 — Renamed the local rate command to `/rates`; no new API requests. `/profitablecritters` now ranks ten results by estimated coins per run.

2026-10-21 (America/New_York) — Prepared v3.5.0 with cached-data commands, retained party discoveries and updated completion/ESP behavior.

2026-10-21 — Added `/sparklingrates` with cached-party biome/manual-check filtering and six boat-visible species.

2026-10-21 — Added `/profitablecritters`: top five shard prices with estimated shards and coins per run, using existing cached prices only.

2026-10-21 — Renamed the default party streak command to `!since`; all mod users reply with their own party-credited streak. Non-detection party messages use shiny wording.

2026-10-20 — Added optional reward-drop ESP and non-leader ticket selection; removed obsolete Hideyho clicks and fixed unresolved party-name suggestions.

2026-10-20 - Added party-name Tab suggestions to Sparkling lookups and player-taking party commands.

2026-10-19 - Added the Garden Phantomleaf Solver and Misc Endgame Features tab with SkyHanni attribution.

2026-10-19 (America/New_York) — Integrated the Gemzie whole-gem selection helper from `c4908de` onto master `a1f98be`. Reviewed helper input, item identity, settings and mixin registration; live podium placement remains unverified.


2026-10-19 (America/New_York) — Reorganized the current implementation reference and retained v3.4.0 release/installation guidance during the master rebase. Reviewed runtime source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; documentation-only changes, no local runtime differences or new live validation.

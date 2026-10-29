# Documentation index

## Purpose and audience

Players: start with [installation and controls](../README.md). Contributors: read [contributing](../CONTRIBUTING.md), then the relevant rule owner. This is a current-implementation reference at the [audited revision](IMPLEMENTATION_AUDIT.md), with live uncertainties stated separately.

## Categorized links

| Audience / topic | Reference |
| --- | --- |
| Common language | [Vocabulary, templates and ownership](DOCUMENTATION_GUIDE.md) |
| Gameplay overview | [Modes and completion](../SAFARI_LOGIC.md), [37 species](critters/README.md) |
| Evidence | [Tracking](critters/TRACKING.md), [exact messages](critters/MESSAGES.md), [floor drops](critters/FLOOR_DROPS.md) |
| API data and recovery | [Safari API lookups, new players and one-minute retries](API_LOOKUPS.md) |
| Cached-data estimates | [Profitable critters](PROFITABLE_CRITTERS.md), [Sparkling rates](SPARKLING_RATES.md) |
| Network investigations | [API fetch audit](API_FETCH_AUDIT.md), [shared API usage report](SHINYHUNTER_API_USAGE_REPORT.md), [unknown-packet disconnect investigation](NETWORK_DISCONNECTS.md) |
| Gameplay display | [Bird completion](BIRD_COMPLETION.md), [missing panel](MISSING_PANEL.md), [Sparkling detection](SPARKLING_DETECTION.md) |
| Controls | [Configuration](CONFIG_DEFAULTS.md), [entry guard](SAFARI_ENTRY_GUARD.md), [warp alerts](WARP_ALERTS.md) |
| Contest and Canyon | [Contest/downtime](CONTEST_DOWNTIME.md), [Honeyhives](HONEYHIVES.md) |
| Misc Endgame Features | [Garden Phantomleaf Solver](PHANTOMLEAF_SOLVER.md) |
| History | [History, imports, achievements and profit](HISTORY_IMPORT.md), [credited party streaks](SINCE_COMMAND.md) |
| Capsule aiming guide | [Capture projections](CAPTURE_PROJECTIONS.md) |
| Development | [Contribution workflow](../CONTRIBUTING.md), [multi-version builds](MULTI_VERSION_BUILDS.md), [source synchronization](SOURCE_SYNC.md) |
| Validation | [Manual scenarios](IMPLEMENTATION_TEST_CHECKLIST.md), [audit discrepancies](IMPLEMENTATION_AUDIT.md) |
| Requests and release notes | [GitHub Issues](https://github.com/johnceoskyblock/sparkling-mutuals/issues), [GitHub Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases) |

## Rule ownership and navigation

The [ownership table](DOCUMENTATION_GUIDE.md#rule-ownership-and-navigation) assigns one current owner per rule. Species pages summarize their exceptions and link there. Current requests, unresolved proposals and approval discussions stay in GitHub Issues; superseded specifications and plans stay in Git history.

### Source and test map

Kotlin source lives under `src/main/kotlin/net/johnceo/sparklingmutuals/`. Packages follow feature responsibilities; file paths match package declarations.

| Package | Responsibility |
| --- | --- |
| `SparklingMutuals.kt` | Feature registration, ordered tick callbacks, disconnect cleanup and HUD registration |
| `alerts` | Warp reminders and alert timing |
| `api` | Profile lookups, API providers, failures and retries |
| `commands` | Local commands, party command parsing, replies and responder election |
| `compat` | Version-specific screen, input, camera and world-rendering API adapters |
| `config` | Saved settings, defaults, config screens and appearance |
| `contest` | Miria contest tracking, tickets, HUD and settings screen |
| `history` | Saved records, live history, achievements, profit and playtime |
| `history/importing` | Log import, backfill and merging imported evidence |
| `history/ui` | History screen and its tabs |
| `honeyhives` | Honeyhive labels, observations, tracking and rendering |
| `hud` | Shared panel drawing, layout and short notices |
| `mixin` | Minecraft hooks; names are registered in `sparkling-mutuals.mixins.json` |
| `misc/phantomleaf` | Garden Phantomleaf sound-distance solver, lifecycle and candidate rendering |
| `party` | Party membership and roster state |
| `safari` | Shared roster, catch parsing, run state, session lifecycle, chat routing and area detection |
| `safari/automation` | Mound clicking, candle targeting, downtime and entry guards |
| `safari/completion` | Mode requirements, scan zones, completion announcements and personal bests |
| `safari/esp` | Entity identification, visibility, capture identity, projections and rendering |
| `safari/hideyho` | Quest prompts, acceptance and hiding-location observations |
| `safari/resources` | Floor drops, starting supplies, bird feeding and inventory reminders |
| `safari/shots` | Opt-in local capsule evidence, bounded export and lifecycle |
| `safari/shots/preview` | Single-switch default-off capsule guide, terrain/critter estimates and passive rendering |
| `safari/sparkling` | Sparkling detection, encounters and pending capture evidence |
| `safari/structures` | Bee nests, nest surveys, mounds and Snooper walls |
| `safari/ui` | Progress/missing panels, capture screen and panel visibility |

Keep shared run evidence in `SafariRun`; Minecraft event handling belongs in the relevant feature integration. Small related data types can stay beside their owner. Separate independent responsibilities when a file becomes difficult to navigate.

### Tests and resources

Tests under `src/test/kotlin/net/johnceo/sparklingmutuals/` follow the same feature packages. `safari/integration` contains tests spanning multiple Safari features. Name suites after the behavior they protect, rather than an issue number or release. Keep regression tests after implementing a feature.

Run `./gradlew.bat clean buildAllTargets` on Windows; use `./gradlew` on other platforms. A focused suite can be selected with `./gradlew.bat :26.1.2:test --tests '*BeeNestSurveyTest'`. Reports are in `versions/<target>/build/reports/tests/test/`. The shared `src/test` suites run for each target; `buildSrc/src/test` protects packaging and release staging.

Runtime assets, mod metadata and mixin registration remain under `src/main/resources/`. Recorded API responses used by tests belong in `src/test/resources/`. Keep resource names stable when reorganizing code.

Generated `.gradle/`, `.kotlin/`, `build/`, `versions/<target>/build/`, IDE files, `run/` and logs are not maintained source. Do not commit authentication files or raw player recordings.

## Last updated

2026-10-21 (America/New_York) — Indexed the cached-data commands and API/network investigation references for v3.5.0.

2026-10-19 - Indexed API empty-discovery handling and automatic one-minute failure recovery.

2026-10-19 - Indexed the Phantomleaf Solver feature and its package; five focused 26.1.2 tests passed, live Garden validation pending.

2026-10-19 (America/New_York) — Consolidated navigation and audited contributor guidance. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; documentation-only changes, no local runtime differences or new live validation.

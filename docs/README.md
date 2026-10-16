# Repository guide

Start with the [project overview](../README.md) for installation and features, or [contributing](../CONTRIBUTING.md) for Java 25 setup and build commands.

## Source code

Kotlin source lives under `src/main/kotlin/net/johnceo/sparklingmutuals/`. Packages follow feature responsibilities; file paths match package declarations.

| Package | Responsibility |
| --- | --- |
| `SparklingMutuals.kt` | Feature registration, ordered tick callbacks, disconnect cleanup and HUD registration |
| `alerts` | Warp reminders and alert timing |
| `api` | Profile lookups, API providers, failures and retries |
| `commands` | Local commands, party command parsing, replies and responder election |
| `config` | Saved settings, defaults, config screens and appearance |
| `contest` | Miria contest tracking, tickets, HUD and settings screen |
| `history` | Saved records, live history, achievements, profit and playtime |
| `history/importing` | Log import, backfill and merging imported evidence |
| `history/ui` | History screen and its tabs |
| `honeyhives` | Honeyhive labels, observations, tracking and rendering |
| `hud` | Shared panel drawing, layout and short notices |
| `mixin` | Minecraft hooks; names are registered in `sparkling-mutuals.mixins.json` |
| `party` | Party membership and roster state |
| `safari` | Shared roster, catch parsing, run state, session lifecycle, chat routing and area detection |
| `safari/automation` | Mound clicking, candle targeting, downtime and entry guards |
| `safari/completion` | Mode requirements, scan zones, completion announcements and personal bests |
| `safari/esp` | Entity identification, visibility, capture identity, projections and rendering |
| `safari/hideyho` | Quest prompts, acceptance and hiding-location observations |
| `safari/resources` | Floor drops, starting supplies, bird feeding and inventory reminders |
| `safari/sparkling` | Sparkling detection, encounters and pending capture evidence |
| `safari/structures` | Bee nests, nest surveys, mounds and Snooper walls |
| `safari/ui` | Progress/missing panels, capture screen and panel visibility |

Keep shared run evidence in `SafariRun`; Minecraft event handling belongs in the relevant feature integration. Small related data types can stay beside their owner. Separate independent responsibilities when a file becomes difficult to navigate.

## Tests and resources

Tests under `src/test/kotlin/net/johnceo/sparklingmutuals/` follow the same feature packages. `safari/integration` contains tests spanning multiple Safari features. Name suites after the behavior they protect, rather than an issue number or release. Keep regression tests after implementing a feature.

Run all tests with `./gradlew.bat test` and build with `./gradlew.bat build` on Windows; use `./gradlew` on other platforms. A focused suite can be selected with `./gradlew.bat test --tests '*BeeNestSurveyTest'`. Reports are in `build/reports/tests/test/`.

Runtime assets, mod metadata and mixin registration remain under `src/main/resources/`. Recorded API responses used by tests belong in `src/test/resources/`. Keep resource names stable when reorganizing code.

## Documentation

| Document | Purpose |
| --- | --- |
| [Safari logic](../SAFARI_LOGIC.md) | Mode goals and gameplay evidence |
| [Critter index](critters/README.md) | Species rules and links to shared tracking/message evidence |
| [Config defaults](CONFIG_DEFAULTS.md) | Default settings and migration expectations |
| [Honeyhives](HONEYHIVES.md) | Hive observation and state rules |
| [Sparkling detection](SPARKLING_DETECTION.md) | Detection evidence and bundled skin registry |
| [History import](HISTORY_IMPORT.md) | Import controls, data sources and estimates |
| [Warp alerts](WARP_ALERTS.md) | Reminder timing and controls |
| [Contest downtime](CONTEST_DOWNTIME.md) | Ticket events and downtime coordination |
| [Manual checklist](IMPLEMENTATION_TEST_CHECKLIST.md) | In-game checks that unit tests cannot establish |
| [Documentation sync](SOURCE_SYNC.md) | Reviewing document revisions against the source baseline |
| [Issue implementation handoff](AI_ISSUE_HANDOFF.md) | Required documentation onboarding, source context and revision checks for issue implementation |
| [GitHub Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases) | Release notes, upgrade guidance and validation for each published version |

Track implementation requests and progress directly in [GitHub Issues](https://github.com/johnceoskyblock/sparkling-mutuals/issues). Read the current issue, relevant comments and reference attachments before implementation. Keep lasting behavior and contributor guidance in the maintained docs above; do not duplicate issue briefs in the repository. Attach issue-specific reference files on GitHub or use immutable links to files already committed in Git history.

## Local and generated files

Gradle's `.gradle/`, `.kotlin/` and `build/`, IDE settings, the development client's `run/`, and local `logs/` are ignored working files. They are not source folders. Keep local authentication files and development profiles in place when doing routine repository cleanup.

## Last updated

2026-10-16 - Made GitHub Issues the home for implementation requests and reference attachments; retained the issue implementation handoff and revision-review workflow.

## What's changed in v3.1.0

This update adds supply labels, persistent Sparkling checks and resource reminders, and fixes Safari completion, Forest nest tracking, Honeyhives and the history screen. It supersedes v3.0.0. The development branch previously named v4 is released as v3.1.0; this is not a 4.0.0 release.

### New features and improvements

- **Typed floor-drop labels:** predict gem colors, bird-food types, Soothing Incense, Shining Coins and Icebreakers from complete display groups. Highlight needed supplies with mode-aware selection. Predictions are visual hints; they never invent pickups or catches.
- **ESP text scale:** resize floor-drop names, critter timers, nest labels and teammate names in the ESP settings tab. Range 0.5×–4×, default 2×; the saved setting survives restarts.
- **Unused starting-supply reminders:** automatic titles and party notices identify held starting bird food or Shining Coins, their destination, owner and missing discoveries. Outstanding party-reported supplies reopen the relevant source requirements until additional spawns are accounted for.
- **Retained Sparkling checks:** keep qualified distinct UUID sightings across scans, unloading, biome revisits and mode changes within a run. Scrappy, Gemzie, Troodon and Gazer show fixed checked counters; captures of previously seen critters do not count twice. Full Clear counters still count actual catches only.
- **Mid-run Forest recovery:** use shared catches plus current live birds to recover missed feeding evidence in a qualifying central scan, with no remaining food-source markers or held food. Extra food and party reminders add work; Macaw pairs count as one feeding opportunity.
- **Entry and resource evidence:** track observed ticket selection, admission, Head Start items and Hunting Hotspot messages without assuming unobserved ticket bonuses. Reconcile starting inventory and pickups without counting the same grant twice.
- **Forest nest survey:** inspect all 13 known nest/hive sites at corrected heights across Forest elevations, classify actual bee-producing nests, and retain evidence through chunk unloading. Grey guidance identifies unverified sites; left- or right-click handles each nest once.
- **Nest counter and recovery:** show the separate Bee nests to punch counter without survey prose. After confirmed punching, wait for distinct spawned Honeybugs to match the actual nest total. Known site coverage plus shared catches/current bees can recover mid-run nest evidence. New nests or unknown sites reopen outstanding work.
- **Icy prerequisite highlights:** retain ordinary ESP for each uncaught Icy species in every mode while the party needs Sparkling Wumpa; catching/checking Wumpa does not waive other species' first catches.
- **Cleaner missing panel:** retain plain species rows and the applicable nest, mound and Snooper-wall helpers. Only Scrappy, Gemzie, Troodon and Gazer have fixed X/X suffixes. Finished Unique/Full Clear panels say All caught and checked!; Sparkling says All checked!.
- **Fresh configuration defaults:** enable independently switchable ESP features, panel borders, warp reminders and capture-chat filtering by default. Coins at Insta Sell and Achievements to Party stay off. Existing saved choices remain intact; modes stay mutually exclusive.
- **Expanded diagnostics and documentation:** `/sm debug` includes nest blocks/survey/render gates, Gimmiegold source/population counts, predicted drops and raw Honeyhive labels. Added notes for all 37 critters, shared tracking rules, Honeyhives and an in-game checklist. The documentation synchronization script detects content revisions while ignoring Windows/Unix line-ending differences.

### Fixes since v3.0.0

- Fix the Sparkling History crash when scrolling above the first entry.
- Use actual nest totals for Honeybug Full Clear captures and personal bests, including released bees outside entity range. Keep spawn checks, ordinary bee ESP and capture requirements separate; ordinary Honeybug ESP ends after the first Unique catch and remains off in Sparkling mode.
- Correct Gimmiegold checks when coin-use chat was missed, a run is joined mid-way, or incense markers remain. Confirmed baseline coin pickups/consumption can resolve misleading predicted markers. Starting/extra coins, held coins and unresolved party notices retain their requirements; historical UUID sightings do not inflate required captures or satisfy a later coin's new check.
- Make Forest bird completion species-specific after feeding is resolved. One shared Macaw catch satisfies its shared Full Clear capture requirement even while other Macaws remain; personal PBs still require a personal catch. Uncaught Bluebirds/Parakeets and additional food keep their own requirements.
- Require Unique's shared catch plus any party-needed local Sparkling check. Revalidate local sources after party done messages and biome reentry, and send local biome-complete announcements only from the current biome. A Gemzie door-opening message alone cannot complete its required checks.
- Stop species-only capture/throw messages from hiding an arbitrary nearby UUID; keep tracking independent of ordinary ESP visibility. Recognize optional Chat Heads text in loot-share messages while separating personal and shared catches.
- Fix HUD editor alert identity so previews, dragging, resizing and saved layouts apply to the intended alert.
- Base Honeyhive readiness on personal server holograms and confirmed loot/Queen Bee messages rather than decorative honey levels. Handle multiline/case-insensitive labels, distinguish adjacent hives, and prevent provisional profile observations from overwriting saved profile state.
- Limit Hideyho inside/outside titles and chat to Unique/Full Clear in Haunted, cancelling pending watches when leaving the biome or switching to Sparkling.

### Installation and validation

Minecraft **26.1.2**, Java **25**, Fabric Loader **0.19.5+**. Requires **Fabric API** and **Fabric Language Kotlin**. Replace the older Sparkling Mutuals JAR with **sparkling-mutuals-3.1.0.jar**; do not install the sources JAR. Existing saved configuration is retained.

Validation: **608 automated regression tests passed**, with a clean release build and checks of the packaged version metadata. Corrected nest highlights were previously confirmed in-game; the remaining latest rendering/server behaviors have not received a complete live Minecraft validation. See [the manual checklist](docs/IMPLEMENTATION_TEST_CHECKLIST.md).

[Full changelog](https://github.com/johnceoskyblock/sparkling-mutuals/compare/v3.0.0...v3.1.0)

## What's changed in v3.0.0

- Added Full Clear, Unique and party-aware Sparkling modes, with separate progress, missing-species and capture panels.
- Added mode-specific completion checks, spawn tracking, structure checks and communal biome/run-complete alerts.
- Added separate Full Clear/Unique biome PBs, solo/duo boss PBs, observed party boss records and capture-time feedback.
- Improved critter ESP lifecycle, floor-supply visibility, profitable Sparkling highlights and per-biome controls.
- Added Shyworm path warnings/countdowns, hidden Litterbug projections/countdowns and teammate outlines/names.
- Added movable inventory/warp alerts, customizable discovery/run-complete sounds and expanded HUD/ESP appearance settings.
- Improved Forest bird feeding/counting and Cavern, Icy and Haunted completion rules, including inherited party progress.
- Added mound-only attack holding and enlarged red candle hitboxes while holding Soothing Incense.
- Improved party command help/reply suppression, roster refreshes and automatic retries after API failures.
- Unified local chat styling and reduced repeated ESP decoding and rendering allocations.

- Added JavaBetter's contributed Shiny Hunter icon and enlarged Hideonwall ESP outlines.
- Added `!m` missing-run replies, Safari Manager kick holds and `!dt` downtime requests.
- Added optional Shiny Hunter cloud lookups and `/sparkling` player cards/comparisons.
- Added party Unique biome PBs and personal honeyhive beacons.
- Added run/sparkling history, log import, achievements and Safari statistics.
- Added Bazaar-based shard/feather profit estimates, a Profit tab and coin achievements.
- Fixed history import dates/deduplication, price refreshes, API-provider changes, hive state and delayed party replies.
- Updated mod metadata, authors and repository links for version 3.0.0.
- Restored optional profitable Unique highlights and biome done messages in every mode.
- Fixed honeyhive loot/refill tracking, Queen Bee refills and server-state checks; removed the invalid hive and redundant countdown/settings.
- Improved party done overrides, independent needed Cavern structure checks, four-UUID Gazer checks and zero-spawn Macaw/Rockmite/Snoozle status.
- Added continuous party-presence checks before Safari runs, arrival grace and entry GUI protection with a Shift-click override.
- Fixed Unique party completion bars outside Cavern and delayed local Forest completion until all feeding, including starting food, is confirmed.
- Fixed Full Clear feeding/capture totals, deduplicated starting food evidence and included later extra food pickups.

Minecraft 26.1.2 / Java 25 / Fabric. Requires Fabric API and Fabric Language Kotlin. Player lookups support a personal Hypixel API key or the optional Shiny Hunter shared API.

Validation: 510 automated regression tests and the clean build passed. Live in-game rendering and server integration have not been tested for this release.

[Full changelog](https://github.com/johnceoskyblock/sparkling-mutuals/compare/v2.0.0...v3.0.0)

## Last updated

2026-10-15 - Prepared the complete v3.1.0 feature, fix, compatibility and validation notes against v3.0.0.

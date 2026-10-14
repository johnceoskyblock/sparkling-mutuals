## In development: v4

- Survey all 13 Forest hive/nest locations; waypoint and count only actual bee nests.
- Remove a nest waypoint and decrement its counter once on either left- or right-click.
- Complete Sparkling Honeybug checks after every nest is punched; Unique requires one capture plus those nest checks when the party needs its sparkling.
- Keep ordinary Honeybug ESP off after the first Unique capture, and require every observed bee captured in Full Clear.
- Show grey guidance for unverified Forest nest sites and separate survey coverage from nests remaining in the missing panel.
- Inspect available Forest block data on arrival across all elevations, retaining survey markers and evidence through chunk unloading.
- Use the verified run's actual Honeybug total for Full Clear captures and personal bests, including released bees outside tracking range; invalidate local completion immediately when survey evidence changes.
- Add nest block, helper and render diagnostics to `/sm debug` for in-game verification.

Development build: 4.0.0-dev. Validation: 530 automated regression tests and the clean build passed. Live in-game interaction/rendering remains untested.

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

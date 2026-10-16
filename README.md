# Sparkling Mutuals

A Fabric mod for Hypixel SkyBlock Safari, sparkling discoveries, and Miria contests.

## Safari tracking

The [Safari logic notes](SAFARI_LOGIC.md) summarize Hypixel evidence and the deductions used for tracking. The [repository guide](docs/README.md) maps the code, tests and documentation for development.

Use the [37 individual critter notes](docs/critters/README.md) for species rules and the [shared evidence notes](docs/critters/TRACKING.md) for tickets, starting resources, messages, UUIDs, and late biome entry.

- **Full Clear mode (`/fc`):** track all required captures and highlight remaining critters.
- **Unique mode (`/unique`):** track one capture per species and highlight missing uniques.
- **Sparkling mode (`/sparkle`):** target sparkling discoveries missing for you or your party.
- **Profitable shard ESP:** optionally keep profitable critters highlighted in Unique and Sparkling modes.
- **Progress HUD:** show elapsed time and overall/per-biome completion.
- **Missing panel:** list remaining species, nests, mounds, and Snooper walls.
- **Biome captures HUD:** show species counts and Rockmite mound statistics.
- **Capture review (`/captures`):** view the current or last run's captures.
- **Biome personal bests:** save separate Full Clear and Unique clear times.
- **Party Unique PBs:** save party biome clear times alongside personal Unique records.
- **Boss personal bests:** save solo/duo Doomspiral and Wumpa times, plus observed party records.
- **Boss capture feedback:** show elapsed capture time and the difference from the previous PB.
- **Biome announcements:** send `fd`, `cd`, `id`, or `hd` when eligible biomes are complete.

## Highlights and helpers

- **Critter ESP:** draw colored outlines around critters; retain enlarged outlines for Hideonwalls.
- **Floor drop ESP:** label predicted Safari supplies by type and highlight needed drops until their requirements are met.
- **Bee nest waypoints:** check 13 Forest locations, ignore beehives, and remove each nest after a left- or right-click.
- **Nest survey guidance:** grey markers guide loading; coverage stays internal and Full Clear uses the actual bee total.
- **Rockmite highlights:** distinguish mounds from spawned silverfish.
- **Snooper wall waypoints:** locate walls to break for Snoozles.
- **Hidden Litterbugs:** show projected positions and countdowns while they are underground.
- **Shyworm helpers:** show projected positions, movement paths, and rest countdowns.
- **Bird food tracking:** track Forest food collection and feeding progress.
- **Wumpa prerequisites:** retain uncaught Icy prerequisite critter highlights in every mode while the party needs Sparkling Wumpa.
- **Detected sparklings HUD:** list loaded detected sparklings across Safari biomes, with biome and distance.
- **Sparkling detection:** identify loaded Sparkling names and 36 known skins across Safari biomes, with shared alert/progress evidence. [Detection and range limits](docs/SPARKLING_DETECTION.md).
- **Teammate outlines:** highlight loaded party members throughout SkyBlock.
- **Teammate names:** show party member names through walls.
- **Hideyho quest clicks:** accept Hideyho's offer by clicking with chat open.
- **Mound Auto Clicker:** hold the attack button on Rockmite mounds for 12 CPS.
- **Candle hitboxes:** make Haunted candle clicks easier while holding Soothing Incense.
- **Painting removal:** hide paintings in the Haunted biome.
- **Darkness removal:** remove the darkness effect in Safari.
- **Chat filtering:** hide repetitive Safari messages while keeping tracking active.
- **Ground capsule hiding:** hide ordinary dropped capsules in Safari.
- **Flying capsule hiding:** hide capsules near the camera at a configurable distance.

## Alerts and contests

- **Sparkling alerts:** show and sound an alert for each detected sparkling critter.
- **Party sparkling announcements:** share the critter, biome, and coordinates in party chat.
- **All Gems alert:** notify when all three Cavern gem colors are in your inventory.
- **All Bird Food alert:** notify after collecting three of each Forest bird food.
- **All Incense alert:** notify when four Soothing Incense are in your inventory.
- **Run completion alert:** notify when all biomes are complete in the active mode.
- **Unused starting supplies:** automatic titles and party reminders identify starting bird food or Shining Coins still needed by the party.
- **Warp alerts (`/alert`, `/alertdelay <seconds>`):** show the reminder and automatically run `/p warp` when the delay expires in the current run. [Timer behavior](docs/WARP_ALERTS.md).
- **Miria contest HUD:** track contest time, score, and tier.
- **Miria warnings:** warn below Uncommon at the selected 5-, 3-, and 1-minute marks.
- **Honeyhive beacons:** mark full Torrhus Canyon hives during an incomplete Miria contest.

## History and achievements

- **Run history (`/sm history`):** browse saved captures, loot shares, splits, bests and run statistics.
- **Sparkling history:** record personal and party finds with ticket counts and dry streaks.
- **Log import (`/sm history import`):** rebuild both run and Sparkling history from Minecraft logs. Also available as **Import History from Logs** beneath the History button in settings. [Import details and estimates](docs/HISTORY_IMPORT.md).
- **Achievements (`/sm achievements`):** track Safari milestones, collection progress and playtime.
- **Profit history:** estimate shard and feather values from Bazaar prices, with a Profit tab and coin milestones.

## Party commands and settings

- **`!mutual` / `!mutuals`:** list shared sparkling discoveries that save Safari time.
- **`!missing <IGN>`:** list a player's missing sparkling discoveries that save time.
- **`!tickets <IGN>`:** show a player's Safari ticket counts.
- **`!m [biome]`:** report what remains in the current Safari run.
- **`!dt [reason]` / `r`:** request downtime after the run and release your hold when ready. Earning an Uncommon contest ticket announces **Ticket earned!** and clears your contest hold.
- **Safari entry guard:** hold Torrhus entry for party readiness and Safari entry for kicks or missing members; no Shift bypass.
- **`/sparkling [IGN|party|IGNs]`:** show Safari player cards or compare sparkling collections.
- **`!pb <biome>`:** share Forest, Cavern, Icy, or Haunted PBs for the active mode.
- **`!pb doom` / `!pb wumpa`:** share solo boss PBs; add `duo` or an IGN for other records.
- **`!commands`:** show the party commands available in the active mode.
- **Settings (`/sm`):** toggle features and open command help.
- **HUD editor (`/sm gui`):** drag HUDs and alerts to move them; scroll to resize.
- **Appearance:** customize critter/waypoint colors, panel borders, backgrounds, and transparency.
- **ESP visibility:** independently restrict each biome and floor drops to your current biome.
- **ESP text scale:** resize floor-drop names, timers, nest labels and teammate names from 0.5× to 4×; default 2×.
- **Alert customization:** choose sounds, volumes, and run completion text.
- **Player lookups:** use your Hypixel key (`/apikey <key>`) or the optional Shiny Hunter shared API.
- **Diagnostics (`/sm debug`):** save entity tracking, nest block data, survey coverage and rendering gates to `latest.log`.

## Installation

- Development builds support Minecraft **26.1.2**, **26.2** and **26.3**. Use Java **25**, Fabric Loader **0.19.5+**, matching Fabric API, and Fabric Language Kotlin.
- Download the JAR from [Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases) and place it in your `mods` folder.
- Development builds: download `jar-mc<version>` from a successful [Actions run](https://github.com/johnceoskyblock/sparkling-mutuals/actions). Install **only one matching JAR**; the 26.1 line supports **26.1.2 only**. See the [version and dependency table](docs/MULTI_VERSION_BUILDS.md).
- Profile lookups use a [Hypixel API key](https://developer.hypixel.net) or the optional Shiny Hunter shared API.
- Release notes are in the [release descriptions](https://github.com/johnceoskyblock/sparkling-mutuals/releases).
- Current published release: **v3.3.0**, with the existing 26.1.2 file `sparkling-mutuals-3.3.0.jar`. The three-target workflow applies to future releases. Remove the older Sparkling Mutuals JAR before installing an update.

## Contributing

Bug reports, testing, documentation improvements and code contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) for setup, testing and pull-request guidance.

## Credits

- JavaBetter contributed player cards, cloud lookups, party helpers, honeyhives, history, achievements and profit tracking; GamingLegend123 contributed the Shiny Hunter icon.
- Adapted work: [CritterMod v0.9.0](https://github.com/MrCloudy2/critterMod/tree/v0.9.0), [ShinyHunter](https://github.com/javabetter/ShinyHunter), [Nebulune](https://github.com/Gaeritag/Nebulune), and [SkyHanni 9.1.0](https://github.com/hannibal002/SkyHanni/tree/9.1.0).
- Settings: [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig).
- Licensing and attribution: [LICENSE](LICENSE) and [third-party notices](licenses).

Documentation revision workflow: [Source synchronization](docs/SOURCE_SYNC.md). Run `./scripts/DocumentationSync.ps1` to find documents changed since the last SRC update.

Honeyhive state rules, data sources and debugging: [Torrhus Canyon Honeyhives](docs/HONEYHIVES.md).

Per-drop colors are configurable in **ESP → Floor-drop colors**. **Miria contest → Auto !dt contest** controls the automatic party downtime request at new contest starts. See [contest request behavior](docs/CONTEST_DOWNTIME.md).

## Last updated

2026-10-16 - Summarized stage-specific Safari entry protection without a Shift override.

# Sparkling Mutuals

A Fabric mod for Hypixel SkyBlock Safari, sparkling discoveries, and Miria contests.

## Party commands

- **Mutual sparklings:** `!mutual` finds sparkling discoveries shared by your party.
- **Missing sparklings:** `!missing <IGN>` lists a player's missing discoveries: all species in full-clear mode or timesaves in unique mode.
- **Safari tickets:** `!tickets <IGN>` shows a player's ticket counts.
- **Personal bests:** `!pb doom`, `!pb wumpa`, and `!pb forest|cavern|icy|haunted` share saved times. Each mod user replies with their own record, labeled Full Clear or Unique Run according to their mode.
- **Command help:** `!commands` shows the available commands. Duplicate lookup replies are suppressed, and lookup errors appear only locally.

## Safari tracking

- **Progress HUD:** shows the run timer and collected species in each biome.
- **Missing panel:** lists uncaught critters and remaining bee nests, Rockmite mounds, and Snooper walls.
- **Capture counts:** tracks catches per critter and Rockmite mound results. `/sparkling catches` opens the current or last run's counts.
- **Run mode:** `/sparkling fc` switches the full-clear preset, PB reply type, and discovery lookups together. On uses Full Clear PBs and all-species lookups; off uses Unique Run PBs and timesaves. Full-clear capture numbers turn green when completion requirements are met.
- **PB tracking:** saves your fastest Doomspiral, Wumpa, full-clear, and unique-biome times from the start of a run and announces new records locally. Unique clears require one personal catch of every species; only your own catches qualify for either biome PB type.

## Safari helpers

- **Miria contest HUD:** tracks Miria's contest timer, tier, and score, with configurable end warnings.
- **Warp reminders:** sends title to warp party before afk timeout. Useful for party leeching so that other players can have a chance to get sparkling critters without using a ticket. Use `/alert` and `/alertdelay <seconds>` to control reminders.
- **Critter ESP:** highlights critters in all four biomes, with individual toggles and biome filters. Unique runs hide collected species until the next run; Rockmite mounds and Snooper walls remain available for shiny checks. Mounds and silverfish have separate controls.
- **Floor drop ESP:** highlights Safari floor drops.
- **Sparkling detection:** highlights nearby sparkling critters and shows their names and distances.
- **Sparkling alerts:** displays an on-screen alert and can announce the critter, biome, and coordinates in party chat.
- **Inventory alerts:** notifies when you have all three gem types in Cavern, three of each bird food in Forest, or four Soothing Incense in Haunted.
- **Bee nests and Snooper walls:** highlights structures that still need checking. Honeybug completion also requires punched nests.
- **Hideyho quest clicks:** lets you accept the current Hideyho quest prompt by clicking with chat open.
- **Auto Clicker:** repeats left clicks while you hold the mouse button in Safari, useful for Rockmites.
- **Painting hider:** hides paintings in the Haunted biome.
- **Darkness removal:** clears the darkness effect in Safari.
- **Capture chat filter:** hides capture-related spam while tracking continues.
- **Capsule hiding:** hides ground capsules and nearby flying capsules.

## Settings and customization

- **Settings:** `/sparkling` opens feature settings, command help, and API key setup.
- **HUD editor:** `/sparkling gui` lets you move and resize panels.
- **Appearance:** customize ESP and waypoint colors, HUD backgrounds, transparency, and borders.

## Installation

Open a successful build in [Actions](https://github.com/johnceoskyblock/sparkling-mutuals/actions), download its **Artifacts** ZIP, and extract the mod JAR into your `mods` folder. Use the JAR without `-sources` in its name. Requires Minecraft 26.1.2, Java 25, Fabric Loader, Fabric API, and Fabric Language Kotlin.

Player lookups require a [Hypixel API key](https://developer.hypixel.net), entered through settings or `/apikey <key>`.

## Credits

Includes work adapted from [CritterMod v0.9.0](https://github.com/MrCloudy2/critterMod/tree/v0.9.0), [ShinyHunter](https://github.com/javabetter/ShinyHunter), [Nebulune](https://github.com/Gaeritag/Nebulune), and [SkyHanni 9.1.0](https://github.com/hannibal002/SkyHanni/tree/9.1.0). Settings use [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig). See [LICENSE](LICENSE) and [third-party notices](licenses) for licensing and attribution.

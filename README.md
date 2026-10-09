# Sparkling Mutuals

A Fabric mod for Hypixel SkyBlock Safari, sparkling discoveries, and Miria contests.

## Party commands

- **Mutual sparklings:** `!mutual` finds sparkling discoveries shared by your party.
- **Missing sparklings:** `!missing <IGN>` lists a player's missing discoveries: all species in Full Clear mode or timesaves in Unique and Sparkling modes.
- **Safari tickets:** `!tickets <IGN>` shows a player's ticket counts.
- **Personal bests:** `!pb doom`, `!pb wumpa`, and `!pb forest|cavern|icy|haunted` share each mod user’s saved times. Full Clear returns biome PBs only; Unique returns biome and Doomspiral/Wumpa PBs; Sparkling returns Doomspiral/Wumpa PBs only. Biome records identify their type.
- **Command help:** `!commands` lists available party commands. Settings help also lists local commands. Replies are staggered by UUID, matching results are suppressed, and lookup errors appear only locally.

## Safari tracking

- **Progress HUD:** shows the run timer and collected species in each biome.
- **Missing panel:** lists uncaught critters and remaining bee nests, Rockmite mounds, and Snooper walls.
- **Capture counts:** tracks catches per critter and Rockmite mound results. `/captures` opens the current or last run's counts. First visiting a biome with earlier party catches and none of your own uses minimum counts and remaining rendered critters to verify party progress.
- **Run mode:** `/fc` selects full clear, Full Clear PBs and all-species lookups. `/unique` selects unique runs, Unique Run PBs and timesaves. Full-clear capture numbers stay white until you visit a biome, then turn green when completion requirements are met. The Modes tab also offers these controls and Sparkling mode.
- **PB tracking:** saves your fastest Doomspiral, Wumpa, full-clear, and unique-biome times from the start of a run and announces new records locally. Unique clears require one personal catch of every species; only your own catches qualify for either biome PB type.

## Safari helpers

- **Miria contest HUD:** tracks Miria's contest timer, tier, and score, with configurable end warnings.
- **Warp reminders:** sends title to warp party before afk timeout. Useful for party leeching so that other players can have a chance to get sparkling critters without using a ticket. Use `/alert` and `/alertdelay <seconds>` to control reminders.
- **Sparkling mode:** `/sparkle` highlights your missing sparkling discoveries when solo, or the party's combined missing discoveries. Updates when players join or leave, with optional profitable-shard ESP. Requires an API key.
- **Critter ESP:** modes select highlights in all four biomes. Unique runs hide collected species, with an optional profitable-shard exception; mounds and walls remain available for shiny checks. Nearby stationary leftover moving-critter models are excluded after two seconds and restored if they move.
- **Hidden Litterbugs:** shows their ESP at the known mansion floor or their last observed emergence height, returning to the real position when they emerge.
- **Floor drop ESP:** hides completed or unneeded drops using discoveries, pickups, inventory and captures. Unique Icy keeps drops hidden; Haunted keeps coin drops while anyone needs Gimmiegold.
- **Sparkling detection:** highlights nearby sparkling critters and shows their names and distances.
- **Sparkling alerts:** displays a centered title, critter, location and screen flash, with a customizable sound and volume. Can announce discoveries in party chat.
- **Inventory alerts:** notifies when you have all three gem types in Cavern, all nine Forest bird food pickups, or four Soothing Incense in Haunted.
- **Forest birds:** tracks food pickups and feeding, adjusts capture requirements for Macaw pairs and spare Macaws, and includes starting bird food.
- **Bee nests and Snooper walls:** highlights structures that still need checking. Honeybug completion requires punched nests in your own biome; nests stop highlighting when you enter a party-cleared Forest with earlier Honeybug catches.
- **Hideyho quest clicks:** lets you accept the current Hideyho quest prompt by clicking with chat open.
- **Auto Clicker:** repeats attacks at 12 CPS while holding Mouse 0 on Rockmite mounds.
- **Full candle hitbox:** makes Haunted candles easier to click while holding Soothing Incense.
- **Painting hider:** hides paintings in the Haunted biome.
- **Darkness removal:** clears the darkness effect in Safari.
- **Capture chat filter:** hides capture-related spam while tracking continues.
- **Capsule hiding:** hides ground capsules and nearby flying capsules.

## Settings and customization

- **Settings:** `/sm` opens feature settings, command help, and API key setup.
- **Troubleshooting:** `/sm debug` saves nearby critter tracking details to your game log.
- **HUD editor:** `/sm gui` lets you move and resize panels.
- **ESP visibility:** the ESP tab has independent current-biome filters for floor drops and each biome's critters. Disable a filter to show eligible targets across loaded biomes.
- **Appearance:** customize ESP and waypoint colors, HUD backgrounds, transparency, and borders.

## Installation

Open a successful build in [Actions](https://github.com/johnceoskyblock/sparkling-mutuals/actions), download its **Artifacts** ZIP, and extract the mod JAR into your `mods` folder. Use the JAR without `-sources` in its name. Requires Minecraft 26.1.2, Java 25, Fabric Loader, Fabric API, and Fabric Language Kotlin.

Player lookups require a [Hypixel API key](https://developer.hypixel.net), entered through settings or `/apikey <key>`.

## Credits

Includes work adapted from [CritterMod v0.9.0](https://github.com/MrCloudy2/critterMod/tree/v0.9.0), [ShinyHunter](https://github.com/javabetter/ShinyHunter), [Nebulune](https://github.com/Gaeritag/Nebulune), and [SkyHanni 9.1.0](https://github.com/hannibal002/SkyHanni/tree/9.1.0). Settings use [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig). See [LICENSE](LICENSE) and [third-party notices](licenses) for licensing and attribution.

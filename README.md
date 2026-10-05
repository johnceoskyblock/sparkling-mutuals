# Sparkling Mutuals

A Fabric client mod for Hypixel SkyBlock Safari: mutual sparkling critters, missing timesaves, ticket lookups, Miria contest tracking and warp reminders.

## Configuration

Run `/sparkling` or `/sparkling config` to open the **MoulConfig** settings screen, using the same library as CritterMod. Categories, search, toggles and sliders use its standard layout with forest green panels, sand text and gold accents. The API key opens a separate masked editor.

- **Party commands:** toggle automatic responses and view command help locally. **Show help** closes settings and opens chat with colored sections, one described command per line, and an explanation of `<IGN>`. This help is local to you.
- **Miria contest:** choose warning times, toggle the HUD and titles, adjust warning sound and volume, and open the HUD editor.
- **Warp reminders:** toggle reminders and set their delay, which defaults to **25 seconds**.
- **Safari helpers:** toggle capture chat hiding, ground/flying capsule hiding, Auto Clicker, Hideyho quest clicks, Haunted painting hiding, shiny detection, sparkling alerts/party announcements, bee-nest and Snooper-wall highlights, and darkness removal.
- **ESP · Floor drops / Cavern / Forest / Icy / Haunted:** through-terrain highlights with biome filters and individual critter toggles.
- **Safari progress:** configure progress, missing species, capture counts, unique-only completion, where the HUD appears, Bee Nests, Rockmite Mounds, Snooper Walls and optional mound results.
- **Customization:** choose GUI borders, backgrounds and transparency, waypoint colors, floor-drop colors and all 37 critter ESP colors. Enter a six-digit hex color at the top and click **Set Color** for each target to apply the same color to multiple items. These buttons only change color; they never enable features. Each target also has its own color picker.
- **API key:** enter, reveal or hide, and save your Hypixel API key.

Toggles and sliders save immediately. Text fields have explicit Save buttons. Existing API keys and HUD settings are retained. Party responses are enabled by default; warp reminders are disabled until enabled.

## Commands

Send `!` commands in **party chat**, including through `/pc` or `/p chat`. Party commands and player names are case-insensitive. Player lookups require a Minecraft IGN.

| Command | Description |
| --- | --- |
| `!commands` | Show a party help menu listing party and local commands; no API key required |
| `!mutual` / `!mutuals` | Find timesave sparkling critters discovered by every party member |
| `!missing <IGN>` | List a player's missing timesave sparkling critters |
| `!ticket <IGN>` / `!tickets <IGN>` | Show Basic, Economy, Premium and First-Class ticket counts |
| `!pb doom` / `!pb wumpa` | Show the responding player's saved Doomspiral or Wumpa personal best; case-insensitive |
| `/sparkling` / `/sparkling config` | Open the configuration screen |
| `/sparkling gui` | Move and resize all HUDs independently, including Safari progress, missing critters, captures and nearby sparklings |
| `/sparkling catches` | Review per-critter captures in the current or last Safari run |
| `/alert` | Toggle warp reminders |
| `/alertdelay <seconds>` | Set the warp reminder delay, from 1 to 86400 seconds |
| `/apikey <key>` | Set or update your Hypixel API key (`/apiKey` and `/APIKEY` also work) |

### One responder per party request

Commands send only successful final results to party chat; there is no preliminary `[SM] Checking …` message. Missing API keys and all other lookup errors are shown locally, allowing another client with a working key to reply. Updated clients stagger lookups by their position in the sorted party roster and cancel their response when they see a matching successful reply. Party chat is also checked for replies received during the roster refresh or lookup, and again immediately before sending. Old error messages do not suppress a successful reply. Non-mod party members can still use the commands. Slow lookups can overlap across clients, so API requests are not guaranteed to run on only one client.

All mod users should install this update: older versions may still send preliminary messages or additional responses. Coordination assumes timely party chat delivery and a stable roster; unusually delayed messages can cause duplicates. Identical requests from the same player are ignored for 10 seconds. A party information timeout is reported locally so the request can be retried.

### Sparkling timesaves

The tracked timesaves are Rockmite, Snoozle, Gemzie, Honeybug, Gazer, Gimmiegold, Doomspiral and Wumpa. **All Birds** means Bluebird, Parakeet and Macaw have all been discovered; it remains a single group in results.

`!mutuals` intersects discoveries across every party member. Successful discoveries used by this command are cached for **60 seconds**, so a newly discovered critter may take up to a minute to appear. Changing the API key clears that cache before the next lookup. Missing-critters and ticket lookups fetch current data.

Examples:

```text
!missing JohnCEO
Missing Timesave Sparklings for JohnCEO: Rockmite, Gemzie, All Birds

!tickets JohnCEO
JohnCEO: Basic (123), Economy (45), Premium (17), First-Class (3)
```

If there are no missing or mutual timesaves, the result says `None`. Unavailable Safari data is reported as unavailable instead of being treated as zero tickets or undiscovered critters.

## Miria contest HUD and warnings

The tracker reads **Miria contests only**. Agatha entries are excluded, including when both contests appear on the scoreboard. The HUD shows time remaining, tier and score using tier colors.

Select any combination of **5m**, **3m** and **1m** warnings, or choose **None** to disable all end-of-contest warnings. All three times are selected by default. Warnings stop after reaching Uncommon or higher. Joining late produces one catch-up warning instead of several at once.

Run `/sparkling gui` or use **Move and resize HUDs** in settings. Drag any panel to move it; hover and scroll to resize it between 15% and 200%. Each panel saves its own position and size. Disabled panels have previews, so the editor works outside Safari too. **Reset positions** restores the default layout.

## Warp reminders

Enable **Warp reminder** in settings or run `/alert`. Each `HOTSPOT! Your Hunting Hotspot is …` message schedules one reminder after the configured delay. A new Hotspot message restarts the countdown. The default is **25 seconds**; `/alertdelay <seconds>` and settings can change it. Changing the delay, disabling reminders or disconnecting cancels a pending reminder.

The Hotspot perk must be unlocked in the Essence Shop. The reminder tells you when to use `/p warp`; assign that command in your preferred keybind mod if desired.

## Safari helpers

**Hide capture chat:** off by default. Hides server messages containing `You threw a`, `CAPTURE!`, `LOOT SHARE!` or `The <critter> escaped`. Suppression happens only at the chat display stage, after incoming-message events, so GUI capture counts, completion and your own Doomspiral/Wumpa PBs continue updating. Local new-PB notifications and player/party chat remain visible.

**Capsule hiding:** adapted from [SkyHanni 9.1.0's CritterCapsuleHider](https://github.com/hannibal002/SkyHanni/blob/9.1.0/src/main/java/at/hannibal2/skyhanni/features/hunting/safari/CritterCapsuleHider.kt). **Hide capsules on ground** and **Hide flying capsules** both default to on and operate only inside Safari. Ground hiding affects ordinary dropped Critter Capsules; Masterful Capsules stay visible on the ground. Flying hiding affects ordinary and Masterful Capsule item displays at **2 blocks or closer to the camera**, including the threshold. Adjust **Flying capsule distance** from **0.5 to 6 blocks**. Capsules farther away remain visible; hiding changes rendering only, so ground capsules can still be collected.

**Auto Clicker:** enabled by default. Hold **Mouse 0** (the left mouse button, also called Mouse 1 by some interfaces) while in Safari to repeat the normal attack action at a fixed **12 CPS**. Intended for Rockmites; it works on other targets too. It pauses when you release the button, leave Safari, open a screen, lose game focus, use an item or target/break a solid block. Normal block breaking remains available. There are no right-click, jitter, whitelist or adjustable-CPS controls. The rate uses a client tick accumulator (12 attacks per 20 ticks), so client lag can lower the real-time rate.

### Safari ESP

Adapted from [Nebulune's SafariESP](https://github.com/Gaeritag/Nebulune/blob/a573283c0553c3db338be1f2d547a53ccb77ac9d/src/main/kotlin/foo/starred/nebulune/modules/impl/render/SafariESP.kt), without requiring Athen or Nebulune. All five ESP groups and each group's **Only in current biome** option default to **on**. Turning the biome restriction off permits highlighting across loaded Safari biomes. These options do not change the existing 80-block sparkling detector. Existing saved choices are preserved; new or missing settings use these defaults:

| ESP group | Individual critters enabled by default |
| --- | --- |
| Floor drops | Floor drops enabled, restricted to your current biome |
| Cavern | Rockmite |
| Forest | Treefrog, Woodchucker, Hideonfloor |
| Icy | Shuddersquid, Billygoat, Nozzlenose |
| Haunted | Duplico, Hideonwall, Hideyho, Doomspiral |

Every other individual critter starts **off** and can be enabled separately. The complete supported roster is:

- **Floor drops:** one translucent outlined tile per block containing at least three string item displays; configurable color.
- **Cavern:** Cavernfish, Flitter, Shyworm, Driftling, Chuckwalla, Rockmite (silverfish and display forms), Scrappy, Snoozle and Gemzie.
- **Forest:** Foxtrot, Bluebird, Honeybug, Treefrog, Woodchucker, Fluffling, Hideonfloor, Parakeet and Macaw.
- **Icy:** Strongarm, Tepid, Polaris, Shuddersquid, Billygoat, Mantis Shrimp, Nozzlenose, Troodon and Wumpa.
- **Haunted:** Areita, Bloodbat, Duplico, Gazer, Litterbug, Solsnatcher, Gimmiegold, Hideonwall, Hideyho and Doomspiral.

All 37 species have independent highlight toggles and colors, initially using Nebulune's palette. Detection preserves its entity, head/skin texture, fish/parrot variant and colored shulker rules, including invisible/passenger silverfish exclusions. Boxes use its critter/display/head dimensions and render through terrain. Only loaded entities are scanned, every five ticks; nothing is highlighted outside Safari. Floor drops and biome ESP have no added 80-block limit.

**Hideyho quest clicks:** enable this toggle, wait for `[MOB] Hideyho: How about it?` and `Select an option: [Sure] [No thanks...]`, then click anywhere with chat open to accept. Both game and chat message events are listened to, and split/inherited button styles are supported, matching ShinyHunter's QuestAccepter. Each click accepts one current prompt using its exact server-provided command/custom action. Other NPC/MOB dialogue, old prompts and world/connection changes clear the offer. Disabled by default.

**Haunted painting hider:** stops rendering paintings only in the Haunted Safari area. Paintings remain in the world and can still be interacted with. Enabled by default.

**Nearby shiny detection:** scans loaded `Sparkling <species>` nametags, pairs them with a nearby critter body, and shows gold highlights plus a nearby list with names and distance. The scan covers a spherical **80-block radius** within the player's current Safari biome. Enabled by default.

Detection is adapted specifically from **CritterMod v0.9.0**, rather than its later releases. It needs a sparkling nametag and loaded entity data; it cannot detect critters that the server has not sent or identify an unnamed critter as sparkling. Biome lookup uses the area table shipped in that release.

**Sparkling alerts and party announcements:** both enabled by default and independently configurable. Each loaded sparkling within the same 80-block biome scan produces an on-screen alert and `SPARKLING <critter>! (<biome> <x> <y> <z>)` party message once per run. Messages are paced to avoid a burst; leaving or disabling announcements clears pending messages. These announcements do not require an API key.

**Bee nests:** enabled by default. Unpunched Forest nests show green boxes through terrain, with name/distance labels. Discovery checks already loaded nearby chunk sections every two seconds, skipping palettes without nests. It also finds decorative nests without block-entity data. Your own punch removes that nest's marker until the next run. Other players' punches leave no observable block change, so they cannot be tracked.

**Highlight Snooper Walls:** enabled by default in Safari helpers. Marks intact walls at the five Cavern locations from CritterMod v0.9.0 with name/distance labels through terrain. Only loaded, non-air wall positions are highlighted. Customize wall, nest and sparkling highlight colors in **Customization**.

**Remove darkness:** enabled by default. Clears the local darkness effect while inside Safari.

## Safari progress and capture counts

The **Progress HUD** and **Missing Panel** default to on. **Show where** controls the Progress HUD: *Only in Safari*, *Safari and entrance* (default), or *Everywhere*. *Safari and entrance* shows progress inside Safari and at the named **Critter Safari Entrance** area in Torrhus Canyon; it does not show elsewhere in Torrhus Canyon. Progress shows one **Collected** total for all 37 critters, with separate colored biome progress bars. Miria, progress, missing, captures, nearby sparklings and sparkling alerts each have independent appearance settings in **Customization**. Borders default to off, with white selected as their initial color. Backgrounds default to black with **20% transparency (80% opacity)**.

The **Missing Panel** and **Biome capture counts HUD** appear only during an active run inside **Forest, Cavern, Icy or Haunted**. They disappear in Safari's middle zone, at the entrance, and outside a run, regardless of **Show where**. Missing species use their rarity colors and quota progress where needed. Hiding these HUDs never erases captures or ends the timer.

**Bee Nests**, **Rockmite Mounds** and **Snooper Walls** are separate, default-on toggles in **Safari progress**. Bee Nests adds the known unpunched nest count to the Forest Missing Panel independently of highlighting. Cavern shows nearby unbroken Rockmite mounds within **64 blocks** when any are found, excluding mounds occupied by visible critters. This is a count of loaded nearby mounds, not all mounds remaining in the biome. Snooper Walls shows intact walls, marks unloaded wall positions as unknown, and reports all broken only after confirming all five positions.

**Mound results** defaults to off. Enable it to add **Mounds broken** and **Mounds with Rockmite** rows to the Cavern capture list. These count the server's empty-mound and Rockmite-reveal messages throughout the run, even while the display is disabled. They remain available with the current or last run until the next run starts, and do not increase critter capture totals.

The timer starts from loading into the Safari, including the boat. It continues through the center's **Critter Safari** area and all four biomes. Returning to **Torrhus Canyon** ends the run and freezes its timer; world changes or another island also end it. Briefly missing sidebar data does not end a run.

**Count Unique Only** defaults to off: completion uses the actual v0.9.0 data quotas (Gemzie and Troodon: 3; Gazer: 4; other species: 1). Turn it on to complete each species with one catch. This changes completion and missing-species displays, never the raw catch counts.

**Biome capture counts** defaults to off. Enable it for a matching panel listing all **9 Forest, 9 Cavern, 9 Icy or 10 Haunted species**, including zeros, while inside that biome. `/sparkling catches` opens the same styled count panel with biome tabs, initially selecting the current or last visited biome; this screen remains available at the center and after leaving. Tracking continues when the HUD is off or hidden. Counts stay through biome/center changes, toggling the setting, and leaving the island; the last run remains available until a new Safari run starts. Run counts are kept for this Minecraft session.

Each successful server `CAPTURE!` or `LOOT SHARE!` message adds one to the same per-species capture count. The HUDs do not distinguish who caught it or store catcher identities. A reward of multiple shards still represents one capture. Failed attempts, inventory transfers and quoted player chat do not count. Only captures observed by this client are available.

## Doomspiral and Wumpa personal bests

Your own `CAPTURE! You caught a Doomspiral` and `CAPTURE! You caught a Wumpa` messages measure elapsed time from the start of the current Safari run. Each species has its own best time; only a faster capture replaces it. A new best saves immediately and sends a **local chat notification**, for example `[SM] New Doomspiral PB: 1:02.250!`. Equal or slower captures do not notify. Loot-share rewards, other players' captures and quoted player chat never set or announce your PB.

Best times save immediately as milliseconds in `config/sparkling-mutuals.properties`, under `pb.doomspiralMillis` and `pb.wumpaMillis`, and survive Minecraft restarts. They are separate from the per-run capture counts.

Send **`!pb doom`** or **`!pb wumpa`** in party chat; capitalization does not matter and no API key is needed. The selected mod responder replies with its own saved best and player name, for example `JohnCEO's Doomspiral PB: 1:02.250`. If that responder has no best yet, it replies `Not recorded yet`. These commands use the same one-responder coordination as other party requests and are included in `!commands`.

## API setup and errors

Obtain a key through the [Hypixel Developer Portal](https://developer.hypixel.net), then save it in **API key** settings or run `/apikey YOUR_API_KEY`. Replace it there whenever it becomes invalid or expires. The HUD, reminders, Safari helpers and help menu work without a key.

Lookups distinguish a missing key, an invalid key, a request limit, unavailable profile/Safari data, an unknown player and an unavailable service. All lookup errors stay in local chat. Only successful results are eligible for party chat, and another matching successful reply suppresses them. Private server error details and the API key are never included in messages.

## Installation

Download the mod JAR from [GitHub Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases). **Safari review build** prereleases contain changes from the unmerged review branch. Each successful push build on that branch publishes a separate prerelease with the tested mod and source JARs. The same files are also available under **Artifacts** on the corresponding GitHub Actions run. If GitHub runners are unavailable, a locally built and tested JAR can be attached to the review prerelease; its release notes identify that build method.

Requires Minecraft **26.1.2**, Java **25**, Fabric Loader **0.19.5 or newer**, Fabric API and Fabric Language Kotlin. HMAPI and MoulConfig are included in the built mod JAR.

1. Install Fabric and the dependencies for Minecraft 26.1.2.
2. Put the compiled mod JAR in your instance's `mods` folder.
3. Launch Minecraft and run `/sparkling` to configure the mod.
4. Save a Hypixel API key for profile lookups.

## Building and tests

With JDK 25 configured, run:

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. The build runs unit tests and puts the mod and source JARs in `build/libs/`. To run only tests, use `./gradlew test`.

The main source, including rendering mixins, is entirely Kotlin. Shared roster, parsing, configuration and HUD helpers reduce duplicated code; scans are throttled and never load chunks.

Tests cover command parsing and responder elections, local-only lookup errors, reply history and duplicate suppression, persisted defaults, shared color buttons that preserve toggles, GUI appearance, mound geometry and run results, wall counts, warning settings, detection radius, sparkling-name matching, actual Hideyho MOB dialogue, styled buttons, capture parsing, quotas, boat/center/departure transitions, capture retention, HUD positioning and persistent personal bests. Party coordination, click packets and Safari rendering should also be tested in Minecraft before release.

## Credits

- [ShinyHunter](https://github.com/javabetter/ShinyHunter): Hideyho quest acceptance and painting suppression reference. Copyright 2026 GamingLegend123; MIT notice in `licenses/ShinyHunter-MIT.txt`.
- [Nebulune](https://github.com/Gaeritag/Nebulune), commit `a573283c0553c3db338be1f2d547a53ccb77ac9d`: all Safari ESP identifiers, biome rules, box dimensions, textures and colors, and the simplified AutoClicker tick accumulator. Copyright 2025 Starred; BSD-3-Clause notice in `licenses/Nebulune-BSD-3-Clause.txt`.
- [CritterMod v0.9.0](https://github.com/MrCloudy2/critterMod/tree/v0.9.0), commit `cdfcb8effb4b3158df50f0c0960c11e536931840`: sparkling detection/alerts/announcements, species/quotas, chat captures, progress/missing panels, nest tracking/waypoints, Snooper wall positions, Rockmite mound detection/results, darkness removal and area lookup reference. Copyright 2026 MrCloudy2; MIT notice in `licenses/CritterMod-MIT.txt`.
- [SkyHanni 9.1.0](https://github.com/hannibal002/SkyHanni/tree/9.1.0): `CritterCapsuleHider.kt` and `CritterCapsuleRules.kt` adapt its capsule hiding behavior with two default-on toggles and close-only flying suppression. These adapted files retain LGPL-2.1; the full license is in `licenses/SkyHanni-LGPL-2.1.txt`, included in the built and source JARs. Their complete modified source is supplied in the source JAR and source archive.
- The Safari area table bundled by CritterMod derives from SkyHanni's Safari path graph.
- [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig) 4.7.2 supplies the settings UI and is bundled as a separate LGPL-3.0 library.

The original Sparkling Mutuals license remains in `LICENSE`; adapted third-party portions retain their MIT, BSD-3-Clause and LGPL-2.1 notices, included in the built JAR and source JAR.

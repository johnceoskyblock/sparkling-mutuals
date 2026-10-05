# Sparkling Mutuals

A Fabric client mod for Hypixel SkyBlock Safari: mutual sparkling critters, missing timesaves, ticket lookups, Miria contest tracking and warp reminders.

## Configuration

Run `/sparkling` or `/sparkling config` to open the **MoulConfig** settings screen, using the same library as CritterMod. Categories, search, toggles and sliders use its standard layout with forest green panels, sand text and gold accents. The API key opens a separate masked editor.

- **Party commands:** toggle automatic responses and view command help locally.
- **Miria contest:** choose warning times, toggle the HUD and titles, adjust warning sound and volume, and open the HUD editor.
- **Warp reminders:** toggle reminders and set their delay, which defaults to **25 seconds**.
- **Safari helpers:** toggle Hideyho quest clicks, Haunted painting hiding and nearby shiny detection.
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
| `/sparkling` / `/sparkling config` | Open the configuration screen |
| `/sparkling gui` | Move and resize the Miria contest HUD |
| `/alert` | Toggle warp reminders |
| `/alertdelay <seconds>` | Set the warp reminder delay, from 1 to 86400 seconds |
| `/apikey <key>` | Set or update your Hypixel API key (`/apiKey` and `/APIKEY` also work) |

### One responder per party request

Commands send only their final result or a single contextual error; there is no preliminary `[SM] Checking …` message. Updated clients stagger lookups by their position in the sorted party roster and cancel their response when they see a matching final reply. Non-mod party members can still use the commands. Slow lookups can overlap across clients, so API requests are not guaranteed to run on only one client.

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

Run `/sparkling gui` or use **Move and resize the HUD** in settings. Drag to move the HUD; hover and scroll to resize it between 50% and 200%. Closing the editor saves its position and size.

## Warp reminders

Enable **Warp reminder** in settings or run `/alert`. Each `HOTSPOT! Your Hunting Hotspot is …` message schedules one reminder after the configured delay. A new Hotspot message restarts the countdown. The default is **25 seconds**; `/alertdelay <seconds>` and settings can change it. Changing the delay, disabling reminders or disconnecting cancels a pending reminder.

The Hotspot perk must be unlocked in the Essence Shop. The reminder tells you when to use `/p warp`; assign that command in your preferred keybind mod if desired.

## Safari helpers

**Hideyho quest clicks:** enable this toggle, open chat after Hideyho's dialogue offers a `Sure` option, and left-click anywhere to accept it. Each click can accept one current prompt; it does not repeatedly click or move the player. Only the server-provided command/custom action attached to `Sure` is used. Other NPC dialogue, old prompts and disconnects clear the offer. Disabled by default.

**Haunted painting hider:** stops rendering paintings only in the Haunted Safari area. Paintings remain in the world and can still be interacted with. Enabled by default.

**Nearby shiny detection:** scans loaded `Sparkling <species>` nametags, pairs them with a nearby critter body, and shows gold highlights plus a nearby list with names and distance. The scan covers a spherical **80-block radius** within the player's current Safari biome. Enabled by default.

Detection is adapted specifically from **CritterMod v0.9.0**, rather than its later releases. It needs a sparkling nametag and loaded entity data; it cannot detect critters that the server has not sent or identify an unnamed critter as sparkling. Biome lookup uses the area table shipped in that release.

## API setup and errors

Obtain a key through the [Hypixel Developer Portal](https://developer.hypixel.net), then save it in **API key** settings or run `/apikey YOUR_API_KEY`. Replace it there whenever it becomes invalid or expires. The HUD, reminders, Safari helpers and help menu work without a key.

Lookups distinguish a missing key, an invalid key, a request limit, unavailable profile/Safari data, an unknown player and an unavailable service. Server error details and the API key are not included in party responses.

## Installation

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

Tests cover command parsing and responder elections, API errors, persisted defaults, warning settings, detection radius and exact sparkling-name matching. Party coordination and Safari rendering should also be tested in Minecraft before release.

## Credits

- [ShinyHunter](https://github.com/javabetter/ShinyHunter): Hideyho quest acceptance and painting suppression reference. Copyright 2026 GamingLegend123; MIT notice in `licenses/ShinyHunter-MIT.txt`.
- [CritterMod v0.9.0](https://github.com/MrCloudy2/critterMod/tree/v0.9.0), commit `cdfcb8effb4b3158df50f0c0960c11e536931840`: sparkling-name matching, label/body pairing, species data and area lookup reference. Copyright 2026 MrCloudy2; MIT notice in `licenses/CritterMod-MIT.txt`.
- The Safari area table bundled by CritterMod derives from SkyHanni's Safari path graph.
- [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig) 4.7.2 supplies the settings UI and is bundled as a separate LGPL-3.0 library.

The original Sparkling Mutuals license remains in `LICENSE`; adapted third-party portions retain their MIT notices, included in the built JAR.

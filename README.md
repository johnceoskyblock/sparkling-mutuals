<h1 align="center">Sparkling Mutuals</h1>

<p align="center">
  <img src="src/main/resources/assets/sparkling-mutuals/icon.png" width="80" height="80" alt="Sparkling Mutuals logo">
</p>

<p align="center">
  <a href="https://discord.com/channels/1567856303341051905/1567859199491186749"><img src="https://img.shields.io/badge/Discord-Community-5865F2?style=for-the-badge&amp;logo=discord&amp;logoColor=white" alt="Discord"></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Made%20with-Kotlin-7F52FF?style=for-the-badge&amp;logo=kotlin&amp;logoColor=white" alt="Made with Kotlin"></a>
  <a href="https://github.com/johnceoskyblock/sparkling-mutuals/releases/latest"><img src="https://img.shields.io/github/downloads/johnceoskyblock/sparkling-mutuals/total?label=Downloads&amp;style=for-the-badge&amp;logo=github" alt="Downloads"></a>
</p>

A Fabric client mod for Hypixel SkyBlock’s Critter Safari.

## Installation

1. Set up **Minecraft 26.1.2, 26.2, or 26.3** with **Java 25** and [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.5 or newer**.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version and [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) in your `mods` folder.
3. Pick one edition and the JAR whose `+mc` suffix matches your Minecraft version. Install it in `mods` and remove older Sparkling Mutuals JARs. [GitHub Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases/latest)
4. Launch the Fabric profile and use `/sm` to open settings.

## Editions

- **Standard** uses JohnKuudra’s visibility restrictions for markers and sparkling alerts. It retains Safari modes, party progress sync, HUDs, and history. Standard builds are available through Actions during release testing. [Original Standard changes](https://github.com/1tsethan/sparkling-mutuals/commit/b297adf)
- **Full** is the original feature set, with optional update downloads that install after the game closes. GitHub JARs keep the existing filename.
- **Full No Auto Update** has the Full feature set with in-game update checks, downloads, and the installer removed. Look for `-full-no-auto-update` in the JAR filename.

The feature list below describes Full. See the [shipping guide](docs/SHIPPING.md) for local builds and release testing.

## Features

### Safari

| Feature | What it does |
| --- | --- |
| Safari modes | Track Sparkling checks, one catch per species, or a Full Clear with `/sparkle`, `/unique`, or `/fc`. |
| Progress HUDs | See remaining critters, checks, captures, and supplies needed to finish the run. |
| Critter ESP | Highlight eligible critters with model glow, optional boxes, and configurable species or biome colors. |
| Floor-drop and structure ESP | Locate biome-specific floor drops, bee nests, Rockmite mounds, Snoozle walls, and other spawn sources. |
| Sparkling alerts | Highlight detected Sparklings and announce sightings with configurable visual and sound alerts. |
| Sparkling lookups | Compare personal and party discoveries with `/sparkling`. Individual cards include total and per-biome captures, also available in party chat with `!captures [IGN]`. Use `!sparkling [IGN]` for total, unique, and duplicate sparklings. |
| Sparkling rates | Use `/rates` for estimates or `!rates` in party chat to share each responding player’s current session catch count and runs per sparkling. |
| Capsule Projection | Preview estimated capsule paths and landing points. |
| Gemzie auto-select | Select the matching hotbar gem while holding right-click over a Gemzie podium. |
| Rockmite auto-clicker | Repeatedly click Rockmite mounds while holding left-click. |
| Candle hitboxes | Use full candle hitboxes while holding incense. |
| Hideyho auto-accept | Automatically accept Hideyho's quest dialogue. |
| Doomspiral throw timer | Time capsule throws around Doomspiral's emergence with a configurable tick countdown. |
| Doomspiral Lottery | Watch a slot machine reveal whether the summoned Doomspiral is Sparkling. |
| Clear Water | Improve underwater visibility with separate water-opacity settings for each Safari biome. |
| Lake Cleared alert | Show a personal alert when your lake objectives are complete. |
| Safari visual cleanup | Hide selected chat messages, capsules, paintings, and darkness effects. |

### Party and entry

| Feature | What it does |
| --- | --- |
| Party sync | [Share Safari objectives and confirmed critter progress](docs/PARTY_SYNC.md) with compatible SafariUtils parties. |
| Hotspot assigning | Coordinate biome assignments with `/hotspot`; party members can use `!hotspot on` or `!hotspot off`. |
| Party commands | Share missing critters, tickets, PBs, streaks, and recorded Emerald Dye counts. Use `!commands` for the list. |
| Teammate ESP | Highlight party members and show their names through walls. |
| Sparkle Warping | Invite and warp players using a Warp List, Solo/Duo modes, and separate reparty and warp timers. |
| Leech mode | Accept whitelisted party invites, leaving the current party first when needed. |
| Sparkling auto-entry | Opt in to using a ticket to join a party member's Safari when shared sync reports an outstanding Sparkling. |
| Entry guard | Hold Safari entry until the party is ready, including downtime and contest checks. |
| Ticket auto-select | Select a preferred Safari ticket tier when entering as a non-leader. |
| Entry cooldown HUD | Show the retry countdown after the server refuses entry to an instance. |
| Miria contests | Track contest progress, ticket readiness, warnings, and customizable ticket-earned announcements. |
| Honeyhives | See Honeyhive locations and refill timers. |

### History and economy

| Feature | What it does |
| --- | --- |
| Run history | Review runs and catches in `/sm history`, or import older runs from Minecraft logs. |
| Personal bests | Track critter and biome completion times, including supported party records. |
| Achievements | Unlock milestones for Safari runs, catches, discoveries, and other progress. |
| Profit history | Review daily, weekly, and session profit. Start a new session with `/newsession`. |
| Hunting Box value | Estimate the combined value of shards in your Hunting Box. |
| Shard sale guidance | Compare Bazaar sell offers, instant selling, and salvaging Safari shards. |
| Profitable critters | Rank critters by ticket type with `/profit` and track targets with an optional checklist HUD. |

### Customization

| Feature | What it does |
| --- | --- |
| HUD layout | Move and resize HUDs with `/sm gui` and choose how each panel expands. |
| HUD appearance | Customize colors, borders, transparency, resource-pack fonts, text shadows, and pixel-aligned scaling. |
| HUD text customization | Set custom headers and separate header, label, and value colors for supported HUDs. |
| Custom critter names | Rename any of the 37 critters for local display while keeping tracking and party messages unchanged. |
| Critter nameplates | Configure optional per-critter nameplates with custom text, distance, size, color, and backgrounds. |
| Banners and catch effects | Customize Safari banners, alerts, and catch celebrations. |

### Other SkyBlock features

| Feature | What it does |
| --- | --- |
| Phantomleaf Solver | Locate Garden Phantomleaves using sound-distance clues. |
| Skills overflow | Show skill XP beyond the level cap in tooltips. |

See the [documentation index](docs/README.md) for feature guides and details.

## Credits

- **SafariUtils** by Serkosea, built on MrCloudy2’s CritterMod. Adapted the settings and Warp List layouts, HUD snapping and icons, Biome Objectives and contest HUDs, encounter banners, catch effects, nameplates and pity tracking, chat filtering, and parts of waypoint, floor-drop, birdfeeder, Scrappy, and Hideyho tracking. Its shared party-sync protocol enables cross-mod coordination. [Repository](https://github.com/Serkosea/safariutils) · [Party-sync notice](licenses/SafariUtils-PartySync-NOTICE.txt)
- **CritterMod** by MrCloudy2. Adapted the critter roster and catch quotas, the Safari area lookup, progress HUD layout, Sparkling announcement behavior, and parts of Rockmite mound and Snoozle wall tracking. [Repository](https://github.com/MrCloudy2/critterMod)
- **ShinyHunter** provided the basis for the `/sparkling` profile card and biome capture stats, Hideyho quest button matching, and hiding Safari paintings. [Repository](https://github.com/javabetter/ShinyHunter)
- **Nebulune** provided critter model identification, palette and box offsets for Safari ESP, and click timing for the Rockmite mound helper. [Repository](https://github.com/Gaeritag/Nebulune)
- **SkyHanni** provided the basis for skill overflow tooltips and level-up messages, capsule hiding, the Phantomleaf Solver, and the Ubik's Cube cooldown timer. The Safari area data also traces back to SkyHanni. [Repository](https://github.com/SkyHanniStudios/SkyHanni)
- **SkyOcean** and its Vanguard Gambling feature inspired the Doomspiral Lottery and informed the original reel animation. [Repository](https://github.com/meowdding/SkyOcean)
- **NoammAddons** informed the Doomspiral throw timer’s server tick clock, the Safari entry cooldown HUD, and teammate ESP. [Repository](https://github.com/Noamm9/NoammAddons)

See [third-party licenses and notices](licenses/) for attribution and license terms.

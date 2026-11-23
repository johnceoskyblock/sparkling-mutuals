<h1 align="center">Sparkling Mutuals</h1>

<p align="center">
  <img src="src/main/resources/assets/sparkling-mutuals/icon.png" width="80" height="80" alt="Sparkling Mutuals logo">
</p>

<p align="center">A Fabric mod for Hypixel SkyBlock’s Critter Safari. Track runs, coordinate your party, and customize your HUDs.</p>

<p align="center">
  <a href="https://github.com/johnceoskyblock/sparkling-mutuals/releases/latest"><img alt="Download on GitHub" src="https://img.shields.io/badge/Download-GitHub-24292f?style=for-the-badge&amp;logo=github"></a>
  <a href="https://github.com/johnceoskyblock/sparkling-mutuals/releases"><img alt="GitHub downloads" src="https://img.shields.io/github/downloads/johnceoskyblock/sparkling-mutuals/total?style=for-the-badge&amp;logo=github"></a>
  <a href="https://discord.com/channels/1567856303341051905/1567859199491186749"><img alt="Discord" src="https://img.shields.io/badge/Community-Discord-5865F2?style=for-the-badge&amp;logo=discord&amp;logoColor=white"></a>
</p>

<p align="center">Minecraft <strong>26.1.2</strong>, <strong>26.2</strong>, and <strong>26.3</strong> · Fabric · Java 25</p>

---

## Features

The features below are available in **Standard**, the default edition of Sparkling Mutuals.

<details>
<summary><strong>Safari tracking and party coordination</strong></summary>

- Unique, Full Clear, and Sparkle modes with progress, objectives, capture counts, and missing critters. Unique and Full Clear also have a biome splits HUD.
- Party sync for biome objectives, confirmed progress, and Hotspot assignments with compatible SafariUtils clients. [Party sync](docs/PARTY_SYNC.md)
- Run history, personal bests, sparkling collections, capture stats, and profit by session, day, or week.
- Custom HUD positions, colors, fonts, critter names, banners, sounds, and catch celebrations.

</details>

<details>
<summary><strong>Safari and SkyBlock helpers</strong></summary>

- Miria contest progress, Torrhus Honeyhive refill beacons, and known Snoozle wall waypoints.
- Capsule trajectory previews and the Doomspiral Lottery.
- Hunting Box valuation and shard sale guidance for sell offers, instant selling, and salvaging.
- Skill overflow tooltips and the Rift’s Ubik’s Cube cooldown timer.

</details>

<details>
<summary><strong>Highlights and interaction shortcuts</strong></summary>

- Visible critter highlights with separate glow and box controls under **Highlights → General**, customizable colors, and generic floor-drop tiles.
- Sparkling alerts and Doomspiral Lottery results based on what you can see.
- **Accept Hideyho Quest** keybind setup at startup and a **Party member kicked** banner for kick reports in party chat.

</details>


---

## Commands

<details>
<summary><strong>Slash commands</strong></summary>

- `/sm` opens settings, `/sm gui` moves and resizes HUDs, and `/sm help` lists commands.
- `/unique`, `/fc`, and `/sparkle` choose the run mode. `/hotspot` toggles biome assignments.
- `/sparkling [IGN|party]` looks up sparkling collections and capture counts. `/captures` shows captures during your current run.
- `/sm history`, `/sm bests`, `/sm sparklings`, `/sm stats`, and `/sm profits` open history tabs. `/newsession` starts a new profit session.

</details>

<details>
<summary><strong>Party commands</strong></summary>

- `!commands` lists available commands. `!captures [IGN]` and `!sparkling [IGN]` look up the sender when you omit the name.
- `!rates` shows responding players’ session sparkling catches and runs per sparkling. `!profit [session|lifetime|week|day]` shows their recorded Safari profit.
- `!dt [reason]` requests downtime. `!hotspot on` and `!hotspot off` set Hotspot assigning for party members using Sparkling Mutuals.

</details>

---

## Installation

1. Set up a supported Minecraft version with **Java 25** and **Fabric Loader 0.19.5 or newer**. Add Fabric API for your Minecraft version and Fabric Language Kotlin. [Fabric installer](https://fabricmc.net/use/installer/) · [Fabric API](https://modrinth.com/mod/fabric-api) · [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
2. Download one Sparkling Mutuals edition and match its `+mc` suffix to your Minecraft version. Replace the previous Sparkling Mutuals JAR in your `mods` folder. [GitHub releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases)
3. Launch the Fabric profile and use `/sm` to open settings. The **What’s New** button shows highlights for your installed edition.

<details open>
<summary><strong>Editions and building from source</strong></summary>

| Edition | Filename pattern | Build command |
| --- | --- | --- |
| **Standard** (default) | `sparkling-mutuals-<version>+mc<minecraft>.jar` | `./gradlew buildAllTargets -Pedition=standard` |
| **Full** | `sparkling-mutuals-<version>-full+mc<minecraft>.jar` | `./gradlew buildAllTargets -Pedition=full` |
| **Full No Auto Update** | `sparkling-mutuals-<version>-full-no-auto-update+mc<minecraft>.jar` | `./gradlew buildAllTargets -Pedition=full-no-auto-update` |

**Standard** is the default edition described above. **Full** is the extended version of the mod, with **Download Update** and opt-in **Auto Update** in `/sm`. Verified updates install after Minecraft closes. **Full No Auto Update** has the same features as Full and uses manual downloads, as does Standard. Standard’s Modrinth publication is pending.

All editions share settings, HUD layouts, and Safari records within the same Minecraft instance at `config/sparkling-mutuals`. Edition-specific preferences are saved for when you switch back.

Use Java 25 to build and replace `./gradlew` with `./gradlew.bat` on Windows. Verified JARs go to `build/distributions/<version>/<edition>`. [Shipping guide](docs/SHIPPING.md)

</details>

---

## Links

- [Documentation and feature guides](docs/README.md)
- [GitHub releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases)
- [GitHub issues](https://github.com/johnceoskyblock/sparkling-mutuals/issues)
- [Discord](https://discord.com/channels/1567856303341051905/1567859199491186749)

## Credits

- **JohnKuudra** contributed Standard’s visible highlights and gameplay improvements. [Standard source](https://github.com/1tsethan/sparkling-mutuals/commit/b297adf)
- **SafariUtils** by Serkosea, built on MrCloudy2’s CritterMod. Adapted settings layouts, HUD snapping and icons, Biome Objectives and contest HUDs, encounter banners, catch effects, chat filtering, and parts of waypoint, floor-drop, birdfeeder, Scrappy, and Hideyho tracking. Its shared party-sync protocol enables cross-mod coordination. [Repository](https://github.com/Serkosea/safariutils) · [Party-sync notice](licenses/SafariUtils-PartySync-NOTICE.txt)
- **CritterMod** by MrCloudy2. Adapted the critter roster and catch quotas, the Safari area lookup, progress HUD layout, Sparkling announcement behavior, and parts of Rockmite mound and Snoozle wall tracking. [Repository](https://github.com/MrCloudy2/critterMod)
- **ShinyHunter** provided the basis for the `/sparkling` profile card and biome capture stats, Hideyho quest button matching, and hiding Safari paintings. [Repository](https://github.com/javabetter/ShinyHunter)
- **Nebulune** provided critter model identification and palette and box offsets for Safari highlights. [Repository](https://github.com/Gaeritag/Nebulune)
- **SkyHanni** provided the basis for skill overflow tooltips and level-up messages, capsule hiding, the Phantomleaf Solver, and the Ubik's Cube cooldown timer. The Safari area data also traces back to SkyHanni. [Repository](https://github.com/SkyHanniStudios/SkyHanni)
- **SkyOcean** and its Vanguard Gambling feature inspired the Doomspiral Lottery and informed the original reel animation. [Repository](https://github.com/meowdding/SkyOcean)
- **NoammAddons** informed the Doomspiral throw timer’s server tick clock and the Safari entry cooldown HUD. [Repository](https://github.com/Noamm9/NoammAddons)
- **Phantom Addons** inspired the edition and download layout. [Repository](https://github.com/PhantomOrigin/Phantom-Addons)

See [third-party licenses and notices](licenses/) for attribution and license terms.

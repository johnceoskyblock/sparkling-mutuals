<h1 align="center">Sparkling Mutuals</h1>

<p align="center">
  <img src="src/main/resources/assets/sparkling-mutuals/icon.png" width="80" height="80" alt="Sparkling Mutuals logo">
</p>

A Fabric client mod for Hypixel SkyBlock’s Critter Safari. Track runs, coordinate with your party, and customize your HUDs.

[GitHub Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases/latest) · [Discord](https://discord.com/channels/1567856303341051905/1567859199491186749)

## Editions

| Edition | Features and updates | JAR filename |
| --- | --- | --- |
| **Standard** | Shared Safari features with visible critter outlines, generic floor-drop highlights, and local sparkling alerts. Includes capsule trajectory previews and the Doomspiral lottery with a locally visible result. Modrinth publication is pending. [Test builds](https://github.com/johnceoskyblock/sparkling-mutuals/actions/workflows/build.yml) | `sparkling-mutuals-<version>+mc<minecraft>.jar` |
| **Full** | Adds through-terrain highlights, remote sparkling alerts, and gameplay automation. Supports in-game update downloads. [GitHub Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases/latest) | `sparkling-mutuals-<version>-full+mc<minecraft>.jar` |
| **Full No Auto Update** | Same gameplay features as Full, with manual updates. [GitHub Releases](https://github.com/johnceoskyblock/sparkling-mutuals/releases/latest) | `sparkling-mutuals-<version>-full-no-auto-update+mc<minecraft>.jar` |

In Full, open the update status in `/sm` and choose **Download Update**. Turn on **Auto Update** there for automatic downloads. Updates install after Minecraft closes.

In Standard, bind **Invite Next Sparkling Player**, **Prepare Party Warp**, and **Accept Hideyho Quest** under Minecraft Controls → Sparkling Mutuals. Each tap sends one invite in priority order for the visible sparkling critter. The warp shortcut prepares a chat command and the Hideyho shortcut selects the current quest prompt. Set invite priorities under `/sm` → Party → Interactions.

All three editions share `config/sparkling-mutuals` within the same Minecraft instance, so settings, HUD layouts, and Safari history carry over. Standard leaves Full-only settings inactive and preserves their saved values. On first launch, missing saves are copied from the old Standard folder.

## Installation

1. Use **Minecraft 26.1.2, 26.2, or 26.3** with **Java 25** and **Fabric Loader 0.19.5 or newer**. [Fabric installer](https://fabricmc.net/use/installer/)
2. Add the Fabric API for your Minecraft version and Fabric Language Kotlin to your `mods` folder. [Fabric API](https://modrinth.com/mod/fabric-api) · [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
3. Install one Sparkling Mutuals JAR with the `+mc` suffix matching your Minecraft version. Remove any previous Sparkling Mutuals JAR from the same `mods` folder.
4. Launch the Fabric profile. Use `/sm` for settings, `/sm gui` to move or resize HUDs, and `/sm help` for the command list.

## Features

- **Safari tracking** — Use `/unique`, `/fc`, or `/sparkle` to track catches and sparkling checks. Progress, objectives, missing critters, captures, and splits have their own HUDs.
- **Party coordination** — Share objectives and confirmed progress with compatible SafariUtils clients. Use `/hotspot` for biome assignments and `/sparkling` for collection and capture stats. Use `!commands` for party commands, including `!captures`, `!sparkling`, `!rates`, and `!profit`. [Party sync](docs/PARTY_SYNC.md)
- **History and profit** — Open `/sm history` for runs, personal bests, catches, and profit. Start a profit session with `/newsession`. Compare sell offers, instant selling, and salvaging from the Hunting Box.
- **Customization** — Adjust HUD layouts, colors, and fonts. Customize local critter names, banners, sounds, and catch celebrations. The Full editions also offer custom nameplates and teammate highlights.
- **SkyBlock helpers** — Track Miria’s contests and Honeyhive timers, view overflow skill levels, and see the Rift’s Ubik’s Cube cooldown.
- **Full editions** — Add Sparkle Warping and the Warp List, leech invites, Safari auto-entry and ticket selection, and capture helpers.

See the [documentation index](docs/README.md) for feature guides and details.

## Credits

- **JohnKuudra** contributed Standard’s line-of-sight restrictions and gameplay changes. [Standard source](https://github.com/1tsethan/sparkling-mutuals/commit/b297adf)
- **SafariUtils** by Serkosea, built on MrCloudy2’s CritterMod. Adapted the settings and Warp List layouts, HUD snapping and icons, Biome Objectives and contest HUDs, encounter banners, catch effects, nameplates and pity tracking, chat filtering, and parts of waypoint, floor-drop, birdfeeder, Scrappy, and Hideyho tracking. Its shared party-sync protocol enables cross-mod coordination. [Repository](https://github.com/Serkosea/safariutils) · [Party-sync notice](licenses/SafariUtils-PartySync-NOTICE.txt)
- **CritterMod** by MrCloudy2. Adapted the critter roster and catch quotas, the Safari area lookup, progress HUD layout, Sparkling announcement behavior, and parts of Rockmite mound and Snoozle wall tracking. [Repository](https://github.com/MrCloudy2/critterMod)
- **ShinyHunter** provided the basis for the `/sparkling` profile card and biome capture stats, Hideyho quest button matching, and hiding Safari paintings. [Repository](https://github.com/javabetter/ShinyHunter)
- **Nebulune** provided critter model identification, palette and box offsets for Safari ESP, and click timing for the Rockmite mound helper. [Repository](https://github.com/Gaeritag/Nebulune)
- **SkyHanni** provided the basis for skill overflow tooltips and level-up messages, capsule hiding, the Phantomleaf Solver, and the Ubik's Cube cooldown timer. The Safari area data also traces back to SkyHanni. [Repository](https://github.com/SkyHanniStudios/SkyHanni)
- **SkyOcean** and its Vanguard Gambling feature inspired the Doomspiral Lottery and informed the original reel animation. [Repository](https://github.com/meowdding/SkyOcean)
- **NoammAddons** informed the Doomspiral throw timer’s server tick clock, the Safari entry cooldown HUD, and teammate ESP. [Repository](https://github.com/Noamm9/NoammAddons)

See [third-party licenses and notices](licenses/) for attribution and license terms.

# Standard edition: Modrinth submission draft

Last updated: 2026-11-23 (America/New_York). Separated the public Standard description from moderator-only edition, updater, license and network disclosures. Submission and approval are pending.

## Public project listing — Standard only

**Name:** Sparkling Mutuals

**Summary:** Safari run tracking, visible critter highlights, party progress, and history for Hypixel SkyBlock.

Sparkling Mutuals is a Fabric client mod for Hypixel SkyBlock's Critter Safari. Track Unique, Full Clear and Sparkle runs with customizable progress, objectives, captures and biome splits HUDs. Review run history, personal bests and profit estimates, and coordinate biome progress and Hotspot assignments with compatible party clients.

### Features

- Visible critter outlines and boxes, customizable highlight colors, generic visible floor-drop tiles, and local sparkling alerts.
- Miria contest reminders, Torrhus Honeyhive refill markers, known Snoozle wall waypoints, capsule trajectory previews, and the Doomspiral Lottery with locally visible results.
- Hunting Box and shard sale guidance, skill overflow tooltips, and the Rift's Ubik's Cube cooldown timer.
- Configurable HUD layouts, fonts, colors, sounds and banners, plus the Accept Hideyho Quest keybind.
- Party commands for collection/capture lookups, session sparkling rates, profit and downtime requests.

Critter highlights and sparkling results use local visibility. Known fixed wall/hive markers and the optional capsule path can remain visible through terrain. Party progress sync does not grant hidden critter highlights or hidden sparkling alerts.

### Installation and updates

Use Minecraft 26.1.2, 26.2 or 26.3 with Java 25, Fabric Loader 0.19.5 or newer, the matching Fabric API, and Fabric Language Kotlin. Declare the two Fabric mods as dependencies on each uploaded version.

Upload only the unsuffixed Standard file matching the game's version: sparkling-mutuals-<version>+mc<minecraft>.jar. Close Minecraft, replace the previous Sparkling Mutuals JAR in mods, then launch and open /sm. Standard uses manual updates and contains no custom update checker, downloader or restart installer. Its settings and Safari records share config/sparkling-mutuals with the other editions; missing former Standard saves are imported once.

### Network use

Safari collection lookups use the hosted profile service or the player's configured Hypixel API connection. Name resolution uses Mojang's public profile API where needed.

Party sync connects to the configured Safari sync services. It sends the local Minecraft UUID for authentication, confirmed party UUIDs and the Safari server identifier to identify the room, and bounded objective progress, discoveries and relevant held quest-item counts. It uses compatible clients' confirmed progress for coordination. See [party sync](PARTY_SYNC.md) for protocol and service details. Replace this relative link with its public repository URL in the final listing.

### License and source

License: LGPL-2.1-only. Third-party components retain their upstream notices and licenses. [License inventory](../licenses/README.md) · [Source repository](https://github.com/johnceoskyblock/sparkling-mutuals)

## Moderation notes — do not copy Full features into the Standard listing

We maintain three editions in one public source repository. Only Standard is proposed for Modrinth. GitHub provides all three, with the edition recorded in fabric.mod.json as custom.sparkling-mutuals:edition. Standard uses the plain filename; Full uses -full and Full No Auto Update uses -full-no-auto-update before +mc.

| Feature / implementation | Standard | Full | Full No Auto Update |
| --- | --- | --- | --- |
| Shared tracking, sync, HUDs and history | Included | Included | Included |
| Through-terrain dynamic-target highlights, custom nameplates, teammate highlights | Excluded; visible critter highlights use the Standard implementation | Included | Included |
| Hidden/remote sparkling alerts | Excluded; local visibility is required | Included | Included |
| Sparkle Warping, Warp List, automatic invites and timed warps | Excluded | Included | Included |
| Automatic ticket/capture input and auto-entry | Excluded | Included | Included |
| Custom release checks, update downloads, restart installer and update controls | Excluded from packaged classes and controls | Included with configurable automatic downloads | Excluded from packaged classes and controls |
| Fixed Snoozle wall/hive markers and optional capsule trajectory through terrain | Included and disclosed for review | Included | Included |

### How exclusions are implemented

- Standard is the default Gradle edition (-Pedition=standard), using src/standard replacements/resources and the exact exclusions in src/standard/exclude.txt. Its mixin registration excludes the Full input/automation hooks. These are build-time source/resource differences, not a hidden runtime toggle.
- Standard and Full No Auto Update both apply src/noUpdates and its exclude.txt. The Full release checker and UpdateTransport/FullUpdateManifest sources are excluded; their ModUpdates replacement exposes only version information and no update operations.
- src/updater Java installer sources are added only for Full in build.gradle.kts. The corresponding Windows restart tests are Full-only.
- buildSrc/src/main/kotlin/DistributionVerifier.kt rejects config/ReleaseUpdateChecker* and update/* classes from both no-updater editions, rejects Full automation/renderer classes from Standard, and validates mixin registrations. It also checks edition identity, target, license metadata and packaged notices.
- Release CI builds and tests all nine edition/Minecraft combinations and runs verifyReleaseMatrix before preparing the public assets. Attach links to the actual tagged run and source/exclusion files for the version submitted.

Standard's profile/party-sync network connections are distinct from a mod updater. The unchanged network services are disclosed above; absence of an updater does not mean the mod is offline.

## Submission checklist

- Upload only the three verified Standard JARs, assigning each to its matching Minecraft version. Do not upload Full or Full No Auto Update to the Standard project, including as additional files.
- Confirm the exact uploaded JAR's metadata, excluded classes/mixins, dependencies, license and matching tagged source. Test the installed Standard build and the affected screens in-game; CI is separate evidence.
- Keep Full-only behavior out of the public Standard description, screenshots and changelog. Supply the comparison and build-time exclusion evidence in moderation notes.
- Select LGPL-2.1-only, preserve attribution/notices, and link corresponding tagged source/build scripts. The prior CC0 dedication covers historical original contributions, not all included third-party work.
- The maintainer confirmed on 2026-11-23 that the icon, lottery image and Max Win image/audio were created for Sparkling Mutuals; the license inventory records that provenance.
- Complete required content disclosures, including network access and AI-generated content where applicable. Do not claim Modrinth or Hypixel approval before it exists.
- Request explicit review of the disclosed fixed markers/trajectory geometry and the shared repository if moderation needs clarification. Another project's approval is not approval of this build.

Policy references: [Modrinth Content Rules](https://modrinth.com/legal/rules) · [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications)

## What’s New in 4.4.4

- The **What’s New** screen now shows highlights for your installed edition, including relevant releases you skipped. Each edition remembers which release notes you’ve read.
- Switching editions in the same Minecraft instance keeps shared settings and Safari records. Saving or resetting settings preserves preferences belonging to the other edition.

## Standard

- The **Highlights** tab has independent **Glow highlights** and **Box highlights** controls. Glow defaults on and boxes default off. Boxes appear only when enabled. Standard uses its built-in biome limits and one customizable highlight color for all Scrappies.
- Torrhus Canyon shows beacons and waypoints for ready Honeyhives from a distance, using known hive locations and saved refill timers.
- With **Respect !dt requests** enabled, the party leader announces outstanding requests when they return to Torrhus Canyon. The new **Party member kicked** option under **Banner → Run and party alerts** shows when a teammate reports a kick during Safari.
- **Accept Hideyho Quest** offers keybind setup at startup when unbound. You can also set it under **Safari → Helpers → Capture tools**.
- Sparkle Warping, its Warp List and banner, and the manual invite and warp shortcuts were removed from Standard. Both Full builds have the automated warping features.

## Release verification

- Publishing verifies all nine edition and Minecraft JARs. Verification retries when GitHub briefly returns stale release notes or title metadata.

## Downloads

GitHub releases provide all three builds for Minecraft **26.1.2**, **26.2**, and **26.3**. Install one JAR with the matching `+mc` suffix.

| Edition | Filename suffix before `+mc` | Updates |
| --- | --- | --- |
| **Standard** | None | Manual downloads |
| **Full** | `-full` | Verified in-game downloads with optional automatic downloads |
| **Full No Auto Update** | `-full-no-auto-update` | Manual downloads |

### Standard fixes
- Keep an already-started Sparkling alert visible for its normal duration after the critter goes out of sight.
- Recognise visible Sparkling skins without requiring a visible nametag, including Hideyho.
- Keep partially visible critters marked with a depth-tested box when their whole-model glow cannot be shown safely.
- Add corroborated particle-based Sparkling identification and SafariUtils-style Safe Mode visibility, including body-only checks for hidden species and prop-aware Duplico sighting.

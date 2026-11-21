## Biome Objectives controls

- Added independent switches for HUD rows such as nests, bird food, feeder contents, podiums, Snoozle walls, Rockmite mounds, coins, and Gazers.
- Added per-species optional Sparkling-check choices for Unique and Sparkle modes. Skipped checks no longer delay biome completion or appear in missing-objective replies; Full Clear requirements and required Unique captures remain in place.
- Hiding a HUD row only changes its display. Optional-check choices also guide related critter and resource ESP, while preserving targets still needed for captures. Skipped checks are not shared as completed evidence.

## Safari Splits HUD

- Added a biome splits HUD for Unique and Full Clear runs, with configurable seconds, tenths, hundredths, or milliseconds.
- Added an optional current-session summary with average run time, run count, credited Sparklings, and runs per Sparkling when available.
- Splits lock against the mode's capture and report requirements. The HUD defaults on and supports normal placement and appearance controls.

## Party commands and capture stats

- Added `!profit` with `session`, `lifetime`, `week`, or `day`; no argument defaults to the current session. Each responding mod user reports their recorded Safari profit, runs, and hourly rate when available, including whether values use sell offers or instant selling.
- Added `!captures [IGN]` for total and per-biome Safari capture counts. With no IGN, it looks up the sender.
- Individual `/sparkling` cards now include those capture totals and biome counts. Unavailable counts are reported as unavailable rather than invented as zero.
- Updated party-command help and player-name suggestions for the new commands.

## Ubik's Cube cooldown

- Added an opt-in Rift Split or Steal cooldown timer under **SkyBlock → Helpers**.
- Tracks the cooldown from the game menu or the server's wait message, saves it by player/profile, and retains it across relogs. The HUD supports placement and appearance settings; the feature defaults off.

## In-game release highlights

- Added a feature highlights screen with curated descriptions of what changed and where to find the new controls.
- Tracks installed versions and unread highlights, including features from releases skipped during an upgrade. The bundled catalog works without fetching release notes and adapts to compact or wide screens.

Builds target **Minecraft 26.1.2, 26.2, and 26.3**. Choose the JAR matching your Minecraft version.

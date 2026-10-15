# Doomspiral

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Haunted. **Recorded Full Clear baseline:** 1. Baseline counts are carried from earlier notes, not newly verified server guarantees.

## Evidence

- **Legendary: can escape a standard capsule; Masterful catches are guaranteed once hit.** An escaped critter may return with a replacement UUID; that alone does not prove an additional Doomspiral spawn.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Haunted; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One shared catch; if anyone still needs this sparkling variant, also satisfy the species' Sparkling evidence. The missing-species row may disappear after the catch. |
| Full Clear | At least 1 confirmed catches, all verified additional spawns captured, no known live remainder, and all relevant opportunities resolved. |
| Sparkling | Require one confirmed shared catch (personal capture or party loot share). Sightings, retained UUIDs, spawn/slain messages and party biome reports cannot mark it checked. Retain the catch for the run. |

## Specific deductions

- **Sparkling-mode Incense ESP:** show only if someone needs Sparkling Doomspiral. Stop for the rest of the run after either four confirmed incense floor pickups or inventory holding four Soothing Incense simultaneously at any point. Starting incense counts toward the inventory cutoff, including two starting items; spending incense or reentering Haunted does not restore ESP. Floor-pickup counts still exclude starting grants. Coin visibility is independent. Every mode applies both the four-floor-pickup and four-held cutoffs, plus the [incense waypoint filter](FLOOR_DROPS.md#incense-waypoint-selection-in-every-mode): closest four locations to (4,47,-27), or closest two with one/two starting incense. Collecting or unloading a selected location does not select a farther replacement.
- Haunted highlights may select predicted Soothing Incense/Shining Coin drops via the [floor-drop predictor](FLOOR_DROPS.md). Prediction does not prove incense acquired/used or a ritual completed.
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Starting grants and Haunted pickups can provide Soothing Incense. Track grants, pickups and actual use separately.
- Summoner-only spawn: `Your ritual summoned a Doomspiral into this world. Stay Still.` Global spawn signal: `Something stirs in the Haunted Biome...`. Having incense alone is not a spawn or catch.
- Global post-capture/slain: `The darkness in the Haunted Biome fades away...`. Keep this phase separate from capture/loot-share attribution. See [exact events](MESSAGES.md#boss-events).

## Last updated

2026-10-15 - Applied incense waypoint selection and collection/inventory cutoffs in every mode; retained capture-required Sparkling completion.

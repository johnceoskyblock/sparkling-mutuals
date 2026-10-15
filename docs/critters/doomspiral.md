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
| Sparkling | Record distinct UUIDs seen inside this species' qualifying scan area and retain their checked progress for the run, including after unloading and biome reentry. Reconcile shared captures without counting a later catch of a seen UUID twice; source requirements remain separate. |

## Specific deductions

- **Sparkling-mode Incense ESP:** show it only if at least one party member needs Sparkling Doomspiral. Keep it enabled until **four Soothing Incense floor pickups** are confirmed; observing a predicted marker, holding starting incense or hearing a spawn announcement is not a floor pickup. If everyone has the discovery, hide Incense ESP. This condition is independent of Shining Coin ESP. In every mode, four confirmed incense floor pickups hide all remaining incense ESP, regardless of Gimmiegold discovery needs; starting inventory alone does not advance this pickup counter.
- Haunted highlights may select predicted Soothing Incense/Shining Coin drops via the [floor-drop predictor](FLOOR_DROPS.md). Prediction does not prove incense acquired/used or a ritual completed.
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Starting grants and Haunted pickups can provide Soothing Incense. Track grants, pickups and actual use separately.
- Summoner-only spawn: `Your ritual summoned a Doomspiral into this world. Stay Still.` Global spawn signal: `Something stirs in the Haunted Biome...`. Having incense alone is not a spawn or catch.
- Global post-capture/slain: `The darkness in the Haunted Biome fades away...`. Keep this phase separate from capture/loot-share attribution. See [exact events](MESSAGES.md#boss-events).

## Last updated

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

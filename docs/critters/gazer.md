# Gazer

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Haunted. **Confirmed fixed spawn/check total:** 4. **Full Clear capture minimum:** 4.

## Evidence

- **Uncommon: guaranteed catch; no capsule escape.** A UUID identifies an observed entity; repeated sightings do not add another Gazer.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Haunted; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One shared catch; if anyone still needs this sparkling variant, also satisfy the species' Sparkling evidence. The missing-species row may disappear after the catch. |
| Full Clear | At least 4 confirmed catches, all verified additional spawns captured, no known live remainder, and all relevant opportunities resolved. |
| Sparkling | Retain distinct Gazer UUIDs seen anywhere in the run. Once four Gazers have been checked, retain 4/4 even when they leave render distance or the player leaves/reenters Haunted. Shared captures can also establish checked spawns, without double counting previously seen Gazers. |

## Specific deductions

- Panel counter: Full Clear shows **X/4 captured** from confirmed shared catches only. Sparkling shows **X/4 checked** from retained distinct run sightings plus safely reconciled catches; Unique shows that checked counter only if anyone needs Sparkling Gazer, retaining unfinished checking after the first catch. Never use a bare ratio. See [panel rules](TRACKING.md#missingcheck-panel-by-mode).
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Gazer is Uncommon and cannot escape a capsule. Like every critter checked in Unique/Sparkling modes, remember every distinct Gazer UUID for the run. Repeated sightings of the same UUID add nothing. Retain partial progress and 4/4 checked after unloading; reset only for a new run.
- Record the shared capture count at each UUID's first live sighting. Combine catches preceding those new sightings with the distinct later sightings; do not add current catches to all historical UUIDs. Catching an already-seen Gazer must not count it twice. Full Clear's X/4 captured remains actual catches only, and Unique still requires its own shared catch.
- Unique requires a successful capture. If anyone still needs Sparkling Gazer, also apply its Sparkling check; observed entities remain separate from catch confirmations.
- Show Gazer exactly once in the missing/check panel: its colored name and gray mode-specific counter. Remove duplicate Gazers captured/Gazers checked footer rows in every mode. A required unfinished Unique check retains the plain Gazer row after its first catch.

## Last updated

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

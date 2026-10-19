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
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected Sparkling entities do not add a separate capture requirement. |
| Full Clear | Its fixed capture total completes progress before a report. The missing row also requires local empty-scan proof (Gemzie inside its zone; Scrappy/Troodon/Gazer in their biome), retained after leaving the radius. Positive remaining population reopens local completion until caught. See the [panel reference](../MISSING_PANEL.md). |
| Sparkling | Retain distinct Gazer UUIDs seen anywhere in the run. Once four Gazers have been checked, retain 4/4 even when they leave render distance or the player leaves/reenters Haunted. Shared captures can also establish checked spawns, without double counting previously seen Gazers. |

## Specific deductions

- Local Full Clear capture/source and empty-scan proof advances progress and removes the missing row without a report. Remote progress without local proof uses report rules.

- Panel counter: Full Clear shows **X/4 captured** from confirmed shared catches only. Sparkling shows **X/4 checked** from retained distinct run sightings plus safely reconciled catches; Unique shows that checked counter only if anyone needs Sparkling Gazer, with the species row removed after the first shared catch. Never use a bare ratio. See [panel rules](TRACKING.md#missingcheck-panel-by-mode).
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Gazer is Uncommon and cannot escape a capsule. Like every critter checked in Unique/Sparkling modes, remember every distinct Gazer UUID for the run. Repeated sightings of the same UUID add nothing. Retain partial progress and 4/4 checked after unloading; reset only for a new run.
- Record the shared capture count at each UUID's first live sighting. Combine catches preceding those new sightings with the distinct later sightings; do not add current catches to all historical UUIDs. Catching an already-seen Gazer must not count it twice. Full Clear's X/4 captured remains actual catches only, and Unique still requires its own shared catch.
- Unique completes from its first shared Gazer catch without an ordinary local checking gate; retained checked evidence remains separate from capture confirmations.
- Show Gazer exactly once in the missing/check panel: its colored name and gray mode-specific counter. Remove duplicate Gazers captured/Gazers checked footer rows in every mode. Unique removes the Gazer row after its first shared catch; only an actual pending Sparkling capture can retain it.

## Last updated

2026-10-17 - Removed detected-Sparkling sightings as a separate completion requirement; normal species rules remain.


2026-10-17 - Removed superseded Unique post-catch checking wording; retained Sparkling counters and Full Clear capture rules.

2026-10-17 - Distinguished local Full Clear missing rows from progress completion.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

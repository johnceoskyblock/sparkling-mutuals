# Troodon

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Icy. **Confirmed fixed spawn/check total:** 3. **Full Clear capture minimum:** 3.

## Evidence

- **Rare: can escape a standard capsule; Masterful catches are guaranteed once hit.** An escaped critter may return with a replacement UUID; that alone does not prove an additional Troodon spawn.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Icy; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected Sparkling entities do not add a separate capture requirement. |
| Full Clear | Its fixed capture total completes progress before a report. The missing row also requires local empty-scan proof (Gemzie inside its zone; Scrappy/Troodon/Gazer in their biome), retained after leaving the radius. Positive remaining population reopens local completion until caught. See the [panel reference](../MISSING_PANEL.md). |
| Sparkling | Record distinct UUIDs seen inside this species' qualifying scan area and retain their checked progress for the run, including after unloading and biome reentry. Reconcile shared captures without counting a later catch of a seen UUID twice; source requirements remain separate. |

## Specific deductions

- Local Full Clear capture/source and empty-scan proof advances progress and removes the missing row without a report. Remote progress without local proof uses report rules.

- Panel counter: Full Clear shows **X/3 captured** from confirmed shared catches only. Sparkling shows **X/3 checked** from retained qualified UUID checks and safely reconciled captures; Unique shows that checked counter only if anyone needs Sparkling Troodon, with the species row removed after the first shared catch. Never use a bare ratio. See [panel rules](TRACKING.md#missingcheck-panel-by-mode).
- **ESP priority in every mode:** if anyone still needs Sparkling Wumpa, force this species' ordinary ESP until its first shared catch, before other mode rules. After that catch, apply normal mode rules. A Wumpa check does not waive another species' catch. See [Icy priority](TRACKING.md#highest-priority-icy-esp-rule-missing-sparkling-wumpa).
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- No additional species-specific server event has been established here. Use its UUIDs, capture/loot-share confirmations and the shared coverage rule.
- Keep this species separate from Wumpa: a Wumpa world event or catch does not replace this species' own capture evidence.

## Last updated

2026-10-17 - Removed detected-Sparkling sightings as a separate completion requirement; normal species rules remain.


2026-10-17 - Removed superseded Unique post-catch checking wording; retained Sparkling counters and Full Clear capture rules.

2026-10-17 - Distinguished local Full Clear missing rows from progress completion.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

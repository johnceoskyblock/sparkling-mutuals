# Gemzie

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Cavern. **Confirmed fixed spawn/check total:** 3. **Full Clear capture minimum:** 3.

## Evidence

- **Epic: can escape a standard capsule; Masterful catches are guaranteed once hit.** An escaped critter may return with a replacement UUID; that alone does not prove an additional Gemzie spawn.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Cavern; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected Sparkling entities do not add a separate capture requirement. |
| Full Clear | Its fixed capture total completes progress before a report. The missing row also requires local empty-scan proof (Gemzie inside its zone; Scrappy/Troodon/Gazer in their biome), retained after leaving the radius. Positive remaining population reopens local completion until caught. See the [panel reference](../MISSING_PANEL.md). |
| Sparkling | Hypixel's exact chamber-opening message checks Gemzie after five seconds without rendered entities. Retained qualified UUID/capture evidence reaching three remains an alternative. The timer also supplies the needed Sparkling check in Unique mode. |

## Specific deductions

- Local Full Clear capture/source and empty-scan proof advances progress and removes the missing row without a report. Remote progress without local proof uses report rules.

- Cavern highlights may select predicted Lime/Purple/Orange Gem drops via the inspected [floor-drop predictor](FLOOR_DROPS.md). This selects unopened markers; placement, spawning and catch requirements do not change.
- Panel counter: Full Clear shows **X/3 captured** from confirmed shared catches only. Sparkling shows **X/3 checked** from retained qualified UUID checks and safely reconciled captures; Unique shows that checked counter only if anyone needs Sparkling Gemzie, with the species row removed after the first catch even if this independent checking is unfinished. Never use a bare ratio. See [panel rules](TRACKING.md#missingcheck-panel-by-mode).
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Cavern floor pickups can supply Purple, Lime and Orange Gems; starting grants can also supply them. Keep the two sources separate.
- Picking up or holding the gems proves resources available, not that Gemzie spawned or was caught.
- Placer-only: `You placed the <Gem color> Gem on its podium!`. Global spawning: `A rumbling sound can be heard, and the door at the back of the chamber opens...`. This exact server message starts the five-second check timer; duplicate messages cannot restart it, and player quotations cannot start it. See [exact resource events](MESSAGES.md#feeder-and-resource-events).
- After five seconds, supply 3/3 checked without sightings or captures. This proof lasts for the run, including Cavern reentry, and applies to Sparkling checking in both Sparkling and Unique modes. Retained qualified UUID checks reconciled with shared captures remain an alternative.
- If Gemzie is Cavern's last pending Sparkling check, the chamber timer can finish Cavern and permit `cd` once five seconds and all other active checks pass.
- The timer supplies no catches. Full Clear retains its capture/absence conditions; Unique Gemzie advances on its first shared capture. A party `cd` still follows the existing remote-report rules.

## Last updated

2026-10-17 - Removed detected-Sparkling sightings as a separate completion requirement; normal species rules remain.


2026-10-18 - Added the five-second chamber-opening Sparkling check, shared with relevant Unique checking and retained for the run.

2026-10-17 - Distinguished local Full Clear missing rows from progress completion.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-16 - Aligned Unique party-report checking with real per-species captures and limited Cavern source contradictions.

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

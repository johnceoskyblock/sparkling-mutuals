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
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected uncaptured Sparklings still block completion. |
| Full Clear | Its fixed capture total completes progress before a report. The missing row also requires local empty-scan proof (Gemzie inside its zone; Scrappy/Troodon/Gazer in their biome), retained after leaving the radius. Positive remaining population reopens local completion until caught. See the [panel reference](../MISSING_PANEL.md). |
| Sparkling | Spawn message starts a pending check. Retain distinct qualified Gemzie UUID checks for the run and reconcile shared catches to reach three; the message alone is insufficient. |

## Specific deductions

- Local Full Clear capture/source and empty-scan proof advances progress and removes the missing row without a report. Remote progress without local proof uses report rules.

- Cavern highlights may select predicted Lime/Purple/Orange Gem drops via the inspected [floor-drop predictor](FLOOR_DROPS.md). This selects unopened markers; placement, spawning and catch requirements do not change.
- Panel counter: Full Clear shows **X/3 captured** from confirmed shared catches only. Sparkling shows **X/3 checked** from retained qualified UUID checks and safely reconciled captures; Unique shows that checked counter only if anyone needs Sparkling Gemzie, with the species row removed after the first catch even if this independent checking is unfinished. Never use a bare ratio. See [panel rules](TRACKING.md#missingcheck-panel-by-mode).
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Cavern floor pickups can supply Purple, Lime and Orange Gems; starting grants can also supply them. Keep the two sources separate.
- Picking up or holding the gems proves resources available, not that Gemzie spawned or was caught.
- Placer-only: `You placed the <Gem color> Gem on its podium!`. Global spawning: `A rumbling sound can be heard, and the door at the back of the chamber opens...`. Allow time for the entities to appear; complete from evidence, not an arbitrary delay. See [exact resource events](MESSAGES.md#feeder-and-resource-events).
- Require three retained qualified Gemzie checks reconciled with shared captures for Sparkling mode. With no catches, all three distinct UUIDs must have been seen inside the scan radius, but they need not remain rendered simultaneously. Record the capture baseline at first sighting to avoid double counting a later catch. The spawn message adds no check; Unique progress still needs only its first shared catch.
- If Gemzie is Cavern's last pending Sparkling check, the spawn message alone must not finish Cavern or send `cd`. Wait for the three-Gemzie evidence and all other active checks.
- A party `cd` can mark Sparkling/Full Clear progress complete while outside Cavern. On entry, revalidate retained qualified checks and sources; reopen Gemzie/Cavern checks otherwise. Full Clear additionally requires its capture/absence conditions; Unique Gemzie advances immediately on its own first shared capture; no cd or local three-spawn scan is required.

## Last updated

2026-10-17 - Aligned Gemzie with retained qualified UUID checks; simultaneous rendering is not required.

2026-10-17 - Distinguished local Full Clear missing rows from progress completion.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-16 - Aligned Unique party-report checking with real per-species captures and limited Cavern source contradictions.

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

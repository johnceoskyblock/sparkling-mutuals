# Hideyho

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Haunted. **Recorded Full Clear baseline:** 1. Baseline counts are carried from earlier notes, not newly verified server guarantees.

## Evidence

- **Rare: can escape a standard capsule; Masterful catches are guaranteed once hit.** An escaped critter may return with a replacement UUID; that alone does not prove an additional Hideyho spawn.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Haunted; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected Sparkling entities do not add a separate capture requirement. |
| Full Clear | Local capture/source proof and a qualified empty scan advance progress and remove the missing row without a report; retain that proof after leaving. Other biomes without local proof use party report plus minimum captures, with existing fixed-count exceptions. Positive remaining critters reopen local completion. See the [per-critter panel reference](../MISSING_PANEL.md). |
| Sparkling | Record distinct UUIDs seen inside this species' qualifying scan area and retain their checked progress for the run, including after unloading and biome reentry. Reconcile shared captures without counting a later catch of a seen UUID twice; source requirements remain separate. |

## Specific deductions

- Local Full Clear checks advance progress and remove the missing row without a report. Remote progress without local proof retains its report gate and existing fixed-count exceptions.

- **Inside/outside announcement is Unique/Full Clear and Haunted-only:** process/arm the hiding-location watch and emit its title/chat message only while the local player is actually in Haunted. Do not arm or announce in Sparkling mode. Switching to Sparkling cancels pending state; capture and Sparkling check tracking continue normally. Safari membership alone is insufficient. Leaving Haunted cancels the pending watch so stale dialogue cannot announce later from another biome. Capture/loot-share tracking remains independent and can still record events received elsewhere.
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Track offer/acceptance, hiding, found dialogue, observed UUID, and the successful found/capture reward as distinct events.
- Hideyho dialogue proves quest state; a hiding location proves where that UUID was seen. Neither alone increments catches.
- A personal found/capture confirmation counts for the player; an attributed loot-share confirmation counts for the named party catcher.
- Exact rewards use `You found the Hideyho` / `finding the Hideyho`; loot-share catcher names may include an optional Chat Heads prefix. See [reward messages](MESSAGES.md#captures-loot-shares-and-rewards).

## Last updated

2026-10-17 - Removed detected-Sparkling sightings as a separate completion requirement; normal species rules remain.


2026-10-17 - Local Full Clear completion also advances progress; other biomes without local proof retain report requirements.

2026-10-17 - Separated local Full Clear missing-panel proof from report-based progress.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

# Wumpa

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Icy. **Recorded Full Clear baseline:** 1. Baseline counts are carried from earlier notes, not newly verified server guarantees.

## Evidence

- **Legendary: can escape a standard capsule; Masterful catches are guaranteed once hit.** An escaped critter may return with a replacement UUID; that alone does not prove an additional Wumpa spawn.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Icy; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected uncaptured Sparklings still block completion. |
| Full Clear | Local capture/source proof and a qualified empty scan advance progress and remove the missing row without a report; retain that proof after leaving. Other biomes without local proof use party report plus minimum captures, with existing fixed-count exceptions. Positive remaining critters reopen local completion. See the [per-critter panel reference](../MISSING_PANEL.md). |
| Sparkling | Require one confirmed shared catch (personal capture or party loot share). Sightings, retained UUIDs, spawn/slain messages and party biome reports cannot mark it checked. Retain the catch for the run. |

## Specific deductions

- Local Full Clear checks advance progress and remove the missing row without a report. Remote progress without local proof retains its report gate and existing fixed-count exceptions.

- Icy highlights may select predicted Icebreaker drops via the [floor-drop predictor](FLOOR_DROPS.md). This does not change any Icy species' capture, source or Sparkling requirements.
- **ESP priority in every mode:** if anyone still needs Sparkling Wumpa, force this species' ordinary ESP until its first shared catch, before other mode rules. After that catch, apply normal mode rules. A Wumpa check does not waive another species' catch. See [Icy priority](TRACKING.md#highest-priority-icy-esp-rule-missing-sparkling-wumpa).
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Global spawning: `You hear the sound of massive footsteps echoing through the Icy Biome... What could it be?`; awoken: `The Wumpa has awoken.` Neither is a capture.
- Other Icy catches remain recorded if they happened before this player visited Icy. A Wumpa catch does not itself prove those species were caught.
- Global post-capture/slain: `The cave opens up again...`. Keep this phase separate from capture/loot-share attribution; `The cave is collapsing...` is not the supplied slain message. See [exact events](MESSAGES.md#boss-events).

## Last updated

2026-10-17 - Local Full Clear completion also advances progress; other biomes without local proof retain report requirements.

2026-10-17 - Separated local Full Clear missing-panel proof from report-based progress.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-15 - Made this boss a capture-required exception to Sparkling checking; sightings and completion messages cannot bypass a confirmed catch.

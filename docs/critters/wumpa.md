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
| Unique | One shared catch; if anyone still needs this sparkling variant, also satisfy the species' Sparkling evidence. The missing-species row may disappear after the catch. |
| Full Clear | At least 1 confirmed catches, all verified additional spawns captured, no known live remainder, and all relevant opportunities resolved. |
| Sparkling | Record distinct UUIDs seen inside this species' qualifying scan area and retain their checked progress for the run, including after unloading and biome reentry. Reconcile shared captures without counting a later catch of a seen UUID twice; source requirements remain separate. |

## Specific deductions

- Icy highlights may select predicted Icebreaker drops via the [floor-drop predictor](FLOOR_DROPS.md). This does not change any Icy species' capture, source or Sparkling requirements.
- **ESP priority in every mode:** if anyone still needs Sparkling Wumpa, force this species' ordinary ESP until its first shared catch, before other mode rules. After that catch, apply normal mode rules. A Wumpa check does not waive another species' catch. See [Icy priority](TRACKING.md#highest-priority-icy-esp-rule-missing-sparkling-wumpa).
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Global spawning: `You hear the sound of massive footsteps echoing through the Icy Biome... What could it be?`; awoken: `The Wumpa has awoken.` Neither is a capture.
- Other Icy catches remain recorded if they happened before this player visited Icy. A Wumpa catch does not itself prove those species were caught.
- Global post-capture/slain: `The cave opens up again...`. Keep this phase separate from capture/loot-share attribution; `The cave is collapsing...` is not the supplied slain message. See [exact events](MESSAGES.md#boss-events).

## Last updated

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

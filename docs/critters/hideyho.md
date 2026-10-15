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
| Unique | One shared catch; if anyone still needs this sparkling variant, also satisfy the species' Sparkling evidence. The missing-species row may disappear after the catch. |
| Full Clear | At least 1 confirmed catches, all verified additional spawns captured, no known live remainder, and all relevant opportunities resolved. |
| Sparkling | Record distinct UUIDs seen inside this species' qualifying scan area and retain their checked progress for the run, including after unloading and biome reentry. Reconcile shared captures without counting a later catch of a seen UUID twice; source requirements remain separate. |

## Specific deductions

- **Inside/outside announcement is Unique/Full Clear and Haunted-only:** process/arm the hiding-location watch and emit its title/chat message only while the local player is actually in Haunted. Do not arm or announce in Sparkling mode. Switching to Sparkling cancels pending state; capture and Sparkling check tracking continue normally. Safari membership alone is insufficient. Leaving Haunted cancels the pending watch so stale dialogue cannot announce later from another biome. Capture/loot-share tracking remains independent and can still record events received elsewhere.
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Track offer/acceptance, hiding, found dialogue, observed UUID, and the successful found/capture reward as distinct events.
- Hideyho dialogue proves quest state; a hiding location proves where that UUID was seen. Neither alone increments catches.
- A personal found/capture confirmation counts for the player; an attributed loot-share confirmation counts for the named party catcher.
- Exact rewards use `You found the Hideyho` / `finding the Hideyho`; loot-share catcher names may include an optional Chat Heads prefix. See [reward messages](MESSAGES.md#captures-loot-shares-and-rewards).

## Last updated

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

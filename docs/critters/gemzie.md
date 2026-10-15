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
| Unique | One confirmed own capture or party loot share. If anyone lacks Sparkling Gemzie, also require its three-Gemzie spawn/check evidence below. If everyone has it, the one catch alone completes Unique. |
| Full Clear | At least 3 confirmed catches, all verified additional spawns captured, no known live remainder, and all relevant opportunities resolved. |
- Sparkling requires three retained qualified UUID checks with safely reconciled shared captures. They need not render simultaneously. A spawn message alone is insufficient; Full Clear retains its separate capture/absence rules.

## Specific deductions

- Cavern highlights may select predicted Lime/Purple/Orange Gem drops via the inspected [floor-drop predictor](FLOOR_DROPS.md). This selects unopened markers; placement, spawning and catch requirements do not change.
- Panel counter: Full Clear shows **X/3 captured** from confirmed shared catches only. Sparkling shows **X/3 checked** from retained qualified UUID checks and safely reconciled captures; Unique shows that checked counter only if anyone needs Sparkling Gemzie, retaining unfinished checking after the first catch. Never use a bare ratio. See [panel rules](TRACKING.md#missingcheck-panel-by-mode).
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Cavern floor pickups can supply Purple, Lime and Orange Gems; starting grants can also supply them. Keep the two sources separate.
- Picking up or holding the gems proves resources available, not that Gemzie spawned or was caught.
- Placer-only: `You placed the <Gem color> Gem on its podium!`. Global spawning: `A rumbling sound can be heard, and the door at the back of the chamber opens...`. Allow time for the entities to appear; complete from evidence, not an arbitrary delay. See [exact resource events](MESSAGES.md#feeder-and-resource-events).
- Retain distinct Gemzie UUIDs from qualifying scans and reconcile shared catches without double counting a seen critter later caught. Require three checked spawns for Sparkling (and Unique's added check when needed); they need not render simultaneously. Neither the spawn message nor repeated sightings add another spawn.
- If Gemzie is Cavern's last pending Sparkling check, the spawn message alone must not finish Cavern or send `cd`. Wait for the three-Gemzie evidence and all other active checks.
- A party `cd` can mark Sparkling/Full Clear progress complete while outside Cavern. On entry, verify three retained qualifying checks; reopen Gemzie/Cavern checks otherwise. Full Clear additionally requires its capture/absence conditions; Unique still ignores party completion messages.

## Last updated

2026-10-15 - Retained qualified distinct UUID checks for every critter throughout Unique/Sparkling runs, with capture reconciliation and independent source gates. Full Clear remains capture-based.

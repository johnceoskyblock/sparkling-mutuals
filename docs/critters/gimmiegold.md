# Gimmiegold

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Haunted. **Rarity:** Rare; standard capsules can fail and the returning model can use a replacement UUID. **Recorded Full Clear fallback:** 3 catches while a larger source/population target is not known. This fallback is not the Sparkling coin-floor quota.

## Sparkling coin-floor check

Floor drops are established at the start of the run. Register each known **Shining Coin floor-drop slot once**, retaining its numeric group identity through collection, unloading and biome changes. Let **F** be that initial coin-slot total, **E** known additional starting/party coins, **C** confirmed shared Gimmiegold captures, and **U** distinct currently rendered uncaptured Gimmiegolds.

The local Sparkling check requires all three:

1. Every registered Shining Coin floor drop has been collected.
2. A sampled inventory contains **zero Shining Coins**; unknown inventory is not empty.
3. In the existing Haunted source-check radius (15 horizontal blocks around X/Z **-4/-64**), **C + U >= F + E**.

Do not replace F with a fixed three-coin shortcut. Three local pickups/coin-use messages cannot excuse a registered remaining coin drop. Do not count all three STRING display members as three coin slots. Complete Soothing Incense groups and arbitrary decorative/model triples are not coins. A registered unloaded coin slot remains outstanding until a matched pickup or a local observation establishes its collection; unloading alone does not lower F. Newly identified coin slots increase F and reopen validation.

Population is shared captures plus **currently rendered** uncaptured models. Repeated scans of different historical UUIDs do not supply missing current population. Escape replacement history cannot inflate the target or discharge a newly reported extra coin. A qualifying completed check persists during ordinary travel/unloading; entering Haunted again requires a fresh local check. New slots, positive remaining markers, held coins and new contribution quantities revoke the validated result.

## Additional party coins

Accept the existing compact unused-item message, including mixed batches: `[SM] Unused floor drops: 2x Shining Coin (missing Sparkling Gimmiegold); 1x Yogi Berry (missing Sparkling Macaw)`. Continue accepting both the old compact `[SM] Use:` message and verbose unused-item notice. Displayed missing species are explanatory; the receiver retains its own party discovery requirements. Each sender's reported Shining Coin quantity adds to E once; repeated notices/echoes are not additional coins, and larger quantities update that sender's contribution. Bird food affects only its own Forest ledger.

Known local starting coins also add to E. The local player's own notice overlaps those coins: use the larger known quantity for that player rather than adding both. Keep extra demand for the remainder of the run after consumption. A new run resets slots, contributions and validation. Source notices never manufacture captures.

## Party completion reports and other modes

- **Sparkling, outside Haunted:** a received party `hd` can override ordinary coin-source, inventory and population checking. No local coin history is required for that remote trust. Entering Haunted retires inherited checking and revalidates local sources. A report received inside Haunted cannot waive them. Detected Sparkling entities do not override normal completion.
- **Unique:** require one actual shared catch, with the shared confirmed-Sparkling exception. A report alone cannot invent that catch. Ordinary local coin checking does not separately gate Unique.
- **Full Clear:** require actual captures of the minimum population, raised by known coin slots/contributions and positive additional critters. A remote report may supply ordinary checking trust but never these captures. Local clear proof still needs resolved sources, no held coins and no remaining critters. Personal PBs retain personal capture evidence. Existing conservative Full Clear source recovery is separate from the new Sparkling slot rule.

The confirmed-Sparkling catch override still completes Unique/Sparkling immediately. Pending detected identities add no separate capture requirement; normal catches, reports and source/check evidence retain their usual meaning.

## Runtime evidence and diagnostics

The source registry runs with the entity scan, independently of HUD/ESP settings and the current biome, so initial coin slots are learned before visiting Haunted. Only validated live STRING reward triples establish predicted coin slots; a nearest-group pickup correlation is recorded separately. Collection by another player can be inferred when every marker of a registered slot is gone while its loaded location remains within local observation range. Partial surviving markers keep the slot outstanding. Chunks/entities outside coverage remain unknown.

`/sm debug` includes **Gimmiegold Sparkling check** with `floorMinimum`, `remaining`, `extraCoins`, `held`, `required`, `capturedPlusRendered` and `checked`. Older `sourcePath`, coin-use and retained-UUID fields describe the separate source-recovery ledger used by Full Clear; retained history is not the Sparkling population counter.

In Sparkling mode, coin ESP requires someone needing Sparkling Gimmiegold. All remaining coin slots stay eligible while needed; incense selection/cutoffs are independent. Hiding a marker never collects it.

## Minecraft checks

- Start a run outside Haunted and inspect the logged initial coin-slot count. Collect all but one: checking stays pending despite sufficient Gimmiegolds and empty inventory. Unload the last marker: it must remain pending. Collect it and verify the final qualifying C + U check completes.
- Compare 1 catch + the remaining rendered Gimmiegolds with the full slot count. Unload models one at a time before the initial check passes: historical UUID totals cannot substitute for the required current population.
- Receive repeated one-/two-coin unused-item notices from multiple players. Verify the required total increases once per sender/quantity, own starting coins are not counted twice, and enough additional current-or-captured Gimmiegolds are needed.
- Receive `hd` outside Haunted: Sparkling checking can complete, Unique still needs a shared catch, and Full Clear still needs its capture minimum. Enter Haunted and confirm local revalidation. Detected Sparkling identities must not override otherwise valid completion.

## Last updated

2026-10-17 - Removed detected-Sparkling sightings as a separate completion requirement; normal species rules remain.


2026-10-17 - Clarified unused floor-drop notices with resource quantities and relevant missing Sparkling discoveries; retained legacy parsing.

2026-10-17 - Replaced Sparkling's fixed-three/retained-UUID completion path with the initial coin-slot total, all slots collected, empty inventory and current captured-plus-rendered population. Added deduplicated party coin contributions and outside-Haunted report trust without waiving capture requirements.

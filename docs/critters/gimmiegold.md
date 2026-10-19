# Gimmiegold

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Haunted. **Rarity:** Rare; standard capsules can fail and the returning model can use a replacement UUID. **Recorded Full Clear fallback:** 3 catches while a larger source/population target is not known. This fallback is not the Sparkling coin-floor quota.

## Sparkling coin-floor check

Floor drops are established at the start of the run. Register each known **Shining Coin floor-drop slot once**, retaining its numeric group identity through collection, unloading and biome changes. Let **F** be that initial coin-slot total, **S** known absolute party starting coins, **C** confirmed shared Gimmiegold captures, and **U** distinct currently rendered uncaptured Gimmiegolds.

The local Sparkling check requires all three:

1. Initial source/report coverage is known and no applicable coin source remains. Marker disappearance alone is not a pickup; fresh sufficient spawning evidence can account for the retained opportunities without adding pickup events.
2. A sampled inventory contains **zero Shining Coins**; unknown inventory is not empty.
3. In the existing Haunted source-check radius (15 horizontal blocks around X/Z **-4/-64**), **C + U >= F + S**.

Do not replace F with a fixed three-coin shortcut. Three local pickups/coin-use messages cannot excuse a registered remaining coin drop. Do not count all three STRING display members as three coin slots. Complete Soothing Incense groups and arbitrary decorative/model triples are not coins. A registered unloaded coin slot remains outstanding until a matched pickup or fresh applicable source/population evidence accounts for its opportunity; unloading alone does not lower F. Newly identified coin slots increase F and reopen validation.

Population is shared captures plus **currently rendered** uncaptured models. Repeated scans of different historical UUIDs do not supply missing current population. Escape replacement history cannot inflate the target or discharge a newly reported extra coin. A qualifying completed check persists during ordinary travel/unloading; entering Haunted again requires a fresh local check. New slots, positive remaining markers, held coins and new contribution quantities revoke the validated result.

## Additional party coins

Use the early absolute `[SM] Starting items:` report as the primary contribution source. It reports original coin/food quantities, including duplicate grants and resources used before dispatch, with current-run marker context and verified roster UUID attribution. Explicit none differs from a missing report. See [the protocol, trigger and limits](TRACKING.md#early-party-starting-resource-synchronization).

Known local starting coins overlap the local player's own report. Repeat messages and legacy `[SM] Use:`, verbose and combined unused-item notices merge sender/type maxima rather than add pickups or another contribution. Keep demand after consumption. Larger amounts/new slots reopen applicable validation. No title creates demand. Unknown old/non-mod/late-join reports cannot be assumed zero.

A fresh qualified local F+S proof also resolves overlapping legacy outstanding-coin bookkeeping against that same verified demand. The legacy three-catch fallback cannot add a second Sparkling quota. Unknown source/report coverage, held coins and positively remaining sources cannot discharge it.

## Party completion reports and other modes

- **Sparkling, outside Haunted:** a verified, current-demand `hd` supplies remote spawning proof without requiring local Haunted renders. It does not waive known held coins, remaining sources or outstanding resource blockers. A report predating new starting contributions/slots must be updated. Entering Haunted retires inherited checking and revalidates local sources. A report received inside Haunted cannot waive them. Detected Sparkling entities do not override normal completion.
- **Unique:** require one actual shared catch, with the shared confirmed-Sparkling exception. A report alone cannot invent that catch. Ordinary local coin checking does not separately gate Unique.
- **Full Clear:** require actual captures of the minimum population, raised by known coin slots/contributions and positive additional critters. A remote report may supply ordinary checking trust but never these captures. Local clear proof still needs resolved sources, no held coins and no remaining critters. Personal PBs retain personal capture evidence. Existing conservative Full Clear source recovery is separate from the new Sparkling slot rule.

The confirmed-Sparkling catch override still completes Unique/Sparkling immediately. Pending detected identities add no separate capture requirement; normal catches, reports and source/check evidence retain their usual meaning.

## Runtime evidence and diagnostics

The source registry runs with the entity scan, independently of HUD/ESP settings and the current biome, so initial coin slots are learned before visiting Haunted. Only validated live STRING reward triples establish predicted coin slots; a nearest-group pickup correlation is recorded separately. Marker disappearance alone never records a pickup by another player. Complete loaded coverage with no remaining sources and sufficient fresh population can account for their spawning opportunities. Partial surviving markers keep the slot outstanding. Chunks/entities outside coverage remain unknown.

`/sm debug` includes **Gimmiegold Sparkling check** with `floorMinimum`, `remaining`, `extraCoins`, `held`, `required`, `capturedPlusRendered` and `checked`. Older `sourcePath`, coin-use and retained-UUID fields describe the separate source-recovery ledger used by Full Clear; retained history is not the Sparkling population counter.

In Sparkling mode, coin ESP requires someone needing Sparkling Gimmiegold. All remaining coin slots stay eligible while needed; incense selection/cutoffs are independent. Hiding a marker never collects it.

Initial catalog completeness requires the full supported STRING phase sequence with no missing/partial group or ID gap, including phase boundaries. F is the observed Haunted coin-group count after the five incense groups, never a fixed three. This supports the existing order classifier; it is not a server reward field. Partial/late loading remains unknown and live classifier fidelity still needs gameplay verification.

## Minecraft checks

- Start a run outside Haunted and inspect the logged initial coin-slot count. Collect all but one: checking stays pending despite sufficient Gimmiegolds and empty inventory. Unload the last marker: it must remain pending. Collect it and verify the final qualifying C + U check completes.
- Compare 1 catch + the remaining rendered Gimmiegolds with the full slot count. Unload models one at a time before the initial check passes: historical UUID totals cannot substitute for the required current population.
- Receive repeated one-/two-coin unused-item notices from multiple players. Verify the required total increases once per sender/quantity, own starting coins are not counted twice, and enough additional current-or-captured Gimmiegolds are needed.
- Receive `hd` outside Haunted: Sparkling checking can complete, Unique still needs a shared catch, and Full Clear still needs its capture minimum. Enter Haunted and confirm local revalidation. Detected Sparkling identities must not override otherwise valid completion.

## Last updated

2026-10-19 (America/New_York) — Early absolute starting-resource reports at HOTSPOT+2s, explicit unknown coverage and local-only later reminders (issue68).

2026-10-17 - Removed detected-Sparkling sightings as a separate completion requirement; normal species rules remain.


2026-10-17 - Clarified unused floor-drop notices with resource quantities and relevant missing Sparkling discoveries; retained legacy parsing.

2026-10-17 - Replaced Sparkling's fixed-three/retained-UUID completion path with the initial coin-slot total, known source/report coverage, no remaining markers, empty inventory and current captured-plus-rendered population. Added deduplicated party coin contributions and outside-Haunted report trust without waiving capture requirements.

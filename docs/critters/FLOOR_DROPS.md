# Typed floor-drop highlights

[Tracking rules](TRACKING.md) · [Critters](README.md)

**Order-based predictions are now implemented in the mod; in-game validation is still required.** Static inspection of the supplied Shiny Hunter Private 2.25 JAR found an entity-order predictor for unopened drops. This is evidence of that mod's algorithm, not a live verification of every Hypixel drop or a direct server field containing its reward.

## Requested behavior

| Biome | Highlight these predicted contents |
| --- | --- |
| Cavern | Lime Gem, Purple Gem, Orange Gem |
| Forest | Wriggleworm, Yogi Berry, Bag of Seeds |
| Haunted | Soothing Incense, Shining Coin |
| Icy | Icebreaker |

Apply item selection alongside the existing mode/discovery/resource-need rules. **All critter spawn, capture, check and completion requirements remain unchanged.** A predicted unopened drop is not a pickup, inventory item, consumed resource or spawned critter. Starting grants remain the separate inventory/message-based evidence already documented.

### Haunted item-specific need rules

- **Shining Coins:** Full Clear keeps all remaining coin drops eligible regardless of discovery ownership or coins already picked up, held, spent, or Gimmiegolds captured. Unique and Sparkling keep all remaining coins eligible whenever anyone needs Sparkling Gimmiegold, and hide them when nobody needs that discovery. No first-coin cutoff applies. The minimum Gimmiegold count does not excuse remaining coins. Coin use/spawn checks remain required afterward, while ordinary Unique progress remains based on its first shared catch.
- **Soothing Incense in Sparkling mode:** show typed Incense ESP only while someone needs Sparkling Doomspiral, fewer than **four incense floor pickups** are confirmed, and the inventory has never held **four Soothing Incense at once** in this run. Either cutoff hides Incense ESP for the rest of the run, including after consumption and biome reentry. Starting items count toward the inventory cutoff (including two starting incense), but not the floor-pickup counter. Hide it immediately if everyone owns the discovery.
- Evaluate coins and incense independently: Gimmiegold need cannot keep incense highlighted when Doomspiral is shared, and four incense pickups cannot hide required coin drops. Uncertain drop typing is not proof that all needed drops were collected. Every mode hides incense after four confirmed incense floor pickups **or after holding four incense simultaneously at any point in the run**, regardless of Gimmiegold needs. Before that cutoff, retain its existing mode-specific eligibility.

In Unique and Sparkling modes, hide all Shining Coin floor-drop ESP when nobody in the party needs Sparkling Gimmiegold, even before collecting any coins. Coin and incense visibility remain independent; these display cutoffs do not establish critter completion.

## Incense selection in every mode

Whenever incense ESP is needed, all **five** known Haunted Soothing Incense floor-drop spawns are eligible in Unique, Full Clear and Sparkling. Do not rank by distance to (4, 47, -27) or reduce the selection for starting incense. Collected drops no longer render; unloaded groups retain the existing normal loaded-entity behavior.

Hide all remaining incense ESP after **four confirmed incense floor pickups**, or after holding four incense simultaneously at any point. Consumption, biome reentry and mode changes do not reset these cutoffs. Existing mode/discovery needs, colors, coin selection and completion evidence stay independent.

## Colors

In **ESP → Floor-drop colors**, independently customize every named quest-drop type: the three gems, three bird foods, Soothing Incense, Shining Coin and Icebreaker. Use that type's chosen color for its outline, fill and name. Older shared floor colors become defaults for each type, and per-item colors persist across restarts. **Customization → All floor-drop colors** applies one color to all types. These choices never bypass drop eligibility; unknown/generic predictions still receive no ESP.

## What the inspected JAR does

1. During an active run, scan loaded, nonremoved `ItemDisplay` entities every five ticks and sort them by **numeric entity ID**, not UUID or distance.
2. Form nonoverlapping triples with IDs `k`, `k+1`, `k+2` and pairwise distances at most **1.5 blocks**. Record their mean X/Y/Z, rounded to two decimals, and item descriptions (item registry ID plus formatting-stripped hover name).
3. Prediction uses only triples whose three descriptions are identical. Sort those candidates by their first ID `k`.
4. Walk biome phases in this fixed order: **Cavern → Forest → Haunted → Icy**. On advancing a phase, reset its ID anchor. The first eligible recorded triple in that phase supplies anchor **a**.
5. Compute zero-based position **i = integer division of (k − a) by 3**. Gaps in IDs matter: this is not just the candidate's rank in the visible list. Apply the table below.
6. Retain seen groups through the run, even after a pickup/unload; repredict when new groups are discovered. Expose a ground marker only while its group is present, predicted, and not already matched to a pickup. A new run clears live groups.

### Encoded prediction sequence

| Phase | Zero-based i | Predicted item |
| --- | --- | --- |
| Cavern | 0–5 | Lime, Purple, Orange, Lime, Purple, Orange Gem |
| Forest | 0–2 | Wriggleworm |
| Forest | 3–5 | Yogi Berry |
| Forest | 6–8 | Bag of Seeds |
| Haunted | 0–4 | Soothing Incense |
| Haunted | 5 onward within that phase | Shining Coin; no upper coin count is encoded in this branch |
| Icy | 0–4 | Icebreaker |

Beyond a supported prefix (Cavern i ≥ 6, Forest i ≥ 9, Icy i ≥ 5), or when phase matching fails, the predictor labels the candidate **XP / Essence / Shard** and enters a generic tail for the rest of that prediction pass. It does not identify the exact generic reward. A later reprediction can change labels if earlier groups become known.

These constants describe the inspected predictor. **Five predicted incense/icebreaker drops do not change the existing four-incense/other resource requirements**, and six predicted gem drops do not mean six Gemzie captures. Do not promote prediction layout constants into new critter quotas.

### Position and classification details

The JAR derives biome from its centroid's X/Z within `x ∈ [-200,100]`, `z ∈ [-150,150]`: east of/equal to x = -50 is Forest for z ≥ 0 and Haunted otherwise; west is Cavern for z ≥ 0 and Icy otherwise. Outside that rectangle it returns unknown. Coordinates locate the actual observed group; the JAR has no fixed coordinate-to-reward map in this predictor.

`FloorDropType` maps predicted labels to display icons: dye colors for gems, string for Wriggleworm, sweet berries, wheat seeds, blaze powder, gold nugget and iron pickaxe for the corresponding quest items. These are **rendered representations of a prediction**, not proof those items were exposed as the unopened reward. The view can filter/color/label per type and optionally draw fake item icons; those icons must never enter real inventory/entity accounting.

## Pickup confirmation and limits

- The JAR parses `FLOOR DROP! You found ... on the ground!`, including quantities before/after the item name, then tries to match the reward to a group: recent click within three seconds and three blocks, otherwise nearest within five blocks. Recently disappeared groups have three seconds of matching grace. This correlation is heuristic, not a server-supplied group-to-reward identifier.
- It records predicted item, actual reward and whether they match in `shinyhunter-floor-drops.json`; it does not simply treat a prediction as a confirmed pickup. Ground markers omit groups with a matched reward or marked absent.
- The anchor is the first **recorded eligible** group in each phase. Joining late, missing early chunks/groups, or a teammate taking drops before observation can anchor it incorrectly. Missing later groups can be represented by ID gaps; missing the true first group is a different problem. Do not silently rebase an established anchor to the earliest remaining live drop.
- Sorting/spawn-order assumptions, phase order, triplet spacing and item-description equality require validation in Minecraft. Numeric IDs are run/world scoped; do not carry them across servers. Unknown/unloaded groups and grouping collisions must remain distinguishable from verified generic rewards.
- A hidden/unselected marker is not an absent resource. The initial coin-slot registry is maintained separately from visual selection and counts one reward group, not its three display members. Prediction uncertainty must not close coin/food/gem work or mark a biome complete. Retain raw presence tracking independently of the typed visual filter.

## Gimmiegold Sparkling coin-slot registry

At run start register each validated Shining Coin **STRING reward triple** once by group ID, independently of highlight settings and the player's biome. Arbitrary identical model/decorative triples and the three members of one reward group must not inflate this count. Retain the registered slots after collection and unloading.

Local Sparkling checking requires all coin slots collected, sampled inventory containing zero coins, and **shared captures + currently rendered uncaptured Gimmiegolds >= initial coin slots + known additional coins**. Three actual pickups cannot waive a remaining registered coin source. Own starting coins and deduplicated party unused-coin notices add opportunities without becoming floor pickups. An unloaded slot remains outstanding until pickup/observable collection evidence resolves it. A party hd can override ordinary checking outside Haunted; entering Haunted revalidates locally. Unique still needs its catch and Full Clear its actual capture minimum. See [Gimmiegold](gimmiegold.md).

Full Clear keeps its separate conservative coin-use/source recovery; its old confirmed-three-source shortcut is not the local Sparkling completion rule.

## Live marker evidence

Completion presence tracking includes only active, visible STRING item displays; a surviving invisible/empty/decorative display ID is not a floor reward. Include partial/unknown live markers as blockers. Resolve a complete triple's members by IDs rather than requiring each member's floored position to equal the centroid's floored position. A triple can cross block boundaries. Predictions remain visual/type hints; actual pickups, coin possession/use and spawned/captured populations are recorded separately.

## Implementation and validation

- Replace/extend the current untyped three-string marker detection with a per-run group/order classifier; keep collection independent of highlight switches and biome visibility. Preserve early anchors/seen groups through biome changes and pickups.
- Keep **predicted type**, **actual pickup**, **live presence** and **confidence/coverage** separate. Only sufficiently supported classifications receive the selected quest-item highlights; uncertain candidates must not masquerade as confirmed contents or completed source coverage.
- Validate predictions against actual pickup messages in every biome, especially incomplete loading, late entry, teammate pickups, ID gaps, unrelated displays and new-run resets. Check boundary positions, partial triples, repeated scans, group disappearance and prediction revision.
- Confirm that selective highlighting changes no mode completion, starting-resource reminders, inventory counts, personal/shared captures or extra-resource reopening rules. Test in IntelliJ/DevAuth before committing/pushing a later code change.

## Inspection provenance

- Supplied file: `ShinyHunterPrivate v2.25 26.1.2.jar` (1,004,176 bytes). Metadata identifies `shinyhunter`, version `2.25`, name `Shiny Hunter Private`, license `MIT`.
- SHA-256: `61678BC859A7E84447051155A395A6D9B65680F7162A6788AD12B233C30323B3`.
- Inspected with `jar tf` and JDK `javap -p -c -constants` / `-v`; the JAR was **not executed**. No JAR or disassembled source was added to this repository.
- Primary methods: `com.shinyhunter.FloorDropLog.scan`, `predict`, `repredict`, `onGround`, `handle`, `nearest`, `biomeAt`; `FloorDropType`; `FloorDropView.onClientTick`. `FloorDropHighlighter` also provides generic clustering; the reward prediction itself is in `FloorDropLog.predict`.
- Auditable bytecode landmarks in `predict`: eligibility/sort offsets 34–95; phase walk 157–205; anchor/index 215–240; mapping 243–379; generic fallback 381–410. `PHASE_BIOMES` and gem order are in the static initializer. These are reproducibility references, not runtime test results.

Current implementation: `FloorDropPredictor.kt` retains numeric-ID group history and predicts labels; `SafariEsp.kt` renders selected markers and names; `SafariFloorDrops.kt` applies resource/mode gates. Raw presence is tracked independently of selected highlights. Nearby pickup correlation is logged as a heuristic, with predicted and actual rewards kept distinct.

### Active highlight cutoffs (2026-10-15)

- Forest: only the nine predicted Wriggleworm/Yogi Berry/Bag of Seeds markers, subject to mode and discovery need.
- Cavern: only the two Lime/Purple/Orange sets. Acquiring one gem of a color hides the other predicted marker of that color, including after the gem is placed.
- Icy: all **five** predicted Icebreaker slots, in Full Clear while one is still needed. Acquiring one hides all remaining Icebreaker highlights. This matches the inspected JAR's five-slot prefix.
- Haunted coin-source completion ignores complete currently loaded typed Incense groups, while raw unknown/partial/generic and coin markers continue to block it. Marker selection and the four-incense cutoff never remove a coin-source blocker; resource pickups/use and captures remain separately confirmed.
- Haunted (**implemented; Minecraft verification pending**): in Sparkling mode, coin ESP requires someone missing Gimmiegold; incense ESP requires someone missing Doomspiral and stops after four confirmed floor pickups or four incense held simultaneously at any point in the run, including starting incense. Both cutoffs apply in every mode; all five incense spawns are eligible while needed.
- Generic/unknown predictions receive no quest-item ESP. Hidden markers remain independent raw presence evidence and cannot complete a critter/source check.

## Last updated

2026-10-17 - Full Clear always retains remaining coin ESP; Unique/Sparkling gate only on party Gimmiegold discovery need, without pickup/holding cutoffs.

2026-10-17 - Removed incense distance/starting-item slot limits; show all five spawns while needed and hide remaining ESP at four pickups or four held.

2026-10-17 - Documented run-start validated coin slots as Gimmiegold Sparkling demand, independent of visibility and decorative triples.

2026-10-15 - Corrected all-mode incense selection to four minus starting incense: 0/1/2 starting items show 4/3/2 nearest locations.

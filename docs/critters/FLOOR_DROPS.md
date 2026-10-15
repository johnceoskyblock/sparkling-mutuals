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

- **Shining Coins:** if anyone needs Sparkling Gimmiegold, require every Shining Coin floor drop collected and keep its typed highlights while drops remain. The minimum Gimmiegold count does not excuse remaining coins. Coin use/spawn checks remain required afterward, including Unique's added Sparkling work.
- **Soothing Incense in Sparkling mode:** show typed Incense ESP only while someone needs Sparkling Doomspiral and fewer than **four incense floor pickups** are confirmed. Hide it once four are picked up, or immediately if everyone owns the discovery. Starting items/predictions/spawn signals do not increment this floor-pickup counter.
- Evaluate coins and incense independently: Gimmiegold need cannot keep incense highlighted when Doomspiral is shared, and four incense pickups cannot hide required coin drops. Uncertain drop typing is not proof that all needed drops were collected. Every mode hides incense after four confirmed incense floor pickups, regardless of Gimmiegold needs. Before that cutoff, retain its existing mode-specific eligibility.

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
- A hidden/unselected marker is not an absent resource. Prediction uncertainty must not close coin/food/gem work or mark a biome complete. Retain raw presence tracking independently of the typed visual filter.

## Coin prediction cannot override confirmed collection

The Haunted prediction branch has no upper coin bound. Its candidate count can include non-coin reward groups and is not a verified coin quota. Gimmiegold's confirmed baseline path uses three actual coin floor pickups, confirmed consumption of all acquired/starting coins, a fresh empty inventory and the required checked/captured population. That evidence outranks residual predicted coin/unknown markers. Additional actual resources and party notices still gate completion; when complete confirmed coin history is unavailable, keep the conservative marker-scan recovery rules. See [Gimmiegold evidence](gimmiegold.md#confirmed-coin-evidence-versus-predicted-markers).

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
- Haunted (**implemented; Minecraft verification pending**): missing Gimmiegold requires collecting all coin drops; it does not control Incense ESP. In Sparkling mode, incense is shown only for missing Doomspiral until four incense floor pickups are confirmed, excluding starting inventory from that pickup counter. The same four-floor-pickup cutoff now applies in every mode. This replaces the earlier coupling of incense to Gimmiegold and its starting-inventory cutoff for Sparkling mode.
- Generic/unknown predictions receive no quest-item ESP. Hidden markers remain independent raw presence evidence and cannot complete a critter/source check.

## Last updated

2026-10-15 - Clarified that unbounded Haunted coin predictions cannot veto fully confirmed baseline coin collection and use.

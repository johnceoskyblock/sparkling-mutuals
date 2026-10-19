# Resources and typed floor drops

## Scope

Authoritative reference for starting grants, floor pickups, held resources, unused-resource reminders, drop prediction and resource highlight selection. [Tracking](TRACKING.md) owns evidence retention, [bird completion](../BIRD_COMPLETION.md) owns feeding/capture formulas, and [species pages](README.md) own completion prerequisites. Predicted contents are not confirmed resources.

## Definitions

A **starting grant** is entry-time resource evidence from Head Start or initial inventory. A **floor pickup** is an actual reward message; a **held resource** is sampled inventory. **Resource use** and **confirmed placement** require their own evidence. A **predicted floor drop** is a label inferred from display grouping and entity order.

A marker hidden by selection is not an absent resource. Unknown/partial markers and unobserved coverage remain distinct from a complete known nonresource reward group.

## Implemented rules

### Starting resources

The supplied ticket observations describe zero ticket-specific items for Basic/Economy/Premium and one for First Class, plus one independent Head Start item when present. First Class has no grant message; inventory is authoritative for the total, including two identical items. The client records observed ticket/admission evidence but does not manufacture inventory from ticket type.

The nine recognized quest types are Icebreaker, Yogi Berry, Wriggleworm, Bag of Seeds, Shining Coin, Purple Gem, Lime Gem, Orange Gem and Soothing Incense. The first available inventory sample opens a three-second starting window. Head Start overlaps that inventory; separately recorded pickups are subtracted rather than counted as grants twice. Entry banners, queue retries and hotspot messages add no resources. See [exact entry/resource messages](MESSAGES.md#entry-and-starting-items).

### Prediction algorithm

Every five ticks in Safari, SafariEsp supplies currently loaded display descriptions to FloorDropPredictor. It forms nonoverlapping numeric-ID triples k, k+1, k+2 with pairwise distance at most 1.5 blocks and identical item-ID/stripped-name descriptions. Centroids round to two decimals. Numeric IDs, not UUIDs or distance order, determine prediction.

Retain recorded groups through unload/pickup. Walk recorded groups in numeric order through Cavern → Forest → Haunted → Icy; the first recorded eligible group in each phase supplies anchor a. Index i is integer division of (k − a) by 3, so ID gaps matter.

| Phase | Zero-based index | Prediction |
| --- | --- | --- |
| Cavern | 0–5 | Lime, Purple, Orange, Lime, Purple, Orange Gem |
| Forest | 0–2 / 3–5 / 6–8 | Wriggleworm / Yogi Berry / Bag of Seeds |
| Haunted | 0–4 / 5 onward | Soothing Incense / Shining Coin |
| Icy | 0–4 | Icebreaker |

Unsupported prefixes or phase mismatch enter an XP / Essence / Shard tail for that pass. Later discovery can revise labels. These constants describe the predictor, not server-confirmed quantities or capture quotas. Biome quadrants use X/Z inside x [-200,100], z [-150,150], split at x = -50 and z = 0; there is no fixed coordinate-to-reward table.

### Highlight selection

| Type | Implemented eligibility, in addition to rendering/config gates |
| --- | --- |
| Gems | Cavern Full Clear, needed Gemzie discovery, or uncaught Unique Gemzie; each acquired/starting gem color hides all remaining predicted markers of that color, including after placement |
| Bird food | Configured floor group; Full Clear, or any needed bird discovery/uncaught Unique bird. Only predicted food groups are selected |
| Shining Coin | Every remaining coin in Full Clear; in Unique/Sparkling only when Gimmiegold discovery is needed. No pickup/holding/capture cutoff |
| Soothing Incense | All five predicted slots eligible, with no distance ranking or reduction for starting incense. Hide after four pickup units or a peak inventory of four. Sparkling additionally needs Doomspiral discovery; Unique/Full Clear additionally require Gimmiegold discovery needed or acquired units + known starting quantity (at least peak held inventory) below four |
| Icebreaker | Automatic selection only in Full Clear, until one acquired/starting Icebreaker |
| Generic/unknown | No quest-item ESP |

Coin and incense selection are independent. Incense peak inventory includes starting items and stays recorded after consumption/travel/mode change. A pickup quantity contributes units to the typed cutoff; Forest reporting uses qualified population/source proof rather than personal pickup counts. Cavern/Icy manual overrides last for the current biome visit and affect the group gate; per-type acquisition/need selection still applies. Floor-group controls are not a universal gate for every automatic Cavern/Icy path.

Per-type colors cover nine quest types and apply to outline/fill/name. Older shared color supplies defaults; All floor-drop colors assigns all types. These choices never create resources or override per-type eligibility.

### Presence and coin-slot registration

Completion presence uses active, visible STRING display markers independently of selected ESP. Complete active triples bind members by IDs, including across block boundaries.

Forest excludes complete groups classified as nonfood, including generic rewards; food and unknown/partial groups remain blockers. Haunted coin presence excludes only complete typed incense groups; generic, coin and partial/unknown groups remain blockers. Thus hidden incense does not become a coin-source requirement, and hidden coins do not vanish from evidence.

Gimmiegold's dedicated Sparkling registry records coin groups only after three active STRING members or an actual matched coin pickup validate them. One group is one slot. Retain registered IDs through collection/unloading; a slot resolves from a matched pickup or disappearance while its chunk is loaded and the player is within 80 blocks. Unobservable absence stays unknown. Registry discovery continues through the run; the implementation does not freeze it at a single startup instant. [Tracking](TRACKING.md#source-evidence-and-retention) owns its completion/retention composition.

### Pickup matching

Actual FLOOR DROP text increments resource evidence independently of predicted labels. The current predictor heuristically matches a pickup to the nearest unassigned group seen within three seconds and five blocks; a near-equal distance tie is rejected. A match records actual reward separately and retires visual availability. It is not a server group-to-reward identifier.

Late entry or missing the earliest groups can establish the wrong anchor. Group equality, ID order and phase assumptions need live confirmation. Do not silently claim absent/unloaded data was collected, or label prediction consistency as server verification.

### Early starting food/coin reports

After observing the boat/center (mapped area 0), the first transition to a biome (areas 1–4) starts a two-second delay. Sample inventory on the boat as well as during clearing. With known multi-player party membership and ready party discovery data, send one compact party report containing only still-held starting food/coins whose related Sparkling discovery is missing. Example: `[SM] Starting items: 2x Bag of Seeds; 1x Shining Coin`. If only a bird is needed, send only the food. If neither category is needed, send nothing. Unknown discovery data postpones the decision instead of assuming every discovery is missing. There is no empty/zero announcement.

Each sender/type quantity is a retained maximum, overlapping the local grant/inventory and its own echo. Early reports update the bird/coin contribution totals directly, not the old delayed-notice outstanding ledger. Repeating an early report, or a later legacy notice for that same sender/type, does not add the quantity again. Floor pickups are separate from starting inventory. An unknown area is not boat departure; a new run owns a fresh schedule. A client that never observes the boat cannot infer the departure time.

### Unused starting food/coin reminders

After the first locally completed biome, starting-resource delivery activates after four seconds. It continues checking unsent eligible resources for the run, even if a teammate's report suppressed this client's biome announcement. Repeated polls do not restart the delay.

| Resource | Discovery gate | Destination/title |
| --- | --- | --- |
| Shining Coin | Gimmiegold needed | Haunted / USE YOUR SHINING COIN! |
| Bag of Seeds, Wriggleworm, Yogi Berry | Any bird needed | Forest / USE YOUR BIRD FOOD! |

Before delivery, resample inventory and rebuild quantities/discovery needs. Remind only with ready party discovery data, while outside the resource's destination and still holding the tracked unused starting quantity. Each item is filtered independently. There is no separate configuration toggle. Eligible items are combined; mixed destinations use USE YOUR STARTING ITEMS! This is a **local title/subtitle only**: no end-of-biome unused-item party message or local chat line is sent. Displaying the title records notification state only; it creates no new source obligation. Unsent suppressed candidates remain eligible.

For mixed-version parties, receivers still accept `[SM] Unused floor drops: ...`, compact `[SM] Use: ...` and verbose unused-starting-item formats. New clients no longer broadcast those end-of-biome messages. Malformed/duplicate-type batches are rejected as a whole.

Legacy-only sender/type notices add contribution maxima and outstanding source work, not pickups or catches. Repeated sender/type notices overlap. New relevant extra demand can reopen source checking; old population alone cannot supply the additional spawn. Early reports instead feed the population targets directly. Full Clear still needs its actual capture formula; ordinary Unique first-catch species do not gain a new source gate.

**Known difference:** a later larger notice from an already outstanding/resolved sender/type increases contribution maxima but does not replace that existing outstanding record. Dedicated bird/coin population targets can therefore diverge from the generic pending-source ledger; see [audit D3](../IMPLEMENTATION_AUDIT.md#d3).

## Exceptions

The separate Full Clear Gimmiegold source-recovery path can accept confirmed pickup/use history despite uncertain markers; local Sparkling still uses its registered slots/current-population validator. Bird Full Clear uses known blockers rather than requiring a positively empty source survey. These are implemented completion distinctions, not highlight guarantees.

## Examples

- Picking up Orange Gem hides both predicted Orange markers; placing it later does not restore them.
- Four incense held together suppresses later incense ESP even after use. Needed coin ESP remains independent.
- A hidden/unloaded registered coin slot remains unresolved without pickup or observable collection evidence.
- Repeating a teammate's one-coin notice adds no second contribution; another sender's coin does add demand.

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee.

- [FloorDropPredictor](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/FloorDropPredictor.kt), [SafariFloorDrops](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariFloorDrops.kt): grouping, prediction, pickup parse and selection.
- [SafariEsp](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/SafariEsp.kt): five-tick caller, active marker/coin registration, observations and rendering; [ForestBirdFoodDrops](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/ForestBirdFoodDrops.kt), [HauntedCoinDrops](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/HauntedCoinDrops.kt): presence filtering.
- [SafariResources](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariResources.kt), [StartingResourceDelivery](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/StartingResourceDelivery.kt), [SafariCompletion](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletion.kt), [SafariTracking](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariTracking.kt): chat/inventory, trigger, dispatch and receipt paths.
- Existing checks: [SafariFloorDropsTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariFloorDropsTest.kt), [StartingResourceDeliveryTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/resources/StartingResourceDeliveryTest.kt), [GimmiegoldCoinFloorCheckTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldCoinFloorCheckTest.kt). Not run during this audit.

### Inspection provenance

The supplied ShinyHunterPrivate v2.25 26.1.2.jar (1,004,176 bytes) identifies shinyhunter 2.25, Shiny Hunter Private, license MIT. SHA-256: 61678BC859A7E84447051155A395A6D9B65680F7162A6788AD12B233C30323B3. Earlier inspection used jar tf and JDK javap -p -c -constants / -v; the JAR was not executed or added to this repository.

Primary methods: com.shinyhunter.FloorDropLog.scan, predict, repredict, onGround, handle, nearest, biomeAt; FloorDropType; FloorDropView.onClientTick. FloorDropHighlighter also clusters; prediction resides in FloorDropLog.predict. Bytecode landmarks: eligibility/sort 34–95; phase walk 157–205; anchor/index 215–240; mapping 243–379; generic fallback 381–410; PHASE_BIOMES/gem order in static initializer. The inspected JAR's recent-click matching and JSON log are provenance of that external algorithm, not features claimed for the current client's simpler pickup matcher.

## Open questions

Validate ordering, late entry, partial/unknown triples, teammate pickups, marker disappearance and prediction revisions against actual server rewards. Client filtering cannot establish whole-world coverage. No live Minecraft validation was performed in this audit.

## Last updated

2026-10-19 (America/New_York) — Added discovery-gated early party quantities and local-title-only completion reminders. Reviewed source: working changes on `78885e0`; automated validation reported with the implementation, live multi-client validation pending.

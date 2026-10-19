# Resources and typed floor drops

## Scope

Authoritative reference for starting grants, floor pickups, held resources, unused-resource reminders, drop prediction and resource highlight selection. [Tracking](TRACKING.md) owns evidence retention, [bird completion](../BIRD_COMPLETION.md) owns feeding/capture formulas, and [species pages](README.md) own completion prerequisites. Predicted contents are not confirmed resources.

## Definitions

A **starting grant** is entry-time resource evidence from Head Start or initial inventory. A **floor pickup** is an actual reward message; a **held resource** is sampled inventory. **Resource use** and **confirmed placement** require their own evidence. A **predicted floor drop** is a label inferred from display grouping and entity order.

A marker hidden by selection is not an absent resource. Unknown/partial markers and unobserved coverage remain distinct from a complete known nonresource reward group.

## Implemented rules

### Starting resources

The supplied ticket observations describe zero ticket-specific items for Basic/Economy/Premium and one for First Class, plus one independent Head Start item when present. First Class has no grant message; inventory is authoritative for the total, including two identical items. The client records observed ticket/admission evidence but does not manufacture inventory from ticket type.

The nine recognized quest types are Icebreaker, Yogi Berry, Wriggleworm, Bag of Seeds, Shining Coin, Purple Gem, Lime Gem, Orange Gem and Soothing Incense. Initial tracking runs before HOTSPOT and through its +2000 ms window, closing before resource input/pickup or dispatch. Head Start overlaps inventory and later floor pickups are subtracted. HOTSPOT starts the announcement timer, not a grant. See [early absolute reports and unknown-evidence limits](TRACKING.md#early-party-starting-resource-synchronization) and [exact messages](MESSAGES.md#entry-and-starting-items).

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

Coin and incense selection are independent. Incense peak inventory includes starting items and stays recorded after consumption/travel/mode change. A pickup quantity contributes units to the typed cutoff; it is distinct from Forest's six **events** reporting gate. Cavern/Icy manual overrides last for the current biome visit and affect the group gate; per-type acquisition/need selection still applies. Floor-group controls are not a universal gate for every automatic Cavern/Icy path.

Per-type colors cover nine quest types and apply to outline/fill/name. Older shared color supplies defaults; All floor-drop colors assigns all types. These choices never create resources or override per-type eligibility.

### Presence and coin-slot registration

Completion presence uses active, visible STRING display markers independently of selected ESP. Complete active triples bind members by IDs, including across block boundaries.

Forest excludes complete groups classified as nonfood, including generic rewards; food and unknown/partial groups remain blockers. Haunted coin presence excludes only complete typed incense groups; generic, coin and partial/unknown groups remain blockers. Thus hidden incense does not become a coin-source requirement, and hidden coins do not vanish from evidence.

Gimmiegold's initial catalog validates active STRING triples and a full supported contiguous Cavern-six/Forest-nine/Haunted-five-incense-plus-variable-coins/Icy-five phase sequence, including phase-boundary ID continuity. Predicted type alone is insufficient. One validated group is one slot; a matched actual coin pickup is separate evidence. Missing/partial groups, unknown active markers, identity conflicts and contradictory matches withhold completeness. F is the observed coin-group count, never a fixed three. Acquisition runs Safari-wide independently of display selection.

Retain group/member identities through collection/unloading. Marker disappearance never records a pickup or lowers F. A matched positive pickup or fresh complete loaded source evidence plus sufficient qualified population can account for opportunities. Unknown coverage stays unknown; new slots/remaining markers reopen validation. The structural order classifier is an implementation assumption requiring live verification, not a server reward field. [Tracking](TRACKING.md#source-evidence-and-retention) owns completion composition.

### Pickup matching

Actual FLOOR DROP text increments resource evidence independently of predicted labels. The current predictor heuristically matches a pickup to the nearest unassigned group seen within three seconds and five blocks; a near-equal distance tie is rejected. A match records actual reward separately and retires visual availability. It is not a server group-to-reward identifier.

Late entry or missing the earliest groups can establish the wrong anchor. Group equality, ID order and phase assumptions need live confirmation. Do not silently claim absent/unloaded data was collected, or label prediction consistency as server verification.

### Unused starting food/coin reminders

After the first locally completed biome, starting-resource delivery activates after four seconds. It continues checking unsent eligible resources for the run, even if a teammate's report suppressed this client's biome announcement. Repeated polls do not restart the delay.

| Resource | Discovery gate | Destination/title |
| --- | --- | --- |
| Shining Coin | Gimmiegold needed | Haunted / USE YOUR SHINING COIN! |
| Bag of Seeds, Wriggleworm, Yogi Berry | Any bird needed | Forest / USE YOUR BIRD FOOD! |

Before delivery, resample inventory and rebuild quantities/discovery needs. Remind only outside the destination while still holding tracked unused starting quantity. Filter every item independently; combine eligible items in one readable title/subtitle, with USE YOUR STARTING ITEMS! for mixed destinations. There is no independent title toggle. Suppressed items remain eligible for later local delivery.

**Delivery is local title only.** No unused-item party command, local chat echo, retry or compact fallback is sent. Presentation records notification state only and never creates contributions or outstanding source work. Normal biome completion words are unchanged. Initial quantities are shared by the separate [HOTSPOT+2s report](TRACKING.md#early-party-starting-resource-synchronization), including explicit none, independent of these reminder gates.

Legacy inbound `[SM] Use:`, verbose starting-item and combined unused-floor-drop formats remain compatible, rejecting malformed/duplicate-type batches. They merge absolute sender/type maxima with early/local evidence, never add a second grant or catch. Larger quantities reopen the dedicated bird/coin demand proof; an older population/report cannot discharge new demand. Fresh qualified coin F+S proof resolves overlapping legacy obligations against that verified quota, including F below/above three. Full Clear retains real captures and its separate conservative source-recovery path; see [audit D3](../IMPLEMENTATION_AUDIT.md#d3) for the historical bookkeeping distinction.

## Exceptions

The separate Full Clear Gimmiegold source-recovery path can accept confirmed pickup/use history despite uncertain markers; local Sparkling still uses its registered slots/current-population validator. Bird Full Clear requires complete current starting reports plus known initial floor coverage or fd, then uses known blockers rather than the local Sparkling source survey. These are implemented completion distinctions, not highlight guarantees.

## Examples

- Picking up Orange Gem hides both predicted Orange markers; placing it later does not restore them.
- Four incense held together suppresses later incense ESP even after use. Needed coin ESP remains independent.
- A hidden/unloaded registered coin slot remains in F; disappearing alone cannot create a pickup or fresh sufficient population.
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

2026-10-19 (America/New_York) — Updated early starting-resource synchronization, verified coverage and local-only reminders for issue #68 while preserving the merged documentation template. Resource runtime reviewed at `76dd746`; prior audit provenance remains scoped to its stated revision. Automated all-target and offline native checks are separate from pending live Hypixel validation.

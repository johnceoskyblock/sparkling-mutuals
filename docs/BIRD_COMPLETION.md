# Forest bird completion

## Scope

Authoritative shared spawning, capture and reporting rules for Bluebird, Parakeet and Macaw. Other Forest species use their own pages. General mode/PB distinctions belong to [SAFARI_LOGIC](../SAFARI_LOGIC.md); resource messages and predictions belong to [MESSAGES](critters/MESSAGES.md) and [FLOOR_DROPS](critters/FLOOR_DROPS.md).

## Definitions

A **feeding opportunity** is the ledger's unit of expected bird population: one Bluebird, one Parakeet, or a Macaw pair. This is implemented accounting, not proof of an exact server population.

**Qualified population** means shared captures plus current uncaptured rendered birds, observed while the player is inside Forest at X/Z 6/51 within 15 horizontal blocks. **Retained spawning proof** is qualified population established with sampled zero held food and zero remaining food markers. It is not a union of old bird UUIDs.

## Implemented rules

### Expected population

Expected opportunities are **max(9, personally collected Forest food quantity) + local extra food + reported extra food**. Nine is the implemented baseline. Local extras include starting inventory/grants and pickups outside Forest, avoiding overlap with early pickup messages. Accepted party notices use sender/type maxima; the local sender's notice overlaps local extras. The first available inventory sample starts the three-second starting-inventory window.

Qualified population is:

Bluebird catches + live Bluebirds + Parakeet catches + live Parakeets + floor((Macaw catches + live Macaws) / 2).

Attraction chat establishes no food use, bird spawn, species total or Macaw pair. Complete active non-food reward triples are excluded from remaining food markers; food and partial/unknown groups still block. This marker scan operates Safari-wide, including the boat and other biomes, independently of selected ESP.

The ledger retains maximum qualified species populations (captures + current renders). It retains a qualified total only when no floor food or held food remains. Incomplete scans do not accumulate separate UUID populations. New food pickup clears retained total proof; known remaining/held food blocks it, and increased demand can exceed it. Forest reentry clears the current total while retaining previously verified proof and species requirements.

### Completion by mode

| Mode | Inside Forest | Outside Forest |
| --- | --- | --- |
| Sparkling | Local feedingResolved: sampled zero floor food, zero held food and current/retained qualified population reaching expected opportunities | Earned local proof, or accepted fd covering current demand with no known food blockers; a retained report plus sufficient actual captures can cover increased demand |
| Unique | Sparkling spawning requirement plus each bird's shared catch; waive Macaw only when completed local proof establishes no Macaws | Earned local spawning proof plus catches/no-Macaw proof; otherwise fd supplies spawning trust. If Bluebird and Parakeet are caught, remoteReady may waive the last Macaw catch |
| Full Clear | Shared captures meet the capture formula below, with no known food blockers | The same actual capture formula; fd supplies no captures |

Full Clear's formula requires Bluebird and Parakeet catches each reaching that species' greatest qualified population. If Macaws are known, at least one Macaw catch is required. Macaw credit is zero before a catch, otherwise max(floor(known qualified Macaw population / 2), ceil(Macaw catches / 2)). Bluebird catches + Parakeet catches + Macaw credit must reach expected opportunities.

This preserves one-catch credit for all established Macaw pairs. Additional ordinary Macaws alone do not block it. However, Full Clear's capturesComplete checks **known** food blockers; unlike feedingResolved, it does not demand a positive empty-food/inventory survey or local spawning proof. Sufficient catches can satisfy it with unknown source observations. This implementation difference is recorded in [audit D9](IMPLEMENTATION_AUDIT.md#d9).

All three birds share the Full Clear capture result. Confirmed Sparkling bird catches are real catches, but do not bypass spawning requirements in Unique/Sparkling. Forest reports do not waive other Forest species' requirements. Personal Full Clear PBs substitute personal captures into the same bird formula.

### Sending fd

Only an initial Forest clearer can automatically report. On the first Forest entry, **no bird captures previously received** establishes clearer eligibility; a detour to another biome does not disqualify the player. Prior bird captures make the entrant ineligible for automatic fd even if their progress later completes.

The clearer additionally needs at least **six separate personal Forest bird-food pickup events**, sampled zero held food, zero remaining food markers, local spawning proof and all selected-mode Forest completion requirements. Six items in one pickup message are one event. The independent All Bird Food alert needs three of each food, not six events.

Inventory is sampled before queuing and before dispatch decisions. Queued reports are revalidated; owning every bird discovery does not bypass supply/clearer gates. A new run resets eligibility, pickup events and report state.

## Exceptions

- Proven no-Macaw spawning can waive the Unique Macaw catch; Full Clear's formula can also complete with no Macaws when the other captures cover demand.
- A remote fd can waive Unique's last Macaw catch outside Forest once Bluebird and Parakeet are caught; it does not prove a personal Macaw capture.
- Additional food changes demand without adding captures. An old report whose recorded target is too small cannot alone waive that new demand.
- Disabling bird ESP does not disable evidence collection. Capsule replacement UUIDs are not accumulated as feeding opportunities; current-population observations still depend on correct loaded entity identity.

## Examples

- Seven Bluebird catches and two live Parakeets, zero floor/held food, and a qualified nine-opportunity observation can satisfy Sparkling without a Parakeet catch. Unique still needs one; Full Clear needs the required Parakeet catches.
- A qualified population of six Bluebirds, two Parakeets and two Macaws represents nine opportunities. Six Bluebird catches, two Parakeet catches and one Macaw catch can satisfy Full Clear while the second Macaw remains.
- A late entrant who already received bird loot shares can complete valid progress but cannot automatically send fd.
- One reported extra food raises the baseline target from nine to ten. Repeating the same sender/type notice does not add another opportunity.

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee.

- [SafariBirdLedger](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariBirdLedger.kt): feedingTarget, render, retainSpawnProof, remoteReady, capturesComplete and reportSuppliesReady.
- [SafariRun](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt): first entry, scanBirdSources, birdSpawnsComplete, uniqueComplete, birdComplete and canReportBiome.
- [SafariBiomeClearTracking](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariBiomeClearTracking.kt) supplies inventory, qualified live birds and Safari-wide marker evidence; [SafariCompletion](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletion.kt) rechecks report dispatch; [ForestBirdFoodDrops](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/ForestBirdFoodDrops.kt) classifies blockers.
- Existing checks: [BirdCompletionPolicyTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/BirdCompletionPolicyTest.kt), [SafariBirdLedgerTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariBirdLedgerTest.kt), [ForestBirdFoodDropsTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/resources/ForestBirdFoodDropsTest.kt). Not run locally in this documentation audit.
- Supplied 2026-10-18 run evidence: nine personal food pickups, seven Bluebird and three Parakeet captures, zero held food and eight raw Forest marker groups classified as generic rewards. This motivated filtering raw markers; it is one reported run, not fresh audit gameplay validation.
- /sm debug logs expected/current/verified population, floor/held food, pickup events, report target and Forest report blockers.

## Open questions

Live-server confirmation remains pending for baseline population, Safari-wide marker delivery, late-entry coverage and escape/replacement behavior. See [audit D9](IMPLEMENTATION_AUDIT.md#d9) for Full Clear's weaker source-observation requirement.

## Last updated

2026-10-19 (America/New_York) — Audited bird population/capture formulas, retained proof and fd eligibility. Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee; static source/caller review, no live Minecraft validation.

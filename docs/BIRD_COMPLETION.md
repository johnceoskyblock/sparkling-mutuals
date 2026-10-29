# Forest bird completion

## Scope

Authoritative shared spawning, capture and reporting rules for Bluebird, Parakeet and Macaw. Other Forest species use their own pages. General mode/PB distinctions belong to [SAFARI_LOGIC](../SAFARI_LOGIC.md); resource messages and predictions belong to [MESSAGES](critters/MESSAGES.md) and [FLOOR_DROPS](critters/FLOOR_DROPS.md).

## Definitions

A **feeding opportunity** is the ledger's unit of expected bird population: one Bluebird, one Parakeet, or a Macaw pair. This is implemented accounting, not proof of an exact server population.

**Qualified population** means shared captures plus current uncaptured rendered birds, observed while the player is inside Forest at X/Z 6/51 within 40 horizontal blocks. **Retained spawning proof** is qualified population established with sampled zero held food and zero remaining food markers. It is not a union of old bird UUIDs.

## Implemented rules

### Expected capture population

Expected opportunities are **max(9, personally collected Forest food quantity) + local extra food + reported extra food + inferred unreported starting food**. Early discovery-gated starting-item reports supply remote contributions before clearing completes; later unused-resource local titles remain, and a separately paced party follow-up after a local biome report names which members still need the related Sparklings without adding source contributions again. Nine is the implemented baseline. Local extras include starting inventory/grants and pickups outside Forest, avoiding overlap with early pickup messages. Accepted party notices use sender/type maxima; the local sender's notice overlaps local extras. The first available inventory sample starts the three-second starting-inventory window.

During the first 30 seconds from run arrival, a loaded party teammate in mapped Forest within 20 blocks of the 40-block scan radius (60 horizontal blocks from X/Z 6/51) contributes one inferred starting food if they have not sent a parsed `[SM] Starting items:` report. The local player is excluded. Repeated sightings, departure/reentry and sightings after 30 seconds add nothing further. Any later starting report replaces this estimate with its actual food quantities; a report listing only coins removes the food estimate. This is a conservative heuristic, not proof that the teammate actually owns food. It raises the shared feeding target without fabricating a pickup, capture or usage event.

Qualified population is:

Bluebird catches + live Bluebirds + Parakeet catches + live Parakeets + floor((Macaw catches + live Macaws) / 2).

Exact bird-attracted system messages confirm one pending local food use each, including one for a Macaw pair. They do not establish captures or the Full Clear population total. Complete active non-food reward triples are excluded from remaining food markers; food and partial/unknown groups still block. This marker scan operates Safari-wide, including the boat and other biomes, independently of selected ESP.

The ledger retains maximum qualified species populations (captures + current renders). It retains a qualified total only when no floor food or held food remains. Incomplete scans do not accumulate separate UUID populations. New food pickup clears retained total proof; known remaining/held food blocks it, and increased demand can exceed it. Forest reentry clears the current total while retaining previously verified proof and species requirements.

### Completion by mode

| Mode | Inside Forest | Outside Forest |
| --- | --- | --- |
| Sparkling | Sampled zero floor/held food and no pending local food-use confirmations; require observed consumption or nine accounted feedings | Earned local food proof, or accepted fd with no held food; party-reported extras do not raise this checking demand |
| Unique | Sparkling spawning requirement plus each bird's shared catch; waive any bird with completed local proof that the species never spawned | Remote fd plus each bird catch, with an unencountered zero-spawn exception for each bird; still-held needed starting food blocks the source completion |
| Full Clear | Shared captures meet the capture formula below, with no known food blockers | The same actual capture formula; fd supplies no captures |

Full Clear's formula requires Bluebird and Parakeet catches each reaching that species' greatest qualified population. If Macaws are known, at least one Macaw catch is required. Macaw credit is zero before a catch, otherwise max(floor(known qualified Macaw population / 2), ceil(Macaw catches / 2)). Bluebird catches + Parakeet catches + Macaw credit must reach expected opportunities.

This preserves one-catch credit for all established Macaw pairs. Additional ordinary Macaws alone do not block it. However, Full Clear's capturesComplete checks **known** food blockers; unlike feedingResolved, it does not demand a positive empty-food/inventory survey or local spawning proof. Sufficient catches can satisfy it with unknown source observations. This implementation difference is recorded in [audit D9](IMPLEMENTATION_AUDIT.md#d9).

All three birds share the Full Clear capture result. Confirmed Sparkling bird catches are real catches, but do not bypass spawning requirements in Unique/Sparkling. Forest reports do not waive other Forest species' requirements. Personal Full Clear PBs substitute personal captures into the same bird formula.

### Sending fd

With Auto biome reports enabled, a Forest entrant can send fd once local food-use/source proof and every other selected-mode Forest requirement pass. Local bird checking follows the rules above; Full Clear retains the shared capture formula. No personal pickup quota or initial-clearer classification is required. This allows a late entrant to report even when somebody else collected and used all the food before that entrant arrived, including before any birds were caught. An already received fd suppresses an automatic echo.

The independent All Bird Food alert still requires three of each food; it is not a reporting gate.

Inventory is sampled before queuing and before dispatch decisions. Queued reports are revalidated; owning every bird discovery does not bypass local source/population gates. A new run resets population, pickup and report state.

## Exceptions

- Proven zero-spawn Bluebird, Parakeet or Macaw can waive that Unique catch; Full Clear's formula can also complete with no Macaws when the other captures cover demand.
- A remote fd can waive Unique's last Macaw catch outside Forest once Bluebird and Parakeet are caught; it does not prove a personal Macaw capture.
- Additional food changes Full Clear capture demand without adding captures. Party-reported extras do not reopen local Sparkling food checks; newly held local food or remaining floor food still blocks them.
- Disabling bird ESP does not disable evidence collection. Capsule replacement UUIDs are not accumulated as feeding opportunities; current-population observations still depend on correct loaded entity identity.

For a local Unique Forest clearer, no catch of a bird species is required if that species has never been seen or caught and qualified population/source evidence resolves all food opportunities. This includes party-reported unused starting food. Remaining food markers, held food or insufficient accounted population block the waiver. That species’ row then disappears and fd may send when every other Forest requirement is complete. A later sighting of that species revokes the zero-spawn waiver.

For a local Unique Forest clearer, no catch of a bird species is required if that species has never been seen or caught and qualified population/source evidence resolves all food opportunities. This includes party-reported unused starting food. Remaining food markers, held food or insufficient accounted population block the waiver. That species’ row then disappears and fd may send when every other Forest requirement is complete. A later sighting of that species revokes the zero-spawn waiver.

## Examples

- Seven Bluebird catches and two live Parakeets, zero floor/held food, and a qualified nine-opportunity observation can satisfy Sparkling without a Parakeet catch. Unique still needs one; Full Clear needs the required Parakeet catches.
- A qualified population of six Bluebirds, two Parakeets and two Macaws represents nine opportunities. Six Bluebird catches, two Parakeet catches and one Macaw catch can satisfy Full Clear while the second Macaw remains.
- A late entrant with bird loot shares can send fd after local population/source proof and the other active-mode requirements pass, provided no fd was received.
- One reported extra food raises the Full Clear capture target from nine to ten. Repeating the same sender/type notice does not add another opportunity.

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

### Remote report priority

While outside Forest, Unique trusts fd after each bird's first catch, or waives an unencountered bird as a zero spawn. Bluebird and Parakeet are now eligible for the same remote zero-spawn exception as Macaw. A prior Forest visit does not prevent this trust. Inside Forest, local feeding/source proof remains required. Full Clear retains its actual bird capture formula in every biome.

Outside-Forest Sparkling checks accept fd while requiring no held food. Later party-reported starting-food contributions do not reopen this source check; Full Clear capture demand still tracks them. Needed starting food still held by this client blocks its destination completion/report.

## Local Sparkling food proof

Sparkling bird checks use Forest floor-food markers and this client's inventory, without party-reported starting food or inferred teammate food increasing that source demand. After an inventory decrease of N food, wait for N exact bird-attracted system messages. Two Macaws is one food-use confirmation, not two. Matching messages just before the inventory packet are retained briefly. With no personal food-use batch observed, the existing late-entry proof of nine accounted feedings remains available. Own capture counts and Full Clear capture bookkeeping remain separate.

## Last updated

2026-10-29 - Simplified known-source replies and local Sparkling source requirements.


2026-10-24 — Expanded bird population/food-use coverage to 40 blocks and added one provisional starting food for unreporting early Forest visitors, replaced by real starting reports.

2026-10-22 — Remote fd can waive unseen zero-spawn birds in Unique; current Forest retains local spawning proof.

2026-10-20 — All three bird types support zero-spawn completion: Unique requires resolved feeding (including known party extras), qualified Forest survey and no catch/sighting of that species. Full Clear retains the actual-population capture formula, which allows zero Bluebirds/Parakeets when the other captures cover all feeding opportunities. No catches are fabricated.

2026-10-20 — Unique bird rows disappear after their first shared capture. Proven no-Macaw absence still waives its row. Forest shows Bird food left to use below the nest footer: the greater of unresolved feeding demand (including reported party extras) and known held plus remaining floor food. This is inferred from captures/render evidence, not a direct server usage counter; spawning/progress/report gates remain independent.

2026-10-20 - Named completion-time party wait reminders reuse current starting-food quantities without counting them twice.

2026-10-19 - Verified local zero-Macaw fd and row removal include all reported starting food; any Macaw encounter retains the catch requirement.

2026-10-19 - Verified local zero-Macaw fd and row removal include all reported starting food; any Macaw encounter retains the catch requirement.

2026-10-19 (America/New_York) — Early starting-food reports feed sender/type maxima; late entrants can report qualified local spawning without six personal pickups. Reviewed source: working changes on `78885e0`; live validation pending.

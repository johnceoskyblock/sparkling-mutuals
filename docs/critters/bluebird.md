# Bluebird

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Forest | Implemented roster |
| Rarity | Uncommon | Supplied server notes; not live revalidated |
| Spawn source | Birdfeeder food opportunities | Implemented evidence path; mechanics unverified |
| Population model | Variable population; 9 implemented baseline feeding equivalents plus known extras | Implemented rule, not server guarantee |
| Full Clear minimum | 0 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | Forest local spawning scan: radius 15 blocks around X/Z (6, 51) | Implemented coverage |

## What makes this critter different

- All three birds share spawning proof; a Macaw pair counts as one feeding equivalent. Historical UUIDs and attraction chat do not establish spawning.

## Spawn prerequisites

Bag of Seeds, Wriggleworm and Yogi Berry create feeding opportunities. Starting grants, floor pickups, held food and party contributions are separate evidence; [bird completion](../BIRD_COMPLETION.md) owns the formula.

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live blue parrot / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Common/Uncommon: supplied notes say no capsule escape; Masterful catches guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Qualified Forest capture-plus-current-bird scan | Feeding equivalents and known species totals | Extra population from historical UUIDs or attraction chat |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary birds. | Inside Forest: qualified shared captures plus current birds cover food opportunities, with no held or known floor food remaining. | Outside Forest: valid fd supplies spawning trust; retained local proof or sufficient captures with a report can also apply. |
| Unique | One shared capture. | Spawning ledger resolved; owned discoveries do not waive spawning. | Outside Forest, fd can supply spawning trust; never adds captures. |
| Full Clear | Bird captures cover feeding target and known Bluebird/Parakeet totals, with one-Macaw credit. | No known held/uncollected floor food. The capture predicate does not additionally require local spawning proof or a positive empty-inventory sample. | fd never supplies missing captures. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses spawning plus own capture/no-Macaw rule. Sparkling lists needed unresolved spawning checks. Full Clear uses the shared bird capture predicate; no additional local empty scan. See [panel rules](../MISSING_PANEL.md). |
| Counters | None in the species row; capture panel shows actual shared catches. |
| Ordinary ESP | Standard mode/discovery/model filters. Unique stops after its own first catch even if spawning remains pending. Sparkling checks add no ordinary model ESP; enabled profitable highlighting remains separate. |
| Source helpers | Food-drop highlights follow [FLOOR_DROPS](FLOOR_DROPS.md); hiding a marker does not use food. |
| Automatic completion announcements | Any Forest entrant may report complete qualified spawning/source proof without a personal pickup quota if no fd was received. All other local active-mode gates still apply; see [bird reporting](../BIRD_COMPLETION.md#sending-fd). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

An fd records the target at receipt; new contributions can outgrow it. Local held food or known Forest floor food blocks remote trust. Inside Forest the local spawning path applies; see [bird rules](../BIRD_COMPLETION.md). See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Completed qualified population proof survives unloading; a new pickup clears it. Known floor food survives unknown scans/travel. Starting/party extras remain run-owned; repeated notices overlap by sender/type. New run resets the ledger. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

Nine feeding equivalents with no held or remaining floor food can finish Sparkling checking. New extra food increases the target; fd cannot create Full Clear captures.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entry: `Bluebird` in `SafariRun` and `SafariEspRules.mobs`; dedicated owners below.

[SafariBirdLedger; called by SafariRun.scanBirdSources / birdComplete](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariBirdLedger.kt)

Existing coverage: [SafariBirdLedgerTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariBirdLedgerTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-26 — Removed check-only ordinary ESP and its per-UUID linger; capture/source and profitable highlighting retain their own rules.

2026-10-20 — This species may have zero spawns. In Unique, waive its catch and remove its missing row only after qualified Forest feeding/source proof accounts for all known food (including party extras), with no sighting or capture of this species. Full Clear permits zero catches of this species when actual catches of the spawned birds satisfy the shared bird-population/food formula. A later sighting revokes the Unique absence waiver.

2026-10-19 (America/New_York) — Updated late-entry reporting and early shared starting-food totals. Reviewed source: working changes on `78885e0`; live validation pending.

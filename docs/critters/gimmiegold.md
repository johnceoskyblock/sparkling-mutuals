# Gimmiegold

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Haunted | Implemented roster |
| Rarity | Rare | Supplied server notes; not live revalidated |
| Spawn source | Shining Coin slots plus starting/party coins | Implemented evidence path; mechanics unverified |
| Population model | Variable; Sparkling target F verified floor slots + S starting coins | Implemented rule, not server guarantee |
| Full Clear minimum | 3 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | Sparkling population radius 15 blocks around X/Z (-4, -64); slots learned Safari-wide | Implemented coverage |

## What makes this critter different

- Sparkling requires complete initial source/current-roster report coverage, no remaining applicable coin sources, sampled zero held coins, and qualified actual captures plus current uncaptured models reaching F + S. Historical UUID totals cannot substitute; neither can a fixed three-coin fallback.
- Full Clear retains a separate conservative recovery ledger: fallback minimum 3 plus larger known targets.

## Spawn prerequisites

Collect registered slots/use all known coins. Grants, pickups, held coins, use messages and contributions remain separate evidence; see [FLOOR_DROPS](FLOOR_DROPS.md).

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live textured item display / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Supplied notes say standard capsules can fail, replacement UUIDs may follow escape, and Masterful catches are guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Coin registry, inventory and qualified current population | Distinct floor opportunities F, known starting coins S, and fresh C + U against F + S; disappearance alone is not collection | Fixed three-slot guarantee, capture from resource use, or replacement UUID history as current population |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary models; confirmed Sparkling capture override applies. | Known initial source/report coverage, no remaining sources, sampled 0 held coins, qualified C + U >= F + S. F is the observed variable count, including a verified zero; unknown is not zero. | Outside Haunted, current-demand hd supplies checking without local renders, retaining known resource blockers. New demand/contradictions invalidate old trust. Inside Haunted local conditions apply. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | None beyond capture; ordinary coin checks do not gate Unique. | Does not supply a capture. |
| Full Clear | At least 3 shared captures, raised by source/contribution and observed-population targets. | Local recovery resolved, no known held coins/no remaining critters; resource/source contradictions may still block progress. | Without local proof, report plus capture minimum can complete progress; row still needs local proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | None in the species row; capture panel shows actual shared catches. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: discovery-gated, normally up to 10 seconds per UUID. Settings and model validity still apply. |
| Source helpers | Remaining eligible coin markers are independent of incense cutoffs; Sparkling requires needed Gimmiegold discovery. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

One [early absolute report](TRACKING.md#early-party-starting-resource-synchronization) shares original coins/food at HOTSPOT+2s. Sender/type maxima overlap local evidence, repeated reports and inbound legacy notices. Larger quantities increase demand without catches and require updated applicable proof. Missing/old-client/late-join reports remain unknown; no title creates demand. Fresh qualified F+S proof also resolves overlapping legacy coin bookkeeping against that same variable quota, without a second fixed-three Sparkling veto. Bird food belongs to Forest.

## Retention and reopening

Retain slot IDs/contribution demand through collection/unloading. Marker disappearance never records a pickup; matched positive pickups or fresh applicable source/population evidence account for opportunities. Unobservable space stays unknown. Travel retains completed validation; Haunted reentry, new slots, remaining markers, held coins or changed contributions revoke it. New run resets all. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

F = 4 and S = 1 require C + U >= 5. Even with zero held coins, one remaining slot blocks checking. After collection, qualifying current population can finish; historical UUIDs cannot supply U.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entry: `Gimmiegold` in `SafariRun` and `SafariEspRules.mobs`; dedicated owners below.

[GimmiegoldSparklingCheck](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldSparklingCheck.kt)

[SafariResources / contribution notices](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariResources.kt)

Existing coverage: [GimmiegoldCoinFloorCheckTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldCoinFloorCheckTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

The original [audit D3](../IMPLEMENTATION_AUDIT.md#d3) describes older generic allocation limits. Current Sparkling uses updated F+S and fresh demand proof, including larger legacy overlaps. Full Clear retains separate actual-capture/source-recovery rules. The structural initial catalog/shared marker and live server grant assumptions still require multiplayer validation.

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-19 (America/New_York) — Updated early starting-resource synchronization, verified coverage and local-only reminders for issue #68 while preserving the merged documentation template. Resource runtime reviewed at `76dd746`; prior audit provenance remains scoped to its stated revision. Automated all-target and offline native checks are separate from pending live Hypixel validation.

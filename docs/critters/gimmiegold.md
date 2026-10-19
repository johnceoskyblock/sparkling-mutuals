# Gimmiegold

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Haunted | Implemented roster |
| Rarity | Rare | Supplied server notes; not live revalidated |
| Spawn source | Shining Coin slots plus starting/party coins | Implemented evidence path; mechanics unverified |
| Population model | Variable; Sparkling target F registered floor slots + E extra coins | Implemented rule, not server guarantee |
| Full Clear minimum | 3 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | Sparkling population radius 15 blocks around X/Z (-4, -64); slots learned Safari-wide | Implemented coverage |

## What makes this critter different

- Sparkling requires all registered slots resolved, sampled zero held coins, and current captured-plus-rendered population F + E. Historical UUID totals cannot substitute.
- Full Clear retains a separate conservative recovery ledger: fallback minimum 3 plus larger known targets.

## Spawn prerequisites

Collect registered slots/use all known coins. Grants, pickups, held coins, use messages and contributions remain separate evidence; see [FLOOR_DROPS](FLOOR_DROPS.md).

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live textured item display / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Supplied notes say standard capsules can fail, replacement UUIDs may follow escape, and Masterful catches are guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Coin registry, inventory and qualified current population | Slot collection and C + U against F + E | Fixed three-slot guarantee, capture from resource use, or replacement UUID history as current population |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary models; confirmed Sparkling capture override applies. | All registered slots resolved, sampled 0 held coins, qualified C + U >= F + E; at least one registered slot required. | Outside Haunted, hd can supply checking trust. Inside Haunted local coin conditions apply. |
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

Unused-item notices overlap by sender/maximum quantity; own notice overlaps starting coins. Extras increase demand without catches. Bird food belongs to Forest. hd trust outside Haunted is revalidated on entry. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Retain slot IDs/contribution demand through collection/unloading. Unobservable missing markers stay unresolved. Travel retains completed validation; Haunted reentry, new slots, remaining markers, held coins or changed contributions revoke it. New run resets all. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

F = 4 and E = 1 require C + U >= 5. Even with zero held coins, one remaining slot blocks checking. After collection, qualifying current population can finish; historical UUIDs cannot supply U.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entry: `Gimmiegold` in `SafariRun` and `SafariEspRules.mobs`; dedicated owners below.

[GimmiegoldSparklingCheck](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldSparklingCheck.kt)

[SafariResources / contribution notices](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariResources.kt)

Existing coverage: [GimmiegoldCoinFloorCheckTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldCoinFloorCheckTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Larger repeated resource notices can diverge between Sparkling maxima and conservative Full Clear recovery; see [audit D3](../IMPLEMENTATION_AUDIT.md#d3).

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

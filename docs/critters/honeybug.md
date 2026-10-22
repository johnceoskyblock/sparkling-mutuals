# Honeybug

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Forest | Implemented roster |
| Rarity | Uncommon | Supplied server notes; not live revalidated |
| Spawn source | Actual bee nests; beehives excluded | Implemented evidence path; mechanics unverified |
| Population model | Retained classified nest population; fallback minimum 3 if total is unverified | Implemented rule, not server guarantee |
| Full Clear minimum | 3 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | 13 configured potential nest sites; loaded block classification rather than a circular check | Implemented coverage |

## What makes this critter different

- Known nest count N is covered by shared captures C plus current bees U. C >= N can complete Full Clear from any biome, subject to larger known population.
- Unique row disappears after one capture; progress additionally needs checking if anyone needs Sparkling Honeybug. Trusted fd supplies checking outside Forest; current Forest needs local nest proof.

## Spawn prerequisites

Classify actual nests/hives in the initial loaded survey. Punching records source work, not a capture.

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live bee / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Common/Uncommon: supplied notes say no capsule escape; Masterful catches guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Retained nest/hive classification and punches | Actual nest count and source work; C + U can account for N | A catch; unclassified/unloaded sites do not prove no nest |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary bees; confirmed Sparkling capture override applies. | Every actual nest punched, or known nest population covered by shared captures plus current bees. | Trusted fd supplies checking outside Forest; reentry requires local nest proof. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | Progress also needs checking if discovery is needed; row uses the capture condition. | Outside Forest, fd supplies needed checking, never the capture. Current Forest requires local nest proof. |
| Full Clear | Capture verified N, raised by known extra population; otherwise use conservative fallback/source proof. | Known N needs no local empty scan or personal punch history. | fd supplies no captures and cannot retire Full Clear nest work before quota. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique removes row after first capture even while checking blocks progress. Full Clear known-N capture proof needs no local empty scan; Sparkling uses needed checking. See [panel rules](../MISSING_PANEL.md). |
| Counters | No species-row ratio; separate nest footer, known/unknown survey waypoints and shared capture count. |
| Ordinary ESP | Unique stops after one shared catch. Full Clear keeps eligible uncaught bees. Sparkling shows no ordinary bee ESP. |
| Source helpers | Full Clear retains unpunched nest helpers until shared verified capture quota or individual punches. Unique/Sparkling use local nest proof/trusted fd and discovery gates, separately from the row. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). Forest also requires initial-clearer status and 6 separate local food pickups. |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Keep catches received outside Forest. Trusted fd survives Forest entry/mode changes; it supplies neither Unique catch nor Full Clear captures. Incomplete later rendering does not erase it. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Classification, maximum known count, captures and trusted fd persist for the run. New actual nests can raise N and reopen local checking. Visibility loss preserves classification; new run resets it. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

With verified N = 4, four shared captures can finish Full Clear outside Forest. In Unique, one capture removes its row while needed checking still blocks progress until local proof/fd.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entry: `Honeybug` in `SafariRun` and `SafariEspRules.mobs`; dedicated owners below.

[BeeNests.tick / clicked / render](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/structures/BeeNests.kt)

[BeeNestSurvey](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/structures/BeeNestSurvey.kt)

Existing coverage: [HoneybugModeEvidenceTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/structures/HoneybugModeEvidenceTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-22 — Honeybug reports supply checking outside Forest only; current Forest uses local nest proof.

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

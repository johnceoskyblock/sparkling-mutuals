# Gemzie

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Cavern | Implemented roster |
| Rarity | Epic | Supplied server notes; not live revalidated |
| Spawn source | Gem podiums and opened chamber | Implemented evidence path; mechanics unverified |
| Population model | Fixed check/capture target 3; server fixed total not revalidated | Implemented rule, not server guarantee |
| Full Clear minimum | 3 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | X/Z (-141, 51); Full Clear radius 30 blocks; Sparkling radius 40 blocks | Implemented coverage |

## What makes this critter different

- Implemented fixed target: 3. Progress and missing-row removal use different Full Clear conditions.
- The exact chamber-opening server line supplies 3 checked after 5 seconds, without rendered Gemzies or catches; duplicates do not restart it.

## Spawn prerequisites

Purple, Lime and Orange Gems can come from starting grants or floor pickups. Placement is distinct from chamber opening; see [resource messages](MESSAGES.md) and [floor drops](FLOOR_DROPS.md).

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live vex / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Supplied notes say standard capsules can fail, replacement UUIDs may follow escape, and Masterful catches are guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Exact chamber-opening server message ([text owner](MESSAGES.md)) | Starts the one-shot 5-second checking timer | Any capture; player quotations cannot start it |
| Retained qualified UUID check ([rules](TRACKING.md)) | Distinct checks reconciled with earlier captures | Historical sighting and its later catch counted twice |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary critters; a confirmed Sparkling capture also completes the check. | Either retained 3-check target in its zone, or 5 seconds after the exact chamber-opening line. | Party cd can supply inherited checking; local scans retire it. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | None. | Does not supply a capture. |
| Full Clear | At least 3 shared captures. Local proof and report-based progress have [separate raised-target scopes](TRACKING.md#full-clear-evidence-and-late-entry). | Qualified empty local scan for row removal/local progress. | Fixed capture target can complete progress without a report; missing row still requires local empty proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | Full Clear: X/3 captured. Sparkling: X/3 checked. Unique: checked counter only while discovery is needed and its row remains. Counts clamp to 3. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: discovery-gated, normally up to 10 seconds per UUID. Settings and model validity still apply. |
| Source helpers | Predicted Purple/Lime/Orange Gem markers follow [floor-drop rules](FLOOR_DROPS.md); prediction is not placement or spawning proof. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. Retained local Full Clear row proof survives leaving its radius; positive remaining population revokes local proof. A new run resets evidence; the ended run can remain displayed. Chamber timer/result also survives Cavern reentry. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

The chamber opens at time 0. At 5 seconds Gemzie reads 3/3 checked with zero captures. Unique stays missing until a shared capture; Full Clear remains 0/3 captured.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Gemzie` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [CavernCompletionZones.zones](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/CavernCompletionZones.kt).

[SparklingChecks.chat / checkCount](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SparklingChecks.kt)

Existing coverage: [GemzieChamberCheckTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/GemzieChamberCheckTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

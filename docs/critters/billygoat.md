# Billygoat

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Icy | Implemented roster |
| Rarity | Rare | Supplied server notes; not live revalidated |
| Spawn source | Ordinary biome population; no dedicated spawn-event gate | Implemented evidence path; mechanics unverified |
| Population model | Implemented minimum; actual population can exceed it | Implemented rule, not server guarantee |
| Full Clear minimum | 2 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | X/Z (-121, -55); Full Clear radius 30 blocks; Sparkling radius 40 blocks | Implemented coverage |

## What makes this critter different

- A shared Wumpa capture marks every Icy species Sparkling-checked; Unique and Full Clear retain their own requirements.

## Spawn prerequisites

None. No species-specific resource or structure gate is used.

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live goat / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Supplied notes say standard capsules can fail, replacement UUIDs may follow escape, and Masterful catches are guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Retained qualified UUID check ([rules](TRACKING.md)) | Distinct checks reconciled with earlier captures | Historical sighting and its later catch counted twice |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary critters; a confirmed Sparkling capture also completes the check. | 2 reconciled checks from qualified UUID sightings/shared captures; retain checks for the run. A shared Wumpa capture bypasses this species check. | Party id can supply inherited checking; local scans retire it. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | None. | Does not supply a capture. |
| Full Clear | At least 2 shared captures. Local proof and report-based progress have [separate raised-target scopes](TRACKING.md#full-clear-evidence-and-late-entry). | Qualified empty local scan for row removal/local progress. | Without local proof, report plus capture minimum can complete progress; row still needs local proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | None in the species row; capture panel shows actual shared catches. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: discovery-gated, normally up to 10 seconds per UUID. Settings and model validity still apply. Needed Sparkling Wumpa forces this Icy species until its first shared catch; entity/group/biome settings still gate it. |
| Source helpers | None. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. Retained local Full Clear row proof survives leaving its radius; positive remaining population revokes local proof. A new run resets evidence; the ended run can remain displayed. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

One party loot share completes Unique Billygoat. Full Clear still needs its implemented minimum and local empty coverage, or report-based progress under the shared rule.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Billygoat` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [IcyCompletionZones.zones](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/IcyCompletionZones.kt).

Existing coverage: [SafariFullClearProgressTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClearProgressTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

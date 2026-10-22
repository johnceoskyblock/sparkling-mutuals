# Driftling

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Cavern | Implemented roster |
| Rarity | Uncommon | Supplied server notes; not live revalidated |
| Spawn source | Ordinary biome population; no dedicated spawn-event gate | Implemented evidence path; mechanics unverified |
| Population model | Implemented minimum; actual population can exceed it | Implemented rule, not server guarantee |
| Full Clear minimum | 3 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | X/Z (-120, 55); Full Clear radius 40 blocks; Sparkling radius 60 blocks | Implemented coverage |

## What makes this critter different

- Sparkling ordinary ESP is exempt from the normal per-UUID 10-second timeout. Reappearance supplies no catch.

## Spawn prerequisites

None. No species-specific resource or structure gate is used.

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live textured armor-stand head / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Common/Uncommon: supplied notes say no capsule escape; Masterful catches guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Retained qualified UUID check ([rules](TRACKING.md)) | Distinct checks reconciled with earlier captures | Historical sighting and its later catch counted twice |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary critters; a confirmed Sparkling capture also completes the check. | 3 reconciled checks from qualified UUID sightings/shared captures; retain checks for the run. | Party cd can supply inherited checking; local scans retire it. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | None. | Does not supply a capture. |
| Full Clear | At least 3 shared captures. Local proof and report-based progress have [separate raised-target scopes](TRACKING.md#full-clear-evidence-and-late-entry). | Qualified empty local scan for row removal/local progress. | Without local proof, report plus capture minimum can complete progress; row still needs local proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | None in the species row; capture panel shows actual shared catches. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: discovery-gated, no ordinary per-UUID timeout for Driftling. Settings and model validity still apply. |
| Source helpers | None. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. Retained local Full Clear row proof survives leaving its radius; positive remaining population revokes local proof. A new run resets evidence; the ended run can remain displayed. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

One party loot share completes Unique Driftling. Full Clear still needs its implemented minimum and local empty coverage, or report-based progress under the shared rule.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Driftling` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [CavernCompletionZones.zones](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/CavernCompletionZones.kt).

Existing coverage: [SafariFullClearProgressTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClearProgressTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-20 — After a shared Driftling/Chuckwalla catch, defer increases to inferred capture targets for 2.5 seconds so delayed old models are not added on top of that catch. Positive remaining evidence still blocks completion during this interval. Stable live evidence afterward can raise targets normally; catches are unchanged.

2026-10-20 — Tightened the late-head fix: actual position movement must be recent (under two seconds), regardless of player distance. A retained visible name alone can no longer keep a frozen model current. Both ESP and remaining-population/completion use this gate.

2026-10-20 — Driftling now shares one live-model gate across ordinary ESP, Sparkling contacts, population targets and completion evidence. A visible current bound label validates an idle model. Without a visible label, actual observed movement and current motion are required; an unlabelled head first loaded after capture supplies no live spawn. A stale known binding stays invalid. New movement can restore the model; this never creates a capture.

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

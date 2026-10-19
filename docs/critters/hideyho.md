# Hideyho

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Haunted | Implemented roster |
| Rarity | Rare | Supplied server notes; not live revalidated |
| Spawn source | Hide-and-seek quest/reward | Implemented evidence path; mechanics unverified |
| Population model | Implemented minimum; actual population can exceed it | Implemented rule, not server guarantee |
| Full Clear minimum | 1 shared capture before species adjustments | Implemented fallback/minimum |
| Local scan coverage | Biome scan; no species-specific center radius | Implemented coverage |

## What makes this critter different

- Inside/outside title/chat works only in Haunted, in Unique/Full Clear, with its setting enabled. Sparkling or leaving Haunted cancels the watch; capture tracking stays independent.

## Spawn prerequisites

Offer/acceptance, hiding and found dialogue are quest states. Successful found reward or attributed loot share supplies a catch; [MESSAGES](MESSAGES.md) owns exact text.

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live textured player model / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Supplied notes say standard capsules can fail, replacement UUIDs may follow escape, and Masterful catches are guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Quest dialogue / located model | Quest state or observed hiding position | Capture until successful found reward; optional Chat Heads catcher prefix is supported |
| Retained qualified UUID check ([rules](TRACKING.md)) | Distinct checks reconciled with earlier captures | Historical sighting and its later catch counted twice |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary critters; a confirmed Sparkling capture also completes the check. | 1 reconciled check from qualified UUID sightings/shared captures; retain checks for the run. | Party hd can supply inherited checking; local scans retire it. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | None. | Does not supply a capture. |
| Full Clear | At least 1 shared capture. Local proof and report-based progress have [separate raised-target scopes](TRACKING.md#full-clear-evidence-and-late-entry). | Qualified empty local scan for row removal/local progress. | Without local proof, report plus capture minimum can complete progress; row still needs local proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | None in the species row; capture panel shows actual shared catches. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: discovery-gated, normally up to 10 seconds per UUID. Settings and model validity still apply. |
| Source helpers | Location announcement uses settled known spots/mansion footprint, not capture proof. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. Retained local Full Clear row proof survives leaving its radius; positive remaining population revokes local proof. A new run resets evidence; the ended run can remain displayed. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

One party loot share completes Unique Hideyho. Full Clear still needs its implemented minimum and local empty coverage, or report-based progress under the shared rule.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Hideyho` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [SafariCompletionZones.uuidMinimums](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionZones.kt).

[HideyhoSpotter / HideyhoLocationWatch](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/hideyho/HideyhoSpotter.kt)

Existing coverage: [HideyhoSpotterTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/hideyho/HideyhoSpotterTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

# Gazer

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Haunted | Implemented roster |
| Rarity | Uncommon | Supplied server notes; not live revalidated |
| Spawn source | Ordinary biome population; no dedicated spawn-event gate | Implemented evidence path; mechanics unverified |
| Population model | Fixed check/capture target 4; server fixed total not revalidated | Implemented rule, not server guarantee |
| Full Clear minimum | 4 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | Biome scan; no species-specific center radius | Implemented coverage |

## What makes this critter different

- Implemented fixed target: 4. Progress and missing-row removal use different Full Clear conditions.

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
| Sparkling | None for ordinary critters; a confirmed Sparkling capture also completes the check. | 4 reconciled checks from qualified UUID sightings/shared captures; retain checks for the run. | Party hd can supply inherited checking; local scans retire it. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | If anyone needs Gazer, four retained reconciled checks are also required for progress. The first catch still removes its missing row. | Outside Haunted hd may supply inherited checking; inside Haunted local checks are required. Never supplies a capture. |
| Full Clear | At least 4 shared captures. Local proof and report-based progress have [separate raised-target scopes](TRACKING.md#full-clear-evidence-and-late-entry). | Qualified empty local scan for row removal/local progress. | Fixed capture target can complete progress without a report; missing row still requires local empty proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | Full Clear: X/4 captured. Sparkling: X/4 checked. Unique: checked counter only while discovery is needed and its row remains. Counts clamp to 4. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: no ordinary model ESP solely for automatic checking. Settings and model validity still apply. |
| Source helpers | Unique Haunted shows Gazers left to check while its discovery is needed and checking remains, even after the first catch removes its row. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of each spawned species; proven zero-spawn types exempt. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. Retained local Full Clear row proof survives leaving its radius; positive remaining population revokes local proof. A new run resets evidence; the ended run can remain displayed. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

4 shared captures can complete Full Clear progress before visiting Haunted; the missing row remains until local empty proof. Repeated sightings of one UUID never supply 4 checks.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Gazer` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [SafariCompletionZones.uuidMinimums](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionZones.kt).

Existing coverage: [SafariFullClearProgressTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClearProgressTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-29 — Gazer checks use the maximum of retained UUID evidence and confirmed shared captures plus current rendered uncaptured Gazers, capped at four. This covers instant catches before UUID observation. Proven progress is retained through unloading/reentry; Full Clear still requires actual captures.

2026-10-27 — Personal Unique PBs exempt positively proven zero-spawn types.

2026-10-26 — Removed ordinary ESP added solely for automatic Sparkling checks and its per-UUID linger. Capture/source and profitable-highlight exceptions remain separate.

2026-10-22 — Separated first-catch Unique rows from needed four-check progress and added the Haunted remaining-check footer.

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

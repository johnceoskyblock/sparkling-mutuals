# Doomspiral

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Haunted | Implemented roster |
| Rarity | Legendary | Supplied server notes; not live revalidated |
| Spawn source | Boss event | Implemented evidence path; mechanics unverified |
| Population model | Implemented capture minimum 1; server spawn mechanics unverified | Implemented rule, not server guarantee |
| Full Clear minimum | 1 shared capture before species adjustments | Implemented fallback/minimum |
| Local scan coverage | Biome scan; no species-specific center radius | Implemented coverage |

## What makes this critter different

- Sparkling needs actual boss-catch evidence, including the accepted global Sparkling-catch override; ritual, spawn and fade-away messages are not captures.

## Spawn prerequisites

Soothing Incense can come from grants/Haunted floor pickups. Holding incense, ritual/spawn signals and captures are separate; see [messages](MESSAGES.md).

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live warden / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Supplied notes say standard capsules can fail, replacement UUIDs may follow escape, and Masterful catches are guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Accepted global Sparkling-catch announcement | Confirmed-Sparkling species override without an ordinary shared-counter increment | Personal capture, personal PB, or other species captures |
| Spawn, awake/ritual and post-capture phase lines ([exact text](MESSAGES.md)) | Server phase signals described in supplied notes | Capture attribution or boss PB eligibility |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | One shared capture, or accepted global Sparkling-catch evidence through the confirmed-Sparkling override. Sightings/phase messages cannot substitute. | None beyond that catch evidence. | Biome reports cannot supply this capture-required check. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | None. | Does not supply a capture. |
| Full Clear | At least 1 shared capture. Local proof and report-based progress have [separate raised-target scopes](TRACKING.md#full-clear-evidence-and-late-entry). | Qualified empty local scan for row removal/local progress. | Without local proof, report plus capture minimum can complete progress; row still needs local proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | None in the species row; capture panel shows actual shared catches. |
| Ordinary ESP | Unique stops after first shared catch; Full Clear keeps eligible uncaught models. Sparkling requires needed discovery and no shared boss catch. |
| Source helpers | All five predicted incense locations are eligible under the existing mode/resource gates, without distance ranking or starting-item slot reduction. Four pickup units or having held four incense suppresses them for the run; Sparkling also needs Doomspiral discovery. See [FLOOR_DROPS](FLOOR_DROPS.md). |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. Boss Solo needs having held 4 Soothing Incense; own boss catch may update Duo. Ordinary attributed loot share can record Observed time only; Sparkling boss text misses the current dedicated PB matcher ([D15](../IMPLEMENTATION_AUDIT.md#d15)). |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. Retained local Full Clear row proof survives leaving its radius; positive remaining population revokes local proof. A new run resets evidence; the ended run can remain displayed. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

Fade-away without a boss catch leaves Sparkling Doomspiral pending. Loot share supplies the check, not a personal Solo PB.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Doomspiral` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [SafariCompletionZones.uuidMinimums](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionZones.kt).

[SafariPersonalBests.captureFeedback / soloEligible](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariPersonalBests.kt)

Existing coverage: [SafariPersonalBestTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariPersonalBestTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

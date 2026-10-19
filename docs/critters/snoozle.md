# Snoozle

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Cavern | Implemented roster |
| Rarity | Rare | Supplied server notes; not live revalidated |
| Spawn source | Snooper walls | Implemented evidence path; mechanics unverified |
| Population model | Variable released population; outcomes and encounters determine demand | Implemented rule, not server guarantee |
| Full Clear minimum | 0 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | Loaded sources sampled Safari-wide; local Full Clear still needs biome scan evidence | Implemented coverage |

## What makes this critter different

- Unique combines capture and source checking even when everyone owns this discovery.
- Without an encounter, resolved sources plus cd can confirm unseen zero spawn without inventing a catch.

## Spawn prerequisites

Resolve all five Snooper walls. Only third-stage wall dialogue confirms destruction; loaded blocks track each fixed position.

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live sniffer / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Supplied notes say standard capsules can fail, replacement UUIDs may follow escape, and Masterful catches are guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Loaded-world source state and terminal dialogue ([text owner](MESSAGES.md)) | Resolved source opportunity; third-stage wall line confirms destruction, not wall identity | A catch; unloaded/unknown sources are not automatically resolved |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary released critters; confirmed Sparkling capture override applies. | Resolve all five Snooper walls; no local critter-radius requirement. | cd can supply inherited checking for unknown sources; positive intact sources contradict it. |
| Unique | One shared capture if encountered; confirmed Sparkling capture overrides the combined check. | Resolve all five Snooper walls; zero captures additionally require no encounter and cd. | cd can supplement checking; cannot erase positive intact sources or supply a catch. |
| Full Clear | Fallback minimum 0 rises with encounters; local proof and report-based progress have [separate raised-target scopes](TRACKING.md#full-clear-evidence-and-late-entry). | Local empty scan and all five Snooper walls resolved. | Without local proof, report plus capture minimum can complete progress; row still needs local proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | Plain species row; separate wall footer including unknown states. |
| Ordinary ESP | Unique stops ordinary released-critter ESP after first shared catch. Full Clear keeps eligible uncaught models. Sparkling uses standard discovery/timeout filters for Snoozle. |
| Source helpers | Separate outstanding-source footer and highlights follow [panel rules](../MISSING_PANEL.md). Unique helper need after a catch is discovery-gated although progress still needs source proof. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Track loaded sources from any Safari biome. Cavern entry retires inherited checking; newly observed intact sources reopen the combined result without erasing captures. Reports cannot discharge positive source contradictions. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Retain mound sites and sampled wall states through unloading; unknown space is not declared broken. Intact-source observations revoke checking; captures persist. New-run/world lifecycle resets surveys. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

One shared Snoozle catch hides ordinary Unique ESP, but an intact source keeps progress/row pending. With no encounter, resolved sources alone remain insufficient for Unique until cd arrives.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entry: `Snoozle` in `SafariRun` and `SafariEspRules.mobs`; dedicated owners below.

[SafariStructures.tick](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/structures/SafariStructures.kt)

[MoundSurvey / SafariMounds / WallSummary](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/structures/SafariStructureRules.kt)

Existing coverage: [UniquePartyReportTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/UniquePartyReportTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

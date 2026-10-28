# Shyworm

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Cavern | Implemented roster |
| Rarity | Common | Supplied server notes; not live revalidated |
| Spawn source | Ordinary biome population; no dedicated spawn-event gate | Implemented evidence path; mechanics unverified |
| Population model | Implemented minimum; actual population can exceed it | Implemented rule, not server guarantee |
| Full Clear minimum | 4 shared captures before species adjustments | Implemented fallback/minimum |
| Local scan coverage | X/Z (-119, 43); Full Clear radius 30 blocks; Sparkling radius 40 blocks | Implemented coverage |

## What makes this critter different

- Supplied activity note describes an eight-cycle rotation, not eight seconds. The implemented post-hide rest display separately counts down 8 seconds; see [audit D4](../IMPLEMENTATION_AUDIT.md#d4).

## Spawn prerequisites

None. No species-specific resource or structure gate is used.

## Evidence

| Evidence | What it establishes | What it does not establish |
| --- | --- | --- |
| Personal capture / party loot share ([exact messages](MESSAGES.md)) | Own capture adds personal + shared evidence; attributed loot share adds shared evidence | Local checks, source resolution or personal PB from another player's catch |
| Live textured armor-stand head / qualified check | Current sighting; qualified evidence contributes under mode rule | Capture or absence inferred from missing outline |
| Supplied capsule notes | Common/Uncommon: supplied notes say no capsule escape; Masterful catches guaranteed once hit. | Server mechanics revalidated by this source audit; see [D2](../IMPLEMENTATION_AUDIT.md#d2) |
| Surfaced / hid / fled / startled lines ([exact text](MESSAGES.md)) | Activity/rest signal; missed-throw interruption differs from capsule escape | Capture, new worm or verified server hitbox |
| Retained qualified UUID check ([rules](TRACKING.md)) | Distinct checks reconciled with earlier captures | Historical sighting and its later catch counted twice |

## Completion by mode

| Mode | Capture requirement | Checking or source requirement | Party-report effect |
| --- | --- | --- | --- |
| Sparkling | None for ordinary critters; a confirmed Sparkling capture also completes the check. | 4 reconciled checks from qualified UUID sightings/shared captures; retain checks for the run. | Party cd can supply inherited checking; local scans retire it. |
| Unique | One shared capture, or the confirmed-Sparkling capture override. | None. | Does not supply a capture. |
| Full Clear | At least 4 shared captures. Local proof and report-based progress have [separate raised-target scopes](TRACKING.md#full-clear-evidence-and-late-entry). | Qualified empty local scan for row removal/local progress. | Without local proof, report plus capture minimum can complete progress; row still needs local proof. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear needs local capture/absence proof, separately from report-based progress. See [panel rules](../MISSING_PANEL.md). |
| Counters | None in the species row; capture panel shows actual shared catches. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: no ordinary model ESP solely for automatic checking. Settings and model validity still apply. |
| Source helpers | Hidden height/path/rest timer are estimates. Connected body-piece contact estimates stay separate from critter identity; see [capture guide](../CAPTURE_SHOT_RECORDING.md). |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of each spawned species; proven zero-spawn types exempt. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. Retained local Full Clear row proof survives leaving its radius; positive remaining population revokes local proof. A new run resets evidence; the ended run can remain displayed. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

A surfaced worm hides after an interrupted attempt: its rest display can count down, while captures stay unchanged. Connected body pieces are not additional worms.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Shyworm` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [CavernCompletionZones.zones](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/CavernCompletionZones.kt).

[ShywormProjection](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/ShywormProjection.kt)

[ShywormBodyDiagnostics](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/ShywormBodyDiagnostics.kt)

[ShywormContactParts](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/ShywormContactParts.kt)

Local body diagnostics: within roughly 8 blocks of an ordinary surfaced worm, leave capsule hiding off and run `/sm debug` three times about 1 second apart in one world. Inspect/share only `SM_SHYWORM_BODY` lines in `logs/latest.log`; no throw or recording is required and nothing uploads automatically. Selection inspects at most 4,096 entries, chooses two nearest heads within 24 blocks and up to 64 nearby/linked candidates; visual candidates are within 8 blocks of a head. Limits are reported, and candidate membership is unverified. The `contact_scene` row contains cached readiness counts. Positions/bounds are eye-relative; rows include numeric relationships, texture hashes, registry IDs, poses/transforms, but omit players, names, UUIDs, raw profiles, arbitrary item data and text content.

Supplied fitting evidence: the clip shows body pieces beyond the head proxy; its recorder export had no native head bounds. Four focused snapshots supported seven separate textured armor-stand head-items, including raised trailing pieces. Provisional contact support requires bounded, unambiguous connection to a current head; it does not use allocation IDs as ownership, bridge visual gaps, predict motion or register extra worms. Stale/ambiguous/unsupported geometry remains unavailable. These observations do not establish a server capture hitbox; independent gameplay confirmation remains pending.

Existing coverage: [ShywormProjectionTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/esp/ShywormProjectionTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Independent live body-contact validation remains pending; supplied clip/focused snapshots are fitting evidence, not server hitbox confirmation.

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Captured model cleanup

Underground or unpaired heads do not count as live remaining population. Surface evidence is required before retaining a last-seen location. Confirmed captures retire an associated UUID family. If the capture cannot identify a UUID, uncertain absent/hidden locations are discarded through the following packet-settling scans; this is visual cleanup, not an inferred capture. A surviving worm can be observed again when it resurfaces, including under the same UUID. Personal and loot-share captures both trigger cleanup. Capture-transition observations do not inflate required captures during the 2.5-second settling window.

## Last updated

2026-10-28 - Fixed late-entry model ghosts and capture cleanup.


2026-10-28 — Suppress retained last-seen markers after confirmed capture completion. Body-segment hits can identify a head only through an unambiguous bounded component; captures retire replacement UUID aliases, and stale captured models cannot recreate markers. Uncaught cooldown markers still persist.

2026-10-27 — Personal Unique PBs exempt positively proven zero-spawn types.

2026-10-27 — Entity disappearance now retains a visual-only last-seen box at the observed surface height, without an eight-second expiry. The same UUID returning or one unambiguous nearby replacement surface UUID replaces the marker with live ESP. An associated confirmed capture removes it, and run/world resets clear all retained markers. Existing mode and biome visibility settings still apply.

2026-10-27 — Underground loaded Shyworms retain their ESP at the previously observed surface height after name-tag expiry. This visual fallback adds no completion/contact evidence. Capture retirement, unloading and run resets remove the marker.

2026-10-26 — Removed ordinary ESP added solely for automatic Sparkling checks and its per-UUID linger. Capture/source and profitable-highlight exceptions remain separate.

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

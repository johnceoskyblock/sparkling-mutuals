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
| PB eligibility | Personal Unique: own catch of each spawned species; proven zero-spawn types exempt. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. Boss Solo requires holding 4 Soothing Incense before the own boss capture; otherwise it records Duo. Later inventory cannot upgrade that capture. Observed Doomspiral is assumed Solo. Ordinary and explicitly SPARKLING capture/reward messages are supported; see [PB rules](../PERSONAL_BESTS.md). |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. Retained local Full Clear row proof survives leaving its radius; positive remaining population revokes local proof. A new run resets evidence; the ended run can remain displayed. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

Fade-away without a boss catch leaves Sparkling Doomspiral pending. Loot share supplies the check, not a personal Solo PB.

## Capsule throw tick timer

Safari helpers > Capture tools contains **Doomspiral capsule throw tick timer** (on by default). The exact system line `Something stirs in the Haunted Biome...` starts one countdown per run, only inside Safari. The HUD is visible only when a living, loaded warden is within 10 blocks of the player (3D entity-position distance). The countdown continues while hidden, so moving into range does not restart it. Quoted party chat and overlay messages cannot trigger it.

The default emergence delay is **98 server ticks**, with a **4-tick throw lead**, giving a 94-tick countdown. Both values are adjustable and saved custom values are preserved. Projectile flight time depends on distance and connection timing.

The timer passively counts nonzero server ping packets, following the clock approach inspected in NoammAddons 1.3.1's Storm PY timer (CC0). Vanilla ping processing and pong responses remain unchanged. No new packets are sent. Seconds are displayed as ticks / 20; actual elapsed time stretches under server lag. At zero it shows **THROW NOW** for up to 40 server ticks. This is a visual cue; it never throws a capsule or supplies capture evidence.

A confirmed shared Doomspiral capture clears the cue. Disabling it, leaving Safari, ending/changing runs, changing world/connection or disconnecting clears the timer. Duplicate trigger messages cannot restart it within the same uninterrupted run context. Move/resize its independent HUD using `/sm gui`; panel styling is under Customization > Tracking HUDs > Doomspiral throw timer HUD.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Doomspiral` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [SafariCompletionZones.uuidMinimums](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionZones.kt).

[SafariPersonalBests.captureFeedback / soloEligible](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariPersonalBests.kt)

Existing coverage: [SafariPersonalBestTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariPersonalBestTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-29 — Updated emergence/throw-lead defaults to 98/4 ticks.

2026-10-29 — Restricted the throw countdown HUD to within 10 blocks of a loaded warden; ticking and HUD-editor preview are unchanged.

2026-10-29 — Added the server-tick capsule throw HUD, adjustable provisional 75-tick emergence estimate and 3-tick throw lead.

2026-10-27 — Personal Unique PBs exempt positively proven zero-spawn types.

2026-10-26 — Clarified Solo/Duo timing and observed boss classification.

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

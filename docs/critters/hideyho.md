# Hideyho

[Index](README.md) · [Terminology](../DOCUMENTATION_GUIDE.md) · [Shared evidence](TRACKING.md)

## Overview

| Property | Value | Status |
| --- | --- | --- |
| Biome | Haunted | Implemented roster |
| Rarity | Rare | Supplied server notes; not live revalidated |
| Spawn source | Hide-and-seek quest/reward | Implemented evidence path; mechanics unverified |
| Population model | Implemented minimum; actual population can exceed it | Implemented rule, not server guarantee |
| Full Clear minimum | 1 confirmed successful find/reward; no model-based target adjustment | Implemented fallback/minimum |
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
| Full Clear | One confirmed successful find/reward (shared capture or attributed loot share). | The reward completes the quest; lingering NPC models, UUID changes and absence scans do not add requirements. Personal PBs still require the personal reward. | A report alone supplies no successful find. |

## Display and highlights

| Outcome | Implemented behavior |
| --- | --- |
| Progress | Mode-specific predicate above; see [shared modes](../../SAFARI_LOGIC.md). |
| Missing/unchecked row | Unique uses its capture/source predicate. Sparkling lists needed unresolved checks. Full Clear removes the row on a confirmed successful reward/attributed loot share, without waiting for the NPC to disappear. See [panel rules](../MISSING_PANEL.md). |
| Counters | None in the species row; capture panel shows actual shared catches. |
| Ordinary ESP | Unique: until first shared capture. Full Clear: eligible uncaught models. Sparkling: no ordinary model ESP solely for automatic checking. Settings and model validity still apply. |
| Source helpers | Location announcement uses settled known spots/mansion footprint, not capture proof. |
| Automatic completion announcements | Shared local whole-biome/report-readiness gates in [mode rules](../../SAFARI_LOGIC.md). |
| PB eligibility | Personal Unique: own catch of every species. Full Clear: personal captures/local proof. Party Unique: separate shared-capture/absence predicate; progress alone is insufficient. |

## Party reports and late entry

Captures received before visiting this biome remain in the run. Reports supply checking trust, never missing capture events. For late entry, use retained shared captures plus qualified local coverage; absence alone is insufficient. See [evidence/lifecycle](TRACKING.md).

## Retention and reopening

Captures and qualified UUID checks persist through unloading/travel. A confirmed quest reward retains Full Clear completion for this run; a leftover or replacement NPC model cannot revoke it. A new run resets evidence; the ended run can remain displayed. Pending detection identities add no separate completion gate; see [audit D1](../IMPLEMENTATION_AUDIT.md#d1).

## Examples

One party loot share completes Unique and shared Full Clear Hideyho. Quest acceptance or ordinary dialogue without a capture/reward does not complete it.

## Implementation and validation

[Shared source/caller map](TRACKING.md#source-and-validation-references) covers capture/event registration, scanner, mode predicates, UI/report consumers, PBs and resets. Species entries: `Hideyho` in `SafariFullClear.minimum` and `SafariEspRules.mobs`; [SafariCompletionZones.uuidMinimums](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionZones.kt).

[HideyhoSpotter / HideyhoLocationWatch](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/hideyho/HideyhoSpotter.kt)

Existing coverage: [HideyhoSpotterTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/hideyho/HideyhoSpotterTest.kt). Source inspection only; no local runtime suite or live Minecraft validation was performed for this documentation audit. CI results are separate.

## Open questions

Population guarantees and capsule success/escape claims from supplied notes were not live revalidated; see [audit D2](../IMPLEMENTATION_AUDIT.md#d2).

## Last updated

2026-10-26 — Removed ordinary ESP added solely for automatic Sparkling checks and its per-UUID linger. Capture/source and profitable-highlight exceptions remain separate.

2026-10-21 — Confirmed Hideyho rewards now complete Full Clear directly, including missing-row removal and capture coloring. Retained quest NPCs and replacement UUID population estimates cannot require additional finds. The completed NPC is excluded from ordinary ESP and remaining-population scans; quest acceptance alone still does not count. Shared and personal reward evidence remain separate.

2026-10-20 — Removed the Hideyho quest-click setting and chat mouse interception; auto-accept is the only acceptance helper.

2026-10-20 — Added Auto-accept Hideyho (enabled by default). Inside Safari, the validated Hideyho dialogue and Sure button send the supplied RunCommand/custom action without opening chat. Connection/world changes and prompt expiry cancel pending offers; accepting consumes the prompt. The former click-anywhere chat feature has been removed. Disable auto-accept to use Hypixel’s original quest buttons.

2026-10-19 (America/New_York) — Uniform species reference; audited evidence, modes, display and lifecycle. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; no local runtime source changes reviewed. See [revision convention](../DOCUMENTATION_GUIDE.md).

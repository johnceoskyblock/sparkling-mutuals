# Rockmite

[Index](README.md) · [Shared evidence rules](TRACKING.md)

**Biome:** Cavern. **Recorded Full Clear baseline:** Variable; determine from actual spawns. Baseline counts are carried from earlier notes, not newly verified server guarantees.

## Evidence

- **Rare: can escape a standard capsule; Masterful catches are guaranteed once hit.** An escaped critter may return with a replacement UUID; that alone does not prove an additional Rockmite spawn.
- Your capture confirmation adds one personal and one shared catch. A party loot-share confirmation adds one shared catch, attributed to its catcher.
- Keep any captures received before visiting Cavern; subtract them from the remaining requirement once, as described in the shared rules.

## Completion

| Mode | Rule |
| --- | --- |
| Unique | One shared Rockmite catch plus every mound resolved, from any Safari biome. Party `cd` can supply checking evidence when sources are unknown; known intact sources block. If no actual Rockmite was encountered, resolved sources plus `cd` confirm the zero-spawn exception without adding a catch. |
| Full Clear | Local capture/source proof and a qualified empty scan advance progress and remove the missing row without a report; retain that proof after leaving. Other biomes without local proof use party report plus minimum captures, with existing fixed-count exceptions. Positive remaining critters reopen local completion. See the [per-critter panel reference](../MISSING_PANEL.md). |
| Sparkling | Resolve every mound anywhere in Safari; no ordinary catch or local critter scan required. Any positively detected uncaptured Sparkling still blocks completion. |

## Specific deductions

- Mounds and walls are observable from the initial boat exit and remain rendered safari-wide. Retain each source for the run, sample all rendered mounds without the old 64-block tracking limit, and retain wall states across chunk unloading. Unloaded/unknown sources are never inferred broken. Full Clear retains its capture, local-proof and party-report rules.

- Local Full Clear checks advance progress and remove the missing row without a report. Remote progress without local proof retains its report gate and existing fixed-count exceptions.

- Unique ordinary silverfish ESP stops after the first shared Rockmite catch. If anyone still needs Sparkling Rockmite, keep mound ESP until all mounds are broken; overall Unique progress also requires that source check.
- Keep source requirements and highlights active while needed, but show only the plain Rockmite row in the missing/check panel. Retain the separate outstanding-source footer according to mode and this species' party discovery requirements. Unique retains this checking row after its catch while source validation is unfinished.
- On Unique biome entry, newly found unbroken mounds reopen Rockmite's combined check and Cavern completion regardless of discovery ownership. Keep the recorded catch and silverfish-ESP suppression; party completion messages do not control this reevaluation.
- Partial-clear entry: minimum shared captures + qualifying UUID-check radius + no remaining critters/spawns establishes Full Clear without seeing captured UUIDs again. Apply species-specific source checks and [shared radius rules](TRACKING.md#partially-cleared-biome).
- Track each mound separately. The four cracking/progress messages do not prove that it broke or released a critter.
- Terminal revealing message: one Rockmite spawned; track its unique silverfish UUID when visible. Terminal empty message: that mound released none.
- Capsule-contact guides exclude the identified mound item display from critter targets. Supported mound displays can receive an amber provisional surface estimate derived from the loaded model and transforms; this does not prove server projectile collision or make the mound capturable. The amber marker can wrap the mound/adjacent terrain faces. A released silverfish Rockmite remains eligible for the cyan critter diamond; mound ESP/source tracking stays independent.
- Full Clear requires every mound resolved and every released Rockmite captured. Sparkling checking requires every mound outcome/opportunity resolved; a released ordinary Rockmite need not be caught just to identify its sparkle status.
- Resolved sources with zero catches remain incomplete in Unique until party `cd` confirms the unseen no-spawn case. A positively encountered ordinary Rockmite still requires a shared catch; no report manufactures captures.

## Last updated

2026-10-17 - Rockmite/Snoozle use retained safari-wide sources: Unique catch plus source proof, report-confirmed unseen zero spawn, and Sparkling checks without ordinary captures.

2026-10-17 - Local Full Clear completion also advances progress; other biomes without local proof retain report requirements.

2026-10-17 - Separated local Full Clear missing-panel proof from report-based progress.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-16 - Aligned Unique party-report checking with real per-species captures and limited Cavern source contradictions.

2026-10-15 - Restored mode- and discovery-gated nest/wall/mound helper footers; kept plain critter rows and removed duplicate Gazer counters.

2026-10-17 - Excluded mound displays from capsule-contact candidates while retaining released Rockmites and existing source/ESP rules.

2026-10-17 - Added provisional visual mound surface estimates and wrapping amber markers, while excluding mounds from critter diamonds and retaining source/ESP rules.

# Missing, Unchecked and progress panels

## Scope

Authoritative UI reference for species-row inclusion, counters, capture colors and source footers. Gameplay completion belongs to [SAFARI_LOGIC](../SAFARI_LOGIC.md); evidence and scan zones belong to [tracking](critters/TRACKING.md). Species-specific requirements appear once in the [37 species pages](critters/README.md), rather than a second roster of numeric quotas here.

## Definitions

A **missing row** is an unfinished species entry, not an inventory/capture total. **Progress** counts completed active-mode species. **Capture colors** consume Full Clear progress and verification state. A **source footer** counts outstanding interactions independently of species rows.

“Unique with a needed Sparkling discovery” is a condition within Unique, not a fourth selectable mode. Common vocabulary follows [the glossary](DOCUMENTATION_GUIDE.md).

## Implemented rules

### Progress and species rows

| Mode | Progress/denominator | Species-row inclusion |
| --- | --- | --- |
| Sparkling | Only party-needed discoveries; ? while discovery state is not ready. No targets displays a complete zero-length bar | Needed species whose sparklingComplete is false, plus uncaught Icy prerequisites while Wumpa is needed and uncaught |
| Unique | All species; uniqueComplete, plus needed Honeybug checking for progress | Species whose uniqueComplete is false; Honeybug's first catch removes its row before its checking-dependent progress can complete |
| Full Clear | All species; fullClearComplete | Species whose fullClearMissingComplete is false |

Full Clear progress can use remote report/capture minima or fixed capture targets while a local missing row remains. A report alone cannot remove that row. Conversely, local capture/source/qualified absence proof can advance both without a report. Qualified absence proof is retained after travel until positive remaining evidence revokes it.

Plain species names use their species colors. No checking prose, absence notes or duplicate Gazer footer is appended. When no species remain, Sparkling says **All checked!**; Unique/Full Clear say **All caught and checked!**. A separate source footer may still appear below that sentence.

In Sparkling, title/editor label is **Unchecked** in every biome. Unique/Full Clear use the existing Missing setting/editor position; the panel title itself uses the biome and “Biome — N left”. Changing mode preserves saved position/settings.

### Fixed counters

Only Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4 have a species-row X/N counter. Counts are clamped to the target.

| Mode | Counter |
| --- | --- |
| Sparkling | X/N checked when its discovery is needed |
| Unique | X/N checked when its discovery is needed, while its species row remains |
| Full Clear | X/N captured using shared catches |

Checked counters use retained/reconciled evidence, not just simultaneous live entities; Gemzie's timer can supply three checked. Counter text establishes no additional requirement by itself. Checked evidence never increments a captured counter.

### Capture colors

Capture panels always display actual shared counts. With Full Clear capture coloring enabled, a verified completed species is green and a verified incomplete species red; unverified rows remain white. Verification is an available biome scan, received report, known Honeybug nest count, or a positive fixed-target capture count.

Thus fixed-target/report-backed green progress can coexist with a missing local species row. Actual captures remain unchanged by color, detection, reports or source helpers.

### Ordinary ESP

Ordinary ESP has its own entity-current, model-label/movement, biome/group and per-species configuration gates. Full Clear normally selects every eligible uncaptured critter; Unique normally selects uncaught species. The profitable-species toggle can additionally retain Hideonfloor, Hideonwall, Chuckwalla, Fluffling and Mantis Shrimp.

Sparkling ordinary ESP normally requires the discovery and uses a ten-second per-UUID display window. Driftling has no such timeout; ordinary Honeybug and released Rockmite are suppressed. Needed Doomspiral stays eligible until a shared catch. Source mounds follow their unfinished checking/helper eligibility. A timer expiring hides an outline without deleting qualified evidence or proving capture.

Highest priority: while the party needs Wumpa, every Icy species with zero shared catches is selected before normal mode/timeout eligibility. Its own first catch ends that override only for that species; group/entity/biome controls still apply. A Wumpa catch can complete all Icy Sparkling checks without supplying other species' first catches, so their forced ordinary ESP may remain while Wumpa discovery state is still needed.

### Source footers and helpers

| Footer | Full Clear | Unique | Sparkling |
| --- | --- | --- | --- |
| Bee nests to punch | Until all nests checked or captures reach known nest quota; fd/live bees alone do not suppress it | Until locally resolved or trusted fd; after first Honeybug catch, keep only if its discovery is needed | Only when Honeybug is needed and local/trusted checking remains unresolved |
| Snooper walls to break | Known wall state exists and not all five broken | Snoozle incomplete and either uncaught or discovery needed | Needed incomplete Snoozle |
| Mounds to break | Positive remaining mound count | Rockmite incomplete and either uncaught or discovery needed | Needed incomplete Rockmite |

Footers appear beneath the species list with gray counts. Unknown wall coverage is preserved. Resolved sources disappear; a punched nest loses its individual waypoint immediately. The nest count helper and nest highlight use the same mode eligibility, while actual waypoint presence depends on unpunched known nests. A helper can remain after the species row clears.

There is no separate bird-food or Gazer-check footer. Ordinary ESP, detected Sparkling glow and source helpers have independent render/config gates; [SAFARI_LOGIC](../SAFARI_LOGIC.md) and [detection](SPARKLING_DETECTION.md) distinguish their outcomes.

## Exceptions

| Species/group | Visible distinction |
| --- | --- |
| Honeybug | Unique first catch removes its row; needed checking may still hold progress and nest footer |
| Rockmite/Snoozle | Their Unique rows wait for applicable source/report conditions, even after a catch |
| Birds | Shared spawning/capture formulas govern rows and progress; [bird policy](BIRD_COMPLETION.md) owns the exceptions |
| Icy prerequisites | Extra prerequisite-only rows are outside Sparkling's progress denominator; a shared Wumpa catch clears every Icy Sparkling row |
| Detected Sparkling | Detection alone adds no row and revokes no completion; normal positive remainder/source evidence is evaluated separately |

## Examples

- Three shared Scrappy catches can show Full Clear progress complete/green while Scrappy remains listed until qualified absence proof.
- Honeybug can be absent from the Unique species list while its nest footer and progress check remain unfinished.
- One Gazer catch followed by three qualified new live Gazers can show 1/4 captured in Full Clear and 4/4 checked in Sparkling. Revisiting an already checked UUID adds nothing.
- A needed Wumpa can put an otherwise-owned uncaught Icy species in Unchecked without increasing the progress denominator.

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee.

- [SafariPanels](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/ui/SafariPanels.kt): progress, missingCritters (also used by !m), missing, fixedCounter and captures.
- [SafariRun](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt): completed, uniqueComplete, fullClearComplete, fullClearMissingComplete and nestCounterNeeded; [SafariHelperRules](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariHelperRules.kt): independent helper gates.
- [SafariTracking](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariTracking.kt) supplies runtime HUD consumers; [SafariBiomeClearTracking](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariBiomeClearTracking.kt) collects evidence independently of HUD visibility.
- Existing checks: [SafariPanelSeparationTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/ui/SafariPanelSeparationTest.kt), [NestCounterPanelTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/ui/NestCounterPanelTest.kt), [FullClearMissingPanelTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/FullClearMissingPanelTest.kt). Not run locally in this audit.

## Open questions

The older Billygoat proposal expected Unique progress to wait for two checks after its first catch. Current uniqueComplete uses the same ordinary first-catch result for row/progress. This proposal/code distinction is recorded in [audit D13](IMPLEMENTATION_AUDIT.md#d13), not presented as implemented. Live layout/entity validation remains pending.

## Last updated

2026-10-19 (America/New_York) — Replaced duplicate species quotas with UI ownership and explicit row/progress/helper distinctions. Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee; static source/caller review, no live Minecraft validation.

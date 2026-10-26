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
| Unique | All species; uniqueComplete plus needed Honeybug checking; needed Gazer/Gimmiegold also require their discovery checks | Uncaught species, except locally proven zero-spawn Bluebird, Parakeet, Macaw, Rockmite and Snoozle. Every first shared catch removes its row immediately; source/checking requirements remain in progress |
| Full Clear | All species; fullClearComplete | Species whose fullClearMissingComplete is false |

Full Clear progress can use remote report/capture minima or fixed capture targets while a local missing row remains. A report alone cannot remove that row. Conversely, local capture/source/qualified absence proof can advance both without a report. Qualified absence proof is retained after travel until positive remaining evidence revokes it.

In Unique mode, locally proven zero-spawn Bluebird, Parakeet, Macaw, Rockmite and Snoozle are removed from the species list. Each bird requires resolved bird feeding including known party extras and no encounter. Rockmite/Snoozle require fully resolved mounds/walls and a local Cavern absence scan with no encounter. Their absence also counts toward progress and local biome announcements; actual capture counts remain zero.

Plain species names use their species colors. Species rows stay plain; outstanding source/check work appears in separate footers. When no species remain, Sparkling says **All checked!**; Full Clear says **All caught and checked!**. Unique says **All caught!** while source/progress work remains, then **All caught and checked!** once progress also completes. A separate source footer may still appear below that sentence.

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

Ordinary ESP has its own entity-current, model-label/movement, biome/group and per-species configuration gates. Full Clear normally selects every eligible uncaptured critter; Unique normally selects uncaught species. In Unique and Sparkle modes, the profitable-species toggle additionally retains the cached `/profitablecritters` top ten by estimated coins per run, excluding Scrappy, Rockmite and Troodon. It follows the same price preference and quote updates without new API requests; unavailable prices provide no extra profitable highlights. See [profitability](PROFITABLE_CRITTERS.md#profitable-shard-esp).

Sparkling ordinary ESP normally requires the discovery and uses a ten-second per-UUID display window. Driftling has no such timeout; ordinary Honeybug and released Rockmite are suppressed. Needed Doomspiral stays eligible until a shared catch. Source mounds follow their unfinished checking/helper eligibility. A timer expiring hides an outline without deleting qualified evidence or proving capture.

Highest priority: while the party needs Wumpa, every Icy species with zero shared catches is selected before normal mode/timeout eligibility. Its own first catch ends that override only for that species; group/entity/biome controls still apply. A Wumpa catch can complete all Icy Sparkling checks without supplying other species' first catches, so their forced ordinary ESP may remain while Wumpa discovery state is still needed.

### Source footers and helpers

| Footer | Full Clear | Unique | Sparkling |
| --- | --- | --- | --- |
| Bird food left to use | Not shown | Remaining feeding demand including known party extras; at least known held plus floor food | Same count, only when Bluebird, Parakeet or Macaw is needed and feeding remains unresolved |
| Bee nests to punch | Until all nests checked or captures reach known nest quota; fd/live bees alone do not suppress it | Until locally resolved or trusted fd; after first Honeybug catch, keep only if its discovery is needed | Only when Honeybug is needed and local/trusted checking remains unresolved |
| Snooper walls to break | Known wall state exists and not all five broken | Snoozle incomplete and either uncaught or discovery needed | Needed incomplete Snoozle |
| Mounds to break | Positive remaining mound count | Rockmite incomplete and either uncaught or discovery needed | Needed incomplete Rockmite |
| Coins left to use | Not shown | Only when Gimmiegold is needed and local coin checking remains: uncollected floor slots/held coins and outstanding required spawns; ? while catalog/inventory is unknown | Same needed-Gimmiegold counter until local checking resolves |
| Gazers left to check | Not shown | Only when Gazer is needed and unresolved: four minus retained reconciled sightings/catches | Same needed-Gazer counter until local checking resolves |

Footers appear beneath the species list with gray counts. Unknown wall coverage is preserved. Resolved sources disappear; a punched nest loses its individual waypoint immediately. The nest count helper and nest highlight use the same mode eligibility, while actual waypoint presence depends on unpunched known nests. A helper can remain after the species row clears.

In Unique, Haunted footers remain after their first catches remove the species rows. Unique and Sparkle Haunted footers disappear after the needed checks resolve. Sparkle shows coin, Gazer, mound, Snoozle-wall, bird-food and bee-nest counters only for the corresponding needed discovery (any needed bird enables bird food); resolved source work hides its counter. Unique Rockmite silverfish ESP ends on the first catch; mound ESP stays on before that catch or while anyone still needs its discovery. A new run restores first-catch eligibility. Ordinary ESP, detected Sparkling glow and source helpers have independent render/config gates; [SAFARI_LOGIC](../SAFARI_LOGIC.md) and [detection](SPARKLING_DETECTION.md) distinguish their outcomes.

## Exceptions

| Species/group | Visible distinction |
| --- | --- |
| Hideyho | Confirmed quest reward completes Full Clear immediately; leftover NPCs do not keep its row or raise its target |
| Honeybug | Unique first catch removes its row; needed checking may still hold progress and nest footer |
| Rockmite/Snoozle | Unique rows disappear on first shared catch, or on qualified local zero-spawn proof |
| Birds | Unique rows track first shared captures separately from spawning-dependent progress; [bird policy](BIRD_COMPLETION.md) owns the exceptions |
| Icy prerequisites | Extra prerequisite-only rows are outside Sparkling's progress denominator; a shared Wumpa catch clears every Icy Sparkling row |
| Detected Sparkling | Detection alone adds no row and revokes no completion; normal positive remainder/source evidence is evaluated separately |

## Examples

- Three shared Scrappy catches can show Full Clear progress complete/green while Scrappy remains listed until qualified absence proof.
- Honeybug can be absent from the Unique species list while its nest footer and progress check remain unfinished.
- One Gazer catch followed by three qualified new live Gazers can show 1/4 captured in Full Clear and 4/4 checked in Sparkling. Revisiting an already checked UUID adds nothing.
- A needed Wumpa can put an otherwise-owned uncaught Icy species in Unchecked without increasing the progress denominator.

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee.

- [SafariPanels](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/ui/SafariPanels.kt): progress, missingCritters (GUI only; !m uses SafariMissingReply), missing, fixedCounter and captures.
- [SafariRun](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt): completed, uniqueComplete, fullClearComplete, fullClearMissingComplete and nestCounterNeeded; [SafariHelperRules](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariHelperRules.kt): independent helper gates.
- [SafariTracking](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariTracking.kt) supplies runtime HUD consumers; [SafariBiomeClearTracking](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariBiomeClearTracking.kt) collects evidence independently of HUD visibility.
- Existing checks: [SafariPanelSeparationTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/ui/SafariPanelSeparationTest.kt), [NestCounterPanelTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/ui/NestCounterPanelTest.kt), [FullClearMissingPanelTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/FullClearMissingPanelTest.kt). Not run locally in this audit.

## Open questions

The older Billygoat proposal expected Unique progress to wait for two checks after its first catch. Current uniqueComplete uses the same ordinary first-catch result for row/progress. This proposal/code distinction is recorded in [audit D13](IMPLEMENTATION_AUDIT.md#d13), not presented as implemented. Live layout/entity validation remains pending.

### Remote report progress

Outside the current biome, a party completion report is trusted even after an earlier visit. Unique retains confirmed first captures, except unencountered zero-spawn birds, Rockmite and Snoozle. Full Clear retains actual capture minimums, known nest/coin targets and its bird capture formula. Current-biome scans remain authoritative. A report cannot invent capture counts or local missing-panel proof.

## Party command: !m

Any party member may request `!m` or `!m <biome>` (including c/f/i/h). A mod client only answers while inside the requested biome; bare `!m` selects that client's current biome. Queued answers are discarded if the responder leaves the biome, run, world or mode. Existing command cooldowns, party-response deduplication and chunk pacing apply; this command makes no API requests.

- **Full Clear:** concise `Critter: remaining` entries use known populations where available, otherwise the species minimum, minus shared captures. Honeybug uses the retained nest-derived target; birds use known spawn totals with the one-Macaw exception. Zero or negative remainders are omitted. Cavern also lists rendered `Mounds` and `Snoozle Walls`; Forest lists food left to use.
- **Unique:** uncaught species only, preserving proven zero-spawn exceptions. Extra check notes appear only for species the party needs shiny. If shiny Honeybug is needed, always include `Make sure to clear all nests`, even after catching one.
- **Sparkle:** outstanding checks only for needed species: nests, mounds, Snoozle Walls, bird food and checking birds, Shining Coins and checking Gimmiegolds, Wumpa/Doomspiral captures, and remaining species checks. Completed checks are omitted.

The compact party reply uses `Missing <Biome>: ...`, `Done` when nothing remains, and ordinary `shiny` wording rather than the detection-alert keyword. Unknown discovery data is reported as loading rather than complete. The GUI missing panel keeps its existing presentation; the command now has its own mode-aware formatter.

## Last updated

2026-10-25 — Sparkle now includes needed-Gimmiegold coins, needed-Gazer checks and needed-bird food counters alongside the existing discovery-gated mound, wall and nest helpers.

2026-10-25 — Profitable-shard ESP uses the shared dynamic top ten in Unique and Sparkle modes instead of a fixed species list.

2026-10-24 — Added mode-specific, current-biome-only !m replies with capture deficits and source/check requirements.

2026-10-22 — Clarified remote Unique/Full Clear progress and current-biome scan authority.

2026-10-22 — Added Unique Haunted coin/Gazer footers and checking-dependent progress; clarified discovery-gated Rockmite mound ESP after the first catch.

2026-10-21 — Hideyho Full Clear now treats the confirmed quest reward as terminal completion: remove its missing row and allow green capture/progress state without NPC absence or additional model-derived captures.

2026-10-20 — All three bird types support zero-spawn completion: Unique requires resolved feeding (including known party extras), qualified Forest survey and no catch/sighting of that species. Full Clear retains the actual-population capture formula, which allows zero Bluebirds/Parakeets when the other captures cover all feeding opportunities. No catches are fabricated.

2026-10-20 — Unique is a capture checklist independent of source progress. Added Forest bird-food footer; empty Unique checklist says All caught! while progress still has work.

2026-10-19 - Documented local Unique zero-spawn row removal, progress and biome reports for Macaw, Rockmite and Snoozle.

2026-10-19 (America/New_York) — Replaced duplicate species quotas with UI ownership and explicit row/progress/helper distinctions. Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee; static source/caller review, no live Minecraft validation.

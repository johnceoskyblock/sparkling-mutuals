# Safari modes and completion

## Scope

Authoritative reference for implemented mode outcomes, announcement eligibility and PB eligibility. Read [the glossary and documentation conventions](docs/DOCUMENTATION_GUIDE.md) first. [Tracking](docs/critters/TRACKING.md) owns evidence and party reconciliation; [species pages](docs/critters/README.md) own species exceptions and numeric values. These describe client implementation, not guaranteed Hypixel mechanics.

## Definitions

**Sparkling**, **Unique** and **Full Clear** are the selectable modes. A needed discovery is one not established as owned by every member of the selected party discovery state. Unknown coverage is not proof everybody owns it.

**Progress completion**, **missing-row removal**, **ordinary ESP**, **source-helper visibility**, **automatic announcements** and **PB eligibility** are separate outcomes. An empty species list does not establish every other outcome. See [panel behavior](docs/MISSING_PANEL.md).

## Implemented rules

### Mode completion

| Mode | Progress requirement | Missing/checklist requirement |
| --- | --- | --- |
| Sparkling | Complete checks for party-needed discoveries; unknown party state shows ? | Needed unfinished species, plus uncaught Icy prerequisites while Wumpa is needed and uncaught |
| Unique | One shared catch per ordinary species, with exceptions below | uniqueComplete; Honeybug's first catch removes its row even when progress still needs checking |
| Full Clear | Local capture/source proof, or report-backed capture minima; fixed-target species can complete progress early | Local capture/source proof and qualified absence coverage; a report alone cannot remove the row |

Ordinary Sparkling checks use retained qualified UUID evidence reconciled with shared catches. Ordinary Unique completion adds no discovery scan/radius requirement. Full Clear uses actual catches, known population targets, source conditions and qualified empty scans; [tracking](docs/critters/TRACKING.md) explains those evidence paths.

Detected Sparkling identities impose **no additional capture gate in any mode**. Confirmed Sparkling catch evidence, including accepted global announcements, overrides ordinary Unique/Sparkling species completion. A global announcement alone supplies no ordinary capture count or personal PB evidence. Birds are evaluated before this override and retain their shared spawning rules. Full Clear retains its normal rules. [Detection](docs/SPARKLING_DETECTION.md) owns identification and notifications.

### Reports and run-complete alerts

Automatic fd, cd, id and hd announcements require current presence in that biome, a capture-evidence scan and all active-mode targets complete under localOnly. Sparkling also requires ready party discoveries. Unique suppresses its own report after receiving that biome's report. [Forest reporting](docs/BIRD_COMPLETION.md#sending-fd) adds clearer/supply requirements.

Queued reports are rechecked against current mode, biome and local requirements before sending. Sends are spaced by at least three seconds; resource reminders share pacing and may delay them. Announcement memory prevents duplicates without adding catches.

The configured run-complete alert requires all active-mode targets complete, a nonempty target set and, in Sparkling, ready party state. It can use remote progress; it does not certify a personal clear.

### Personal bests

| Record | Implemented eligibility |
| --- | --- |
| Personal Unique biome | A personal catch of every species; no absent-species waiver |
| Party Unique biome | Each species has a shared catch or local uniqueUnavailable absence proof; checked after catches/scans while Unique is selected |
| Personal Full Clear biome | Observed scan, personal capture/source requirements and no remaining critters, except permitted remaining Macaws |
| Doomspiral Solo | Personal boss capture after holding four incense at once during the run |
| Wumpa Solo | Personal boss capture plus a personal catch of every other Icy species |
| Boss Duo / observed party | Personal boss capture for Duo; accepted attributed party boss text for observed records |

Party Unique PBs do not call uniqueComplete: local Rockmite/Snoozle absence can qualify without cd, and a first catch does not add their progress source gates. Boss PB parsing also uses its own matcher, which does not accept the SPARKLING modifier in the ordinary boss-capture position. See [Party Unique PB differences D14](docs/IMPLEMENTATION_AUDIT.md#d14) and [boss matcher D15](docs/IMPLEMENTATION_AUDIT.md#d15).

Changing mode selects presentation presets and PB category without clearing run catches or qualified evidence. A new run creates a fresh ledger.

## Exceptions

| Species/group | Completion exception |
| --- | --- |
| Bluebird, Parakeet, Macaw | Food/population rules and no-Macaw/one-Macaw exceptions: [bird completion](docs/BIRD_COMPLETION.md) |
| Honeybug | Needed checking gates Unique progress, but the first catch removes its row. Full Clear uses the known nest population when available |
| Rockmite, Snoozle | Unique adds resolved sources or accepted unknown-source report trust; resolved sources plus cd may confirm unseen zero spawn. Positive intact sources contradict trust |
| Wumpa, Doomspiral | Needed Sparkling checks require actual catch evidence: a shared catch or accepted global Sparkling-catch override; sightings, spawn/slain text and reports cannot replace it |
| Icy after Wumpa capture | Any shared Wumpa catch completes every Icy Sparkling check for the run, without adding other species' catches |
| Gemzie | Exact chamber-opening server text gives retained checking proof after five seconds, never catches |
| Gimmiegold | Sparkling uses registered coin slots plus extras and qualified current population; hd may substitute outside Haunted |
| Scrappy, Gemzie, Troodon, Gazer | Fixed capture targets allow Full Clear progress before local missing-row absence proof |

Species pages and [source evidence](docs/critters/TRACKING.md#source-evidence-and-retention) detail these exceptions. Source helpers and ordinary ESP are separate presentation decisions.

## Examples

- One Billygoat shared catch completes Unique and removes its row even when its Sparkling is needed. Sparkling separately needs two reconciled checks; Full Clear needs captures and qualified absence.
- Three Gemzie catches can complete Full Clear progress before an empty scan removes its row. The chamber timer supplies checking only.
- Receiving id supplies no Wumpa catch. A later shared Wumpa catch completes all Icy Sparkling checks without changing other capture counters.

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee; documentation-only working differences do not change these predicates.

- [SafariRun](src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt): completed, uniqueComplete, sparklingComplete, fullClearComplete, fullClearMissingComplete.
- [SafariFullClear](src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClear.kt): modes/minima/PB eligibility; [SafariPersonalBests](src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariPersonalBests.kt): record-specific predicates.
- [SafariCompletionEvents](src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionEvents.kt) and [SafariCompletion](src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletion.kt): announcements, alert, queues and reminders.
- [Initializer](src/main/kotlin/net/johnceo/sparklingmutuals/SparklingMutuals.kt) registers chat/ticks; [SafariTracking](src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariTracking.kt) records catches/PBs; [SafariBiomeClearTracking](src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariBiomeClearTracking.kt) supplies scans and local completion; [SafariPanels](src/main/kotlin/net/johnceo/sparklingmutuals/safari/ui/SafariPanels.kt) consumes outcomes.
- Existing checks: [UniqueCompletionTest](src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/UniqueCompletionTest.kt), [SafariFullClearProgressTest](src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClearProgressTest.kt), [SafariCompletionAnnouncementsTest](src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionAnnouncementsTest.kt). Not run in this documentation audit; no live Minecraft results are claimed.

## Open questions

Numeric minima and whole-biome coverage are not certified by client predicates. [The consolidated audit](docs/IMPLEMENTATION_AUDIT.md) records documentation mismatches, PB differences and server uncertainties.

## Last updated

2026-10-19 (America/New_York) — Consolidated implemented mode, announcement and PB distinctions. Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee; static source/caller review, no live Minecraft validation.

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
| Unique | One shared catch per ordinary species, with exceptions below | uniqueComplete; Honeybug, Gazer and Gimmiegold can leave the capture list while needed discovery checks still hold progress |
| Full Clear | Local capture/source proof, or report-backed capture minima; fixed-target species can complete progress early | Local capture/source proof and qualified absence coverage; a report alone cannot remove the row |

Unique can waive an uncaught Macaw after complete local bird feeding (including known party starting food) with no Macaw encounter. It can also waive an uncaught Rockmite/Snoozle after complete local source resolution and a Cavern absence scan with no encounter. These species disappear from Missing and may complete fd/cd once all other local requirements pass. No catch is fabricated.

Ordinary Sparkling checks use retained qualified UUID evidence reconciled with shared catches. Ordinary Unique completion adds no discovery scan/radius requirement except the documented Honeybug, Gazer and Gimmiegold checking gates when their discovery is needed. In Haunted, Gazer requires four retained reconciled checks and Gimmiegold requires resolved floor coins, no held coins and qualified spawn evidence; a first ordinary catch or received hd cannot replace that local work. Full Clear uses actual catches, known population targets, source conditions and qualified empty scans; [tracking](docs/critters/TRACKING.md) explains those evidence paths.

Valid selected profile members with no Safari/Sparkling discovery fields have zero discoveries and need every species checked. Actual failed background discovery lookups retry after one minute in the same mode; temporary roster changes cannot leave a finished lookup marked busy. See [API data and recovery](docs/API_LOOKUPS.md).

Detected Sparkling identities impose **no additional capture gate in any mode**. Confirmed Sparkling catch evidence, including accepted global announcements, overrides ordinary Unique/Sparkling species completion. A global announcement alone supplies no ordinary capture count or personal PB evidence. Birds are evaluated before this override and retain their shared spawning rules. Full Clear retains its normal rules. [Detection](docs/SPARKLING_DETECTION.md) owns identification and notifications.

### Reports and run-complete alerts

Automatic fd, cd, id and hd announcements require current presence, a capture-evidence scan, no still-held needed starting items for that destination, and all active-mode targets complete under localOnly. Sparkling also requires ready party discoveries. Unique suppresses its own report after receiving that biome's report; Forest/Haunted suppress received-report echoes in every mode. [Forest reporting](docs/BIRD_COMPLETION.md#sending-fd) uses complete qualified local bird spawning and source evidence, without a personal pickup quota, for initial and late entrants alike. Haunted late entrants can report complete local coin/population checks with the biome's other requirements.

Queued reports are rechecked against current mode, biome and local requirements before sending. Biome sends are spaced by at least three seconds. Unused-resource local titles remain, and one locally confirmed own or shared clear per run can be followed by a paced party reminder batch listing unused starting food/coins, the IGNs needing their related Sparklings and a request to wait. This follow-up is revalidated before sending and does not add resource contributions twice. Once per run, 0.5 seconds after the server hotspot message confirms boat departure, a compact party message shares only unused starting food/coins associated with missing party discoveries ; unknown discovery data postpones it. Named needs appear in the completion reminder. See [resource timing](docs/critters/FLOOR_DROPS.md#early-starting-foodcoin-reports). Announcement memory prevents duplicates without adding catches.

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
| Rockmite, Snoozle | Current Cavern uses local source checks. Outside Cavern, cd supplies checking after a catch and may waive an unencountered zero spawn; returning restores local requirements |
| Wumpa, Doomspiral | Needed Sparkling checks require actual catch evidence: a shared catch or accepted global Sparkling-catch override; sightings, spawn/slain text and reports cannot replace it |
| Icy after Wumpa capture | Any shared Wumpa catch completes every Icy Sparkling check for the run, without adding other species' catches |
| Gemzie | Exact chamber-opening server text gives retained checking proof after five seconds, never catches |
| Gimmiegold | Sparkling uses registered coin slots plus extras and retained qualified capture/check coverage; hd may substitute outside Haunted |
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

### Current biome and report priority

Outside the current biome, trust a party completion report even after an earlier visit. Unique requires one confirmed shared capture per ordinary species, with zero-spawn exceptions for unencountered birds, Rockmite and Snoozle. Full Clear still requires actual capture minimums, known nest/coin targets and its bird capture formula. Current-biome local scans remain authoritative; reports supply no capture or personal-PB credit.

Send at most one unused starting-item party reminder batch per run after a locally confirmed clear with an own or peer-supplied report. Each client checks its own items, so a peer's report does not suppress the reminder. Needed items still held for the current destination block its local completion/report until used. Long batches remain paced, but later biome clears cannot repeat a batch already sent.

## Last updated

2026-10-25 — Gimmiegold qualified checks retain through partial unloading. In Unique mode, captured Rockmite/Snoozle only require source checking when the party needs that shiny; Full Clear source/capture rules remain unchanged.

2026-10-22 — One-per-run unused-item reminder batches, shared local clears and stronger remote report trust with capture requirements retained.

2026-10-22 — Kept needed Haunted discovery checks separate from first-catch Unique rows; Rockmite mounds remain highlighted after a catch only while its discovery is needed.

2026-10-20 — Extended local Unique zero-spawn waivers to Bluebird and Parakeet alongside Macaw. Full Clear already permits absent bird species when actual catches satisfy the shared food/population formula; regression coverage now includes either bird being absent with extra party food.

2026-10-20 — Unique missing rows now track shared first catches (with proven zero-spawn waivers), while progress and biome reports retain source gates. Unique coin ESP without a needed Gimmiegold discovery stops after two floor pickups. Starting-item reports omit names/needs; post-biome reminders retain them. Hideyho offers can auto-accept inside Safari.

2026-10-20 - Beginning unused-supply messages name missing members and use a 0.5-second post-boat delay.

2026-10-20 - Added named party reminders for required unused starting supplies immediately after local biome reports.

2026-10-19 - Added local Unique zero-spawn Cavern completion and confirmed no-Macaw Forest reporting after all food obligations resolve.

2026-10-19 - Documented zero-discovery new players and automatic one-minute recovery of Sparkling-mode API failures.

2026-10-19 (America/New_York) — Early discovery-gated quantities replace completion-time resource chat; late-entry fd/hd use local spawning proof and received-report suppression. Reviewed source: working changes on `78885e0`; live validation pending.

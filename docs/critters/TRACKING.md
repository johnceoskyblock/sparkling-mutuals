# Shared Safari evidence and retention

## Scope

Authoritative reference for capture/check evidence, source surveys, reconciliation, party trust and run retention. [Mode outcomes](../../SAFARI_LOGIC.md), [UI](../MISSING_PANEL.md), [bird completion](../BIRD_COMPLETION.md), [resources/drop prediction](FLOOR_DROPS.md) and [exact messages](MESSAGES.md) have separate owners. Common terms follow [the glossary](../DOCUMENTATION_GUIDE.md).

## Definitions

A **live sighting** identifies a currently eligible loaded entity. A **qualified UUID check** additionally satisfies that species' player-position/biome gate. **Retained check evidence** survives loss of visibility within the same run. A **reconciled population** combines catches and sightings using their evidence baselines; it is not a raw sum of all UUIDs ever seen.

A personal capture increments both personal and shared counts. Party loot share increments only shared counts. A global Sparkling catch announcement can establish Sparkling-capture state without incrementing either count. A party completion report establishes permitted remote checking/progress, never a catch.

## Implemented rules

### Capture and event evidence

| Evidence | Establishes | Does not establish |
| --- | --- | --- |
| Successful personal capture/finding reward | One personal and shared catch | Shard quantity is not capture quantity |
| Attributed loot-share reward | One shared catch | A personal catch or personal-clear PB |
| Accepted Sparkling global announcement | Species Sparkling-capture state, reconciled with reward evidence | An ordinary capture counter increment |
| Capsule hit confirmation | Attempt at the named species | A catch or unique entity association |
| Escape | Failed attempt | An extra spawn or catch |
| Ordinary spawn/source activity text | Its parsed source/activity transition | A capture, or arbitrary UUID attribution |
| Floor pickup / initial inventory | Resource evidence under [resource rules](FLOOR_DROPS.md) | Capture or confirmed placement |
| Party fd/cd/id/hd | Report state under the policy below | Personal evidence or resource consumption |

The chat path strips Minecraft formatting, splits server multiline components and separates player chat before server-event matching. [MESSAGES](MESSAGES.md) preserves supplied quotations and distinguishes observed text from implemented parser scope.

### Qualified checks and reconciliation

For each qualifying nonbird UUID, SparklingChecks stores the shared catch total at its first sighting. The checked lower bound is the maximum of current catches and, for each stored baseline, that baseline plus UUIDs first seen at or after it. Repeated sightings of the same UUID add nothing. This avoids counting an already-seen critter again when its catch arrives, while allowing earlier catches plus later sightings.

Qualified histories survive unloading, biome travel, reentry and mode changes. Reentry removes inherited ordinary party checks and source completion flags, not UUID history. Out-of-zone sightings add no qualified history. Gemzie's exact chamber-opening message starts a single five-second timer; its run-owned proof supplies at least three checked thereafter, independently of scans. Duplicate text does not restart it; quoted player chat does not start it.

Birds use qualified **current** rendered population plus captures instead of accumulated UUIDs. Gimmiegold's dedicated Sparkling validator retains qualified UUID checks with capture baselines, so partial unloading cannot lose a previously seen critter. New coin-source/contribution demand clears the prior cohort; these checks never become actual captures. Seeing an ordinary entity, or a positive Sparkling name/skin, never increments a capture counter.

### Scan zones

These zones qualify the **player's X/Z position**, in horizontal blocks. They do not promise server render coverage. Local ordinary evidence also requires the species' biome to be the active scanned biome.

| Species | Center X/Z | Sparkling radius | Full Clear radius |
| --- | --- | --- | --- |
| Foxtrot, Fluffling | 3/47 | 40 | 30 |
| Treefrog, Woodchucker, Hideonfloor | 3/47 | 55 | 50 |
| Cavernfish | -85/81 | 40 | 30 |
| Flitter | -84/62 | 40 | 30 |
| Shyworm | -119/43 | 40 | 30 |
| Driftling | -120/55 | 60 | 40 |
| Chuckwalla | -100/45 | 60 | 40 |
| Gemzie | -141/51 | 40 | 30 |
| Strongarm | -107/-56 | 40 | 30 |
| Tepid, Mantis Shrimp, Nozzlenose | -73/-46 | 40 | 30 |
| Polaris | -110/-78 | 40 | 30 |
| Shuddersquid | -127/-48 | 40 | 30 |
| Billygoat | -121/-55 | 40 | 30 |
| Areita, Bloodbat, Duplico, Litterbug, Solsnatcher, Hideonwall | -3/-63 | 40 | 30 |

Scrappy, Troodon, Gazer and Hideyho have biome-qualified UUID minima without another species position zone. Doomspiral's Full Clear absence uses its biome scan; its Sparkling check requires catch evidence, including the accepted global Sparkling-catch override. Source species use their source-specific paths. Birds use X/Z 6/51, radius 40; Gimmiegold uses X/Z -4/-64, radius 15 for qualified source/check validation.

### Full Clear evidence and late entry

For implemented minimum M and shared captures C, the minimum outstanding captures are max(0, M − C); known population or extra-resource targets can increase this. Catches received before visiting a biome persist once. Catches before the client joined are unknown.

Local Full Clear missing-row proof requires the capture target, resolved sources and qualified absence. It persists after leaving the zone/biome. Positive eligible remaining sightings revoke that local absence proof. Local checklist/PB observations retain capture-plus-current-population targets; unloading does not erase them. Outside the current biome, a party report instead uses actual capture minimums, known nest/source totals, real coin demand and the bird capture formula. Old UUID history and source/absence scan contradictions do not raise that remote minimum. New explicit unused-resource demand and actual held items remain separate blockers. Remaining Macaws retain their one-capture exception under the bird formula.

Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4 can complete progress from their fixed capture totals before local absence removes their missing row. Other species without local proof normally need a biome report plus minimum captures. Honeybug and birds have explicit exceptions. Neither a report nor historical checked UUIDs supplies personal Full Clear evidence.

### Source evidence and retention

| Source | Implemented evidence and retention |
| --- | --- |
| Honeybug nests | Retain the greatest found/punched nest counts and improved coverage. Known population requires a positive nest count and no unknown sites, or a completed nest survey. Local checking resolves from all nests checked or a fresh Forest render showing shared catches + current uncaptured bees reaching that count. Retain confirmed proof through unloading, reports and reentry; a block refresh cannot latch stale live counts. Newly expanded nest evidence invalidates prior local checking |
| Honeybug Full Clear | Known nest population plus actual captures can complete from any biome without another scan/report, also respecting known population targets. Unknown coverage uses the fallback/species capture path |
| Rockmite mounds | Retained mound sites, loaded-world absence and terminal source outcomes. An unobservable site remains unknown; an empty outside-Cavern scan with no known sites invents no survey |
| Snoozle walls | Five fixed block sites retain loaded observations. Air means broken; a loaded nonair site means intact. Unloaded sites retain their previous state |
| Gimmiegold Sparkling | Registered validated coin slots must all resolve, sampled held coins must be zero, and qualified capture/check coverage must reach slots + deduplicated extras. UUID checks retain through unloading and reentry; new sources/extra demand clear the previous cohort, while unresolved markers and held coins still block |
| Gimmiegold Full Clear | Separate source recovery can use no remaining coin markers or at least three actual floor pickups, sufficient coin use and sampled empty inventory in the qualifying environment. Additional coins/notices and capture/absence requirements still apply |

Loaded Cavern mound/wall observations run from **any Safari biome** without a player-distance cutoff, independently of HUD/ESP. Positive intact sources can reopen their own species even if its discovery is owned; a party report takes priority while outside that biome. Observed source completion can resolve a contradiction without entering Cavern. Source proof does not manufacture a catch. Bird source retention belongs to [BIRD_COMPLETION](../BIRD_COMPLETION.md); slot registration and resource reminders belong to [FLOOR_DROPS](FLOOR_DROPS.md).

### Party reports and reentry

| Outcome | Accepted report effect | Revalidation |
| --- | --- | --- |
| Ordinary Unique | Outside a reported biome, one confirmed shared catch completes an ordinary species | Current biome retains local checking gates |
| Optional Unique spawns | Remote reports can waive unencountered zero-spawn birds, Rockmite and Snoozle | An encountered species still needs its capture; local absence proof remains required inside |
| Honeybug Unique/Sparkling | fd supplies checking outside Forest; Unique still needs a catch | Current Forest requires local nest evidence |
| Ordinary Sparkling | Outside a reported biome, its report substitutes checking | Reentry uses local proof |
| Gimmiegold Sparkling | hd covers its recorded coin demand outside Haunted | Held coins or later demand still block; current Haunted needs local validation |
| Birds | Remote fd covers its recorded food demand | Held food and later extras still block; Full Clear keeps actual captures |
| Full Clear | Remote report plus actual capture minimums and known nest/coin/bird targets | Current biome retains local source/scan requirements |
| Wumpa/Doomspiral Sparkling | Report cannot substitute for required capture | Existing shared-catch requirements remain |


Reports are recognized from parsed party message bodies, case-insensitively, with an optional trailing period. The implementation does not cross-check the sender against a separate current-party roster here. Server delivery in the Party channel is the trust boundary; arbitrary quoted nonparty text does not become a report.

Early `[SM] Starting items: ...` party reports establish sender/type food/coin contribution maxima before clearing completes; they never add captures or resource use. Send only unused starting items tied to a missing party discovery, 0.5 seconds after leaving the observed boat and ready discovery data. End-of-biome feedback also includes one named party reminder batch per run after a locally confirmed own or shared clear. See [resource timing](FLOOR_DROPS.md#early-starting-foodcoin-reports).

A late Forest entrant may report fd from qualified capture-plus-current-render/source proof without six personal pickups if no fd was received. Late Haunted entrants can likewise report hd after local Gimmiegold and the biome's other active-mode requirements pass. Forest/Haunted reports already received suppress automatic echoes in every mode; Unique retains its report suppression for all biomes.

### Capsule escape and replacement identity

The supplied server rule is: standard Common/Uncommon capsules guarantee capture **once hit**; standard Rare/Epic/Legendary can escape; Masterful guarantees every rarity once hit. Throws can miss. The implemented hit/escape matchers and capture ledger keep attempts separate from catches.

Escape-capable species can reportedly return with replacement UUIDs. The retained-check algorithm does not explicitly merge old and replacement UUIDs; therefore its checked count is implemented evidence, not a verified distinct original population. Current-population scans avoid a union of historical identities, but this does not prove perfect escape identity recovery. See [audit uncertainty D8](../IMPLEMENTATION_AUDIT.md#d8). The reported Macaw rehighlight after a failed capture is evidence of a presentation change, not confirmed UUID replacement without paired logs.

### Run lifecycle

SafariLedger creates a run on detected arrival; a later entry banner confirms the same arrival without erasing it. A player death does not end the run: an unmapped death/respawn position while the area still says Critter Safari keeps the active run, and a temporarily missing local player in the same world does not reset location or sparkling alert memory. Captures, checks, source evidence, biome reports, resources and the original timer remain attached to that same run. Confirmed entrance/outside locations, actual world changes and disconnects end the active run. Unknown location alone does not end it. Last-run display can remain after departure; it is not a new active run. A new run resets catches, checks, contributions, slots, source surveys and notification histories. Display switches do not control evidence collection.

## Exceptions

A confirmed Sparkling catch overrides ordinary Unique/Sparkling checks before ordinary source gates; birds are evaluated first. Needed bosses still need capture evidence; an accepted global Sparkling-catch announcement can provide their own-species override without incrementing shared counters. A shared Wumpa catch checks all Icy species in Sparkling. These are completion policy exceptions, owned by [SAFARI_LOGIC](../../SAFARI_LOGIC.md#exceptions), not deductions that every ordinary source was resolved.

## Examples

- Two catches before the first qualified sighting plus one new UUID yield three reconciled checks. Seeing that UUID again adds none; its later catch is not blindly added to the earlier sighting.
- A teammate sends cd while a loaded wall is intact: Snoozle remains incomplete until contrary local source evidence resolves it.
- A trusted fd followed by incomplete Forest rendering does not revoke Honeybug checking; it does not add a Honeybug catch.
- An unloaded coin slot remains unresolved unless observable disappearance or matched pickup evidence resolves it.
- Seven Foxtrots observed before fd establish a local target of seven. Six later catches followed by fd after disappearance can complete remote progress while the local missing row/PB still needs seven, because the remote target did not record that earlier population ([D17](../IMPLEMENTATION_AUDIT.md#d17)).

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee.

- [SparklingChecks](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SparklingChecks.kt): retained baselines, qualification, timer and report reentry; [SafariRun](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt): caller composition, targets, nest/source exceptions and reports.
- [SafariMessages](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariMessages.kt), [SafariCatch](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariCatch.kt), [SafariTracking](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariTracking.kt): registered chat routing and capture attribution.
- [SafariStructures](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/structures/SafariStructures.kt), [BeeNests](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/structures/BeeNests.kt), [SafariBiomeClearTracking](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariBiomeClearTracking.kt), [SafariCompletionZones](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionZones.kt): loaded-world and position inputs.
- [GimmiegoldSparklingCheck](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldSparklingCheck.kt), [SafariLedger](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariLedger.kt): dedicated population and lifecycle.
- Existing checks include [RetainedSparklingChecksTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/RetainedSparklingChecksTest.kt), [SafariPartyCheckRoutingTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/SafariPartyCheckRoutingTest.kt), [FullClearMissingPanelTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/FullClearMissingPanelTest.kt). Not run locally in this audit; runtime wiring was inspected directly.

## Open questions

Hypixel population guarantees, precise server render coverage and replacement UUID behavior remain unverified. Exact prerequisite-to-boss-spawn relationships require live evidence; inventory alone is not proof of a spawn. [The audit report](../IMPLEMENTATION_AUDIT.md) separates these uncertainties from documentation mismatches.

### Current biome and remote report priority

A retained report is authoritative only while outside its biome, even if this client visited it earlier. Old source/UUID/absence observations do not block that remote shortcut. Unique still requires one actual shared capture for each ordinary species. Bluebird, Parakeet, Macaw, Rockmite and Snoozle can be waived remotely at zero captures only if never encountered. Encountered species still require a real catch. Needed starting items held for the destination continue to block completion/reporting.

Full Clear still requires actual minimum captures, known Honeybug nest totals, source-derived Gimmiegold coin demand, and the existing bird capture formula. Reports never invent captures or personal PB evidence. Returning to the biome restores its local scan/source checks. Honeybug checking now follows this same current-biome rule rather than retaining fd trust inside Forest. Sparkling-mode boss capture requirements remain unchanged. Later food/coin demand can invalidate an older report; held resources are not considered used merely because a report arrived.

Each client can send one unused starting-item reminder batch per run after a local clear, even if a peer sent that biome report first. A report for a different biome alone never arms that client's reminder.

## Positive labels for remaining captures

Every actual critter, including native Minecraft mobs, must have an associated current species label before contributing to remaining-capture observations or local population targets. A texture, movement, or registered entity alone cannot establish that it still needs capturing. Label identity persists, with a one-second packet grace; missing labels expire rather than blocking a biome forever. This does not infer a capture: minimum capture counts and existing coverage/source requirements still apply. Rockmite mound displays, bee nests, and Snoozle walls remain separate structure sources. Existing explicit visual-only cooldown/distance helpers do not supply remaining-capture evidence.

## Missing command ownership and output

!m replies only come from a client assigned to the requested biome at the run's 30-second mark. That assignment persists after travel; before the checkpoint or when the checkpoint was not observed, this client does not reply. Bare !m selects the responder's checkpoint biome. Sparkling source notes are limited to known remaining mounds, Snoozle walls, unopened Gemzie chamber, bird food, bee nests, Gazers, and coins, filtered to party needs. Icy has no extra Sparkling notes. Unique prefixes the uncaught type list with Uniques left to capture; Full Clear keeps positive minimum-capture deficits. Unknown counts and generic check-spawns/loading notes are omitted.

## Last updated

2026-10-30 — Capture reconciliation now survives the timeout and head/capsule UUID changes: pending captures are matched against decreases in the visible species population before model-count estimates can grow again. Positive remainders still block completion. Debug includes the pending capture count (`captureDebt`). This extends the earlier 2.5-second guard, which alone did not prevent inferred fourth Shrimp/Troodon targets.

2026-10-30 — All critters now defer population-target increases for 2.5 seconds after a capture, including Mantis Shrimp and Troodon. Positive remaining models still block completion; a qualified empty scan ends the transition so a genuinely new sighting can raise the target immediately. Debug logs show per-species captures, inferred targets, remaining models and scan confirmation.

2026-10-29 - Simplified known-source replies and local Sparkling source requirements.


2026-10-28 - Required positive current labels for remaining-capture evidence Safari-wide.


2026-10-25 — Honeybug local checking proof now retains after a complete fresh render; fd remains an outside-Forest override only, and expanded nest evidence reopens local work.

2026-10-25 — Gimmiegold retains qualified check UUIDs through unloading; Unique Rockmite/Snoozle source requirements apply after first capture only when their shiny is needed.

2026-10-24 — Preserve the active run during unmapped death/respawn positions and temporary local-player replacement. Confirmed departures still end the run; the next run starts with fresh progress.

2026-10-22 — Remote reports take priority outside the current biome, including previously visited biomes; Unique first catches and Full Clear capture minimums remain required.

2026-10-22 — Documented needed Unique Gazer/Gimmiegold checks after the first capture and local Haunted revalidation.

2026-10-21 — Full Clear Gimmiegold capture targets are coin-derived, not raised by historical/replacement model UUID population estimates. A cleared dedicated coin-source ledger plus sufficient actual captures and no live remainder completes the species even if the older generic floor-marker scan remains blocked by unrelated rewards. Legacy coin proof remains a fallback; additional real coins and held coins still block completion. Added a three-coin whole-Haunted regression.

2026-10-20 — After a shared Driftling/Chuckwalla catch, defer increases to inferred capture targets for 2.5 seconds so delayed old models are not added on top of that catch. Positive remaining evidence still blocks completion during this interval. Stable live evidence afterward can raise targets normally; catches are unchanged.

2026-10-20 — Driftling movement now expires independently of distance and label visibility. Chuckwalla needs current visible label confirmation. Both checks run before ordinary ESP, population estimates and completion scans; debug output reports each lifecycle gate. After a Cavern completion report and minimum catches, changed Driftling/Chuckwalla lifecycle states are also logged automatically (bounded per run), so a future incident can be investigated even without /sm debug.

2026-10-20 — Late-loaded Driftling armor-stand heads now require current visible label evidence or observed live movement (with no stale known label). ESP and population/completion consume this same gate, preventing frozen leftovers from increasing targets or keeping a biome incomplete. Other species retain their existing policies.

2026-10-19 - Added local Unique zero-spawn Cavern completion from resolved sources and absence, without a circular cd requirement.

2026-10-19 (America/New_York) — Added early contribution routing, local-title reminders, target-scoped hd trust and late-entry reporting. Reviewed source: working changes on `78885e0`; live validation pending.

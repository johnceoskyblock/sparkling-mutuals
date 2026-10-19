# Shared Safari evidence and retention

## Scope

Authoritative reference for capture/check evidence, source surveys, reconciliation, party trust and run retention. [Mode outcomes](../../SAFARI_LOGIC.md), [UI](../MISSING_PANEL.md), [bird completion](../BIRD_COMPLETION.md), [resources/drop prediction](FLOOR_DROPS.md) and [exact messages](MESSAGES.md) have separate owners. Common terms follow [the glossary](../DOCUMENTATION_GUIDE.md).

## Definitions

A **live sighting** identifies a currently eligible loaded entity. A **qualified UUID check** additionally satisfies that species' player-position/biome gate. **Retained check evidence** survives loss of visibility within the same run. A **reconciled population** combines catches and sightings using their evidence baselines; it is not a raw sum of all UUIDs ever seen.

A personal capture increments both personal and shared counts. Party loot share increments only shared counts. A global Sparkling catch announcement can establish Sparkling-capture state without incrementing either count. A party completion report establishes permitted remote checking/progress, never a catch.

## Implemented rules

### Early party starting-resource synchronization

The exact server `HOTSPOT! Your Hunting Hotspot is the <Biome> Biome!` message starts one run-owned **+2000 ms** announcement. Arrival, Manager admission, first HUD render and area polling do not start it. Repeated triggers cannot restart/duplicate it; disconnect, world change and run replacement cancel old work, including queued callbacks.

Sample genuine initial inventory before HOTSPOT and for at most two seconds afterward, closing before first resource input/pickup or dispatch. Head Start is an overlapping lower bound; known First Class plus Head Start requires both grants to be witnessed. Preserve identical/mixed original grants through use; later floor pickups are subtracted. New initial evidence/reports support at most two random grants; oversized, missing or late evidence remains unknown rather than invented empty inventory.

Send one concise absolute message, independently of mode, discovery loading and HUD/ESP: `[SM] Starting items: 2x Bag of Seeds [run:<marker UUID without hyphens>]`, or mixed `1x Shining Coin; 1x Yogi Berry`, or explicit `none`. The marker is a witnessed display UUID shared by this run's source catalog. Missing marker evidence keeps dispatch/admission pending. Roster loading does not delay this client's own report; confirmed solo records locally without a pointless party command. Reports are quantities, never catches/spawns/completion.

The live inbox admits only anchored Party envelopes, resolves the sender to a current-roster UUID and rechecks owner/world/connection/roster epoch plus current marker provenance. Guild/private/quoted/nonparty and stale-marker text is rejected. GAME/CHAT echoes and repeated formats merge sender/type **maxima**, including local snapshot overlap and legacy inbound notices. Demand persists after consumption. Every current roster member, including self, must have a complete report; explicit none is received zero, absence/old-client/late-join evidence is unknown. Do not broadcast another player's inventory.

Legacy bare unused-item notices and normal `fd`/`cd`/`id`/`hd` remain accepted with current receipt-time identity checks, but carry no on-wire run marker; an unobservable old legacy message cannot be proven new. They cannot falsely mark a missing complete starting report as received. New resource/source demand invalidates older proof and remote report stamps. Known held/remaining resources still block; inside Haunted local evidence is authoritative. Late unused-resource reminders are [local titles only](FLOOR_DROPS.md#unused-starting-foodcoin-reminders), with no party/local-chat fallback or new contribution.

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

Birds use qualified **current** rendered population plus captures instead of accumulated UUIDs. Gimmiegold's dedicated Sparkling validator also uses current qualified population, not its historical generic check counter. Seeing an ordinary entity, or a positive Sparkling name/skin, never increments a capture counter.

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

Scrappy, Troodon, Gazer and Hideyho have biome-qualified UUID minima without another species position zone. Doomspiral's Full Clear absence uses its biome scan; its Sparkling check requires catch evidence, including the accepted global Sparkling-catch override. Source species use their source-specific paths. Birds use X/Z 6/51, radius 15; Gimmiegold uses X/Z -4/-64, radius 15 for its source/current-population validation.

### Full Clear evidence and late entry

For implemented minimum M and shared captures C, the minimum outstanding captures are max(0, M − C); known population or extra-resource targets can increase this. Catches received before visiting a biome persist once. Catches before the client joined are unknown.

Local Full Clear missing-row proof requires the capture target, resolved sources and qualified absence. It persists after leaving the zone/biome. Positive eligible remaining sightings revoke that absence proof. Local checklist/PB population observations retain a target based on captures plus current uncaptured entities; unloading does not erase that local target. Remote progress uses a separate target populated only by observations made after a report, or for a fixed-target species. A larger population seen only before a later report can therefore remain a local blocker without raising the remote-progress target; see [audit D17](../IMPLEMENTATION_AUDIT.md#d17). Remaining Macaws have the separate bird exception.

Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4 can complete progress from their fixed capture totals before local absence removes their missing row. Other species without local proof normally need a biome report plus minimum captures. Honeybug and birds have explicit exceptions. Neither a report nor historical checked UUIDs supplies personal Full Clear evidence.

### Source evidence and retention

| Source | Implemented evidence and retention |
| --- | --- |
| Honeybug nests | Retain the greatest found/punched nest counts and improved coverage. Known population requires a positive nest count and no unknown sites, or a completed nest survey. Local checking resolves from all nests checked or shared catches + current uncaptured bees reaching that count. Newly expanded nest evidence invalidates prior local checking |
| Honeybug Full Clear | Known nest population plus actual captures can complete from any biome without another scan/report, also respecting known population targets. Unknown coverage uses the fallback/species capture path |
| Rockmite mounds | Retained mound sites, loaded-world absence and terminal source outcomes. An unobservable site remains unknown; an empty outside-Cavern scan with no known sites invents no survey |
| Snoozle walls | Five fixed block sites retain loaded observations. Air means broken; a loaded nonair site means intact. Unloaded sites retain their previous state |
| Gimmiegold Sparkling | Known initial source/report coverage, no remaining coin sources, sampled zero held coins and qualified real captures + current uncaptured models reaching F + S. Marker disappearance is not a pickup. Earned proof retains through unloading; new demand/positive source contradictions revoke it and Haunted reentry resets population validation |
| Gimmiegold Full Clear | Separate source recovery can use no remaining coin markers or at least three actual floor pickups, sufficient coin use and sampled empty inventory in the qualifying environment. Additional coins/notices and capture/absence requirements still apply |

Loaded Cavern mound/wall observations run from **any Safari biome** without a player-distance cutoff, independently of HUD/ESP. Positive intact sources can reopen their own species even if its discovery is owned. Observed source completion can resolve a contradiction without entering Cavern. Source proof does not manufacture a catch. Bird source retention belongs to [BIRD_COMPLETION](../BIRD_COMPLETION.md); slot registration and resource reminders belong to [FLOOR_DROPS](FLOOR_DROPS.md).

### Party reports and reentry

| Outcome | Accepted report effect | Revalidation |
| --- | --- | --- |
| Ordinary Unique | None needed after its shared first catch | No ordinary discovery scan gate |
| Unique Rockmite/Snoozle | cd may supply unknown checking after a catch; resolved sources + cd can confirm unseen zero spawn | Known intact sources cannot be erased by another report; encountered uncaught species cannot use zero-spawn trust |
| Honeybug Unique/Sparkling | fd supplies checking, never captures | Trust survives Forest entry and incomplete later renders |
| Ordinary Sparkling | Report substitutes checking | Entry/local scan clears inherited ordinary checking; retained local UUID evidence remains |
| Gimmiegold Sparkling | hd may substitute outside Haunted | Inside Haunted requires dedicated local slot/inventory/population validation |
| Birds | Mode/location-specific trust | [Bird report policy](../BIRD_COMPLETION.md#completion-by-mode) |
| Full Clear progress | Report plus real capture targets, unless local/fixed/Honeybug path already qualifies | Positive remaining entities, intact sources and applicable resources still block; a report alone does not remove local missing rows |
| Wumpa/Doomspiral Sparkling | Spawn/slain text and biome reports cannot substitute for capture | Shared catch or accepted global Sparkling-catch override completes that boss; only a shared Wumpa counter increment checks all Icy species |

Reports are recognized case-insensitively from verified current-roster Party message bodies, with an optional trailing period. The live inbox owns admission and queued owner checks; pure parser helpers alone are not a runtime trust boundary. Current-demand hd can supply outside-Haunted checking without local renders, but cannot waive known held/remaining or outstanding resource blockers.

### Capsule escape and replacement identity

The supplied server rule is: standard Common/Uncommon capsules guarantee capture **once hit**; standard Rare/Epic/Legendary can escape; Masterful guarantees every rarity once hit. Throws can miss. The implemented hit/escape matchers and capture ledger keep attempts separate from catches.

Escape-capable species can reportedly return with replacement UUIDs. The retained-check algorithm does not explicitly merge old and replacement UUIDs; therefore its checked count is implemented evidence, not a verified distinct original population. Current-population scans avoid a union of historical identities, but this does not prove perfect escape identity recovery. See [audit uncertainty D8](../IMPLEMENTATION_AUDIT.md#d8). The reported Macaw rehighlight after a failed capture is evidence of a presentation change, not confirmed UUID replacement without paired logs.

### Run lifecycle

SafariLedger creates a run on detected arrival; a later entry banner confirms the same arrival without erasing it. Entrance/outside, world change and disconnect end the active run. Unknown location alone does not end it. Last-run display can remain after departure; it is not a new active run. A new run resets catches, checks, contributions, slots, source surveys and notification histories. Display switches do not control evidence collection.

## Exceptions

A confirmed Sparkling catch overrides ordinary Unique/Sparkling checks before ordinary source gates; birds are evaluated first. Needed bosses still need capture evidence; an accepted global Sparkling-catch announcement can provide their own-species override without incrementing shared counters. A shared Wumpa catch checks all Icy species in Sparkling. These are completion policy exceptions, owned by [SAFARI_LOGIC](../../SAFARI_LOGIC.md#exceptions), not deductions that every ordinary source was resolved.

## Examples

- Two catches before the first qualified sighting plus one new UUID yield three reconciled checks. Seeing that UUID again adds none; its later catch is not blindly added to the earlier sighting.
- A teammate sends cd while a loaded wall is intact: Snoozle remains incomplete until contrary local source evidence resolves it.
- A trusted fd followed by incomplete Forest rendering does not revoke Honeybug checking; it does not add a Honeybug catch.
- An unloaded coin slot remains in F; disappearance alone never records collection. A matched pickup or fresh applicable complete source/population proof must account for its opportunity.
- Seven Foxtrots observed before fd establish a local target of seven. Six later catches followed by fd after disappearance can complete remote progress while the local missing row/PB still needs seven, because the remote target did not record that earlier population ([D17](../IMPLEMENTATION_AUDIT.md#d17)).

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee.

- [SparklingChecks](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SparklingChecks.kt): retained baselines, qualification, timer and report reentry; [SafariRun](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt): caller composition, targets, nest/source exceptions and reports.
- [SafariMessages](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariMessages.kt), [SafariCatch](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariCatch.kt), [SafariTracking](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariTracking.kt): registered chat routing and capture attribution.
- [SafariStructures](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/structures/SafariStructures.kt), [BeeNests](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/structures/BeeNests.kt), [SafariBiomeClearTracking](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariBiomeClearTracking.kt), [SafariCompletionZones](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionZones.kt): loaded-world and position inputs.
- [GimmiegoldSparklingCheck](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/GimmiegoldSparklingCheck.kt), [SafariLedger](../../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariLedger.kt): dedicated population and lifecycle.
- Existing checks include [RetainedSparklingChecksTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/RetainedSparklingChecksTest.kt), [SafariPartyCheckRoutingTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/SafariPartyCheckRoutingTest.kt), [FullClearMissingPanelTest](../../src/test/kotlin/net/johnceo/sparklingmutuals/safari/completion/FullClearMissingPanelTest.kt). Not run locally in this audit; runtime wiring was inspected directly.

## Open questions

Initial complete STRING phase classification, universal startup loading/shared UUID fidelity and real grant timing need live multi-client confirmation. Missing source/report coverage stays unknown. Hypixel population guarantees, precise server render coverage and replacement UUID behavior remain unverified. Exact prerequisite-to-boss-spawn relationships require live evidence; inventory alone is not proof of a spawn. [The audit report](../IMPLEMENTATION_AUDIT.md) separates these uncertainties from documentation mismatches.

## Last updated

2026-10-19 (America/New_York) — Updated early starting-resource synchronization, verified coverage and local-only reminders for issue #68 while preserving the merged documentation template. Resource runtime reviewed at `76dd746`; prior audit provenance remains scoped to its stated revision. Automated all-target and offline native checks are separate from pending live Hypixel validation.

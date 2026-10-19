# Missing panel: every critter and mode

This is a reference to the **current local code**, reviewed against the maintained gameplay rules on 2026-10-17. It covers all 37 roster critters. The [species notes](critters/README.md) describe gameplay rules to implement/review; this document records what the panels currently do.

Each Full Clear table cell states when the critter **leaves the local missing panel and advances local progress**. Biomes without local clear evidence use the remote report rules below. In Unique and Sparkling, the cells describe both missing-row removal and progress unless an exception below says otherwise. “Unique + Sparkling” means Unique mode when someone in the party still needs that species' Sparkling discovery; it is not a fourth selectable mode.

## Shared rules

- **Catch** means a confirmed shared catch: your capture or a party member's loot-share capture. A capsule hit, escape, spawn announcement or ordinary sighting is not a catch. Hideyho finding rewards count.
- **Report** means a received biome completion message: Forest `fd`, Cavern `cd`, Icy `id`, Haunted `hd`. Reports do not create catches.
- **Full Clear progress:** your current biome advances per species as its local capture/source and qualified empty-scan requirements pass, without a party report. Retain that earned local progress after leaving. Other biomes without local proof wait for a report and their capture minimum. Scrappy, Gemzie, Troodon and Gazer retain fixed-total early completion; Honeybug uses its retained initial nest count and actual shared captures, without a report or render scan. After a report (or for those fixed species), positively scanned remaining critters raise the required capture total. Empty scans, unloading and repeated reports do not erase a known remainder. Macaw keeps its one-catch exception.
- **Full Clear missing panel:** no party report is required. Meet the capture minimum and known remaining-population target, resolve species-specific sources, and confirm no ordinary critter remains during a qualified local scan. Macaw keeps its one-catch exception. Retain qualified coverage after leaving the radius or biome; a positive remaining sighting revokes it. New supplies and unfinished sources can reopen the row; Sparkling detections cannot. Fixed-count species also need local missing-panel proof.
- **Unique:** most species leave the panel and advance progress on their first shared catch, even if a party member needs the Sparkling discovery. There is currently no separate ordinary-species Sparkling scan gate for Unique progress. Rockmite/Snoozle additionally need resolved safari-wide sources or trusted cd checking evidence, and Honeybug progress additionally needs checking/report trust when its discovery is needed; its missing row still clears on the first catch.
- **Sparkling:** normally only species whose Sparkling discovery a party member needs appear in the checklist/progress denominator. Qualified checks use retained UUID sightings reconciled against shared catches; all critters do not have to remain rendered simultaneously. Catches also contribute to the check count. Bird checks use food/population evidence instead of distinct bird UUIDs. Gimmiegold uses captured + current rendered population against the initial coin-slot total, rather than historical checked UUIDs.
- **Detected Sparkling:** an unresolved Sparkling sighting keeps that species incomplete in every mode, even if its ordinary requirement was met. Confirmed Sparkling capture resolves the sighting; when none remain pending, Unique and Sparkling complete that species immediately. Full Clear still requires its normal captures and sources, using local proof or the remote report rules. Multiple pending Sparklings must all be resolved.
- **Pending extra supplies:** outstanding party deliveries block Full Clear source work and Sparkling birds until new spawn evidence resolves them. Gimmiegold Sparkling instead adds each reported coin quantity to its slot-derived population target; remote hd trust can override ordinary checking outside Haunted. Ordinary Unique first-catch completion does not use this gate.

Numeric minima below are values in the code, not a claim that Hypixel guarantees those spawn counts. Zero/variable minima are called out explicitly.

## Forest

The three birds are exceptions to the general per-species first-catch/report summaries above. Their progress and missing rows use [the shared bird spawning/capture policy](BIRD_COMPLETION.md) inside and outside Forest. Other Forest species retain their individual rules.

| Critter | Full Clear | Unique | Unique + needed Sparkling | Sparkling: local check |
| --- | --- | --- | --- | --- |
| [Foxtrot](critters/foxtrot.md) | 6 shared catches + qualified empty scan | 1 catch | 1 catch | 6 qualified checks in its zone |
| [Bluebird](critters/bluebird.md) | Expected bird opportunities captured, including known rendered normal birds; one-Macaw exception retained | Spawning proof + 1 catch | Same as Unique | Local qualified spawning inside Forest; fd outside; no catch required |
| [Honeybug](critters/honeybug.md) | Initial known nest population captured, from any biome, without a local empty scan or report | 1 catch removes row and completes progress when no discovery is needed | 1 catch removes row; progress additionally needs local nest checking or trusted fd | Local nests checked/accounted for, or trusted fd; no ordinary catch required |
| [Treefrog](critters/treefrog.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks in its zone |
| [Woodchucker](critters/woodchucker.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks in its zone |
| [Fluffling](critters/fluffling.md) | 1 shared catch + qualified empty scan | 1 catch | 1 catch | 1 qualified check in its zone |
| [Hideonfloor](critters/hideonfloor.md) | 1 shared catch + qualified empty scan | 1 catch | 1 catch | 1 qualified check in its zone |
| [Parakeet](critters/parakeet.md) | Same shared bird population requirement as Bluebird | Spawning proof + 1 catch | Same as Unique | Local qualified spawning inside Forest; fd outside; no catch required |
| [Macaw](critters/macaw.md) | Same expected bird population, with one catch crediting established Macaw pairs; zero catches allowed if none spawned | Spawning + 1 catch, local no-Macaw proof, or fd outside when both other birds are caught | Same as Unique | Same spawning requirement as the other birds |

Full Clear's common positive-remainder rule applies to every Forest row except Macaw. Honeybug uses the verified actual nest count as its local capture target instead of the fixed three-catch fallback. Like other species, its local completion advances progress without a report. Positively known uncaught bees still count toward the normal capture requirement; detected Sparkling identities add no separate gate.

### Bird food and no-Macaw proof

Use [Forest bird completion](BIRD_COMPLETION.md) for the three birds. Base demand is nine food opportunities plus known local starting and party-notified food. A qualified scan at X/Z 6/51 within 15 blocks uses shared captures + current renders, with two Macaws per opportunity. Attraction chat is ignored. Remaining Forest floor-food markers and held bird food block completion; floor markers are checked Safari-wide.

Inside Forest, Sparkling requires the local population proof and Unique adds one catch per bird, with the proven no-Macaw exception. Outside Forest, fd supplies spawning trust and may waive only the last Macaw catch once the two other birds have shared catches. Full Clear needs actual expected captures everywhere, retaining one-Macaw credit. A report never supplies missing captures. Automatic fd additionally requires an initial Forest clearer with six separate local food pickups and no held/remaining food.

### Honeybug sources

Let **C** be shared Honeybug catches, **U** currently rendered uncaptured Honeybugs, and **N** the verified positive count of actual bee-producing nests. Nest highlights and the nest reminder resolve when the mod user has punched every actual nest, or **C + U >= N**. Historical UUID sightings are not added. In particular, **C >= N** accounts for the nest population without requiring bees to have been rendered earlier. Unknown nest sites do not establish the final N.

Unique always removes the Honeybug species row at one catch. If someone needs Sparkling Honeybug, unfinished nest work stays as a separate reminder/highlight after that catch. If everyone owns the discovery, the first catch also removes the Unique nest helper. Sparkling shows nest work only when the party needs the discovery. Full Clear still needs the bees captured, even after nests stop highlighting.

A received fd supplies trusted Honeybug checking in Unique/Sparkling and remains valid after later Forest entry; render absence or incomplete later scans cannot revoke it. Unique progress additionally needs its first shared catch when the discovery is needed, and its species row clears on that catch independently. Full Clear uses the retained initial N and actual captures, not the report. Detected Sparkling entities do not override reports in any mode.

## Cavern

| Critter | Full Clear | Unique | Unique + needed Sparkling | Sparkling: local check |
| --- | --- | --- | --- | --- |
| [Cavernfish](critters/cavernfish.md) | 4 shared catches + qualified empty scan | 1 catch | 1 catch | 4 qualified checks in its zone |
| [Flitter](critters/flitter.md) | 6 shared catches + qualified empty scan | 1 catch | 1 catch | 6 qualified checks in its zone |
| [Shyworm](critters/shyworm.md) | 4 shared catches + qualified empty scan | 1 catch | 1 catch | 4 qualified checks in its zone |
| [Driftling](critters/driftling.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks in its zone |
| [Chuckwalla](critters/chuckwalla.md) | 2 shared catches + qualified empty scan | 1 catch | 1 catch | 2 qualified checks in its zone |
| [Rockmite](critters/rockmite.md) | All mounds broken + known Rockmite population caught + no live Rockmites | 1 catch + all mounds resolved; cd may supply unknown checks or confirm unseen zero spawn | Same source rule; intact mounds reopen completion | All mounds confirmed broken; no ordinary Rockmite catch required |
| [Scrappy](critters/scrappy.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks |
| [Snoozle](critters/snoozle.md) | All 5 walls broken + known Snoozle population caught + no live Snoozles | 1 catch + all 5 walls resolved; cd may supply unknown checks or confirm unseen zero spawn | Same source rule; intact walls reopen completion | All 5 Snooper walls confirmed broken; no ordinary Snoozle catch required |
| [Gemzie](critters/gemzie.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | Five seconds after Hypixel's chamber-opening message, or 3 qualified checks in its zone; timer proof also supplies the needed Sparkling check in Unique |

Known remaining populations must still be caught. A zero minimum does not fabricate a catch; Rockmite and Snoozle can locally clear without catches only when their sources are resolved and no uncaught population is known. In Unique, all sources resolved plus party cd can confirm unseen zero-spawn Rockmite/Snoozle; a positively encountered critter still needs its first catch.

For Unique, source contradictions reopen Rockmite/Snoozle regardless of discovery ownership. Loaded mound/wall evidence can be collected from outside Cavern; unloaded or unknown areas do not prove a source was handled. Repeating `cd` does not erase a recorded needed-source contradiction. A confirmed Sparkling catch has the shared override described above.

## Icy

| Critter | Full Clear | Unique | Unique + needed Sparkling | Sparkling: local check |
| --- | --- | --- | --- | --- |
| [Strongarm](critters/strongarm.md) | 4 shared catches + qualified empty scan | 1 catch | 1 catch | 4 qualified checks in its zone |
| [Tepid](critters/tepid.md) | 6 shared catches + qualified empty scan | 1 catch | 1 catch | 6 qualified checks in its zone |
| [Polaris](critters/polaris.md) | 2 shared catches + qualified empty scan | 1 catch | 1 catch | 2 qualified checks in its zone |
| [Shuddersquid](critters/shuddersquid.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks in its zone |
| [Billygoat](critters/billygoat.md) | 2 shared catches + qualified empty scan | 1 catch | 1 catch; no additional scan gate for progress | 2 qualified checks in its zone |
| [Mantis Shrimp](critters/mantis-shrimp.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks in its zone |
| [Nozzlenose](critters/nozzlenose.md) | 2 shared catches + qualified empty scan | 1 catch | 1 catch | 2 qualified checks in its zone |
| [Troodon](critters/troodon.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks |
| [Wumpa](critters/wumpa.md) | 1 shared catch + qualified empty scan | 1 catch | 1 catch | 1 actual shared catch; sighting/spawn message/report alone is insufficient |

Until Wumpa is caught, when the party needs Sparkling Wumpa, **every uncaught Icy species also appears in the Sparkling missing panel as a Wumpa prerequisite**, even if everyone owns that species' discovery. These prerequisite-only species are not added to the Sparkling progress denominator. Any confirmed personal or shared Wumpa capture marks every Icy species checked and clears all Icy missing rows in Sparkling mode for the rest of the run, regardless of scans or other catches. Unique and Full Clear retain their normal requirements.

## Haunted

| Critter | Full Clear | Unique | Unique + needed Sparkling | Sparkling: local check |
| --- | --- | --- | --- | --- |
| [Areita](critters/areita.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks in its zone |
| [Bloodbat](critters/bloodbat.md) | 3 shared catches + qualified empty scan | 1 catch | 1 catch | 3 qualified checks in its zone |
| [Duplico](critters/duplico.md) | 2 shared catches + qualified empty scan | 1 catch | 1 catch | 2 qualified checks in its zone |
| [Gazer](critters/gazer.md) | 4 shared catches + qualified empty scan | 1 catch | 1 catch | 4 qualified checks |
| [Litterbug](critters/litterbug.md) | 4 shared catches + qualified empty scan | 1 catch | 1 catch | 4 qualified checks in its zone |
| [Solsnatcher](critters/solsnatcher.md) | 4 shared catches + qualified empty scan | 1 catch | 1 catch | 4 qualified checks in its zone |
| [Gimmiegold](critters/gimmiegold.md) | At least 3 catches, increased by known population/extra demand + resolved coin sources/uses + no live Gimmiegolds | 1 catch | 1 catch; coin/source checks do not separately gate Unique progress | All initial Shining Coin slots collected, no held coins, and shared captures + currently rendered Gimmiegolds >= initial coin slots + known extra coins; remote hd can override outside Haunted |
| [Hideonwall](critters/hideonwall.md) | 2 shared catches + qualified empty scan | 1 catch | 1 catch | 2 qualified checks in its zone |
| [Hideyho](critters/hideyho.md) | 1 shared catch + qualified empty scan | 1 finding reward | 1 finding reward | 1 qualified check or shared finding reward |
| [Doomspiral](critters/doomspiral.md) | 1 shared catch + qualified empty scan | 1 catch | 1 catch | 1 actual shared catch; ritual/spawn message/report alone is insufficient |

Gimmiegold Sparkling proof uses the retained initial coin slots, all slots collected, empty inventory, and current captured-plus-rendered population, including extra coins from deduplicated party notices. The separate Full Clear source-recovery path can use empty markers or confirmed pickup/use history. The source survey is established near Haunted's scan center; check demand also incorporates spent coins and newly discovered extras. A mid-run visitor's empty inventory alone does not prove another player's coins were used. Remote Full Clear progress uses the report/minimum/remainder rule, increasing the minimum for actual extra/starting coins and never waiving known unused coins. Local progress and the missing row use the stricter coin checks when positional scan evidence is available.

## Scan zones and retained checks

These are **player-position qualification zones** for local Sparkling evidence, not a guarantee that every entity within that distance is loaded. Coordinates are X/Z; distance is horizontal blocks.

| Species | Center | Sparkling radius |
| --- | --- | --- |
| Foxtrot, Fluffling | 3, 47 | 40 |
| Treefrog, Woodchucker, Hideonfloor | 3, 47 | 55 |
| Cavernfish | -85, 81 | 40 |
| Flitter | -84, 62 | 40 |
| Shyworm | -119, 43 | 40 |
| Driftling | -120, 55 | 60 |
| Chuckwalla | -100, 45 | 60 |
| Gemzie | -141, 51 | 40 |
| Strongarm | -107, -56 | 40 |
| Tepid, Mantis Shrimp, Nozzlenose | -73, -46 | 40 |
| Polaris | -110, -78 | 40 |
| Shuddersquid | -127, -48 | 40 |
| Billygoat | -121, -55 | 40 |
| Areita, Bloodbat, Duplico, Litterbug, Solsnatcher, Hideonwall | -3, -63 | 40 |
| Gimmiegold | -4, -64 | 15 |

Full Clear uses the same centers with a 30-block radius for zoned species, except Treefrog/Woodchucker/Hideonfloor (50) and Driftling/Chuckwalla (40). Gimmiegold uses its separate 15-block Haunted source survey. Source-based species retain their own requirements.

Scrappy, Troodon, Gazer and Hideyho use the required counts from qualified observations in their biome without an additional per-species position zone. Wumpa/Doomspiral use actual catches. Honeybug, Rockmite, Snoozle and birds use the source checks above.

Qualified nonbird UUID checks are retained when entities unload and across biome re-entry. The tracker records the shared capture total when each UUID is first seen to reduce sighting/catch double counting. It does not explicitly link an escaped critter's replacement UUID to its previous UUID; this document does not assume that such UUID changes are fully resolved.

## Party trust and local versus shared completion

For **Full Clear progress and capture colors**, local capture/source proof and a qualified empty scan advance each species in the biome you are clearing without a report. The same proof removes its missing row and persists after moving away. For other biomes without local proof, report plus minimum captures is the shared completion path, with the existing fixed-count and Honeybug exceptions. Accepted report trust remains valid until contradicted by positive remainder/source evidence. Reports never remove a local missing row by themselves. Personal bests retain their separate personal-capture requirements.

For **Unique panels**, first shared catches advance species independently of reports, except Rockmite/Snoozle awaiting source resolution or cd confirming unseen zero spawn, and needed Honeybug checking/report confirmation. Needed Cavern sources can contradict their completion even when visible from another biome. Other ordinary species do not wait for a local Sparkling check.

For **Sparkling panels**, a trusted report can replace ordinary local check evidence. Entering/scanning the biome clears that inherited ordinary check state so local evidence can revalidate it; remaining needed mounds/walls can contradict source completion. Gimmiegold uses its hd override outside Haunted and strict local checking inside. Bird food, Macaw no-spawn, pending delivery and actual pending-Sparkling gates retain their separate rules. Wumpa/Doomspiral still need a catch.

## X/X text and bottom reminders

Only **Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4** receive a species-row counter:

| Mode | Suffix |
| --- | --- |
| Full Clear | `X/N captured`, using shared captures |
| Unique / Unique + Sparkling | `X/N checked` only if the party needs that Sparkling discovery; the row still disappears when its Unique completion condition passes |
| Sparkling | `X/N checked` only if the party needs that Sparkling discovery |

Counters are clamped to N. They describe evidence; their presence does not add a completion requirement beyond the table above.

The current bottom reminders are separate from species rows:

- **Bee nests to punch:** In Full Clear, retain highlights on unpunched actual nests until shared Honeybug captures reach the retained verified nest count. Neither fd nor captures plus currently rendered bees can hide those remaining nests. Punching an individual nest immediately removes its waypoint; when every nest is punched there are no nest waypoints even if bees still need catching. Unique/Sparkling retain their existing local-check and trusted-fd behavior. In Unique the helper remains after the first catch only for a needed Honeybug discovery; Sparkling also requires that discovery to be needed. No historical-UUID comparison or fixed three-nest guard gates this footer.
- **Snooper walls to break:** requires known walls not all broken. Full Clear keeps it; Unique keeps it while Snoozle is incomplete and either uncaught or its Sparkling discovery is needed; Sparkling keeps it while a needed Snoozle check is incomplete.
- **Mounds to break:** requires a positive remaining mound count. Mode eligibility follows the Snoozle rule, using Rockmite.

There is currently no separate bird-food-remaining or Gazer-checked footer. Gazer uses its species-row counter. In Sparkling mode the panel title and editor label are **Unchecked** across Forest, Cavern, Icy and Haunted. The saved panel setting/position stays shared with Missing. The empty list says `All checked!` in Sparkling and `All caught and checked!` in the other modes; a source footer can still appear separately.

## Billygoat example: current behavior

The requested example separates Unique's missing row from its Sparkling progress check. **Current code uses the same first-catch completion for both**, so a needed Sparkling Billygoat does not hold Unique progress open for two checks. Full Clear removes Billygoat from the missing panel and advances local progress after at least two shared catches and an empty scan within 30 blocks of X/Z -121/-55, retaining that proof after leaving. Without local proof, remote progress waits for `id` plus its capture requirement. Sparkling uses two retained qualified checks rather than requiring both to be simultaneously rendered. Unique's separate Sparkling-progress gate in the proposed example remains a documented difference.

## Source owners

- [SafariPanels](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/ui/SafariPanels.kt): list inclusion, progress denominator, X/X suffixes and bottom reminders.
- [SafariRun](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt): shared catches, mode completion, reports, positive remainders, sources and exceptions.
- [SafariFullClear](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClear.kt): capture minima and four fixed totals.
- [SparklingChecks](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SparklingChecks.kt): retained checks, source checks, coin accounting and Wumpa prerequisites.
- [SafariCompletionZones](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariCompletionZones.kt): biome zone owners and additional UUID minima.
- [SafariBirdLedger](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariBirdLedger.kt): food targets and announced versus scanned population.
- [SafariBiomeClearTracking](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariBiomeClearTracking.kt): runtime scan inputs.
- [SafariHelperRules](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariHelperRules.kt): source reminder eligibility.

## Last updated

2026-10-18 - Replaced bird announcement/source assumptions with qualified render/capture rules and restricted fd eligibility.

2026-10-17 - Removed detected-Sparkling sightings as a separate completion requirement; normal species rules remain.


2026-10-17 - Full Clear retains unpunched nest highlights until the known nest population is captured; fd and rendered bees cannot hide them. Unique/Sparkling report trust is unchanged.

2026-10-17 - Rockmite/Snoozle use retained safari-wide sources: Unique catch plus source proof, report-confirmed unseen zero spawn, and Sparkling checks without ordinary captures.

2026-10-17 - Updated Honeybug capture/check/report separation and the all-biome Sparkling Unchecked label.

2026-10-17 - Updated Gimmiegold slot-derived Sparkling checks and outside-Haunted hd trust while retaining Unique/Full Clear captures.

2026-10-17 - Audited source/report separation and reconciled no-Macaw, extra-coin and cross-biome remainder rules.

2026-10-17 - Full Clear progress now advances from local completion in the clearing biome; remote biomes retain report rules and earned local progress survives travel.

2026-10-17 - Clarified independent Honeybug species and nest rows, actual-nest capture completion, and remote Forest-report trust retired on entry.

2026-10-17 - Added all 37 critters with current missing/progress behavior in each mode, source reminders and explicit differences from the proposed Billygoat example. Implemented and tested Full Clear local missing-panel separation; in-game validation remains outstanding.

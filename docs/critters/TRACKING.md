# Shared Safari evidence rules

[Critters](README.md) · [Mode summary](../../SAFARI_LOGIC.md)

Based on the supplied Hypixel Safari observations, clarified on 2026-10-14. [Exact supplied messages and audiences](MESSAGES.md) are recorded separately; descriptions here are semantic. Chosen mode goals are policy; server evidence establishes whether their conditions hold.

## Entry and starting resources

[Entry protection](../SAFARI_ENTRY_GUARD.md) holds Torrhus entry for requester readiness and Safari-instance entry for current-party tab presence, plus a two-second pause after each verified kick message, on every attempt, regardless of run detection; holding Shift bypasses only the current interaction. These checks do not add catches, starting items or confirmed admissions.

- Entry is through the Safari Manager at Critter Safari Entrance on the canyon server (called Torrhus Canyon in the supplied description).
- The global entry banner names the IGN (possibly with rank/Chat Heads text); the separate queue attempt is visible only to the leader. The transfer goes to `mini` plus four characters. Queue attempts are not multiple completed entries.
- The Manager admits leaders with `I already saw your ticket, so you're free to go.` and members with `Looks good to me. Have fun out there!`. Keep the applicable acknowledgement associated with the same entry; a retry alone must not award another starting item.
- Record ticket type: Basic, Economy, Premium or First Class. Basic adds no ticket-specific starting quest item. No additional Economy/Premium effect is specified here.
- Head Start grants one random quest item via `HEAD START! You started with a <Quest Item>!`. First Class independently grants one item **without a message**: inspect initial inventory immediately after Head Start when present. Do not add the named Head Start item again on top of that inventory total. Keep grants/hotspot separate from later floor pickups.

| Head Start | Basic / Economy / Premium | First Class |
| --- | --- | --- |
| Absent | 0 starting items | 1 starting item |
| Present | 1 starting item | 2 starting items |

Nine possible starting items: **Icebreaker, Yogi Berry, Wriggleworm, Bag of Seeds, Shining Coin, Purple Gem, Lime Gem, Orange Gem, Soothing Incense**. Two grants need not be different. Initial inventory supplies the total; account separately for any subsequent floor pickups.

## Early party starting-resource synchronization

The exact server message `HOTSPOT! Your Hunting Hotspot is the <Biome> Biome!` is the maintainer-confirmed departure trigger. Two seconds later, send one concise absolute report of every witnessed starting Shining Coin, Bag of Seeds, Wriggleworm and Yogi Berry, independently of discovery loading, mode, HUD and ESP. Examples: `[SM] Starting items: 2x Bag of Seeds` and `[SM] Starting items: 1x Shining Coin; 1x Yogi Berry`; use `none` for a witnessed empty applicable grant set.

The live protocol appends ` [run:<32-hex reward-display UUID>]`. A receiver must have witnessed that same real STRING reward marker in its current run, and must verify the parsed sender's UUID against the current party roster. Bind queued lookups to the run/world/connection and invalidate pending work on party changes. Guild/private/quoted/nonparty messages do not supply contributions. New unscoped Starting items messages are not admitted by the live inbox. Never broadcast on another player's behalf.

Preserve early inventory/grant witnesses before use and close starting inference before the first post-HOTSPOT resource interaction/pickup or at the two-second deadline. Head Start supplies one overlapping lower bound; silent First Class inventory can show the second grant, including an identical item. Floor pickups stay separate. If both grants were never observed before consumption, or the first snapshot/marker context is late, do not invent the missing item or report an unwitnessed zero. Missing roster/reports and incomplete initial sources remain unknown. Context unavailable at the two-second boundary delays dispatch rather than fabricating provenance. Duplicate HOTSPOT/entry/polls, own echoes, repeats and legacy notices cannot add the same sender/item twice. Consumption never reduces known opportunity demand.

Older/non-mod clients and late joiners may leave report coverage unknown. Legacy unused-item notices can provide conservative deduplicated quantities, but cannot establish a complete original inventory. Bare legacy notices and normal done words have no shared on-wire run token: only current live, roster-verified messages are admitted; stored history is never replayed. A previously unseen delayed legacy line cannot be intrinsically distinguished from a fresh identical one. New scoped starting reports reject other-run marker identities.

## Predicting unopened floor-drop types

The supplied Shiny Hunter JAR predicts quest-drop types from loaded display groups and numeric entity spawn order; it does not read a literal reward name from each unopened marker. See [mapping, evidence and implementation handoff](FLOOR_DROPS.md). Requested filtering: Cavern gems; Forest bird food; Haunted incense/coins; Icy Icebreakers. This changes highlight selection only: all critter spawn/check/capture rules remain unchanged. Predictions never count as pickups, used resources or proof that no needed drops remain.

Haunted typed need checks are independent: missing Sparkling Gimmiegold requires collecting **all Shining Coin drops**, not just satisfying its minimum spawn count. In Sparkling mode, missing Doomspiral keeps **Incense ESP until four confirmed incense floor pickups or four incense held at once at any point in the run**, including starting incense; everyone owning Doomspiral suppresses Incense ESP entirely. Sparkling mode hides coin ESP entirely if nobody needs Gimmiegold. Every mode further limits incense to the closest four known spawns to (4,47,-27), three with one starting incense, or two with two starting incense, and stops incense ESP after four floor pickups or four held simultaneously. See [Haunted filtering](FLOOR_DROPS.md#haunted-item-specific-need-rules) and [waypoint selection](FLOOR_DROPS.md#incense-waypoint-selection-in-every-mode).

## Evidence ledger

| Hypixel evidence | Deduction |
| --- | --- |
| Critter UUID | Observed entity identity; Rare/Epic/Legendary escape returns may use a replacement UUID |
| `You threw a critter capsule at the <critter type>!` | Hit confirmed; a capture attempt has started for that species |
| Critter escaped your critter capsule message | That attempt failed; the critter remains uncaught |
| Personal capture confirmation | One personal and shared catch of the named species |
| Party loot-share confirmation | One shared catch; preserve catcher identity when supplied; no personal catch |
| Floor-drop pickup confirmation | Resource acquired from the floor; keep biome/item/quantity when supplied |
| Starting-item grant | Starting resource, distinct from a floor pickup |
| Birdfeeder spawn announcement | Informational only; never establishes bird spawning or food use |
| Mound terminal reveal / empty message | One mound resolved with one Rockmite / no Rockmite |
| Wall destroyed message | One wall resolved; does not by itself prove a Snoozle spawned |
| Coin gobbled / critter appeared message | One coin consumed and one Gimmiegold spawned |
| Hotspot announcement | Run context; no extra catch or spawn unless a further rule establishes it |

Keep resource grants, floor pickups, resource uses, spawns, sightings and catches separate. A spawn announcement followed by its UUID must not become two spawns. A capture reward quantity is not a number of captured critters. Attribute a catch to a UUID only when the correspondence is established; species-only chat cannot identify an arbitrary nearby UUID.

## Capsule escape and replacement identity

- With a **standard capsule**, Common/Uncommon catches are guaranteed once hit; Rare/Epic/Legendary can escape. **Masterful capsules guarantee capture once hit for every rarity**. Any thrown capsule can miss; no hit means no confirmed attempt.
- Throw/input alone does not establish a hit or capture attempt. `You threw a Critter Capsule at the <Critter>!` confirms the hit and starts an attempt for that species. It does not identify a particular UUID by itself.
- The critter-escaped-your-capsule message ends that attempt unsuccessfully; catches remain unchanged. Only the successful capture or loot-share confirmation increments its corresponding catch count.
- Retry sequence: **throw → hit-confirmation → escape → throw → hit-confirmation → … → capture confirmation**. Each confirmed retry is another attempt at an uncaught critter, not an additional spawn. No hit confirmation means the throw is not a confirmed attempt; missing messages leave the outcome unknown.
- For the escape-capable rarities, a returned critter may have a different UUID. Their distinct UUID totals across the run are therefore not a reliable population count or proof that every original critter was checked. Do not apply this escape explanation to Common/Uncommon identity changes.
- For Unique/Sparkling checking, retain each distinct qualified UUID for the run; repeated sightings of the same UUID add nothing. Retired, removed or captured models are not new live sightings. A sighting or escape does not add a confirmed catch or food/coin-use event.
- Do not require matching old/new UUIDs merely to apply retained UUID checking. Historical spawn/catch attribution still needs evidence; species, position and timing alone do not justify merging unrelated events.
- Reported Macaw test: failed capsule capture followed by reappearance and renewed Sparkling ESP. Replacement UUID is a possible explanation; ESP re-highlighting alone does not verify the UUID change. Confirm old/new UUIDs and the escape event together when investigating.

## Late biome entry and remaining counts

- Keep this run's capture and loot-share confirmations even while the player is in another biome. Visiting that biome later does not reset or duplicate them.
- For an established species minimum **M** and **C** confirmed shared catches already received, the minimum remaining requirement is **max(0, M − C)**. Scan for remaining live critters and unresolved spawn sources, accounting for possible escape replacements.
- Reaching M does not discard additional known spawns. Full Clear also needs every verified additional spawn accounted for and all relevant opportunities resolved. Do not add caught totals and seen UUID totals blindly; they may describe the same critters.
- Catches before this client joined the run are unknown unless Hypixel provides retrospective evidence. Empty loaded space alone cannot reconstruct those missing catches or prove a whole biome clear.

### Partially cleared biome

- **Full Clear:** local capture/source and qualified empty-scan completion advances progress without a report; variable species in other biomes without local proof require a party biome report and real minimum captures. Fixed-count species can complete early. Positive remaining critters or known unfinished sources reopen only their species; personal PBs retain strict local/personal evidence.
- **Sparkling:** inside the species' Sparkling scan radius, require retained distinct qualified UUID checks and safely reconciled shared captures to reach its Sparkling minimum, plus its special source requirements. Partial catches are useful but not sufficient below that threshold. Merely entering the biome, scanning outside its radius or seeing no entities without enough catches is insufficient.
- Minimum captures plus a qualifying in-radius absence scan must not remain pending solely because the original UUIDs are gone. Conversely, the minimum alone or an out-of-radius empty scan must not produce completion. Known additional outstanding spawns still matter; capsule escape replacement UUIDs do not create additional required spawns.
- Apply these radius rules where a species has a defined check radius. Structure/feeding/coin species retain their specific source checks; do not invent a radius or silently waive unresolved sources.
- Treefrog/Woodchucker scan coverage is reported at approximately 50–55 blocks from Forest center. Preserve that range until the exact boundary is established. Completion-check coverage is separate from ESP visibility; a rendered critter lacking an outline remains positive presence evidence.

### Forest mid-run: Honeybug

- A teammate's previous nest punches are not directly observable. Count shared confirmed catches plus currently loaded, uncaptured bee UUIDs as alternative evidence of bees already released. Do not add historical UUIDs for captured bees to their catches again.
- Use the actual bee-producing nest count. Once shared catches plus currently loaded uncaptured bees account for that count, nests can be unhighlighted in every mode. Confirmed handling of all actual nests also establishes that all nest opportunities are resolved.
- Nest highlights and Honeybug ESP are independent: Full Clear keeps uncaptured bees highlighted; Unique stops bee ESP and removes the species row after one shared catch, even with a missing sparkling discovery; Sparkling never shows ordinary Honeybug ESP. If the party still needs Sparkling Honeybug, Unique retains the separate nest footer/highlights until all nests are personally punched or shared catches plus currently rendered bees reach the actual nest count. Otherwise its first catch also hides the nest helper. Sparkling retains its needed nest work.
- Honeybug's needed checking remains separate from Unique's first-catch species row: outside Forest, progress waits for the trusted fd report; local resolved nests also supply checking. Trust survives later Forest entry. Actual sparkling messages/detection remain independent of ordinary bee ESP. See [Honeybug](honeybug.md).

### Haunted mid-run: Gimmiegold

- Gimmiegold Sparkling checking uses the retained initial Shining Coin floor-slot total: known source/report coverage, no remaining coin markers or held coins, and shared captures + current rendered uncaptured Gimmiegolds reaching the slot count plus known extra coins. Unknown catalog/inventory stays pending. A demand-current, roster-verified hd supplies remote spawning proof only outside Haunted, without waiving known held/remaining resource blockers. Unique still needs one actual catch; Full Clear retains actual minimum captures and its source/absence rules.
- If the minimum is not met, keep it pending. A teammate may still hold Shining Coins; this explains the empty world but is an inference, not confirmation of their inventory. Some earlier Gimmiegold catches below the minimum do not close this mid-run case.
- The minimum without the absence evidence is insufficient. New floor drops or outstanding Gimmiegolds must be accounted for rather than hidden by the earlier deduction.

### Forest mid-run: birds

Use [Forest bird completion](../BIRD_COMPLETION.md) for Bluebird, Parakeet and Macaw. Base demand is nine food/spawn opportunities plus known absolute starting-food contributions. Only qualified renders inside the X/Z 6/51, radius-15 bird circle establish spawning; shared captures + current birds account for it, with two Macaws per opportunity. Feeder attraction chat is ignored. Raw remaining Forest floor food is checked Safari-wide; locally held bird food also blocks completion.

Inside Forest, Sparkling requires all expected spawning; Unique adds a shared catch of each bird, except proven no-Macaw spawning. Outside, fd supplies spawning trust; Unique may waive only the last Macaw catch once Bluebird and Parakeet are caught. Full Clear always needs the real expected capture population, retaining one-Macaw credit; fd alone cannot complete birds. Other Forest species keep their own rules.

### Forest birds: shared feeder, qualified reports

An initial Forest clearer needs six separate local bird-food floor pickups, empty bird-food inventory, no remaining food markers and local mode completion to send fd. Late entrants cannot send it. Local starting food counts even without a party notice. Extra-food notices increase demand once per sender/type; old reports cannot waive the new demand. Revalidate queued reports after the current inventory sample. See the linked bird policy for examples and validation.

An earlier detour to another biome does not disqualify the Forest clearer: the late-arrival check uses bird captures already received before their first Forest entry, alongside the six local pickup requirement. Retain earned bird spawning proof and completion through travel and reentry without requiring fd, including a qualified no-Macaw finding. New food or increased expected demand still blocks insufficient retained proof.

## Unused starting food/coin reminders

Starting quantities are shared early as described above, not when a player clears a biome. End-of-clearing reminders are **local titles only**. Keep the existing four-second trigger/spacing, current held quantities, independent discovery gates and destination suppression: food only while a bird discovery is needed and outside Forest; coins only while Gimmiegold is needed and outside Haunted. There is no new setting.

Single-category titles remain **USE YOUR BIRD FOOD!** / **USE YOUR SHINING COIN!**. A mixed reminder uses **USE YOUR STARTING ITEMS!** with one combined item/quantity/destination subtitle, so one title does not immediately overwrite the other. Recheck eligibility at display time; a temporarily suppressed item remains eligible later. Mark only local presentation state after display.

Do not send an automatic unused-item party message, local chat echo, replacement label, retry or fallback. A title never creates or appends a contribution/outstanding obligation. Existing legacy inbound formats remain accepted through the common sender/type maximum ledger. Normal `fd`/`cd`/`id`/`hd`, ticket readiness and unrelated party commands are unchanged. Additional known quantities reopen only their affected spawning requirements; they never add catches or floor pickups.

## Completion deductions

### Highest-priority Icy ESP rule: missing Sparkling Wumpa

- If any party member still needs Sparkling **Wumpa**, enable ordinary ESP for each Icy species with zero confirmed shared catches: Strongarm, Tepid, Polaris, Shuddersquid, Billygoat, Mantis Shrimp, Nozzlenose, Troodon and Wumpa.
- Apply this before every mode's normal ESP eligibility rules, even when the species' own sparkling discovery is already shared or its check/timeout would otherwise hide it. This rule does not mark anything captured or complete.
- After the first own capture or party loot share of a species, stop forcing its ESP through this override and apply that mode's remaining rules. Full Clear or profitable ESP may still keep it highlighted. Evaluate each species independently.
- A Wumpa check/catch does not supply another Icy species' first catch. While the party still needs its discovery, do not end this override for an uncaptured species solely because Wumpa was checked.

### Mode completion

- **Sparkling:** check the spawns of species for which someone in the party needs the Sparkling variant. No general capture requirement; spawning prerequisites still apply where necessary.
- **Unique:** ordinary species complete from one shared catch; Honeybug additionally needs checking/report confirmation in progress when its discovery is needed, while its missing row still clears on the first catch. Rockmite/Snoozle need one shared catch plus resolved safari-wide sources; `cd` can confirm an unseen zero-spawn case after all sources resolve; confirmed Sparkling capture overrides and the no-Macaw exception remain independent.
- **Full Clear:** local capture/source and qualified empty-scan completion advances progress without a report; variable species in other biomes without local proof require a party biome report and real minimum captures. Fixed-count species can complete early. Positive remaining critters or known unfinished sources reopen only their species; personal PBs retain strict local/personal evidence.
- When the missing/check panel is fully complete, show **All caught and checked!** in Unique and Full Clear, and **All checked!** in Sparkling. Retained checked UUIDs and actual capture counts remain distinct.

- **Unique:** ordinary species complete from one shared catch; Honeybug additionally needs checking/report confirmation in progress when its discovery is needed, while its missing row still clears on the first catch. Rockmite/Snoozle need one shared catch plus resolved safari-wide sources; `cd` can confirm an unseen zero-spawn case after all sources resolve; confirmed Sparkling capture overrides and the no-Macaw exception remain independent.
- **Full Clear:** local capture/source and qualified empty-scan completion advances progress without a report; variable species in other biomes without local proof require a party biome report and real minimum captures. Fixed-count species can complete early. Positive remaining critters or known unfinished sources reopen only their species; personal PBs retain strict local/personal evidence.
- **Sparkling:** use the retained UUID-check rule below plus species-specific source checks. Seeing an ordinary critter can contribute without catching it. The intended beneficiary's missing-discovery set must itself be known; these events alone do not establish everyone's collection.
- No visible critter in currently loaded space is only local absence. Unknown nests/walls/food/coins or incomplete UUID coverage keep the corresponding check pending. More positive evidence can reopen a previous deduction.
- Party biome reports are accepted as shared completion state under the rules below, but never manufacture Hypixel catches or personal PB evidence. Highlight timers, rendered boxes and arbitrary delays do not prove completion.

### General Sparkling count rule

- For non-bird species being checked in Unique or Sparkling mode (except Gimmiegold's current-population validation), record each distinct UUID seen in its qualifying scan area once and retain it for the remainder of that run. Preserve partial totals and completed X/X checks when entities unload, render distance changes, the player changes mode, or a biome is revisited. A new run resets the history.
- Scrappy, Gemzie, Troodon and Gazer show these retained totals in their fixed X/X checked counters. Other species retain the same internal check evidence without adding new fixed counter suffixes.
- Repeated scans of one UUID add nothing. Record the shared catch count at each UUID's first live sighting. For each such baseline, the baseline catches plus distinct UUIDs first seen at or after that baseline are a valid checked-population lower bound. Take the largest bound or confirmed capture count. This combines earlier loot shares with later sightings without counting a seen critter again when its catch arrives.
- Outside a species' defined scan area, sightings do not establish a new qualified check. Previously qualified checks remain valid. Source gates remain separate: food, coins, nests, walls, mounds, new resources and unresolved party-resource notices can still block combined completion without deleting the UUID history. Reentry revalidates these sources; retained global mound/wall proof remains usable without a local critter scan.
- Full Clear remains based on actual shared/personal capture requirements and live remainder/source checks. Retained UUID checking never manufactures captures, food use, coin spending or personal PBs.

## Missing/check panel by mode

| Mode | Species rows |
| --- | --- |
| Full Clear | Local capture/source proof and a qualified empty scan advance progress and remove the missing row without a report; retain that proof after leaving. Other biomes without local proof use party report plus minimum captures, with existing fixed-count exceptions. Positive remaining critters reopen local completion. See the [per-critter panel reference](../MISSING_PANEL.md). |
| Unique | One confirmed shared catch immediately completes Unique and removes the missing row, without a local Sparkling scan/radius requirement. Detected Sparkling entities do not add a separate capture requirement. |
| Sparkling | Show a **needs checking** checklist for required discoveries. Remove a species after all its Sparkling requirements pass; reopen it if later qualifying evidence invalidates completion. This is a check list, not a capture-only missing list. |

- In every mode, list only plain critter names in their species colors. The only gray text on a critter row is the applicable fixed `X/M captured` or `X/M checked` suffix. Do not append checking prose or duplicate species counters. Rockmite/Snoozle awaiting source resolution or cd confirmation of no spawn remain listed.
- Only **Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4** have the confirmed fixed-spawn **X/X** suffix. Label the quantity explicitly by mode; never show a bare `1/4`.
- **Full Clear:** local capture/source and qualified empty-scan completion advances progress without a report; variable species in other biomes without local proof require a party biome report and real minimum captures. Fixed-count species can complete early. Positive remaining critters or known unfinished sources reopen only their species; personal PBs retain strict local/personal evidence.
- **Unique:** ordinary species complete from one shared catch; Honeybug additionally needs checking/report confirmation in progress when its discovery is needed, while its missing row still clears on the first catch. Rockmite/Snoozle need one shared catch plus resolved safari-wide sources; `cd` can confirm an unseen zero-spawn case after all sources resolve; confirmed Sparkling capture overrides and the no-Macaw exception remain independent.
- **Sparkling:** use the same **X/M checked** calculation as Unique's added check. One qualifying live Gazer with no catches shows **1/4 checked**; spawning/seeing a critter does not become a capture. All species retain qualified UUID checks for the run. Escape replacements and spawn announcements themselves never add captures.
- Do not add X/X suffixes to other critter names. Source requirements still gate completion; the three conditional source-helper footers below remain separate from the species list. Food/spawn gates and independent bird completion still apply.

### Missing panel presentation

In Full Clear, retain highlights on unpunched actual nests until shared Honeybug captures reach the retained verified nest count. Neither fd nor captures plus currently rendered bees can hide those remaining nests. Punching an individual nest immediately removes its waypoint; when every nest is punched there are no nest waypoints even if bees still need catching. Unique/Sparkling retain their existing local-check and trusted-fd behavior.

Forest retains **Bee nests to punch** while nest work is unresolved. Cavern retains **Snooper walls to break** for intact/unknown walls and **Mounds to break** while mounds remain. Keep these in a separate footer beneath the plain species list, with their counts in gray.

- **Full Clear:** local capture/source and qualified empty-scan completion advances progress without a report; variable species in other biomes without local proof require a party biome report and real minimum captures. Fixed-count species can complete early. Positive remaining critters or known unfinished sources reopen only their species; personal PBs retain strict local/personal evidence.
- **Unique:** ordinary species complete from one shared catch; Honeybug additionally needs checking/report confirmation in progress when its discovery is needed, while its missing row still clears on the first catch. Rockmite/Snoozle need one shared catch plus resolved safari-wide sources; `cd` can confirm an unseen zero-spawn case after all sources resolve; confirmed Sparkling capture overrides and the no-Macaw exception remain independent.
- **Sparkling:** show unfinished source work only if someone needs that species' discovery.
- Gate each helper independently by Honeybug, Snoozle or Rockmite; another missing discovery cannot keep it visible. Hide resolved sources. Preserve unknown wall counts rather than claiming those walls are broken.
- Gazer has no separate footer in any mode; keep its only counter on its plain species row. No extra absence prose or survey warnings are restored.

**Wumpa and Doomspiral are capture-required exceptions in Sparkling mode.** A needed boss remains unchecked until this run receives a personal capture or party loot-share confirmation. Seeing or retaining its UUID, a spawn/awoken/slain message, and a party biome-completion report cannot substitute for that capture. The confirmed catch completes its check immediately and remains valid across unloading, biome reentry and mode changes. Other species keep their existing checking rules.

Positive Sparkling names and skin matches use [one detector path](../SPARKLING_DETECTION.md) for alerts and the detected list. Ordinary scan circles remain unchanged; delayed post-start skin updates are reread, and every currently loaded positive can be detected beyond the former 80-block display cap. Detection does not add a capture requirement.

## Sparkling detection does not gate completion

The user reports repeated entities for the same Sparkling during capsule capture. A detected name/skin identity therefore does not add a mandatory capture requirement in Unique, Full Clear or Sparkling mode. Keep detection, alerts and confirmed catches separate from completion policy.

- Retain model/name-tag aliases for alerts and the detected list. Known resolved aliases stay excluded from the detected HUD and live-population accounting. New or lingering detections cannot reopen completed species or add missing-panel targets.
- Apply the normal capture, source, party-report and qualifying-check rules. Pending detected identities cannot block species/biome progress, queued biome reports, run-complete alerts or Unique PBs.
- Confirmed Sparkling personal catches, loot shares and trusted global catch announcements still complete ordinary Unique/Sparkling species immediately, even if duplicate detected identities remain. The three birds also require their shared spawning/source evidence. Full Clear retains its normal capture/source requirements. Reconcile paired announcement/reward messages without manufacturing ordinary catches or PB evidence from a global announcement.

## Full Clear report trust and positive revalidation

- **Honeybug exception:** retain the initial verified bee-producing nest count for the run. That population caught completes Full Clear from any biome without a local empty scan or party report. A trusted fd supplies Unique/Sparkling checking and survives Forest entry, but never supplies captures. Unique's missing species row still needs its first actual catch; progress also needs checking/report trust when Sparkling Honeybug is needed. Needed Sparkling Honeybug nest work remains separately after that catch until all nests are personally punched or shared catches + currently rendered bees reach the verified nest count; if nobody needs that discovery, the first catch also removes the Unique nest helper. See [Honeybug](honeybug.md) for the mode/report details.

- **Variable species:** qualified local capture/source and empty-scan completion advances progress and removes the missing row without a report; retain that proof after leaving the scan radius or biome until positive remaining evidence reopens it. For other biomes without local proof, progress and capture colors require a received biome-done report plus the applicable shared capture minimum. Minimum catches alone or an unqualified empty scan cannot establish local completion. See the [per-critter missing-panel reference](../MISSING_PANEL.md).
- **Known fixed counts:** Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4 can complete progress from their confirmed shared capture totals without waiting for a report or an exhaustive UUID survey. Their Full Clear missing rows still need local empty-scan proof. Checked UUIDs never increment their captured counters.
- Each species is evaluated independently. A report never adds captures or personal PB evidence. Numeric minima retain the existing baseline caveat; dynamic bird/mound quantities use their confirmed source evidence. Macaw retains its one-catch/no-spawn exception.
- Reports persist across entry, unloading and incomplete scan coverage. Only **positive remaining-critter evidence** reopens that species, including loaded critters observed from a different biome. Retain the number of additional captures evidenced by the current population; do not accumulate historical UUIDs or count escape replacements repeatedly. Later capture confirmations resolve this work without seeing the old identities again.
- Known unbroken Rockmite mounds/Snoozle walls, and explicit outstanding extra-resource notices remain positive blockers. Unknown unloaded space is not an unbroken source or an uncaptured critter. Additional ordinary Macaws do not defeat the one-catch exception. A report alone cannot establish the zero-catch no-Macaw outcome: require local resolved-food/no-spawn proof, or one real catch. Known held food, held coins, new required coin spawns and unresolved food delivery remain blockers even after a report.
- Strict local capture/source evidence remains separate for the mod's own completion reports and personal PBs. Trusted remote progress is not proof of a personal clear. Unique and Sparkling use their separate rules.

## Biome completion and party synchronization

- **Unique progress advances per species on its first confirmed personal capture or received loot share**, including catches received outside that biome. Local Sparkling scans, radii, source counts and other species' catches do not gate that ordinary progress. Trust the party to check missing discoveries.
- **Rockmite and Snoozle are the exceptions:** each completes from one shared catch plus all its safari-wide sources resolved. A party Cavern-done (`cd`) report can supply unknown checking evidence after a catch. Without a catch, require resolved sources, no actual species encounter, and cd confirming no spawn. No catch of another Cavern species is required. Reports persist across biome changes. Positive Sparkling captures retain the global completion override.
- After `cd`, positively observed unbroken mounds reopen only Rockmite, and intact walls reopen only Snoozle, regardless of whether anyone needs the corresponding Sparkling variant. Repeated reports cannot erase known unbroken sources.
- Observe loaded wall positions and all rendered Cavern mound sites without a player-distance cutoff from **any Safari biome**, regardless of HUD/ESP settings. Positive loaded-world completion can resolve an earlier contradiction without entering Cavern. Unloaded walls and unobservable mound sites remain unknown; an empty scan outside Cavern with no known mound sites must not invent a completed survey.
- The Unique missing panel follows the same completion rules: ordinary captured species disappear; Rockmite/Snoozle awaiting source resolution or cd confirmation of unseen zero spawn remain listed. Ordinary first-catch ESP still stops after a catch.
- Detected, uncaptured Sparklings remain mandatory capture blockers in every mode. A party report cannot resolve one or manufacture captures, personal PB evidence or item consumption.
- Outgoing completion messages still require being in the current biome. Purely local Cavern source checks may qualify the mod's own `cd`; progress accepts that report when it arrives in chat. Do not echo received completion reports.
- Full Clear retains its existing capture/report rules. Sparkling uses resolved global mound/wall proof without ordinary catches or a local critter scan, retaining that proof on entry. Other Unique species do not revert because ordinary local discovery checks are missing.

### Unique with a missing party discovery

| Species | Unique progress and missing panel | Discovery work |
| --- | --- | --- |
| Rockmite | Shared catch plus all mounds resolved, or report-confirmed unseen zero spawn; intact sources reopen completion | Loaded mound-site evidence may resolve it from another biome |
| Snoozle | Shared catch plus all five walls resolved, or report-confirmed unseen zero spawn; intact sources reopen completion | Loaded wall-block evidence may resolve it from another biome |
| Honeybug | First catch removes its species row; progress also needs checking/report trust if its discovery is needed | Retain party fd through later entry, or finish local nests |
| Bluebird / Parakeet / Macaw | Shared catch plus bird spawning proof; retain the documented no-Macaw exceptions | Qualified renders inside Forest; fd trust outside, with known food blockers |
| Every other species | Complete and remove the row after the first shared catch | Trust party checking; local Sparkling scan/source work does not gate Unique progress |

Sparkling mode retains its actual per-species source/scan rules. Positive detections add no separate capture obligation in any mode. Source highlights can remain independent of progress; no ordinary species needs a local scan before its Unique catch is counted.

### Delayed spawn evidence: Gemzie

- The exact Hypixel message `A rumbling sound can be heard, and the door at the back of the chamber opens...` checks Gemzie after five seconds, without entity sightings. Duplicate messages do not restart the delay; player quotations do not start it. The proof lasts for the run, including Cavern reentry.
- The timer supplies 3/3 checked in Sparkling mode and the needed Sparkling check in Unique mode. Qualified retained UUID/capture evidence remains an alternative. Cavern may complete once Gemzie's delay and the remaining active mode checks pass.
- This proof supplies no captures. Unique Gemzie still needs its first shared catch. Full Clear retains its capture/absence requirements, including after a remote `cd`.

## Variable spawn and activity events

| Source | Observed progression | Deduction / limit |
| --- | --- | --- |
| Birdfeeder | Bluebird / Parakeet attracted; known plural `Two Macaws were attracted to the Birdfeeder!` | Informational only; use qualified render/capture evidence instead |
| Rockmite mound | Four cracking/progress lines, then terminal reveal or terminal empty | Progress is not a spawn; terminal outcome resolves that mound |
| Snooper wall | Ran headfirst / some cracks → keep running / more cracks → destroyed / maybe something hiding | Third line confirms destruction; use resulting UUIDs/catches to establish Snoozles |
| Shyworm | Surfaced nearby / don't let it see you → eight rotations → hid back into ground | Activity cycle; not a catch and not assumed eight seconds |
| Shyworm interrupted | Fled because startled after missed capsule, or saw a player and fled | Interrupted cycle; no successful catch implied |
| Haunted sewer | Coin gobbled and Gimmiegold appeared | One consumed coin creates one new critter UUID; count event and later sighting once |

Known mound progress text retained from existing server-message notes: `Small cracks begin to form in the mound...`; `The cracks seem to be getting larger, keep hitting it!`; `Chunks of the mound begin falling away...`; `The mound is about to fall to pieces! Keep going!`. The supplied explanation associates the last progress line with an empty outcome; **the terminal reveal/empty message remains the conclusive outcome**. Exact progress variants for each outcome have not been separately established here.

Resources: Cavern floor drops include the three gem colors and other items; Icy floor drops supply Icebreakers; Haunted floor drops supply Soothing Incense and Shining Coins. Forest bird-food pickup/feeding evidence belongs in the bird ledger. A starting grant can introduce these resources without a matching biome floor pickup.

### Full Clear Gimmiegold source recovery

For the observed three-floor-coin baseline, confirmed pickup/use history outranks speculative marker typing: three actual floor coins collected, all acquired/starting coins consumed and a fresh empty coin inventory establish the source in the qualifying Haunted area. This is Full Clear source recovery only; Sparkling uses the initial-slot/current-population rule below and Unique keeps its first-catch rule. Unknown/predicted markers cannot invent more coin opportunities after that baseline. Additional actual coins and outstanding party notices remain blockers. Without full local confirmation, retain the conservative scan-based source recovery. Retained UUID totals are not coin-use totals and must not inflate a subsequent extra coin's requirement. See [Gimmiegold](gimmiegold.md).

### Gimmiegold Sparkling source total and party trust

Retain validated source-group identities Safari-wide, independently of highlights. Local Sparkling requires complete initial catalog/report coverage, no known remaining coin markers, empty sampled coin inventory and a qualified **C + U >= F + S** proof (shared real captures + current uncaptured canonical models; distinct observed coin groups + known absolute starting coins). There is no three-coin Sparkling fallback. Disappearance alone neither confirms a pickup nor lowers F; positive population/source evidence may establish all opportunities without fabricating pickup events.

Outside Haunted, a fresh applicable `hd` can provide remote spawning proof without local renders. Known held/remaining resources still block it. New contributions or slots invalidate the old demand stamp and reopen announcements. Inside Haunted, local source/population evidence is authoritative. Unique and Full Clear retain actual capture requirements. See [Gimmiegold](gimmiegold.md).

## Evidence still to pin down

- Exact boss spawn/post-capture messages are now recorded in [MESSAGES.md](MESSAGES.md). Remaining prerequisite-to-spawn relationships still need evidence; do not invent them from having items alone.
- Confirm numeric species minima and total coin opportunities from Hypixel. The supplied bird example establishes nine Forest food pickups plus applicable starting food; party contributions can add more feeding demand. Other existing baselines remain in the critter index, not promoted to new server facts.
- Preserve formatting, multiline entry banners and optional Chat Heads segments when testing the [supplied message variants](MESSAGES.md). The supplied coin message confirms the spelling **Gimmiegold**.

After one local clearing trigger, keep independently eligible **local title** candidates available; repeated polls do not restart the delay. Recompute held quantities, destination and discovery need before display. Showing a title marks presentation only. The HOTSPOT signal opens a bounded two-second initial grant window; sample before the first resource input, then freeze before use/pickup. Known ticket/grant minima and the two-grant cap prevent invented zero/oversized reports.

## Last updated

2026-10-19 (America/New_York) — Early absolute starting-resource reports at HOTSPOT+2s, explicit unknown coverage and local-only later reminders (issue68).

2026-10-18 - Applied qualified bird renders, mode-specific remote trust and initial-clearer fd eligibility.


2026-10-17 - Removed detected-Sparkling completion gates in all modes while retaining alerts and confirmed-catch completion.

2026-10-17 - Aligned entry guards with tab-first Safari presence, two-second kick pauses, ticket-message downtime release and Shift override.

2026-10-17 - Added per-interaction Shift entry override with local guidance and background missing-member name lookup.

2026-10-17 - Clarified unused floor-drop notices with resource quantities and relevant missing Sparkling discoveries; retained legacy parsing.

2026-10-17 - Full Clear retains unpunched nest highlights until the known nest population is captured; fd and rendered bees cannot hide them. Unique/Sparkling report trust is unchanged.

2026-10-17 - Rockmite/Snoozle use retained safari-wide sources: Unique catch plus source proof, report-confirmed unseen zero spawn, and Sparkling checks without ordinary captures.

2026-10-17 - Retained Honeybug initial nests and fd checking trust; separated needed Unique progress from its first-catch missing row and named every Sparkling checklist Unchecked.

2026-10-17 - Added Gimmiegold initial-slot/current-population validation and sender-deduplicated extra coins; remote hd only overrides ordinary outside-Haunted checks.

2026-10-17 - Audited all documented rules; clarified report contradictions, no-Macaw proof and reminders independent of report sending.

2026-10-17 - Local Full Clear completion also advances progress; other biomes without local proof retain report requirements.

2026-10-17 - Distinguished Honeybug first-catch rows from needed nest reminders and documented remote Forest-report trust until entry.

2026-10-17 - Separated Full Clear's local missing-panel proof from report-based progress; retained qualified coverage after leaving the scan radius.

2026-10-16 - Added Full Clear report/minimum gating, fixed-count early completion and positive-only revalidation without exhaustive UUID coverage.

2026-10-16 - Full Clear progress uses report plus minimum captures, with early fixed-count completion and positive-evidence reopening.

2026-10-16 - Unique now advances per species without local discovery checks; Rockmite/Snoozle need cd, with loaded-source checks available outside Cavern.

2026-10-16 - Unique advances per shared species capture; Rockmite/Snoozle additionally require cd with loaded-source revalidation.

2026-10-16 - Unique trusts party biome checking after one real shared capture of every species; only needed Rockmite/Snoozle sources can contradict that report. Observed uncaptured Sparklings still block every mode.

2026-10-16 - Shortened starting-resource party reminders and batched all eligible items into one message, preserving independent source obligations and legacy parsing.

2026-10-16 - Confirmed Sparkling captures immediately complete Unique/Sparkling species; Full Clear completes only after remaining captures/sources, with no stale Sparkling obligation.

2026-10-16 - Replaced distinct bird UUID checks with shared captures plus current nearby bird counts, feeder evidence, raw Forest drops and inventory; no-Macaw completion applies in all modes.

2026-10-16 - Added Unique's resolved no-Macaw exception for Forest progress and missing-panel removal; retained real catch/PB evidence.

2026-10-15 - Corrected incense limits to 4/3/2 for 0/1/2 starting incense; retained unified positive detection and ordinary scan circles.

2026-10-15 - Fixed complete, independently gated starting-resource reminder delivery (#55).
2026-10-16 - Linked entry readiness/kick/presence protection; retained all shared evidence rules.
2026-10-17 - Clarified continuous entry presence checks regardless of run detection; retained all shared evidence rules.

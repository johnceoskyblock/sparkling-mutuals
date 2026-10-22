# Manual implementation scenarios

## Purpose

Select the scenarios affected by a change. These are procedures and expected observations, **not completed test receipts**. Current behavior belongs to the linked rule owners; [audit findings](IMPLEMENTATION_AUDIT.md) distinguish suspected defects from server uncertainties.

## Prerequisites

Use a matching client/JAR, record revision, mode, party state and relevant configuration, and preserve the original evidence. Read [the documentation index](README.md) and the affected species/feature. Do not send unreviewed raw logs, recordings or credentials. Native offline fixtures cannot certify Hypixel mechanics.

## Procedure

### Safari-wide Cavern sources

- Exit the boat and remain outside Cavern. In Unique, receive one Rockmite and one Snoozle loot share, then have a teammate break all mounds and all five walls. Each species should complete without cd or entering its scan circle.
- In Sparkling, repeat without catches: needed Rockmite/Snoozle checks complete from resolved sources anywhere in Safari. Detected Sparkling identities must not revoke otherwise valid species or biome completion.
- In Unique with no actual Rockmite/Snoozle encountered or caught, resolve all sources: remain pending until party cd. A seen but uncaught ordinary critter must still require a shared catch. Verify no catch is invented by the report.
- Leave/reenter Cavern, toggle modes and unload chunks: retain source proof. Unknown/unloaded sites must not become broken; newly observed intact sources reopen their species. Full Clear still needs actual captures and its existing local-proof/report rules.

### Floor drops

- **Coin ESP across modes:** with all discoveries owned, confirm Full Clear highlights remaining coins before/after picking up and spending several coins, while Unique/Sparkling hide them. With Gimmiegold missing, all three modes retain every remaining coin highlight. Switch modes repeatedly; pickups and spending must not create a permanent cutoff.
- **Captured Sparkling remnants:** capture a Sparkling with multiple name-tag/model carriers, then leave the captured model or tags loaded. The Detected sparklings HUD must remove it, and no stale tag/body may reopen its missing row or inflate its population target. Ordinary Unique/Sparkling species complete after confirmed Sparkling capture; birds retain their spawning requirements; Full Clear completes when its actual ordinary population/source requirements are also met. A separate new detection alone must not reopen completion; only independent ordinary remainder/source evidence can reopen the applicable Full Clear outcome.

- **Forest:** the predictor supports a nine-food prefix: three Wriggleworms, three Yogi Berries and three Bags of Seeds. No XP/essence/shard markers should receive quest ESP. Check the label against each actual reward. Repeat with a starting food and with a teammate stacking extra food.
- **Cavern:** two predicted Lime/Purple/Orange sets. Pick up Orange: both Orange markers should disappear from ESP while unacquired Lime/Purple remain. Repeat for each color. Placing gems and changing biomes must not restore already satisfied colors. Check a starting gem as well.
- **Icy:** Full Clear offers all five predicted Icebreaker markers while none has been acquired. One pickup or starting Icebreaker hides the rest, including after use. Unique and Sparkling should not automatically show Icebreakers.
- **Haunted coins:** when someone needs Sparkling Gimmiegold, all remaining predicted coin drops stay eligible and the minimum capture count cannot waive their collection. Test with Doomspiral already shared so Incense ESP is off in Sparkling mode while coin ESP remains.
- **Haunted incense in Sparkling:** only a missing Doomspiral discovery enables it. Starting incense and inventory totals do not advance its floor-pickup counter. Three pickup units with no separate peak/starting-amount cutoff keep it eligible; the fourth unit turns it off even when Gimmiegold is still needed. When everyone owns Doomspiral it stays off. In Unique and Full Clear, the fourth actual incense floor pickup also hides every remaining incense marker, even while Gimmiegold is needed. Spending incense or changing biomes must not restore those markers. When everyone owns Gimmiegold, Unique/Sparkling hide coin ESP; Full Clear retains all remaining coins regardless of pickups, holdings or capture completion.
- Check floor controls, biome-only visibility and mode/discovery changes against the documented group/per-type composition; automatic Cavern/Icy paths do not universally use the floor-group enable switch. Selection must change highlights without changing catches, inventory, resource use or completion evidence.
- Unload/reload chunks, cross biome boundaries and let a teammate collect drops. Earlier observed anchors must survive. Start a new Safari/server: previous numeric entity IDs and resource cutoffs must reset.
- Try entering after earlier drops have already been taken. Predictions may be wrong when the true first group was never observed. Record mismatches with `/sm debug` and the pickup-correlation log; a prediction is never a confirmed resource pickup.

- **Gimmiegold green capture row:** collect/use every coin and catch all resulting Gimmiegolds, including starting/extra coin spawns. In the Haunted check radius the capture row turns green even with remaining incense markers. Repeat with an uncaught Gimmiegold, unused coin, extra-resource notice and unknown/partial marker: completion stays pending as applicable. Spawned-but-uncaught critters may satisfy Sparkling checks, never Full Clear captures.

### Forest and Macaw

- **Full Clear nests after fd:** retain an initial four-nest survey, receive fd and enter Forest later. With fewer than four shared Honeybug captures, all unpunched nests still highlight, including when remaining bees are rendered. Punch one: only its waypoint disappears. At four shared catches, all remaining nest highlights hide. With all nests punched but insufficient catches, no nest markers remain, but Honeybug capture completion stays pending. Repeat in Unique/Sparkling: fd still hides the checking helpers.

- **Mid-run Forest:** enter after teammates used the food, without local pickup/feeder history. With a verified local Forest survey, nine accounted-for feeding opportunities (shared catches + current live birds, two Macaws per opportunity) and no remaining food-source markers/held food should resolve feeding. Test all caught, all live and mixed populations. Sparkling can complete with live birds; Full Clear still needs Bluebird/Parakeet captures and only one shared Macaw catch.
- In Unique/Sparkling, repeat with eight opportunities, incomplete source observations, a hidden food marker, held starting food and increased extra-food demand: remain pending where local/remote proof is insufficient. In Full Clear, use its actual capture formula and known blockers; unknown floor/inventory samples alone are not a blocker ([D9](IMPLEMENTATION_AUDIT.md#d9)). Observe duplicate/replacement UUID behavior rather than asserting identity recovery. Resolve additional food with additional population. Leave/reenter Forest to revalidate source conditions while retaining earned bird population proof, not historical bird UUID totals. Confirm no personal captures, food pickups/use counts or personal food-completion alert are fabricated.
- **Macaw override:** in Full Clear, catch one Macaw personally or receive its loot share. While known food remains unused, Macaw stays incomplete. Once the actual Full Clear bird capture formula passes with no known food blockers, all bird capture rows turn green even with several Macaws still visible. Extra Macaws alone must not block Forest's completion message.
- New starting/floor/party-reported food reopens the food requirement. Resolve every new feeding; the same one recorded Macaw catch remains sufficient afterward.
- For Full Clear, real bird captures must cover expected spawning and known normal-bird population. A remaining known Bluebird or Parakeet keeps the shared bird capture requirement pending. Feeder attraction chat must not establish spawns.
- A Macaw pair means two birds and one feeding. Bird feeding uses current population instead of accumulated UUIDs; record actual escape/replacement identity behavior. Retained ordinary checks have no explicit replacement merge ([D8](IMPLEMENTATION_AUDIT.md#d8)).
- Check Honeybug with a teammate releasing bees before you arrive. Shared catches plus current uncaptured bees can resolve all identified bee-producing nests once site coverage is known. Ordinary Honeybug ESP: Full Clear keeps uncaught bees, Unique stops after one shared catch, Sparkling shows no ordinary bee ESP.
- Pending in-game Honeybug check: in Unique, verify one shared catch removes the species row; the nest footer/highlights remain only for a needed Sparkling Honeybug and resolve after every actual nest is personally punched or shared catches plus live uncaptured bees reach the actual nest count. Check both owned and missing party discoveries. In Full Clear, retain the verified initial nest population and complete from its shared capture messages in any biome, without waiting for fd or another local empty scan. Receive fd outside Forest and verify Honeybug checking trust persists after Forest entry despite incomplete rendering. Unique still needs one shared catch for progress, while Full Clear still needs the initial nest population captured. Detected Sparkling entities must not add a separate completion requirement.
- Discover another nest or an unknown nest site after earlier progress: outstanding source work must reopen. Sparkling detection should still work independently of ordinary bee ESP.

### Mode checks and party synchronization

- **Unique:** each ordinary first shared catch immediately advances progress and removes its missing row, even outside that biome with all local discovery checks unfinished. Rockmite/Snoozle need their own catch plus resolved sources, or cd supplying unknown checking. Positively observed intact sources reopen only their respective species regardless of discovery ownership.
- Send/receive fd/cd/id/hd. Ordinary Unique catches advance without reports; Rockmite/Snoozle can use cd for unknown checking or resolved unseen zero spawn; their own catch plus resolved local sources needs no cd or other Cavern catch. Report echo is suppressed. Full Clear/Sparkling retain their report/source revalidation.
- Catch critters through loot shares while outside their biome, then enter it. Counts must persist once. Reaching the minimum plus a qualifying empty scan should not require old captured UUIDs to reappear.
- In Sparkling, test three live UUIDs; one shared catch plus two live; two catches plus one live; three catches plus none. Repeated sightings of one UUID never add progress. Distinct qualified UUID checks across separate scans retain progress; all required critters need not be rendered simultaneously. Retired/captured models are excluded from new live sightings.
- **Gemzie:** Hypixel's exact door-opening message must check Gemzie after five seconds without scans, including the needed Sparkling check in Unique. Verify 4,999 ms remains pending and 5,000 ms supplies 3/3 checked; duplicate messages cannot restart the delay, player quotations cannot start it, and biome reentry retains it. A new run resets it. The timer supplies no catches; Unique still needs its first shared catch and Full Clear keeps its capture/absence requirements.
- **Icy priority:** while anyone needs Sparkling Wumpa, each uncaught Icy species gets ordinary ESP in every mode. A shared Wumpa catch completes every Icy Sparkling check, but does not supply another species' actual first catch; the Icy ESP priority remains per-species while Wumpa discovery is needed. After a species' first shared catch, its normal mode rules resume.
- Unique biome announcements require current presence in that biome. Receiving its last loot share while elsewhere must not send the completion word remotely. Enter and validate it locally.
- Scrappy, Gemzie, Troodon and Gazer are the only species with fixed X/X suffixes. Full Clear must say X/M captured using confirmed catches only; Sparkling and relevant Unique checks say X/M checked using retained qualified UUIDs reconciled with catch baselines. A caught Gazer plus three live Gazers must show 1/4 captured in Full Clear and 4/4 checked in Sparkling. In Unique these appear only for needed discoveries. Verify all modes show only colored critter names and gray applicable fixed counters: no survey warning, absence prose or duplicate Gazer row. Retain the separate Forest bee-nest and Cavern wall/mound footers for outstanding work: Full Clear regardless of discoveries, Unique before the first catch or while that species' needed Sparkling check is unfinished, Sparkling only for missing discoveries. Resolve sources and confirm their footers disappear. Source requirements/highlights still apply.
- Break every Snooper wall and mound. Progress dialogue must not count as a catch or terminal outcome. Newly found intact sources must reopen their applicable source checks even after an earlier report, including Unique when the discovery is owned.

### Resources, captures and entry

- Finish a different biome while holding unused starting bird food or a coin. If the relevant discovery is missing, receive an automatic title (USE YOUR BIRD FOOD! / USE YOUR SHINING COIN!) and one compact combined party notice such as `[SM] Unused floor drops: 1x Shining Coin (missing Sparkling Gimmiegold); 1x Yogi Berry (missing Sparkling Macaw)`. The mixed-resource title is USE YOUR STARTING ITEMS!; sender and item types provide player/destination information, while each group names its missing Sparkling targets. Test several bird foods together and verify only party-missing birds are listed. Verify each included item retains independent spawn obligations. Neither has a config toggle. Suppress duplicates, used resources, notices in the destination biome, and notices when everyone owns the relevant discoveries.
- Receive an unused-resource notice after Forest/Haunted was complete. It must reopen the affected source work without adding a pickup or catch. Repeated notices and old population cannot resolve it. Require every extra feeding/coin spawn; Full Clear still needs its applicable captures, with the Macaw exception above.
- Enter partially cleared Haunted with zero drops and no Gimmiegolds. Fewer than three shared catches stays pending. For local Sparkling completion, require the retained initial coin-slot registry to be resolved and enough captured/current population; absence alone cannot establish an unknown catalog. Remote hd can override ordinary checks while outside Haunted.
- Check Basic and First Class, with and without Head Start. Initial inventory should account for zero/one/two starting resources, including two identical items. The Head Start message and inventory must overlap once. Queue retries, entry banners and hotspot messages must not manufacture grants or catches. Ticket type remains unknown if no identifiable selection was observed.
- Test standard capsule misses, confirmed hits, escapes, retries and successful captures. Only capture/loot-share confirmations increment catches. A species-only hit/catch must not retire an arbitrary nearby UUID. Test a Masterful capsule and a replaced Macaw UUID too.
- Test optional Chat Heads text in entry and loot-share messages. Shard quantities are rewards, not capture counts. Own catches and shared catches remain separate; shared catches must not create personal PBs.
- Disable HUD/ESP, perform work, then reenable them. Tracking should have continued. Verify a personal Forest Full Clear PB still requires a personal Macaw catch even though one party catch satisfies shared Full Clear progress.

### Updated GUI defaults and Hideyho scope

- **ESP text scale:** verify default 2× label size; move the ESP-tab slider between 0.5, 1, 2 and 4. Check distant floor names, critter timers, nest labels and teammate names for readable size, centering, colors and unchanged visibility. Close/reopen settings and restart the client to confirm persistence.
- With a fresh or partial configuration, independently switchable GUI features start on except Coins at Insta Sell, Achievements to Party, Warp reminders and Capture projections. Modes remain mutually exclusive; numeric/text selections and mode presets keep their established behavior.
- Save disabled features and panel borders, enable the default-off exceptions, then restart: ordinary explicit choices must persist except documented startup ESP enable presets and mode-owned fields ([D5](IMPLEMENTATION_AUDIT.md#d5)).
- Arm Hideyho in Haunted, then leave before it settles. The pending watch must cancel. Hideyho dialogue and entities observed from Forest, Cavern or Icy must not emit inside/outside titles/chat there. In Unique and Full Clear, entering Haunted can still detect its current settled hiding location. In Sparkling, no inside/outside title or chat should appear. Switch to Sparkling while a watch is armed: cancel it without a delayed alert. Return to Unique/Full Clear and confirm normal detection resumes.

### HUD editor and Honeyhives

- Open `/sm gui`. Hover each green alert: All Gems, All Bird Food, All Incense and Run completion must match their displayed text. Drag/resize each separately, close the editor and trigger that alert; it must use the corresponding saved position/scale/style. Reopen and restart to verify persistence. Check Warp reminder as well and try a custom Run completion title.
- Follow [Honeyhive tests](HONEYHIVES.md#source-and-validation-references), especially the screenshot's refill countdown versus green beacon, adjacent hive independence, profile-not-yet-detected scans and the exact Queen Bee proc. `/sm debug` now includes raw hive labels and saved-state diagnostics.

### Gimmiegold completion recovery and retained Gazer checks

- In the sewer, collect/use all coins and load the resulting Gimmiegolds. Sparkling completes with the required captured + live population; Unique also needs one shared catch. Full Clear turns green only after every required Gimmiegold is actually caught. Repeat with missed spawn messages, starting/extra coins and an extra-resource party notice.
- No held coins and sufficient current population must not bypass an unresolved registered coin slot in local Sparkling. Full Clear marker uncertainty uses its separate confirmed-coins recovery path when all actual conditions pass; do not impose the Sparkling slot rule on that recovery. Empty/invisible display survivors must not keep the source pending; a complete incense triple crossing block boundaries must not count as coins. Leave render range after validation: the Gimmiegold check remains. Acquire an additional coin: require another Gimmiegold. Reenter Haunted: revalidate sources and the current captured-plus-rendered population; historical Gimmiegold UUIDs cannot supply that population.
- See the four Gazers one at a time, moving each out of render range before the next. The gray checked counter retains each distinct sighting and stays at 4/4 after all four, including leaving/reentering Haunted. Repeated sightings of one UUID do not add progress. Catch an already-seen Gazer and confirm it is not counted twice. In Full Clear only actual catches increment X/4 captured. A new run resets retained sightings.
- If Gimmiegold remains pending, run `/sm debug` in the sewer and inspect the logged coin/source/population counts and remaining marker count.

### Retained checks for every critter

- In Unique and Sparkling, see Scrappy, Gemzie, Troodon and Gazer one UUID at a time. Unload each before the next: partial and completed X/X checked counters must remain steady. Repeat for ordinary species. Gimmiegold Sparkling uses captured + current rendered population, not old UUID checks. Birds ignore feeder announcements and use qualified shared-capture/current-render evidence, never historical UUID counts.
- Revisit a biome and switch modes: retain qualified UUID history for the same run. New food/coin/source work may reopen completion without erasing checked sightings. A new run must clear all retained checks and the coin-slot/contribution ledger.
- Check duplicate UUIDs, catches of already-seen critters and earlier loot shares followed by new sightings. Repeated identical UUIDs must not add checks. Capture-baseline reconciliation should follow the implemented formula; unpaired replacement UUIDs are an uncertainty to reproduce ([D8](IMPLEMENTATION_AUDIT.md#d8)). Out-of-area sightings do not establish qualified checks; Full Clear captured counters and personal PBs never advance from sightings.

### Supplied Gimmiegold debug-log regression

- Reproduce three floor coins picked/used, none held, one Gimmiegold caught and two still in the water. Unique completes with its first catch. Sparkling completes only if all registered initial coin slots are collected, inventory is empty and the captured/current population reaches the slot-derived target; decorative predictions are not extra slots. Full Clear stays red until the remaining two are caught, then turns green.
- `/sm debug` should show the new Sparkling record with `floorMinimum=3 remaining=0 required=3 capturedPlusRendered=3 checked=true` for a confirmed three-slot run. The separate Full Clear ledger can still show `sourcePath=confirmed-coins`. Retained UUID history can be larger; it must not become the required capture count or make a fourth coin require nine Gimmiegolds.
- Repeat with only two actual floor coins plus a starting coin, a held coin, an unconsumed fourth actual coin and an unresolved party notice. Those cases must not bypass their applicable source requirements.

### Completed missing-panel wording

- Finish every required catch and check in Unique or Full Clear: the empty missing panel says **All caught and checked!**. Sparkling retains **All checked!**.
- Unique ordinary species complete on the first shared catch without needing discovery/radius/source scans. Rockmite/Snoozle need source resolution or permitted cd trust; resolved unseen zero spawn also requires cd. Sparkling retains its discovery checks; Full Clear retains remaining capture/source requirements.

### Ticket earned and automatic warp

- Follow [ticket-earned checks](CONTEST_DOWNTIME.md#source-and-validation-references): one party announcement at Uncommon, own contest hold cleared immediately, remote sender-specific release and other downtime holds retained. Repeat with contest HUD/auto-request disabled and at a new contest window.
- Follow [warp timer checks](WARP_ALERTS.md#source-and-validation-references): enabled expiry shows the alert and sends one `/p warp`; disabled/replaced timers, player-chat quotes, departures and changed instances cannot fire stale warps. Verify settings and command controls persist.

### Floor-drop colors and automatic contest request

- In **ESP → Floor-drop colors**, set the nine quest types to different colors. Verify outline, fill and name labels match each type; close/reopen settings and restart to confirm persistence. Test two gem colors and coin/incense together. Existing filtering must still hide unneeded drops.
- Check an older config with only the shared floor color: all types inherit it. Use **Customization → All floor-drop colors** to recolor every type, then change an individual color again and save without modifying the shared control: the individual choice must survive.
- Follow [automatic contest downtime checks](CONTEST_DOWNTIME.md#source-and-validation-references): enabled sends one party request from the confirmed leader at a new contest start; members and moderators remain silent, disabled stays silent, the toggle persists, and joining/enabling mid-contest does not send a stale request.

### Shared Sparkling detector and skin range

- Follow [detector checks](SPARKLING_DETECTION.md#source-and-validation-references): delayed post-start skins, skin-before-name aliasing, unchanged ordinary scan circles, real-player exclusion, cross-biome loaded detections and the observed Duplico unload point. The sheet covers 36 skins; Tepid keeps the name fallback.

- Inside Safari, receive WAX_ON packets without a name/skin: show one unknown alert/party notice per run and retain coordinates/world labels. Repeated bursts and new locations remain silent; a later identified species may alert once.
- Receive particles from another loaded biome, lower vanilla particle settings and unload the area: process received packets and retain last-seen locations, without claiming server-wide delivery. Torrhus/outside-Safari particles produce no markers.
- Identify a body/name within three blocks of a location: retire its anonymous waypoint and cancel unsent unknown text/banner. Test moving/nearby critters to observe grouping ambiguity, then start a new run and verify locations/notification histories reset.

### Sparkling detections do not gate completion

- In each mode and biome, isolate a new name/skin identification after normal completion without introducing independent ordinary remainder/source evidence. Identification alone adds no gate or duplicate notification. If a new eligible uncaught entity also appears, evaluate its ordinary Full Clear population effect separately. Include a Sparkling Macaw with the bird formula already satisfied.
- Reproduce duplicate detections during capsule capture, unload/reenter and switch modes. Unresolved detection identities alone must preserve completion; independently observed ordinary remainder/source evidence still applies. Repeat with alerts/ESP disabled.
- Confirm a personal Sparkling capture, loot share or global catch announcement. Unique/Sparkling complete ordinary species even with duplicate identities, while birds retain spawning requirements; Full Clear still requires its normal catches and sources. Ordinary sightings never fabricate catches or personal PBs.
- Detect a new Sparkling after a biome report or run-complete alert: do not reopen or send a duplicate. Real outstanding ordinary capture/source work must still block normally.

### All five incense spawns in every mode

- In Unique, Full Clear and Sparkling, enable the relevant incense requirement and verify all five available spawns can highlight, including the farthest from (4,47,-27). Repeat with zero, one and two starting incense: no distance/slot limit applies.
- With zero starting incense and no other cutoff, three single-unit pickups leave remaining incense eligible; the fourth unit hides all incense ESP. Spending items, reentry and mode changes must not restore it. Four held simultaneously also keeps the existing permanent cutoff.
- Confirm mode/discovery gates, colors and coin visibility still work independently. Collected drops themselves no longer render.

### Sparkling Haunted floor-drop filters

- In Unique/Sparkling, with nobody missing Gimmiegold, coin ESP stays hidden before any coin pickup; Full Clear retains it. Mark Gimmiegold needed and verify coin ESP returns independently of Doomspiral/incense needs.
- Start with two incense, collect two more and hold four simultaneously: incense ESP stops in every mode, even with fewer than four floor pickups. Spend the incense and leave/reenter Haunted: it stays hidden. A new run resets this state. Verify the existing four-floor-pickup cutoff still works too.

### Sparkling boss capture exceptions

- With Wumpa or Doomspiral missing from party discoveries, load the boss in Sparkling mode and wait beyond its ordinary scan interval: it must remain in the missing panel and must not increase Checked progress. Repeat after unloading/reentry, spawn/slain messages and received `id`/`hd` reports.
- Catch it personally, then repeat in a new run with a party loot share instead. Either confirmation completes its check immediately and retains completion after reentry. Separately, an accepted global Sparkling boss-catch announcement completes that boss via the override without adding a shared counter; only a shared Wumpa counter increment triggers all-Icy checking. Wumpa shared capture checks all Icy species; other species otherwise use their documented rules; Full Clear retains its capture/source/absence requirements.

### Combined history import

- Open the History tab: **History** is first and **Import History from Logs** is immediately below. The old launch-import toggle is absent. Both the button and `/sm history import` import runs and Sparkling finds together, with progress and a completion count.
- Follow the [history import Minecraft checks](HISTORY_IMPORT.md#source-and-validation-references), including an older-log folder, a repeated import, existing native records, estimates, a corrupt archive, persistence and an ongoing run. Verify the explicit Profit price action also reprices imported records ([D6](IMPLEMENTATION_AUDIT.md#d6)). Unknown-duration imports with all 37 species can satisfy current speed-award predicates ([D7](IMPLEMENTATION_AUDIT.md#d7)); record that actual behavior as a suspected defect scenario.

### Documentation revisions

- Run `./scripts/DocumentationSync.ps1`: unchanged normalized document hashes should report no changed documents; that result does not certify runtime correctness.
- Edit a rule and its Last updated section: the command must list that document, including when edited twice on the same date. A new document should appear as New; a removed one as Removed.
- Read [SOURCE_SYNC.md](SOURCE_SYNC.md) before recording a new baseline. Record only when its documented source-review contract is satisfied for every covered document, with discrepancies represented; documentation review does not imply code changes or completed gameplay tests.

Issue #55: one clearing completion must deliver coin plus food, two foods and two-item stacks without another completion event. Recheck destinations, discovery needs and consumed quantities before one combined title/chat. Check one message contains every currently eligible item, duplicate sends are suppressed, and malformed received batches cause no partial progress changes. Old verbose notices must still parse. Suppress only the irrelevant item and retry unsent items if eligibility returns. Delay world loading beyond three seconds and verify initial inventory/HEAD START/floor pickups remain distinct. Confirm separate new-spawn obligations and clean state on new run.

### Credited party Sparkling streak commands

- Follow [streak command checks](SINCE_COMMAND.md#source-and-validation-references): teammates' credited catches, unique versus repeat finds, active versus outside-run credit, critter/catcher details without recorded wording, missing history, multiple mod clients and restart/import persistence.

- Have multiple players request downtime during a run. Return to Torrhus: one combined party reminder names every requester and reason, including when you did not request downtime. Resume/ticket/departure before publication must remove obsolete requests; two mod clients should suppress duplicate reports.

Verify per-species Unique progress by receiving each ordinary first catch from another biome with no report or local scan. Rockmite/Snoozle require their own catch plus resolved sources or permitted cd checking trust, independently of other Cavern catches. Observe loaded mounds/walls while standing outside Cavern and verify their source contradiction reopens the applicable species regardless of discovery ownership; unloaded sites cannot prove completion. Confirm verified source completion restores them without entering Cavern. Detected Sparkling entities add no separate completion requirement in any mode; no fabricated captures/PBs or echoed reports.

Verify Full Clear report trust: capture a variable species' minimum and perform a qualified empty local scan before its biome report; local progress, capture colors and missing rows must complete without the report and persist after travel. Other biomes without local proof retain the report/minimum path. Receive the biome report and verify minimum-satisfied species complete independently, without needing to render every UUID. Below-minimum species stay pending. Scrappy/Gemzie/Troodon/Gazer may complete from their fixed capture totals before a report. Reenter and positively detect remaining critters: only that species reopens. Capture the evidenced remainder without reloading old UUIDs and verify completion returns. Unloading or a repeated report alone cannot capture a known remainder. Retain Macaw's one-catch/no-spawn exception, positively unbroken sources and outstanding extra-resource notices; detected Sparkling identities add no separate gate. Personal PBs require their own strict evidence.

- For the suspected source-ledger difference, receive a one-item notice then a larger notice from the same sender/type. Contribution maxima increase but the existing outstanding record is not replaced; compare dedicated and generic targets rather than expecting a corrected implementation ([D3](IMPLEMENTATION_AUDIT.md#d3)).
- For Party Unique PBs, compare first catches/local zero-spawn absence against HUD source/report gates; they are distinct predicates ([D14](IMPLEMENTATION_AUDIT.md#d14)). For Sparkling boss reward text, record its accepted run capture and missing dedicated boss PB feedback ([D15](IMPLEMENTATION_AUDIT.md#d15)).
- Observe seven Foxtrots before fd, receive six captures, then fd after the live remainder disappears. Current remote progress may complete with the local target/missing row still at seven; this report-timing difference is expected actual behavior to reproduce ([D17](IMPLEMENTATION_AUDIT.md#d17)).

### Source synchronization regressions

- Outside Forest, receive fd with Bluebird and Parakeet caught but no Macaw: Unique may complete Macaw without inventing a catch. Enter Forest and require qualified local spawning/no-Macaw proof. Full Clear must still satisfy actual expected captures.
- In Full Clear after `cd`, observe an uncaught Cavern species while standing in Forest. Only that species reopens; unloading and another report do not resolve it, but its additional catch does.
- After `fd`/`hd`, acquire unused food/coins. The affected Full Clear rows reopen; a fourth actual coin requires a fourth capture. Existing Sparkling and Unique policies remain separate.
- Receive a teammate's biome-done report before finishing your own Unique clearing work, with an eligible starting coin/food held. Once local clearing passes, the local starting-item title still displays once, without an unused-item party message; repeated scans cannot keep restarting its delay.

### Gimmiegold initial coin slots

- Check `/sm debug` before entering Haunted: the Sparkling record reports distinct initial coin slots, not three display entities per slot or decorative triples. Collect all but one with enough critters already spawned; completion stays pending. Unloading the last marker cannot collect it.
- With every slot collected and no coins held, qualify a scan near the sewer. Shared captures + currently rendered uncaptured Gimmiegolds must reach the initial slot total. Sequential historical sightings cannot substitute for that population. Incense highlights remain independent.
- Send a mixed unused-items notice with two coins and one food from a teammate, then repeat it. Only two extra Gimmiegolds are required; another sender adds their own amount. Own starting coins and the own echoed notice overlap.
- Receive hd outside Haunted: ordinary Sparkling checks may complete. Unique still requires one shared catch and Full Clear still requires the real minimum captures. Enter Haunted or receive hd while inside: local sources/inventory/population must pass. A detected Sparkling does not override otherwise valid completion.

### Retained Honeybug nests and Unchecked naming

- Exit the boat for the first time and record actual nest count N. Unload Forest and receive N bee capture messages: Full Clear Honeybug completes without revisiting its render range. No report alone may manufacture these captures.
- With Sparkling Honeybug needed in Unique, one catch removes the missing row but progress stays pending until local nest checking or fd. Without the needed discovery, one catch completes ordinary Unique progress. In Sparkling no ordinary catch is needed.
- Receive fd, then enter/reenter Forest with incomplete rendering: Honeybug checking remains trusted. Detect another Sparkling identity and verify it does not revoke normal completion in any mode.
- Switch to Sparkling in each biome: the checklist header and HUD editor use Unchecked. Unique/Full Clear retain Missing behavior and saved panel positions/settings survive mode changes.

- Forest reporter: regardless of personal pickups or prior bird catches, require qualified capture-plus-current-render spawning proof and no held/remaining food before fd. Test late entry both before any bird catch and after loot shares, including known extra starting food. All other active-mode gates still apply; a received fd suppresses an echo. Test new food arriving while a report is queued.

### Capture-shot collection

- Follow the complete [collection checklist](CAPTURE_SHOT_RECORDING.md#collection-procedure), beginning with a small ordinary-capsule sanity export. Confirm useful raw/tick/display/target fields before larger or Masterful collection.
- Verify off/start/status/stop/export, duration/input/byte limits, opening/finalizing refusal, both hands and variants, and block-to-air fallthrough. Starting outside a loaded Safari world must fail. Unsupported held items must not become inputs.
- Compare recorder off/on with ESP display disabled and capsule hiding off/on in separate sessions. Existing capture/history/completion totals, retained checks, alerts and settings must behave identically. Confirm no party chat or automatic upload; local start/stop/error notices are expected.
- Exercise delayed metadata, pre-existing capsules, rapid throws, another nearby thrower, escape/removal/unload and a reused entity ID. Keep uncertain ownership/outcomes unknown; absence or timeout must not become a miss. Repeated genuine feedback stays separate, shared catches/startles never settle local captures.
- Check movement, camera, target bounds and loaded-block obstructions; preserve raw packet paths, arrival times, reconstructed endpoints and smooth samples separately. No requested chunk loads; raw discovery and cache sampling are separate from contact-scene budgets ([D11](IMPLEMENTATION_AUDIT.md#d11)). Synthetic tests and startup checks do not establish Hypixel field fidelity.
- Transfer/disconnect while recording and while a writer finishes; old-world files must not receive new-world input and reconnect remains off. Check completed and partial files using replay. Errors must preserve older exports and report once locally.
- Reserve fresh validation sessions before model inspection; verify file/session overlap rejection, per-variant frozen parameters, conditioning exclusion and explicit uncurated/ambiguous/incomplete exclusions. Optional predictions compare only exact observed times; last_observed_error refers to the last matched point with uncovered observations counted, not automatically the final observation ([D10](IMPLEMENTATION_AUDIT.md#d10)). Last observed position is not proven impact.

### Recorder v2 volume/error follow-up

- Automated: 64 moving targets plus busy target packets; input snapshots; static suppression; shared focus cadence; range changes; capsule packet preservation; 180-second / 20-throw and 600-second / nine-throw encoded budgets under unchanged caps. Check original local exceptions at hook/codec/writer boundaries and incomplete exports with safe status.
- Automated scan recovery: clearing the real entity-map type between batches must skip empty slots within the allowance, retain later capsule packets and allow normal finalization; genuine inspection failures still propagate for diagnostics.
- Gameplay pending: repeat the small ordinary-capsule sanity block in a populated Safari area, then inspect target coverage and raw capsule continuity with hiding off/on. Record status near start/stop. On error, retain the chosen JSONL and recorder-related `latest.log` excerpt; synthetic tests do not establish live field fidelity.

### Experimental capsule path

- On 26.1.2, test Iris absent, installed with shaders disabled, two representative active packs, reloading a pack, switching packs and toggling shaders off/on in the same world. Check first/front/rear views and Freecam; both amber terrain rings and cyan critter diamonds retain the flat guide's width/outline. Compare through-terrain off/on with an intervening camera sight-line wall. The guide draws after shader composition; it must not inherit pack lighting, disappear behind that wall with visibility enabled, write depth, cast guide shadows or alter existing ESP. Hide HUD/open a screen/switch worlds: no stale late guide; ordinary capsule hiding and recorder eligibility remain independent. Keep exact tested pack/version receipts and do not treat those checks as validation of every pack or of Hypixel capture physics.

- Fresh/missing/invalid configuration defaults off; settings save, reopen and restart retain deliberate choices. Toggling must not start recording or change capture/history/completion/ESP state.
- Enable the single Capture projections switch with the former second key false or absent: the full guide and terrain/critter markers must appear. Test all four legacy boolean combinations: preserve the old master choice, ignore the obsolete key and omit it on save. Fresh/missing/invalid master values default off.
- Test broader upward/downward angles, ordinary and Masterful main-hand capsules, walking, sprinting, sneaking, jumping/falling and all three normal perspectives. No stationary wait applies to the visual guide. Flight, riding, lava, unsupported items and off-hand-only use remain excluded; switching items must not leave stale geometry.
- Turn the camera and vary FOV, window size and GUI scale. Hide HUD or open any screen: hide geometry. Disconnect/world transfer: discard old stability/geometry. Disable the option: no preview line or marker.
- With Show guide through terrain off, place the preview line and an existing ESP outline behind a sight-line obstacle: the path is occluded, existing through-terrain ESP remains visible. Turn it on: the guide also becomes visible. Check normal path continuity and finite geometry. Visibility does not establish collision or target selection.
- Compare visible ordinary capsule centers with the guide while stationary in the tested range; keep recorder exports separate if used. Offline startup, synthetic geometry tests and numeric observation residuals do not certify Hypixel appearance, interpolation, impact or capture success. Record live checks still pending.

### Experimental terrain/critter contacts

- Strict recorder probes allow pitch through ±90 degrees but retain ordinary main-hand/dry/first-person/stationary/two-tick and hiding-off conditions. Exercise all four ground/flying capsule-hiding combinations: ordinary/Masterful guides and contact markers stay visible; existing dropped/flying capsule hiding retains its normal variant/distance rules. Toggle hiding around an active preview without a stability wait. Check moving and water estimates, opt-out/screens/hidden HUD, and first/front/rear views. Strict recorder probes remain unavailable with either hiding setting on and require two fresh stable ticks after both are off; raw observation collection stays independent. No recording starts from a preview.

- Only Capture projections enables the guide; width defaults 3 and saves/clamps 1–5. Toggle off/on, save/reopen/restart and retain the deliberate choice. Width and through-terrain preferences must survive migration.
- Switch capsule variants while moving: guide must update immediately, follow interpolated player eye and current character aim and remain finite. Masterful/moving paths are entirely dashed without a text overlay; stopping with an ordinary capsule restores the observed-region styling. Check normal and detached-camera aim. Unsupported item switches and opt-out must remove stale geometry. Recorder contact probes must still require two stable ordinary grounded ticks, with no probe for Masterful/moving inputs and no automatic recording.
- Check a wall inside the first 1.7 blocks before a critter, target before wall, inside-solid origin, slabs/stairs, lava/unsupported fluid and unloaded boundaries. One amber ring or cyan diamond only; uncertainty/caps never become a landing or miss. Critter proxies are unpadded/current-position and independent of ESP visibility.
- Compare the same dry and water-filled route: water must not truncate it or alter its coordinates. A submerged solid/waterlogged shape or a known critter can still stop it; lava and unloaded cells remain boundaries. Water before the final contact produces fully dashed geometry, including when aiming from land. Standing and swimming in water support ordinary/Masterful guides in normal views; flight/riding/lava and opt-out remain restricted. Dry-only recorder eligibility must reject both standing and swimming water states, and calibration snapshots must retain their original fluid boundary. Check entering/leaving water invalidates cached display state.
- In a crowded scene with more than 64 identified bodies, verify known ring/diamond contacts remain visible while internal diagnostics retain provisional coverage. Turn aim toward a late-discovered target and allow a bounded sweep to refresh priorities. Routine metadata must not restart the sweep; add/remove churn must still invalidate safely. Unknown terrain/unsupported fluid and stale selected bodies must not show farther contacts. Repeat with recording off/on; an incomplete raw export must stay incomplete.
- Turn camera, change FOV/GUI scale, open screens, hide HUD, switch item/world. No stale marker or text overlay. Dark outline/core use the selected depth mode without writes; old ESP remains visible through terrain.
- Repeatedly turn yaw/pitch across block edges with high frame rates and recording off/on. The geometry must remain current without text appearing. Repeated unchanged frames must not exhaust sixteen full checks; movement, metadata/equipment/passenger changes, removal, ID reuse and capture/model updates must invalidate old contacts promptly. Shyworm server capture bounds remain unverified.
- No Uncalibrated/Provisional/contact/restriction text appears in gameplay. Edit all HUDs has no Capsule status sample; other HUDs remain editable and keep saved positions through restart and Reset positions.
- Toggle Show guide through terrain off/on in a controlled scene: the same arc/marker should be hidden/visible behind an intervening sight-line obstacle. A nearer predicted terrain collision still clips the route in either mode. Check both outline and core/marker depth behavior without depth writes; do not infer server collision accuracy from visibility.
- With the character stationary, detach/turn/move Freecam: the world-space route must remain anchored to the character's eye/aim. Return to normal control and turn the character: the route must update responsively. Check yaw/pitch across render fractions and cycle front/rear third person with the single switch enabled. Route coordinates stay fixed when only perspective changes; geometry uses camera-relative display once. Third-person views must remain ineligible for recorder calibration, and returning to first person requires two strict stable ticks. Detached-camera viewing does not certify server firing behavior.
- Aim across an identified Rockmite mound: its transformed loaded model hull can stop the amber **Estimated mound surface** before the ground beneath it; it must never yield a cyan diamond. Its ESP/source highlight stays intact, and a released silverfish still yields a critter contact. Change the mound transform/remove it before the next tick: an old endpoint must be suppressed. Recorder calibration snapshots omit this display-only surface estimate.
- Aim at floor/wall seams, convex steps, slabs and mound edges: the amber ring wraps adjoining faces, with no chords through solid corners or across disconnected gaps/narrow slits. Check unknown/over-budget patches and retain a maximum 24 segments. The flight path must not bend along the surface.
- Aim at a surfaced Shyworm body/tail while its head is low: supported pieces can produce a cyan contact without adding worms to tracking/counts. Test disconnected pieces, different textures/poses, two nearby heads, a changed off-path head, movement/removal/identity reuse and world reset. Body support must invalidate promptly and respect existing scan/validation budgets.
- Repeat the body/tail check with more than 64 unrelated contact targets, over 300 scene entries and an unrelated equipped stand receiving metadata each tick: the initial association sweep must finish and known body contacts may appear while global coverage stays Provisional. A queued stand immediately gates body contacts until checked; draining harmless metadata can resume support without another finished sweep. Check empty-head labels, later equipment identification, coalescing, sixteen checks per tick, queue overflow and safe-sweep recovery. Displace a connector from retained target slots between two heads: ambiguity must remain. Reveal a 65th connection node through metadata exactly at a sweep boundary: overflow must not be cleared in that tick. More than 64 connection nodes or sixteen heads must withhold body estimates. Check the 160 + 64 + 16 + 16 inspection allocation and cached readiness counters.
- Check real-writer v2 plus strict v1 replay, local target aliases, UTF-8/finite/line bounds, omitted detail flags, one probe per ordinary input and unchanged raw packets/association. Both named populated workloads retain every capsule packet under unchanged caps.
- Run the configured three-target verification, helper/Python/package tests, applicable offline loaded fixtures and bounded scene profiling. Live Hypixel broader-angle launch, endpoint and proxy validation remain pending; use the six-throw sanity block before the fitting/validation matrix.

### Shyworm body diagnostics

- Run `/sm debug` in Safari with an identified Shyworm and nearby unknown displays/interaction parts. Check `SM_SHYWORM_BODY` snapshot/candidate rows, relative positions, native zero bounds, armour poses/equipment appearance, display transforms and vehicle/passenger links. Repeated captures have distinct snapshot IDs. No body proxy or recorder state changes.
- Verify no-head/outside-Safari cases, players/items excluded, direct carriers outside the proximity radius retained, nearest-head selection, 4,096-entry/2-head/64-part/16-passenger limits and explicit omissions. A missing head or truncated candidate list must not become evidence of body absence.
- Give fixtures distinctive custom names, raw profile values and arbitrary item data; prefixed JSON must omit them. Preserve finite JSON with unavailable nonfinite fields and cap block-state text. General debug output remains separate. Live collection: three surfaced ordinary-Shyworm snapshots in one world, roughly one second apart, without throwing or recording.

## Expected results

Record observed versus expected outcomes for each selected scenario, including unperformed cases. For linked suspected-defect scenarios, expected actual behavior is the current predicate described in the audit, not the desired future correction. Progress, row removal, ordinary ESP, source helpers, announcements and PBs are separate consumers. Static/unit checks establish only their stated scope; a screenshot or a passing build does not establish server capture physics.

## Troubleshooting

If a scenario fails, preserve a minimal reproduction and compare the current owner/caller path. Consult [known discrepancies](IMPLEMENTATION_AUDIT.md) before interpreting a historical expectation as implemented behavior. A recording marked incomplete stays incomplete; absence, removal or a final sample is not automatically a miss/impact. Do not broaden a focused fix to unrelated pending work.

### Early starting contributions and late reports

- Observe the central boat, leave into a biome and verify one early party announcement after two seconds. With coin + food held, test needs for both, birds only, Gimmiegold only, and neither. Expect exactly the eligible items/quantities and no zero report. With discovery API loading, wait for readiness instead of announcing every item.
- Start with two identical foods/coins and with a silent second grant; verify quantities. Consume one before dispatch, and separately pick up floor food before dispatch: only the unused starting quantity is announced. Check repeated boat transitions, new runs, reconnects and failed-send retry.
- Two clients receive the early report and its echo. Verify Forest and Haunted targets increase once per sender/type, own reports overlap inventory, and legacy notices do not duplicate early contributions.
- Receive hd, then a newly learned starting coin: the stale target must not waive it. Outside Haunted, a fresh hd can supply trust; inside Haunted, use F + S versus captured + current uncaptured renders and local source proof.
- Enter Forest late with existing bird catches and no fd: insufficient qualified population blocks; enough population plus empty held/floor food permits fd once the other active-mode checks pass, without six personal pickups. Repeat Haunted with hd and the registered coin target. Received fd/hd must not be echoed.
- Finish a biome with eligible starting food/coins still held. Expect a readable local title only, no unused-item party/local chat message. Normal fd/cd/id/hd still work. Multi-client Hypixel validation is pending; offline tests cannot establish server delivery or live marker completeness.

## Unique Haunted checking and Rockmite highlights

- In Unique with a party member missing Gazer/Gimmiegold, catch each once: both rows disappear, but Haunted progress and hd remain pending. Gazers left to check counts distinct retained checks down from four; repeated UUIDs do not double count.
- Observe coin floor slots: uncollected coins stay counted after travel/unloading, collected-but-held coins remain work, and qualified used/spawned evidence resolves the counter. Unknown catalog/inventory shows ?. New party coin demand reopens it. Received hd inside Haunted cannot bypass local checking.
- Repeat with both discoveries shared: first catches complete their Unique requirements and neither footer appears. Full Clear/Sparkling keep their own displays.
- Before the first Unique Rockmite catch, show mounds and silverfish. Afterwards hide silverfish; retain mound ESP only if anyone still needs Sparkling Rockmite. A new run restores both.

## Last updated

2026-10-22 — Added Unique Haunted progress/footer and discovery-dependent Rockmite ESP gameplay checks.

2026-10-19 (America/New_York) — Added early resource, local-title and late-entry report scenarios. Reviewed source: working changes on `78885e0`; live validation pending.

# Safari implementation test checklist

Use the existing IntelliJ Minecraft Client/DevAuth configuration. These are manual checks to perform in Minecraft; automated unit tests do not verify Hypixel entity layouts or rendering. Start with a fresh run so the predictor sees the earliest possible drop groups. Use `/sm debug` and inspect the client log if a predicted name disagrees with a pickup.

## Safari-wide Cavern sources

- Exit the boat and remain outside Cavern. In Unique, receive one Rockmite and one Snoozle loot share, then have a teammate break all mounds and all five walls. Each species should complete without cd or entering its scan circle.
- In Sparkling, repeat without catches: needed Rockmite/Snoozle checks complete from resolved sources anywhere in Safari. A positively detected uncaptured Sparkling must keep its species and biome pending.
- In Unique with no actual Rockmite/Snoozle encountered or caught, resolve all sources: remain pending until party cd. A seen but uncaught ordinary critter must still require a shared catch. Verify no catch is invented by the report.
- Leave/reenter Cavern, toggle modes and unload chunks: retain source proof. Unknown/unloaded sites must not become broken; newly observed intact sources reopen their species. Full Clear still needs actual captures and its existing local-proof/report rules.

## Floor drops

- **Coin ESP across modes:** with all discoveries owned, confirm Full Clear highlights remaining coins before/after picking up and spending several coins, while Unique/Sparkling hide them. With Gimmiegold missing, all three modes retain every remaining coin highlight. Switch modes repeatedly; pickups and spending must not create a permanent cutoff.
- **Captured Sparkling remnants:** capture a Sparkling with multiple name-tag/model carriers, then leave the captured model or tags loaded. The Detected sparklings HUD must remove it, and no stale tag/body may reopen its missing row or inflate its population target. Unique/Sparkling complete after capture; Full Clear completes when its actual ordinary population/source requirements are also met. A separate newly spawned Sparkling must still reopen completion.

- **Forest:** only nine food markers: three Wriggleworms, three Yogi Berries and three Bags of Seeds. No XP/essence/shard markers should receive quest ESP. Check the label against each actual reward. Repeat with a starting food and with a teammate stacking extra food.
- **Cavern:** two predicted Lime/Purple/Orange sets. Pick up Orange: both Orange markers should disappear from ESP while unacquired Lime/Purple remain. Repeat for each color. Placing gems and changing biomes must not restore already satisfied colors. Check a starting gem as well.
- **Icy:** Full Clear offers all five predicted Icebreaker markers while none has been acquired. One pickup or starting Icebreaker hides the rest, including after use. Unique and Sparkling should not automatically show Icebreakers.
- **Haunted coins:** when someone needs Sparkling Gimmiegold, all remaining predicted coin drops stay eligible and the minimum capture count cannot waive their collection. Test with Doomspiral already shared so Incense ESP is off in Sparkling mode while coin ESP remains.
- **Haunted incense in Sparkling:** only a missing Doomspiral discovery enables it. Starting incense and inventory totals do not advance its floor-pickup counter. Three confirmed floor pickups keep it on; the fourth turns it off even when Gimmiegold is still needed. When everyone owns Doomspiral it stays off. In Unique and Full Clear, the fourth actual incense floor pickup also hides every remaining incense marker, even while Gimmiegold is needed. Spending incense or changing biomes must not restore those markers. When everyone owns Gimmiegold, Unique/Sparkling hide coin ESP; Full Clear retains all remaining coins regardless of pickups, holdings or capture completion.
- Check floor ESP switches, biome-only visibility and mode/discovery changes. Selection must change highlights without changing catches, inventory, resource use or completion evidence.
- Unload/reload chunks, cross biome boundaries and let a teammate collect drops. Earlier observed anchors must survive. Start a new Safari/server: previous numeric entity IDs and resource cutoffs must reset.
- Try entering after earlier drops have already been taken. Predictions may be wrong when the true first group was never observed. Record mismatches with `/sm debug` and the pickup-correlation log; a prediction is never a confirmed resource pickup.

- **Gimmiegold green capture row:** collect/use every coin and catch all resulting Gimmiegolds, including starting/extra coin spawns. In the Haunted check radius the capture row turns green even with remaining incense markers. Repeat with an uncaught Gimmiegold, unused coin, extra-resource notice and unknown/partial marker: completion stays pending as applicable. Spawned-but-uncaught critters may satisfy Sparkling checks, never Full Clear captures.

## Forest and Macaw

- **Full Clear nests after fd:** retain an initial four-nest survey, receive fd and enter Forest later. With fewer than four shared Honeybug captures, all unpunched nests still highlight, including when remaining bees are rendered. Punch one: only its waypoint disappears. At four shared catches, all remaining nest highlights hide. With all nests punched but insufficient catches, no nest markers remain, but Honeybug capture completion stays pending. Repeat in Unique/Sparkling: fd still hides the checking helpers.

- **Mid-run Forest:** enter after teammates used the food, without local pickup/feeder history. With a verified local Forest survey, nine accounted-for feeding opportunities (shared catches + current live birds, two Macaws per opportunity) and no remaining food-source markers/held food should resolve feeding. Test all caught, all live and mixed populations. Sparkling can complete with live birds; Full Clear still needs Bluebird/Parakeet captures and only one shared Macaw catch.
- Repeat with eight opportunities, duplicate/replacement UUIDs across different scans, an unknown Forest survey, a hidden remaining marker, a held starting food and an extra-food party notice: remain pending. Resolve additional food with additional population. Leave/reenter Forest to revalidate source conditions while retaining checked UUIDs. Confirm no personal captures, food pickups/use counts or personal food-completion alert are fabricated.
- **Macaw override:** in Full Clear, catch one Macaw personally or receive its loot share. While known food remains unused, Macaw stays incomplete. Once all known food has produced its birds, Macaw turns green even with several Macaws still visible. Extra Macaws alone must not block Forest's completion message.
- New starting/floor/party-reported food reopens the food requirement. Resolve every new feeding; the same one recorded Macaw catch remains sufficient afterward.
- Bluebird and Parakeet complete independently after food use: remaining Bluebirds keep Bluebird red without blocking an otherwise resolved Parakeet. Verify additional announcements beyond local food counts are retained.
- A Macaw pair means two birds and one feeding. Escape/replacement UUIDs must not add feedings or inflate population quotas.
- Check Honeybug with a teammate releasing bees before you arrive. Shared catches plus current uncaptured bees can resolve all identified bee-producing nests once site coverage is known. Ordinary Honeybug ESP: Full Clear keeps uncaught bees, Unique stops after one shared catch, Sparkling shows no ordinary bee ESP.
- Pending in-game Honeybug check: in Unique, verify one shared catch removes the species row; the nest footer/highlights remain only for a needed Sparkling Honeybug and resolve after every actual nest is personally punched or shared catches plus live uncaptured bees reach the actual nest count. Check both owned and missing party discoveries. In Full Clear, retain the verified initial nest population and complete from its shared capture messages in any biome, without waiting for fd or another local empty scan. Receive fd outside Forest and verify Honeybug checking trust persists after Forest entry despite incomplete rendering. Unique still needs one shared catch for progress, while Full Clear still needs the initial nest population captured. A detected uncaught Sparkling must block completion throughout.
- Discover another nest or an unknown nest site after earlier progress: outstanding source work must reopen. Sparkling detection should still work independently of ordinary bee ESP.

## Mode checks and party synchronization

- **Unique:** each ordinary first shared catch immediately advances progress and removes its missing row, even outside that biome with all local discovery checks unfinished. Rockmite/Snoozle need their own catch plus cd; needed positively observed unbroken sources can reopen only their respective species.
- Send/receive fd/cd/id/hd. Ordinary Unique catches advance without reports; Rockmite/Snoozle need cd without waiting for every other Cavern catch. Report echo is suppressed. Full Clear/Sparkling retain their report/source revalidation.
- Catch critters through loot shares while outside their biome, then enter it. Counts must persist once. Reaching the minimum plus a qualifying empty scan should not require old captured UUIDs to reappear.
- In Sparkling, test three live UUIDs; one shared catch plus two live; two catches plus one live; three catches plus none. Repeated sightings of one UUID never add progress. Distinct qualified UUID checks across separate scans retain progress; all required critters need not be rendered simultaneously. Retired/captured models are excluded from new live sightings.
- **Gemzie:** the door-opening message alone must not complete Cavern. Wait for three accounted-for Gemzies in the qualifying scan. Repeat after a remote `cd` in Full Clear/Sparkling. Unique Gemzie completes on its first shared catch, without a local scan or cd gate.
- **Icy priority:** while anyone needs Sparkling Wumpa, each uncaught Icy species gets ordinary ESP in every mode. A Wumpa catch/check must not waive another species' first catch. After a species' first shared catch, its normal mode rules resume.
- Unique biome announcements require current presence in that biome. Receiving its last loot share while elsewhere must not send the completion word remotely. Enter and validate it locally.
- Scrappy, Gemzie, Troodon and Gazer are the only species with fixed X/X suffixes. Full Clear must say X/M captured using confirmed catches only; Sparkling and relevant Unique checks say X/M checked using catches plus current qualifying UUIDs. A caught Gazer plus three live Gazers must show 1/4 captured in Full Clear and 4/4 checked in Sparkling. In Unique these appear only for needed discoveries. Verify all modes show only colored critter names and gray applicable fixed counters: no survey warning, absence prose or duplicate Gazer row. Retain the separate Forest bee-nest and Cavern wall/mound footers for outstanding work: Full Clear regardless of discoveries, Unique before the first catch or while that species' needed Sparkling check is unfinished, Sparkling only for missing discoveries. Resolve sources and confirm their footers disappear. Source requirements/highlights still apply.
- Break every Snooper wall and mound. Progress dialogue must not count as a catch or terminal outcome. Newly found intact sources must reopen needed checks even after an earlier completion report.

## Resources, captures and entry

- Finish a different biome while holding unused starting bird food or a coin. If the relevant discovery is missing, receive an automatic title (USE YOUR BIRD FOOD! / USE YOUR SHINING COIN!) and one compact combined party notice such as `[SM] Unused floor drops: 1x Shining Coin (missing Sparkling Gimmiegold); 1x Yogi Berry (missing Sparkling Macaw)`. The mixed-resource title is USE YOUR STARTING ITEMS!; sender and item types provide player/destination information, while each group names its missing Sparkling targets. Test several bird foods together and verify only party-missing birds are listed. Verify each included item retains independent spawn obligations. Neither has a config toggle. Suppress duplicates, used resources, notices in the destination biome, and notices when everyone owns the relevant discoveries.
- Receive an unused-resource notice after Forest/Haunted was complete. It must reopen the affected source work without adding a pickup or catch. Repeated notices and old population cannot resolve it. Require every extra feeding/coin spawn; Full Clear still needs its applicable captures, with the Macaw exception above.
- Enter partially cleared Haunted with zero drops and no Gimmiegolds. Fewer than three shared catches stays pending. For local Sparkling completion, require the retained initial coin-slot registry to be resolved and enough captured/current population; absence alone cannot establish an unknown catalog. Remote hd can override ordinary checks while outside Haunted.
- Check Basic and First Class, with and without Head Start. Initial inventory should account for zero/one/two starting resources, including two identical items. The Head Start message and inventory must overlap once. Queue retries, entry banners and hotspot messages must not manufacture grants or catches. Ticket type remains unknown if no identifiable selection was observed.
- Test standard capsule misses, confirmed hits, escapes, retries and successful captures. Only capture/loot-share confirmations increment catches. A species-only hit/catch must not retire an arbitrary nearby UUID. Test a Masterful capsule and a replaced Macaw UUID too.
- Test optional Chat Heads text in entry and loot-share messages. Shard quantities are rewards, not capture counts. Own catches and shared catches remain separate; shared catches must not create personal PBs.
- Disable HUD/ESP, perform work, then reenable them. Tracking should have continued. Verify a personal Forest Full Clear PB still requires a personal Macaw catch even though one party catch satisfies shared Full Clear progress.

## Updated GUI defaults and Hideyho scope

- **ESP text scale:** verify default 2× label size; move the ESP-tab slider between 0.5, 1, 2 and 4. Check distant floor names, critter timers, nest labels and teammate names for readable size, centering, colors and unchanged visibility. Close/reopen settings and restart the client to confirm persistence.
- With a fresh or partial configuration, independently switchable GUI features start on except Coins at Insta Sell, Achievements to Party, Warp reminders and Experimental capsule path. Modes remain mutually exclusive; numeric/text selections and mode presets keep their established behavior.
- Save disabled features and panel borders, enable the default-off exceptions, then restart: those explicit choices must persist.
- Arm Hideyho in Haunted, then leave before it settles. The pending watch must cancel. Hideyho dialogue and entities observed from Forest, Cavern or Icy must not emit inside/outside titles/chat there. In Unique and Full Clear, entering Haunted can still detect its current settled hiding location. In Sparkling, no inside/outside title or chat should appear. Switch to Sparkling while a watch is armed: cancel it without a delayed alert. Return to Unique/Full Clear and confirm normal detection resumes.

## HUD editor and Honeyhives

- Open `/sm gui`. Hover each green alert: All Gems, All Bird Food, All Incense and Run completion must match their displayed text. Drag/resize each separately, close the editor and trigger that alert; it must use the corresponding saved position/scale/style. Reopen and restart to verify persistence. Check Warp reminder as well and try a custom Run completion title.
- Follow [Honeyhive tests](HONEYHIVES.md#diagnostics-and-minecraft-checks), especially the screenshot's refill countdown versus green beacon, adjacent hive independence, profile-not-yet-detected scans and the exact Queen Bee proc. `/sm debug` now includes raw hive labels and saved-state diagnostics.

## Gimmiegold completion recovery and retained Gazer checks

- In the sewer, collect/use all coins and load the resulting Gimmiegolds. Sparkling completes with the required captured + live population; Unique also needs one shared catch. Full Clear turns green only after every required Gimmiegold is actually caught. Repeat with missed spawn messages, starting/extra coins and an extra-resource party notice.
- No held coins and a sufficient population must not bypass remaining live coin/unknown markers. Empty/invisible display survivors must not keep the source pending; a complete incense triple crossing block boundaries must not count as coins. Leave render range after validation: the Gimmiegold check remains. Acquire an additional coin: require another Gimmiegold. Reenter Haunted: revalidate sources and the current captured-plus-rendered population; historical Gimmiegold UUIDs cannot supply that population.
- See the four Gazers one at a time, moving each out of render range before the next. The gray checked counter retains each distinct sighting and stays at 4/4 after all four, including leaving/reentering Haunted. Repeated sightings of one UUID do not add progress. Catch an already-seen Gazer and confirm it is not counted twice. In Full Clear only actual catches increment X/4 captured. A new run resets retained sightings.
- If Gimmiegold remains pending, run `/sm debug` in the sewer and inspect the logged coin/source/population counts and remaining marker count.

## Retained checks for every critter

- In Unique and Sparkling, see Scrappy, Gemzie, Troodon and Gazer one UUID at a time. Unload each before the next: partial and completed X/X checked counters must remain steady. Repeat for ordinary species. Gimmiegold Sparkling uses captured + current rendered population, not old UUID checks. Birds instead retain feeder announcements and use shared catches plus current nearby birds, never historical UUID counts.
- Revisit a biome and switch modes: retain qualified UUID history for the same run. New food/coin/source work may reopen completion without erasing checked sightings. A new run must clear all retained checks and the coin-slot/contribution ledger.
- Check duplicate UUIDs, catches of already-seen critters and earlier loot shares followed by new sightings. None may double count a critter. Out-of-area sightings do not establish qualified checks; Full Clear captured counters and personal PBs never advance from sightings.

## Supplied Gimmiegold debug-log regression

- Reproduce three floor coins picked/used, none held, one Gimmiegold caught and two still in the water. Unique completes with its first catch. Sparkling completes only if all registered initial coin slots are collected, inventory is empty and the captured/current population reaches the slot-derived target; decorative predictions are not extra slots. Full Clear stays red until the remaining two are caught, then turns green.
- `/sm debug` should show the new Sparkling record with `floorMinimum=3 remaining=0 required=3 capturedPlusRendered=3 checked=true` for a confirmed three-slot run. The separate Full Clear ledger can still show `sourcePath=confirmed-coins`. Retained UUID history can be larger; it must not become the required capture count or make a fourth coin require nine Gimmiegolds.
- Repeat with only two actual floor coins plus a starting coin, a held coin, an unconsumed fourth actual coin and an unresolved party notice. Those cases must not bypass their applicable source requirements.

## Completed missing-panel wording

- Finish every required catch and check in Unique or Full Clear: the empty missing panel says **All caught and checked!**. Sparkling retains **All checked!**.
- Unique ordinary species complete on the first shared catch without needing discovery/radius/source scans. Rockmite/Snoozle await cd and needed loaded-source resolution. Sparkling retains its discovery checks; Full Clear retains remaining capture/source requirements.

## Ticket earned and automatic warp

- Follow [ticket-earned checks](CONTEST_DOWNTIME.md#minecraft-checks): one party announcement at Uncommon, own contest hold cleared immediately, remote sender-specific release and other downtime holds retained. Repeat with contest HUD/auto-request disabled and at a new contest window.
- Follow [warp timer checks](WARP_ALERTS.md#minecraft-checks): enabled expiry shows the alert and sends one `/p warp`; disabled/replaced timers, player-chat quotes, departures and changed instances cannot fire stale warps. Verify settings and command controls persist.

## Floor-drop colors and automatic contest request

- In **ESP → Floor-drop colors**, set the nine quest types to different colors. Verify outline, fill and name labels match each type; close/reopen settings and restart to confirm persistence. Test two gem colors and coin/incense together. Existing filtering must still hide unneeded drops.
- Check an older config with only the shared floor color: all types inherit it. Use **Customization → All floor-drop colors** to recolor every type, then change an individual color again and save without modifying the shared control: the individual choice must survive.
- Follow [automatic contest downtime checks](CONTEST_DOWNTIME.md#minecraft-checks): enabled sends one party request from the confirmed leader at a new contest start; members and moderators remain silent, disabled stays silent, the toggle persists, and joining/enabling mid-contest does not send a stale request.

## Shared Sparkling detector and skin range

- Follow [detector checks](SPARKLING_DETECTION.md#minecraft-checks): delayed post-start skins, skin-before-name aliasing, unchanged ordinary scan circles, real-player exclusion, cross-biome loaded detections and the observed Duplico unload point. The sheet covers 36 skins; Tepid keeps the name fallback.

## Any detected Sparkling blocks completion

- In each mode and biome, detect a Sparkling whose discovery everyone owns. It must appear in the missing panel and prevent biome/run completion, even if its ordinary species requirement was already met. Include a Sparkling Macaw after one ordinary Macaw catch.
- Unload it, leave/reenter and switch modes. The obligation remains. A normal same-species catch, UUID checking and received biome report cannot clear it. Repeat with alert/ESP switches disabled and a loaded Sparkling across a biome boundary.
- Confirm a personal Sparkling capture, party loot share or global Sparkling capture announcement: resolve one obligation. Two Sparklings of the same species need two catches; the announcement and reward for one catch must not count twice. A lingering label already seen as caught must not reopen completion. Unique/Sparkling must immediately complete the species and remove its missing row, even with unfinished ordinary checks. Full Clear must remain incomplete until normal captures/sources pass, then turn progress/capture panels complete and remove its missing row; no stale Sparkling obligation may remain.
- Detect a new Sparkling after a biome report has already been sent: reopen the affected biome and suppress queued reports/run alerts until the new capture is resolved. Start a new run and verify old obligations are gone.

## All five incense spawns in every mode

- In Unique, Full Clear and Sparkling, enable the relevant incense requirement and verify all five available spawns can highlight, including the farthest from (4,47,-27). Repeat with zero, one and two starting incense: no distance/slot limit applies.
- At three confirmed floor pickups, remaining incense stays eligible; the fourth pickup hides all remaining incense ESP. Spending items, reentry and mode changes must not restore it. Four held simultaneously also keeps the existing permanent cutoff.
- Confirm mode/discovery gates, colors and coin visibility still work independently. Collected drops themselves no longer render.

## Sparkling Haunted floor-drop filters

- With nobody missing Sparkling Gimmiegold, coin ESP stays hidden before any coin pickup. Mark Gimmiegold needed and verify coin ESP returns independently of Doomspiral/incense needs.
- Start with two incense, collect two more and hold four simultaneously: incense ESP stops in every mode, even with fewer than four floor pickups. Spend the incense and leave/reenter Haunted: it stays hidden. A new run resets this state. Verify the existing four-floor-pickup cutoff still works too.

## Sparkling boss capture exceptions

- With Wumpa or Doomspiral missing from party discoveries, load the boss in Sparkling mode and wait beyond its ordinary scan interval: it must remain in the missing panel and must not increase Checked progress. Repeat after unloading/reentry, spawn/slain messages and received `id`/`hd` reports.
- Catch it personally, then repeat in a new run with a party loot share instead. Either confirmation completes its check immediately and retains completion after reentry. Other species still complete by their documented checking rules; Full Clear retains its capture/source/absence requirements.

## Combined history import

- Open the History tab: **History** is first and **Import History from Logs** is immediately below. The old launch-import toggle is absent. Both the button and `/sm history import` import runs and Sparkling finds together, with progress and a completion count.
- Follow the [history import Minecraft checks](HISTORY_IMPORT.md#minecraft-checks), including an older-log folder, a repeated import, existing native records, estimates, a corrupt archive, persistence and an ongoing run.

## Documentation revisions

- Run `./scripts/DocumentationSync.ps1`: a synchronized source baseline should report no changed documents.
- Edit a rule and its Last updated section: the command must list that document, including when edited twice on the same date. A new document should appear as New; a removed one as Removed.
- Read [SOURCE_SYNC.md](SOURCE_SYNC.md) before recording a new baseline. Record only after the corresponding source has been reviewed, implemented and tested.

Issue #55: one clearing completion must deliver coin plus food, two foods and two-item stacks without another completion event. Recheck destinations, discovery needs and consumed quantities before one combined title/chat. Check one message contains every currently eligible item, duplicate sends are suppressed, and malformed received batches cause no partial progress changes. Old verbose notices must still parse. Suppress only the irrelevant item and retry unsent items if eligibility returns. Delay world loading beyond three seconds and verify initial inventory/HEAD START/floor pickups remain distinct. Confirm separate new-spawn obligations and clean state on new run.

## Credited party Sparkling streak commands

- Follow [streak command checks](SINCE_COMMAND.md#minecraft-checks): teammates' credited catches, unique versus repeat finds, active versus outside-run credit, critter/catcher details without recorded wording, missing history, multiple mod clients and restart/import persistence.

- Have multiple players request downtime during a run. Return to Torrhus: one combined party reminder names every requester and reason, including when you did not request downtime. Resume/ticket/departure before publication must remove obsolete requests; two mod clients should suppress duplicate reports.

Verify per-species Unique progress by receiving each ordinary first catch from another biome with no report or local scan. Rockmite/Snoozle require their own catch plus cd, regardless of other Cavern catches. Observe loaded mounds/walls while standing outside Cavern and verify needed species reopen; unloaded sites cannot prove completion. Confirm verified source completion restores them without entering Cavern. Positively detected uncaptured Sparklings still block all modes; no fabricated captures/PBs or echoed reports.

Verify Full Clear report trust: capture a variable species' minimum and perform a qualified empty local scan before its biome report; local progress, capture colors and missing rows must complete without the report and persist after travel. Other biomes without local proof retain the report/minimum path. Receive the biome report and verify minimum-satisfied species complete independently, without needing to render every UUID. Below-minimum species stay pending. Scrappy/Gemzie/Troodon/Gazer may complete from their fixed capture totals before a report. Reenter and positively detect remaining critters: only that species reopens. Capture the evidenced remainder without reloading old UUIDs and verify completion returns. Unloading or a repeated report alone cannot capture a known remainder. Retain Macaw's one-catch/no-spawn exception, positively unbroken sources, outstanding extra-resource notices and actual detected-Sparkling obligations. Personal PBs require their own strict evidence.

## Source synchronization regressions

- Receive `fd` with no Macaw catch and no local food/no-spawn proof: Macaw remains pending. Establish the local no-spawn proof or receive one real catch; confirm the matching path completes without inventing a catch.
- After `cd`, observe an uncaught Cavern species while standing in Forest. Only that species reopens; unloading and another report do not resolve it, but its additional catch does.
- After `fd`/`hd`, acquire unused food/coins. The affected Full Clear rows reopen; a fourth actual coin requires a fourth capture. Existing Sparkling and Unique policies remain separate.
- Receive a teammate's biome-done report before finishing your own Unique clearing work, with an eligible starting coin/food held. Once local clearing passes, the starting-item reminder still sends once; repeated scans cannot keep restarting its delay.

## Gimmiegold initial coin slots

- Check `/sm debug` before entering Haunted: the Sparkling record reports distinct initial coin slots, not three display entities per slot or decorative triples. Collect all but one with enough critters already spawned; completion stays pending. Unloading the last marker cannot collect it.
- With every slot collected and no coins held, qualify a scan near the sewer. Shared captures + currently rendered uncaptured Gimmiegolds must reach the initial slot total. Sequential historical sightings cannot substitute for that population. Incense highlights remain independent.
- Send a mixed unused-items notice with two coins and one food from a teammate, then repeat it. Only two extra Gimmiegolds are required; another sender adds their own amount. Own starting coins and the own echoed notice overlap.
- Receive hd outside Haunted: ordinary Sparkling checks may complete. Unique still requires one shared catch and Full Clear still requires the real minimum captures. Enter Haunted or receive hd while inside: local sources/inventory/population must pass. A pending actual Sparkling cannot be waived by a report.

## Retained Honeybug nests and Unchecked naming

- Exit the boat for the first time and record actual nest count N. Unload Forest and receive N bee capture messages: Full Clear Honeybug completes without revisiting its render range. No report alone may manufacture these captures.
- With Sparkling Honeybug needed in Unique, one catch removes the missing row but progress stays pending until local nest checking or fd. Without the needed discovery, one catch completes ordinary Unique progress. In Sparkling no ordinary catch is needed.
- Receive fd, then enter/reenter Forest with incomplete rendering: Honeybug checking remains trusted. Detect an actual uncaught Sparkling and verify it still blocks every mode.
- Switch to Sparkling in each biome: the checklist header and HUD editor use Unchecked. Unique/Full Clear retain Missing behavior and saved panel positions/settings survive mode changes.

## Last updated

2026-10-17 - Added stale Sparkling alias/population and all-mode coin ESP regressions to live checks.

2026-10-17 - Replaced incense distance-ranking checks with all-five selection and four-pickup cutoff checks.

2026-10-17 - Clarified unused floor-drop notices with resource quantities and relevant missing Sparkling discoveries; retained legacy parsing.

2026-10-17 - Added mode-specific nest highlight checks after fd and individual punches.

2026-10-17 - Added safari-wide mound/wall, Unique zero-spawn and pending-Sparkling checks.

2026-10-17 - Added initial nest retention, capture-based remote Full Clear, persistent Honeybug report trust and all-biome Unchecked checks.

2026-10-17 - Added dynamic Gimmiegold coin-slot checks, current population, party contributions and outside-only report trust.

2026-10-17 - Reconciled superseded checks and added source-audit regressions; all live checks remain pending.

2026-10-17 - Pending Minecraft verification: Full Clear local species completion should advance progress and remove its missing row without a report; move out of its radius/biome and retain that earned progress. In parallel, shared catches in an unvisited biome should wait for its report, subject to existing fixed-count/Honeybug/Macaw exceptions. Positive remaining critters must still reopen their species.

2026-10-17 - Added pending Minecraft checks for independent Honeybug/nest rows, actual nest totals and remote Forest-report retirement on entry.

2026-10-16 - Added Full Clear report/minimum gates, fixed-count early completion and positive-only revalidation tests.

2026-10-16 - Replaced Unique local discovery gates with per-species captures; added cd-gated Cavern and outside-biome loaded-source tests.

2026-10-16 - Added capture-gated Unique party-report trust and per-species Cavern reversal checks.

2026-10-16 - Added compact batched starting-item reminders and receiver compatibility checks.

2026-10-16 - Added all-species Sparkling completion overrides and Full Clear completion after resolving the Sparkling plus remaining captures.

2026-10-16 - Added current bird-count accounting and the captured/scanned Macaw-pair regression; no-Macaw completion in every mode.

2026-10-16 - Added leader-only automatic contest requests and a combined party downtime reminder naming all requesters and reasons.

2026-10-16 - Added !since party-credit regressions and critter/catcher message checks.

2026-10-15 - Added ticket-earned release/announcement and automatic warp timer checks, retaining detector and incense regressions.

2026-10-15 - Fixed complete, independently gated starting-resource reminder delivery (#55).

## Capture-shot collection

- Follow the complete [collection checklist](CAPTURE_SHOT_RECORDING.md#first-collection-checklist), beginning with a small ordinary-capsule sanity export. Confirm useful raw/tick/display/target fields before larger or Masterful collection.
- Verify off/start/status/stop/export, duration/input/byte limits, opening/finalizing refusal, both hands and variants, and block-to-air fallthrough. Starting outside a loaded Safari world must fail. Unsupported held items must not become inputs.
- Compare recorder off/on with ESP display disabled and capsule hiding off/on in separate sessions. Existing capture/history/completion totals, retained checks, alerts and settings must behave identically. Confirm no automatic chat or upload.
- Exercise delayed metadata, pre-existing capsules, rapid throws, another nearby thrower, escape/removal/unload and a reused entity ID. Keep uncertain ownership/outcomes unknown; absence or timeout must not become a miss. Repeated genuine feedback stays separate, shared catches/startles never settle local captures.
- Check movement, camera, target bounds and loaded-block obstructions; preserve raw packet paths, arrival times, reconstructed endpoints and smooth samples separately. No chunk loads or whole-world/per-frame scans. Synthetic tests and startup checks do not establish Hypixel field fidelity.
- Transfer/disconnect while recording and while a writer finishes; old-world files must not receive new-world input and reconnect remains off. Check completed and partial files using replay. Errors must preserve older exports and report once locally.
- Reserve fresh validation sessions before model inspection; verify file/session overlap rejection, per-variant frozen parameters, conditioning exclusion and explicit uncurated/ambiguous/incomplete exclusions. Optional predictions match exact observed times only. Last observed position is not proven impact.

### Recorder v2 volume/error follow-up

- Automated: 64 moving targets plus busy target packets; input snapshots; static suppression; shared focus cadence; range changes; capsule packet preservation; 180-second / 20-throw and 600-second / nine-throw encoded budgets under unchanged caps. Check original local exceptions at hook/codec/writer boundaries and incomplete exports with safe status.
- Automated scan recovery: clearing the real entity-map type between batches must skip empty slots within the allowance, retain later capsule packets and allow normal finalization; genuine inspection failures still propagate for diagnostics.
- Gameplay pending: repeat the small ordinary-capsule sanity block in a populated Safari area, then inspect target coverage and raw capsule continuity with hiding off/on. Record status near start/stop. On error, retain the chosen JSONL and recorder-related `latest.log` excerpt; synthetic tests do not establish live field fidelity.

## Experimental capsule path

- Fresh/missing/invalid configuration defaults off; settings save, reopen and restart retain deliberate choices. Toggling must not start recording or change capture/history/completion/ESP state.
- Enable in Safari helpers, disable both capsule-hiding settings, hold an ordinary main-hand capsule in first person and stand still on the ground. After two stable ticks, expect an amber path and **Experimental path**, with no impact marker. Its initial unseen launch segment is omitted and its nominal limit is 15 blocks.
- Test pitch -10 through +1 degrees inclusive and immediately outside both limits. Walk, jump/fall, swim, fly, ride, sneak and sprint: suppress immediately; settle for two ticks before returning. Switch ordinary/Masterful/unsupported items, hands and third-person view between ticks: no stale line.
- Turn the camera and vary FOV, window size and GUI scale. Hide HUD or open any screen: hide geometry/status. Disconnect/world transfer: discard old stability/geometry. Disable the option: no preview status or line.
- In the same controlled scene, place the preview line and an existing ESP outline behind one solid obstacle: the path is occluded, existing through-terrain ESP remains visible. Check normal unoccluded path continuity and finite geometry. Depth occlusion does not establish collision or target selection.
- Compare visible ordinary capsule centers with the guide while stationary in the tested range; keep recorder exports separate if used. Offline startup, synthetic geometry tests and numeric observation residuals do not certify Hypixel appearance, interpolation, impact or capture success. Record live checks still pending.

## Last updated

2026-10-16 (America/New_York) - Added conservative preview controls, lifecycle, camera and terrain/ESP regression checks.

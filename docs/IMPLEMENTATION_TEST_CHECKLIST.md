# Safari implementation test checklist

Use the existing IntelliJ Minecraft Client/DevAuth configuration. These are manual checks to perform in Minecraft; automated unit tests do not verify Hypixel entity layouts or rendering. Start with a fresh run so the predictor sees the earliest possible drop groups. Use `/sm debug` and inspect the client log if a predicted name disagrees with a pickup.

## Floor drops

- **Forest:** only nine food markers: three Wriggleworms, three Yogi Berries and three Bags of Seeds. No XP/essence/shard markers should receive quest ESP. Check the label against each actual reward. Repeat with a starting food and with a teammate stacking extra food.
- **Cavern:** two predicted Lime/Purple/Orange sets. Pick up Orange: both Orange markers should disappear from ESP while unacquired Lime/Purple remain. Repeat for each color. Placing gems and changing biomes must not restore already satisfied colors. Check a starting gem as well.
- **Icy:** Full Clear offers all five predicted Icebreaker markers while none has been acquired. One pickup or starting Icebreaker hides the rest, including after use. Unique and Sparkling should not automatically show Icebreakers.
- **Haunted coins:** when someone needs Sparkling Gimmiegold, all remaining predicted coin drops stay eligible and the minimum capture count cannot waive their collection. Test with Doomspiral already shared so Incense ESP is off in Sparkling mode while coin ESP remains.
- **Haunted incense in Sparkling:** only a missing Doomspiral discovery enables it. Starting incense and inventory totals do not advance its floor-pickup counter. Three confirmed floor pickups keep it on; the fourth turns it off even when Gimmiegold is still needed. When everyone owns Doomspiral it stays off. In Unique and Full Clear, the fourth actual incense floor pickup also hides every remaining incense marker, even while Gimmiegold is needed. Spending incense or changing biomes must not restore those markers. When everyone owns Gimmiegold, one acquired coin hides further coin ESP.
- Check floor ESP switches, biome-only visibility and mode/discovery changes. Selection must change highlights without changing catches, inventory, resource use or completion evidence.
- Unload/reload chunks, cross biome boundaries and let a teammate collect drops. Earlier observed anchors must survive. Start a new Safari/server: previous numeric entity IDs and resource cutoffs must reset.
- Try entering after earlier drops have already been taken. Predictions may be wrong when the true first group was never observed. Record mismatches with `/sm debug` and the pickup-correlation log; a prediction is never a confirmed resource pickup.

- **Gimmiegold green capture row:** collect/use every coin and catch all resulting Gimmiegolds, including starting/extra coin spawns. In the Haunted check radius the capture row turns green even with remaining incense markers. Repeat with an uncaught Gimmiegold, unused coin, extra-resource notice and unknown/partial marker: completion stays pending as applicable. Spawned-but-uncaught critters may satisfy Sparkling checks, never Full Clear captures.

## Forest and Macaw

- **Mid-run Forest:** enter after teammates used the food, without local pickup/feeder history. Inside 15 blocks horizontally of (6, 51), nine accounted-for feeding opportunities (shared catches + current live birds, two Macaws per opportunity) and no remaining food-source markers/held food should resolve feeding. Test all caught, all live and mixed populations. Sparkling can complete with live birds; Full Clear still needs Bluebird/Parakeet captures and only one shared Macaw catch.
- Repeat with eight opportunities, duplicate/replacement UUIDs across different scans, an out-of-radius scan, a hidden remaining marker, a held starting food and an extra-food party notice: remain pending. Resolve additional food with additional population. Leave/reenter Forest to revalidate source conditions while retaining checked UUIDs. Confirm no personal captures, food pickups/use counts or personal food-completion alert are fabricated.
- **Macaw override:** in Full Clear, catch one Macaw personally or receive its loot share. While known food remains unused, Macaw stays incomplete. Once all known food has produced its birds, Macaw turns green even with several Macaws still visible. Extra Macaws alone must not block Forest's completion message.
- New starting/floor/party-reported food reopens the food requirement. Resolve every new feeding; the same one recorded Macaw catch remains sufficient afterward.
- Bluebird and Parakeet complete independently after food use: remaining Bluebirds keep Bluebird red without blocking an otherwise resolved Parakeet. Verify additional announcements beyond local food counts are retained.
- A Macaw pair means two birds and one feeding. Escape/replacement UUIDs must not add feedings or inflate population quotas.
- Check Honeybug with a teammate releasing bees before you arrive. Shared catches plus current uncaptured bees can resolve all identified bee-producing nests once site coverage is known. Ordinary Honeybug ESP: Full Clear keeps uncaught bees, Unique stops after one shared catch, Sparkling shows no ordinary bee ESP.
- Discover another nest or an unknown nest site after earlier progress: outstanding source work must reopen. Sparkling detection should still work independently of ordinary bee ESP.

## Mode checks and party synchronization

- **Unique:** one shared catch removes the missing-species row. If its discovery is still needed, unfinished Sparkling work remains as one plain species row and blocks combined completion. If everyone owns it, the catch is sufficient.
- Send/receive `fd`, `cd`, `id`, `hd`. Unique progress must stay based on catches and local checks. Full Clear/Sparkling can accept remote completion, then revalidate on entry and reopen failing species/sources.
- Catch critters through loot shares while outside their biome, then enter it. Counts must persist once. Reaching the minimum plus a qualifying empty scan should not require old captured UUIDs to reappear.
- In Sparkling, test three live UUIDs; one shared catch plus two live; two catches plus one live; three catches plus none. Repeated sightings of one UUID never add progress. Distinct qualified UUID checks across separate scans retain progress; all required critters need not be rendered simultaneously. Retired/captured models are excluded from new live sightings.
- **Gemzie:** the door-opening message alone must not complete Cavern. Wait for three accounted-for Gemzies in the qualifying scan. Repeat after a remote `cd` and in Unique when someone needs Gemzie.
- **Icy priority:** while anyone needs Sparkling Wumpa, each uncaught Icy species gets ordinary ESP in every mode. A Wumpa catch/check must not waive another species' first catch. After a species' first shared catch, its normal mode rules resume.
- Unique biome announcements require current presence in that biome. Receiving its last loot share while elsewhere must not send the completion word remotely. Enter and validate it locally.
- Scrappy, Gemzie, Troodon and Gazer are the only species with fixed X/X suffixes. Full Clear must say X/M captured using confirmed catches only; Sparkling and relevant Unique checks say X/M checked using catches plus current qualifying UUIDs. A caught Gazer plus three live Gazers must show 1/4 captured in Full Clear and 4/4 checked in Sparkling. In Unique these appear only for needed discoveries. Verify all modes show only colored critter names and gray applicable fixed counters: no survey warning, absence prose or duplicate Gazer row. Retain the separate Forest bee-nest and Cavern wall/mound footers for outstanding work: Full Clear regardless of discoveries, Unique before the first catch or while that species' needed Sparkling check is unfinished, Sparkling only for missing discoveries. Resolve sources and confirm their footers disappear. Source requirements/highlights still apply.
- Break every Snooper wall and mound. Progress dialogue must not count as a catch or terminal outcome. Newly found intact sources must reopen needed checks even after an earlier completion report.

## Resources, captures and entry

- Finish a different biome while holding unused starting bird food or a coin. If the relevant discovery is missing, receive an automatic title (USE YOUR BIRD FOOD! / USE YOUR SHINING COIN!) and party notice naming the player, item, amount, destination and missing discovery. Neither has a config toggle. Suppress duplicates, used resources, notices in the destination biome, and notices when everyone owns the relevant discoveries.
- Receive an unused-resource notice after Forest/Haunted was complete. It must reopen the affected source work without adding a pickup or catch. Repeated notices and old population cannot resolve it. Require every extra feeding/coin spawn; Full Clear still needs its applicable captures, with the Macaw exception above.
- Enter partially cleared Haunted with zero drops and no Gimmiegolds. Fewer than three shared catches stays pending. Three or more, with no known unused coins or outstanding extra-resource notices, can establish completion without local pickup history.
- Check Basic and First Class, with and without Head Start. Initial inventory should account for zero/one/two starting resources, including two identical items. The Head Start message and inventory must overlap once. Queue retries, entry banners and hotspot messages must not manufacture grants or catches. Ticket type remains unknown if no identifiable selection was observed.
- Test standard capsule misses, confirmed hits, escapes, retries and successful captures. Only capture/loot-share confirmations increment catches. A species-only hit/catch must not retire an arbitrary nearby UUID. Test a Masterful capsule and a replaced Macaw UUID too.
- Test optional Chat Heads text in entry and loot-share messages. Shard quantities are rewards, not capture counts. Own catches and shared catches remain separate; shared catches must not create personal PBs.
- Disable HUD/ESP, perform work, then reenable them. Tracking should have continued. Verify a personal Forest Full Clear PB still requires a personal Macaw catch even though one party catch satisfies shared Full Clear progress.

## Updated GUI defaults and Hideyho scope

- **ESP text scale:** verify default 2× label size; move the ESP-tab slider between 0.5, 1, 2 and 4. Check distant floor names, critter timers, nest labels and teammate names for readable size, centering, colors and unchanged visibility. Close/reopen settings and restart the client to confirm persistence.
- With a fresh or partial configuration, independently switchable GUI features start on except Coins at Insta Sell and Achievements to Party. Modes remain mutually exclusive; numeric/text selections and mode presets keep their established behavior.
- Save disabled features and panel borders, enable the two default-off exceptions, then restart: those explicit choices must persist.
- Arm Hideyho in Haunted, then leave before it settles. The pending watch must cancel. Hideyho dialogue and entities observed from Forest, Cavern or Icy must not emit inside/outside titles/chat there. In Unique and Full Clear, entering Haunted can still detect its current settled hiding location. In Sparkling, no inside/outside title or chat should appear. Switch to Sparkling while a watch is armed: cancel it without a delayed alert. Return to Unique/Full Clear and confirm normal detection resumes.

## HUD editor and Honeyhives

- Open `/sm gui`. Hover each green alert: All Gems, All Bird Food, All Incense and Run completion must match their displayed text. Drag/resize each separately, close the editor and trigger that alert; it must use the corresponding saved position/scale/style. Reopen and restart to verify persistence. Check Warp reminder as well and try a custom Run completion title.
- Follow [Honeyhive tests](HONEYHIVES.md#diagnostics-and-minecraft-checks), especially the screenshot's refill countdown versus green beacon, adjacent hive independence, profile-not-yet-detected scans and the exact Queen Bee proc. `/sm debug` now includes raw hive labels and saved-state diagnostics.

## Gimmiegold completion recovery and retained Gazer checks

- In the sewer, collect/use all coins and load the resulting Gimmiegolds. Sparkling completes with the required captured + live population; Unique also needs one shared catch. Full Clear turns green only after every required Gimmiegold is actually caught. Repeat with missed spawn messages, starting/extra coins and an extra-resource party notice.
- No held coins and a sufficient population must not bypass remaining live coin/unknown markers. Empty/invisible display survivors must not keep the source pending; a complete incense triple crossing block boundaries must not count as coins. Leave render range after validation: the Gimmiegold check remains. Acquire an additional coin: require another Gimmiegold. Reenter Haunted: revalidate sources while retaining checked UUIDs.
- See the four Gazers one at a time, moving each out of render range before the next. The gray checked counter retains each distinct sighting and stays at 4/4 after all four, including leaving/reentering Haunted. Repeated sightings of one UUID do not add progress. Catch an already-seen Gazer and confirm it is not counted twice. In Full Clear only actual catches increment X/4 captured. A new run resets retained sightings.
- If Gimmiegold remains pending, run `/sm debug` in the sewer and inspect the logged coin/source/population counts and remaining marker count.

## Retained checks for every critter

- In Unique and Sparkling, see Scrappy, Gemzie, Troodon and Gazer one UUID at a time. Unload each before the next: partial and completed X/X checked counters must remain steady. Repeat for ordinary species and source-controlled birds/Gimmiegold.
- Revisit a biome and switch modes: retain qualified UUID history for the same run. New food/coin/source work may reopen completion without erasing checked sightings. A new run must clear all retained checks.
- Check duplicate UUIDs, catches of already-seen critters and earlier loot shares followed by new sightings. None may double count a critter. Out-of-area sightings do not establish qualified checks; Full Clear captured counters and personal PBs never advance from sightings.

## Supplied Gimmiegold debug-log regression

- Reproduce three floor coins picked/used, none held, one Gimmiegold caught and two still in the water. Unique (with its first catch) and Sparkling should complete despite leftover predicted markers. Full Clear stays red until the remaining two are caught, then turns green.
- `/sm debug` should show `sourcePath=confirmed-coins`, three accounted-for required checks, two live UUIDs and one capture. Retained UUID history can be larger; it must not become the required capture count or make a fourth coin require nine Gimmiegolds.
- Repeat with only two actual floor coins plus a starting coin, a held coin, an unconsumed fourth actual coin and an unresolved party notice. Those cases must not bypass their applicable source requirements.

## Completed missing-panel wording

- Finish every required catch and check in Unique or Full Clear: the empty missing panel says **All caught and checked!**. Sparkling retains **All checked!**.
- In Unique, a first catch must not complete a species while its party-needed Sparkling check remains unfinished. In Sparkling, unneeded discoveries are excluded except necessary spawn prerequisites. In Full Clear, resolve all possible captures and spawn sources, with only one Macaw catch required after food is resolved.

## Ticket earned and automatic warp

- Follow [ticket-earned checks](CONTEST_DOWNTIME.md#minecraft-checks): one party announcement at Uncommon, own contest hold cleared immediately, remote sender-specific release and other downtime holds retained. Repeat with contest HUD/auto-request disabled and at a new contest window.
- Follow [warp timer checks](WARP_ALERTS.md#minecraft-checks): enabled expiry shows the alert and sends one `/p warp`; disabled/replaced timers, player-chat quotes, departures and changed instances cannot fire stale warps. Verify settings and command controls persist.

## Floor-drop colors and automatic contest request

- In **ESP → Floor-drop colors**, set the nine quest types to different colors. Verify outline, fill and name labels match each type; close/reopen settings and restart to confirm persistence. Test two gem colors and coin/incense together. Existing filtering must still hide unneeded drops.
- Check an older config with only the shared floor color: all types inherit it. Use **Customization → All floor-drop colors** to recolor every type, then change an individual color again and save without modifying the shared control: the individual choice must survive.
- Follow [automatic contest downtime checks](CONTEST_DOWNTIME.md#minecraft-checks): enabled sends one party request at a new contest start, disabled stays silent, the toggle persists, and joining/enabling mid-contest does not send a stale request.

## Shared Sparkling detector and skin range

- Follow [detector checks](SPARKLING_DETECTION.md#minecraft-checks): delayed post-start skins, skin-before-name aliasing, unchanged ordinary scan circles, real-player exclusion, cross-biome loaded detections and the observed Duplico unload point. The sheet covers 36 skins; Tepid keeps the name fallback.

## Any detected Sparkling blocks completion

- In each mode and biome, detect a Sparkling whose discovery everyone owns. It must appear in the missing panel and prevent biome/run completion, even if its ordinary species requirement was already met. Include a Sparkling Macaw after one ordinary Macaw catch.
- Unload it, leave/reenter and switch modes. The obligation remains. A normal same-species catch, UUID checking and received biome report cannot clear it. Repeat with alert/ESP switches disabled and a loaded Sparkling across a biome boundary.
- Confirm a personal Sparkling capture, party loot share or global Sparkling capture announcement: resolve one obligation. Two Sparklings of the same species need two catches; the announcement and reward for one catch must not count twice. A lingering label already seen as caught must not reopen completion. Other mode requirements still apply afterward.
- Detect a new Sparkling after a biome report has already been sent: reopen the affected biome and suppress queued reports/run alerts until the new capture is resolved. Start a new run and verify old obligations are gone.

## Incense distance selection in every mode

- In Unique, Full Clear and Sparkling, compare incense highlights against 3D distance to **(4,47,-27)**. Without starting incense only the four closest known locations can show. With one starting incense, show three; repeat with two starting incense and expect two.
- Collect or unload one of those locations: a farther excluded location must not replace it. Spend starting incense, leave/reenter and switch modes: keep the three-slot or two-slot limit for one or two starting incense respectively. Newly loaded closer spawns may refine the known ranking. A new run resets it.
- Later Haunted pickups must not count as starting items. Confirm all existing mode/discovery gates, four-held/four-picked cutoffs, colors and coin visibility still work independently.

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

Issue #54: earn source/radius checks, exit radius, unload, revisit and switch modes; retain local proof. Contrast a party-only report with locally earned proof. New sources and uncaught Sparklings must reopen checks; Full Clear still requires catches. Repeat on all three configured targets.

## Last updated

2026-10-15 - Added ticket-earned release/announcement and automatic warp timer checks, retaining detector and incense regressions.

2026-10-15 - Clarified run-retained Sparkling/source evidence and independent party revalidation (#54).

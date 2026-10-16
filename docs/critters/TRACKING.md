# Shared Safari evidence rules

[Critters](README.md) · [Mode summary](../../SAFARI_LOGIC.md)

Based on the supplied Hypixel Safari observations, clarified on 2026-10-14. [Exact supplied messages and audiences](MESSAGES.md) are recorded separately; descriptions here are semantic. Chosen mode goals are policy; server evidence establishes whether their conditions hold.

## Entry and starting resources

- Entry is through the Safari Manager at Critter Safari Entrance on the canyon server (called Taurus Canyon in the supplied description).
- The global entry banner names the IGN (possibly with rank/Chat Heads text); the separate queue attempt is visible only to the leader. The transfer goes to `mini` plus four characters. Queue attempts are not multiple completed entries.
- The Manager admits leaders with `I already saw your ticket, so you're free to go.` and members with `Looks good to me. Have fun out there!`. Keep the applicable acknowledgement associated with the same entry; a retry alone must not award another starting item.
- Record ticket type: Basic, Economy, Premium or First Class. Basic adds no ticket-specific starting quest item. No additional Economy/Premium effect is specified here.
- Head Start grants one random quest item via `HEAD START! You started with a <Quest Item>!`. First Class independently grants one item **without a message**: inspect initial inventory immediately after Head Start when present. Do not add the named Head Start item again on top of that inventory total. Keep grants/hotspot separate from later floor pickups.

| Head Start | Basic / Economy / Premium | First Class |
| --- | --- | --- |
| Absent | 0 starting items | 1 starting item |
| Present | 1 starting item | 2 starting items |

Nine possible starting items: **Icebreaker, Yogi Berry, Wriggleworm, Bag of Seeds, Shining Coin, Purple Gem, Lime Gem, Orange Gem, Soothing Incense**. Two grants need not be different. Initial inventory supplies the total; account separately for any subsequent floor pickups.

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
| Birdfeeder spawn announcement | Stated species/quantity spawned; one feeding event |
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

- **Full Clear:** confirmed shared catches meet the species minimum, the player enters that species' UUID-check radius, and the scan shows no remaining critters or unresolved spawns. Count it fully cleared even if the player never saw the already-captured UUIDs. Earlier loot shares supply the catch evidence; do not require those captured entities to reappear for checking.
- **Sparkling:** inside the species' Sparkling scan radius, require retained distinct qualified UUID checks and safely reconciled shared captures to reach its Sparkling minimum, plus its special source requirements. Partial catches are useful but not sufficient below that threshold. Merely entering the biome, scanning outside its radius or seeing no entities without enough catches is insufficient.
- Minimum captures plus a qualifying in-radius absence scan must not remain pending solely because the original UUIDs are gone. Conversely, the minimum alone or an out-of-radius empty scan must not produce completion. Known additional outstanding spawns still matter; capsule escape replacement UUIDs do not create additional required spawns.
- Apply these radius rules where a species has a defined check radius. Structure/feeding/coin species retain their specific source checks; do not invent a radius or silently waive unresolved sources.
- Treefrog/Woodchucker scan coverage is reported at approximately 50–55 blocks from Forest center. Preserve that range until the exact boundary is established. Completion-check coverage is separate from ESP visibility; a rendered critter lacking an outline remains positive presence evidence.

### Forest mid-run: Honeybug

- A teammate's previous nest punches are not directly observable. Count shared confirmed catches plus currently loaded, uncaptured bee UUIDs as alternative evidence of bees already released. Do not add historical UUIDs for captured bees to their catches again.
- Use the actual bee-producing nest count. Once shared catches plus currently loaded uncaptured bees account for that count, nests can be unhighlighted in every mode. Confirmed handling of all actual nests also establishes that all nest opportunities are resolved.
- Nest highlights and Honeybug ESP are independent: Full Clear keeps uncaptured bees highlighted; Unique stops bee ESP and removes the species row after one shared catch, even with a missing sparkling discovery; Sparkling never shows ordinary Honeybug ESP. Needed nest work can retain the nest row/highlights after Unique's species row disappears.
- Honeybug's nest-source rule overrides the generic Sparkling shortcut: one catch does not establish that all nest opportunities are checked. Actual sparkling messages/detection remain independent of ordinary bee ESP. See [Honeybug](honeybug.md).

### Haunted mid-run: Gimmiegold

- On entering partially cleared Haunted, if no floor drops and no Gimmiegolds remain, compare this run's Gimmiegold capture/loot-share total with its minimum. If the minimum is met, mark Gimmiegold completed/checked without requiring this player's own coin-pickup/use history or sighting the already-captured UUIDs.
- If the minimum is not met, keep it pending. A teammate may still hold Shining Coins; this explains the empty world but is an inference, not confirmation of their inventory. Some earlier Gimmiegold catches below the minimum do not close this mid-run case.
- The minimum without the absence evidence is insufficient. New floor drops or outstanding Gimmiegolds must be accounted for rather than hidden by the earlier deduction.

### Forest mid-run: birds

- Missing earlier food pickups or feeder announcements must not strand bird completion. Within 15 blocks horizontally of the existing Forest scan center **(6, 51)**, a completed entity scan with no remaining food-source markers and no held bird food may establish the feeding baseline from **shared confirmed catches + distinct current live uncaptured UUIDs**.
- Require at least **nine feeding opportunities**, plus known starting/extra food demand. Bluebirds and Parakeets count individually; two Macaws account for one feeding. Do not add personal catches again on top of shared catches, accumulated historical UUIDs or spawn messages on top of their entities. An incomplete Macaw pair cannot establish another full feeding opportunity.
- Use raw floor markers independently of ESP selection. Any remaining Forest marker whose reward cannot be confirmed as irrelevant conservatively blocks this absence fallback, even when hidden by ESP settings. A predicted generic reward alone cannot prove there is no bird food. Unknown/unperformed scans and out-of-radius entry cannot establish this evidence.
- This resolves the feeding source only. Full Clear still needs the applicable catches and absence of remaining Bluebirds/Parakeets; one shared Macaw catch remains sufficient even with live extra Macaws. Sparkling may complete using current live birds; Unique still requires its shared catch and any needed Sparkling check. Personal PBs still require personal catches.
- Do not manufacture food pickups, food-use announcements, captures or personal food-completion alerts. Known unused starting/party resources remain blockers. A new pickup after this recovery requires additional bird population; the old nine cannot resolve it again. Revalidate on Forest reentry and reopen on new food or feeder announcements.

### Forest birds: shared feeder, independent completion

- Personal food comes from starting grants and Forest pickups. Other party members can also bring starting food, collect Forest food or add food to an already active feeder. Local pickups are a minimum known contribution, not the feeder's total input or an upper limit on spawns.
- Hypixel sends **no food placement/stacking message**. Infer food progress from initial inventory, pickups, later inventory and spawn announcements; do not require a placement confirmation or assume another player's food is visible in local inventory.
- Example: one starting food + nine personal Forest pickups = ten personal foods. More than ten feeding/spawn announcements implies another contribution; **more than ten bird entities alone does not**, because one Macaw announcement produces two birds. Record feeding events and stated bird quantities separately.
- For each species, reconcile shared confirmed catches with currently scanned, uncaptured UUIDs. Their sum accounts for birds captured or still present, including birds produced by teammates' food. Do not add spawn-message totals on top of the same birds or count escape replacements as additional spawns.
- Correlate that evidence with the minimum known feeding demand, including starting food. Use announcements to account for queued food being consumed. Merely putting food in the feeder does not prove that it has finished producing birds; local inventory alone cannot show a teammate's remaining food.
- **Full Clear baseline:** all personally acquired starting/floor food must have been used, with known outstanding feeder food resolved. Before this, all three bird rows stay red. Having captured current birds does not waive this baseline while food remains unused.
- After that baseline, evaluate **Bluebird and Parakeet independently** with a qualifying bird-area scan: no remaining live birds of that species means its capture row turns green; birds of that species still present keep only its row red. **Macaw Full Clear exception:** one confirmed shared Macaw catch completes its row even if additional Macaws remain in the scan. Those remaining Macaws do not block Forest Full Clear or its completion message. Personal Full Clear PBs retain the personal-catch requirement. Proven no-spawn outcomes remain separate from catches; the one-catch exception never fabricates a catch.
- Further food contributions or new spawn messages reopen feeding/species evaluation; do not freeze green rows or discard announcements beyond the local food count. Sparkling and Unique-with-missing-discovery also keep the shared feeding opportunities required, without requiring every ordinary bird captured for a Sparkling check.

## Unused starting food/coin reminders

- Track Shining Coins and bird food granted at entry through Head Start/First Class, using the initial inventory evidence. Bird food means Bag of Seeds, Wriggleworm or Yogi Berry. Starting possession alone does not trigger a reminder.
- When the player finishes the biome they are clearing, check for a tracked starting resource still held and unused. This is the end of their clearing work, not a requirement to wait until every biome is already complete. Recheck actual inventory; do not remind about items already used.

| Resource | Discovery gate | Destination |
| --- | --- | --- |
| Shining Coin | At least one party member needs Sparkling Gimmiegold | Haunted |
| Any bird food | At least one party member needs Sparkling Bluebird, Parakeet or Macaw | Forest |

- Automatically send both a **local title alert** and a **party notice** when the discovery gate passes, the unused starting item remains held, and the player is **outside its destination biome** at the end-of-clearing check. **No config toggle or config-GUI option** controls either notification. If everyone has the relevant discoveries, suppress both. Do not apply this alert to unrelated starting items.
- Title text: **USE YOUR SHINING COIN!** or **USE YOUR BIRD FOOD!**. The detail identifies the held item/quantity, destination (Haunted/Forest), and the party's missing sparkling discovery. These are reminder titles, not new-resource or completion announcements.
- Do not alert a player currently clearing Haunted about their coin, or a player currently clearing Forest about their food. Their ordinary source work handles those items. Gate on actual current biome, not an assumed assignment.
- Example: a Cavern clearer finishes with a starting coin still held and the party needs Gimmiegold: alert them to use it in Haunted and notify the party. A starting food held by a Cavern clearer similarly requires a Forest reminder only if a bird discovery is needed.
- The party notice uses the **same automatic trigger, discovery gate and suppression rules as the title**; it does not have a separate enable switch. Identify the player, item/quantity, destination and that the party still needs the relevant sparkling variant, so another mod can treat it as **outstanding source evidence**, not a catch or completion report. Exact outgoing wording is not yet specified. Suppress duplicate titles/notices for the same outstanding item state.
- A relevant coin notice reopens Gimmiegold/Haunted's required Sparkling work; a relevant food notice reopens the needed bird checks/Forest feeding work. Do not reset unrelated species or counts. This can invalidate an earlier biome-complete state; the empty-biome/minimum inference does not override explicit evidence of an unused extra resource.
- Preserve Unique's rule that completion words do not affect its progress. An unused-resource notice is different evidence: it can reopen an added Sparkling/source requirement when a party discovery is missing, while the recorded Unique catch remains secured. Full Clear/Sparkling likewise revalidate requirements rather than retaining a stale completion report.
- Treat reminders as outstanding-resource status, not additive pickup events: repeat notices must not manufacture more food/coins. Resolve the pending source work through subsequent use/spawn/check/capture evidence under the active mode's rules.
- **Do not mark the affected biome done again until the reported extra resource has been used and its critter has spawned.** Food requires the resulting bird spawn; a coin requires the resulting Gimmiegold spawn. A cleared inventory or repeated biome-done word alone does not release this pending-resource condition.
- At the notice, retain the already-accounted-for population. New spawn evidence must account for the **additional** bird/Gimmiegold, not reuse catches/UUIDs that satisfied the old minimum. Use the spawn message or a reconciled increase in **shared captures + current live uncaptured critters**; shared captures already include personal catches, so do not add those twice. Bird evidence combines the relevant bird species and respects paired Macaw quantities.
- Resolve each reported resource/quantity once. For multiple food items/coins, account for every resulting feeding/coin spawn; do not treat one new entity as proof that all reported items were consumed. Repeated scans or escape replacement UUIDs do not supply additional spawns.
- Only then reevaluate all active-mode checks and biome completion. The new spawn releases the extra-resource gate, not Full Clear's capture requirements: newly spawned uncaptured critters still block Full Clear, except additional Macaws after its one-catch requirement and all known food use are satisfied. Unique retains its shared catch plus applicable Sparkling checks, and its current-biome-only announcement guard.

## Completion deductions

### Highest-priority Icy ESP rule: missing Sparkling Wumpa

- If any party member still needs Sparkling **Wumpa**, enable ordinary ESP for each Icy species with zero confirmed shared catches: Strongarm, Tepid, Polaris, Shuddersquid, Billygoat, Mantis Shrimp, Nozzlenose, Troodon and Wumpa.
- Apply this before every mode's normal ESP eligibility rules, even when the species' own sparkling discovery is already shared or its check/timeout would otherwise hide it. This rule does not mark anything captured or complete.
- After the first own capture or party loot share of a species, stop forcing its ESP through this override and apply that mode's remaining rules. Full Clear or profitable ESP may still keep it highlighted. Evaluate each species independently.
- A Wumpa check/catch does not supply another Icy species' first catch. While the party still needs its discovery, do not end this override for an uncaptured species solely because Wumpa was checked.

### Mode completion

- **Sparkling:** check the spawns of species for which someone in the party needs the Sparkling variant. No general capture requirement; spawning prerequisites still apply where necessary.
- **Unique:** capture every species at least once through own catches or party loot shares, and check all applicable spawn opportunities for each species whose Sparkling variant anyone still needs.
- **Full Clear:** capture every possible critter and resolve its spawn-source work. Macaw is the exception: one confirmed shared Macaw catch is sufficient after all known food/source requirements are resolved, even if additional Macaws remain. Personal PBs still require the corresponding personal catch.
- When the missing/check panel is fully complete, show **All caught and checked!** in Unique and Full Clear, and **All checked!** in Sparkling. Retained checked UUIDs and actual capture counts remain distinct.

- **Unique:** one own or party catch secures the species. If anyone in the party still lacks its sparkling variant, also require that species' Sparkling evidence before marking its overall Unique progress complete. The first catch removes ordinary first-catch ESP; if required checking remains, retain a plain species row in the missing/check panel. A fully resolved variable source can prove unavailable instead of inventing a catch; unknown is not unavailable.
- **Full Clear:** satisfy the established minimum, account for all additional verified spawns, capture outstanding critters, and resolve every relevant spawn source. A grant, resource use, sighting, disappearance or party done claim is not a catch.
- **Sparkling:** use the retained UUID-check rule below plus species-specific source checks. Seeing an ordinary critter can contribute without catching it. The intended beneficiary's missing-discovery set must itself be known; these events alone do not establish everyone's collection.
- No visible critter in currently loaded space is only local absence. Unknown nests/walls/food/coins or incomplete UUID coverage keep the corresponding check pending. More positive evidence can reopen a previous deduction.
- Party biome reports are accepted as shared completion state under the rules below, but never manufacture Hypixel catches or personal PB evidence. Highlight timers, rendered boxes and arbitrary delays do not prove completion.

### General Sparkling count rule

- For **every species** being checked in Unique or Sparkling mode, record each distinct UUID seen in its qualifying scan area once and retain it for the remainder of that run. Preserve partial totals and completed X/X checks when entities unload, render distance changes, the player changes mode, or a biome is revisited. A new run resets the history.
- Scrappy, Gemzie, Troodon and Gazer show these retained totals in their fixed X/X checked counters. Other species retain the same internal check evidence without adding new fixed counter suffixes.
- Repeated scans of one UUID add nothing. Record the shared catch count at each UUID's first live sighting. For each such baseline, the baseline catches plus distinct UUIDs first seen at or after that baseline are a valid checked-population lower bound. Take the largest bound or confirmed capture count. This combines earlier loot shares with later sightings without counting a seen critter again when its catch arrives.
- Outside a species' defined scan area, sightings do not establish a new qualified check. Previously qualified checks remain valid. Source gates remain separate: food, coins, nests, walls, mounds, new resources and unresolved party-resource notices can still block combined completion without deleting the UUID history. Reentry revalidates these sources.
- Full Clear remains based on actual shared/personal capture requirements and live remainder/source checks. Retained UUID checking never manufactures captures, food use, coin spending or personal PBs.

## Missing/check panel by mode

| Mode | Species rows |
| --- | --- |
| Full Clear | List species not fully captured: below its capture minimum, or live critters of that species still remain in the qualifying spawning-radius scan. Remove only after the minimum, in-radius absence and applicable source conditions pass. Outside the required radius, unverified absence stays pending. |
| Unique | List uncaught species and caught species whose required Sparkling check is unfinished. Use the plain critter name for both; a secured catch and unfinished checking remain separate evidence. Remove the row when its applicable requirements pass. |
| Sparkling | Show a **needs checking** checklist for required discoveries. Remove a species after all its Sparkling requirements pass; reopen it if later qualifying evidence invalidates completion. This is a check list, not a capture-only missing list. |

- In every mode, list only plain critter names in their species colors. The only gray text on a critter row is the applicable fixed `X/M captured` or `X/M checked` suffix. Do not append checking prose or duplicate species counters. Keep a caught Unique species listed while its required check is unfinished.
- Only **Scrappy 3, Gemzie 3, Troodon 3 and Gazer 4** have the confirmed fixed-spawn **X/X** suffix. Label the quantity explicitly by mode; never show a bare `1/4`.
- **Full Clear:** `X/M captured`, where X is confirmed shared catches only, capped at M for display. Scanned live UUIDs cannot increment it: one caught Gazer plus three live Gazers shows **1/4 captured**, and the live Gazers still block Full Clear completion.
- **Unique:** show `X/M checked` only when someone needs that species' sparkling discovery. Its first catch secures only the Unique capture requirement; the added check still accounts for all M spawns. X is retained distinct qualified UUID checking reconciled with shared captures without double counting, capped at M. Example: one caught and one live Gemzie gives **2/3 checked**. If everyone owns the discovery, omit the counter and require only the first catch.
- **Sparkling:** use the same **X/M checked** calculation as Unique's added check. One qualifying live Gazer with no catches shows **1/4 checked**; spawning/seeing a critter does not become a capture. All species retain qualified UUID checks for the run. Escape replacements and spawn announcements themselves never add captures.
- Do not add X/X suffixes to other critter names. Source requirements still gate completion; the three conditional source-helper footers below remain separate from the species list. Food/spawn gates and independent bird completion still apply.

### Missing panel presentation

Forest retains **Bee nests to punch** while nest work is unresolved. Cavern retains **Snooper walls to break** for intact/unknown walls and **Mounds to break** while mounds remain. Keep these in a separate footer beneath the plain species list, with their counts in gray.

- **Full Clear:** show outstanding source work regardless of party discoveries.
- **Unique:** show source work before the first shared catch; afterward retain it only if that species' Sparkling discovery is needed and its check remains unfinished.
- **Sparkling:** show unfinished source work only if someone needs that species' discovery.
- Gate each helper independently by Honeybug, Snoozle or Rockmite; another missing discovery cannot keep it visible. Hide resolved sources. Preserve unknown wall counts rather than claiming those walls are broken.
- Gazer has no separate footer in any mode; keep its only counter on its plain species row. No extra absence prose or survey warnings are restored.

**Wumpa and Doomspiral are capture-required exceptions in Sparkling mode.** A needed boss remains unchecked until this run receives a personal capture or party loot-share confirmation. Seeing or retaining its UUID, a spawn/awoken/slain message, and a party biome-completion report cannot substitute for that capture. The confirmed catch completes its check immediately and remains valid across unloading, biome reentry and mode changes. Other species keep their existing checking rules.

Positive Sparkling names and skin matches now use [one detector path](../SPARKLING_DETECTION.md) for alerts and capture requirements. Ordinary scan circles remain unchanged; delayed post-start skin updates are reread, and every currently loaded positive can be detected beyond the former 80-block display cap.

## Sparkling captures block biome completion in every mode

Any positively detected **Sparkling critter** creates a capture requirement for its species and biome in the current run, even if everybody already owns that discovery. This rule applies in Unique, Full Clear and Sparkling modes, including a Sparkling Macaw after the ordinary one-Macaw requirement is met.

- Record distinct Sparkling name/skin detections as their entities load, aliasing each model and its name tag into one identity, independently of alert/ESP toggles, the player's current biome and the ordinary 80-block display radius. Retain unresolved sightings through unloading, biome reentry and mode switches; a new run starts with a fresh ledger.
- Keep the affected species in the missing panel and out of completed progress while any detected Sparkling of that species remains unresolved. In Sparkling mode this adds a temporary capture target even when the party does not need its discovery. Party biome reports, normal population checks and ordinary catches cannot resolve it.
- Require a confirmed Sparkling personal capture, Sparkling loot share or trusted global `SPARKLING! <player> caught/found a SPARKLING <species>!` message. Reconcile the global announcement and its capture/loot-share reward as one catch, in either order. Each confirmed catch resolves one outstanding sighting of the same species; multiple detected Sparklings require multiple catches.
- Reopen a previously completed biome when a new Sparkling is detected. Suppress queued biome reports and run-complete alerts while capture obligations remain. Once caught, reevaluate the mode's normal sources/checks/captures before completing. Do not manufacture ordinary catch totals or PB evidence from a global Sparkling announcement.

## Biome completion and party synchronization

- Locally complete the **current biome** when every required critter satisfies its active-mode completion check. Send its party completion word: **Forest `fd`, Cavern `cd`, Icy `id`, Haunted `hd`**. This lets other mod users synchronize; receiving a report alone must not echo it back as a new local completion.
- **Unique additionally requires at least one confirmed shared capture of every species in that biome.** Own captures and received loot shares count. All active Unique checks still apply, including Sparkling requirements for missing party discoveries. A no-spawn/unavailable outcome can resolve helper work but cannot substitute for a required Unique biome catch.
- **Unique announcement guard:** send a biome's completion word only while this player is currently inside that biome and all its Unique/local Sparkling checks pass. Captures/loot shares received elsewhere may advance its progress but must not send its completion message remotely. On later entry, revalidate locally before announcing; do not send a previously queued report from another biome.

| Active mode | Effect of a received biome completion report |
| --- | --- |
| Unique | Its progress GUI does not listen to biome-completion reports. Use capture/loot-share logs and local critter checks only; local biome completion still needs every species caught. |
| Full Clear | Mark the reported biome completed and update its progress GUI until contradictory local evidence requires revision. |
| Sparkling | Mark the reported biome completed and update its progress GUI until contradictory local evidence requires revision. |

- On entering a reported-complete biome, scan and revalidate each critter against the active mode's rules. Keep valid completion; reopen failing critters and the biome, and update the progress GUI. Received completion is not an irreversible lock.
- **Unique also revalidates its own locally completed biomes on entry**, independently of any party completion report. For each species, require its capture check plus its Sparkling evidence whenever anyone currently lacks that discovery. If any requirement fails, reopen that species' completion and the biome until all requirements pass again; retain valid catches and first-catch ESP/missing-row behavior.
- **Unique progress inputs:** confirmed own catches + confirmed party loot shares + this client's active-mode checks. Party `fd`/`cd`/`id`/`hd` messages are not inputs. Local entry checks can reduce progress or reopen a biome despite its captures being secured; completing the outstanding Sparkling checks restores completion.
- Unique example: a shared Rockmite catch already exists, but someone needs Sparkling Rockmite and the entering player finds unbroken mounds. Keep the capture evidence, reopen the Rockmite Sparkling/source check and Cavern completion, and reflect the incomplete combined check in progress. Resolve the mounds before Cavern can complete again. Apply the same rule to every species' added Sparkling requirement, not only mounds.
- Example: in Sparkling, someone still needs Rockmite and the entering player finds unbroken mounds. Rockmite becomes incomplete again; the biome/progress display must reflect the remaining mound work despite the earlier `cd`.
- A party report never adds catches, marks resources consumed or proves local observations happened. Changing modes must not let a stored Full Clear/Sparkling report inflate Unique progress. Reevaluate using the selected mode.

### Unique with a missing party discovery

| Species | After first shared catch | Additional Sparkling evidence |
| --- | --- | --- |
| Rockmite | Remove ordinary silverfish ESP | Keep mound ESP and a plain Rockmite checking row until every mound is broken/resolved |
| Snoozle | Catch requirement secured | Break every Snooper wall and resolve its Sparkling opportunities |
| Honeybug | Remove ordinary bee ESP | Keep needed nest highlights and a plain Honeybug checking row until all nests handled or catches + live uncaptured bees account for the actual nest count |
| Gimmiegold | Catch requirement secured | Use all Shining Coins in the local player's inventory and establish at least three distinct spawn opportunities from reconciled catches, spawn messages and UUID observations; include additional known coins/spawns |
| Gemzie | One shared catch always required for Unique | Spawn message starts a pending check; require three retained qualified Gemzie checks reconciled with shared captures inside its Sparkling scan area. If everyone already has the discovery, Unique needs only the catch |

This composition applies to every species, not just the four examples. Use the species' actual Sparkling conditions; do not treat one catch as sufficient for a multi-source check. Do not copy all Sparkling-mode ESP behavior into Unique: ordinary critter outlines and source helpers have their own rules. Preserve the established mid-run Gimmiegold inference where teammate inventory is unavailable.

### Delayed spawn evidence: Gemzie

- A spawn announcement can precede entity arrival. It starts a pending Sparkling check, not immediate completion. Inside Gemzie's Sparkling scan radius, retain distinct qualified Gemzie UUIDs across scans and reconcile shared captures to establish at least three checked spawns. With zero catches, all three must have been seen, but they need not be rendered simultaneously. The door-opening announcement alone remains insufficient.
- Do not complete/announce Cavern just because the Gemzie spawn message was its last expected event. Wait for the actual three-Gemzie evidence and the remaining mode checks; use evidence rather than a fixed delay.
- Outside Cavern, a received `cd` may fill Full Clear/Sparkling progress. On entry, revalidate the three-Gemzie count and reopen incomplete critter/biome checks; Full Clear also needs its catches/absence rules. Unique ignores `cd`, but uses this spawn check when its added Sparkling requirement applies.

## Variable spawn and activity events

| Source | Observed progression | Deduction / limit |
| --- | --- | --- |
| Birdfeeder | Bluebird / Parakeet attracted; known plural `Two Macaws were attracted to the Birdfeeder!` | Count the stated spawn quantity, once per announcement; feeding is not capture |
| Rockmite mound | Four cracking/progress lines, then terminal reveal or terminal empty | Progress is not a spawn; terminal outcome resolves that mound |
| Snooper wall | Ran headfirst / some cracks → keep running / more cracks → destroyed / maybe something hiding | Third line confirms destruction; use resulting UUIDs/catches to establish Snoozles |
| Shyworm | Surfaced nearby / don't let it see you → eight rotations → hid back into ground | Activity cycle; not a catch and not assumed eight seconds |
| Shyworm interrupted | Fled because startled after missed capsule, or saw a player and fled | Interrupted cycle; no successful catch implied |
| Haunted sewer | Coin gobbled and Gimmiegold appeared | One consumed coin creates one new critter UUID; count event and later sighting once |

Known mound progress text retained from existing server-message notes: `Small cracks begin to form in the mound...`; `The cracks seem to be getting larger, keep hitting it!`; `Chunks of the mound begin falling away...`; `The mound is about to fall to pieces! Keep going!`. The supplied explanation associates the last progress line with an empty outcome; **the terminal reveal/empty message remains the conclusive outcome**. Exact progress variants for each outcome have not been separately established here.

Resources: Cavern floor drops include the three gem colors and other items; Icy floor drops supply Icebreakers; Haunted floor drops supply Soothing Incense and Shining Coins. Forest bird-food pickup/feeding evidence belongs in the bird ledger. A starting grant can introduce these resources without a matching biome floor pickup.

### Authoritative Gimmiegold source evidence

For the observed three-floor-coin baseline, confirmed pickup/use history outranks speculative marker typing: three actual floor coins collected, all acquired/starting coins consumed and a fresh empty coin inventory establish the source in the qualifying Haunted area. Required retained checking still applies in Unique/Sparkling; Full Clear still requires actual captures. Unknown/predicted markers cannot invent more coin opportunities after that baseline. Additional actual coins and outstanding party notices remain blockers. Without full local confirmation, retain the conservative scan-based source recovery. Retained UUID totals are not coin-use totals and must not inflate a subsequent extra coin's requirement. See [Gimmiegold](gimmiegold.md).

## Evidence still to pin down

- Exact boss spawn/post-capture messages are now recorded in [MESSAGES.md](MESSAGES.md). Remaining prerequisite-to-spawn relationships still need evidence; do not invent them from having items alone.
- Confirm numeric species minima and total coin opportunities from Hypixel. The supplied bird example establishes nine Forest food pickups plus applicable starting food; party contributions can add more feeding demand. Other existing baselines remain in the critter index, not promoted to new server facts.
- Preserve formatting, multiline entry banners and optional Chat Heads segments when testing the [supplied message variants](MESSAGES.md). The supplied coin message confirms the spelling **Gimmiegold**.

After one clearing-completion trigger, keep all unsent independently eligible starting-item reminders available for the run. Recompute quantity, destination and missing discoveries immediately before each delivery; only delivered notices mark notification/source state. Space titles by four seconds. Suppression or cancellation does not consume an unsent reminder. Initial inventory sampling starts at the first available inventory sample, rather than world-arrival time; grants overlap it and separately recorded later floor pickups are subtracted.

## Last updated

2026-10-15 - Corrected incense limits to 4/3/2 for 0/1/2 starting incense; retained unified positive detection and ordinary scan circles.

2026-10-15 - Fixed complete, independently gated starting-resource reminder delivery (#55).

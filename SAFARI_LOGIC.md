# Safari logic notes

These notes describe evidence from Hypixel and the deductions used to track Safari. They are concise rules to guide implementation, not a record of every current code behavior.

- [Shared Safari evidence](docs/critters/TRACKING.md): entry, tickets/Head Start, resources, UUIDs, capture/loot-share events, variable spawns and late biome entry.
- [All 37 critters](docs/critters/README.md): one editable page per species, with its evidence and completion rules.
- [Exact Hypixel messages](docs/critters/MESSAGES.md): supplied quotations, audiences, Chat Heads variants and event meanings.
- [Typed floor drops](docs/critters/FLOOR_DROPS.md): inspected entity-order prediction and future biome/item filtering; critter requirements remain unchanged.
- [Config GUI defaults](docs/CONFIG_DEFAULTS.md): independent features on by default; Insta Sell valuation and achievement party announcements off.

| Mode | Chosen goal | Required evidence |
| --- | --- | --- |
| Unique | Secure each species once; add its Sparkling check when anyone lacks that discovery | Own capture or party loot share plus any required Sparkling evidence; biome completion requires a catch of every species |
| Full Clear | Capture every possible critter, with the one-Macaw exception | Established minimum plus additional verified spawns; resolve remaining critters and spawn sources. Macaw needs one shared catch after all known food is resolved |
| Sparkling | Check relevant missing-discovery opportunities | Inside the species' scan radius: retained qualified UUID checks, reconciled with shared captures, reach its Sparkling minimum; special source rules still apply |

Completed missing panels say **All caught and checked!** in Unique and Full Clear; Sparkling says **All checked!**. Unique requires every species caught at least once plus all required checking for party-missing discoveries. Sparkling checks only party-needed discoveries, subject to their spawn prerequisites.

Personal catches and party catches stay distinct. Starting grants, floor pickups, resource uses, spawns and sightings never manufacture catches. Keep catches received before visiting a biome; subtract them from its minimum once. Unknown coverage remains pending.

The [missing/check panel](docs/critters/TRACKING.md#missingcheck-panel-by-mode) is mode-specific: Full Clear lists capture shortages/live remainders; Unique lists uncaught species and plain species rows for unfinished required checking; Sparkling lists discoveries still needing checks. Only Scrappy, Gemzie, Troodon and Gazer have fixed-spawn X/X check suffixes; Unique shows them only for missing party discoveries.

All missing/check panels list plain critter names in species colors with only applicable gray `X/M captured` or `X/M checked` suffixes. Below that species list, retain the conditional bee-nest, Snooper-wall and Rockmite-mound footers: Full Clear shows outstanding work regardless of discoveries; Unique retains it before the first catch or for a needed unfinished Sparkling check; Sparkling retains it only for a needed unfinished discovery. No duplicate Gazer counter, checking prose or absence notes. Source tracking and highlights remain active independently.

Fixed-spawn counters explicitly label **X/M captured** in Full Clear (confirmed catches only) and **X/M checked** in Sparkling/Unique's needed discovery checks (retained qualified UUIDs reconciled with shared captures). Hideyho's inside/outside watch/title/chat operates only in Unique or Full Clear while the local player is in Haunted; leaving Haunted or switching to Sparkling cancels its pending watch. Sparkling mode emits no inside/outside title or chat.

**Highest-priority Icy ESP:** if anyone still needs Sparkling Wumpa, highlight every Icy species until its first shared catch, before normal mode rules. Then apply those rules independently for that species. Checking/catching Wumpa does not replace the other species' catches. See [Icy ESP priority](docs/critters/TRACKING.md#highest-priority-icy-esp-rule-missing-sparkling-wumpa).

A biome completes when every critter passes its active-mode check; send `fd`/`cd`/`id`/`hd` in party chat. Received biome reports update **Full Clear/Sparkling** progress, but **never Unique** progress or catch counts. Unique biome completion also requires at least one shared catch of every species, even when a variable species was proven unavailable. On entering a reported-complete biome, scan again and reopen any failing critter/biome checks. See [party synchronization](docs/critters/TRACKING.md#biome-completion-and-party-synchronization).

Unique also rechecks locally completed biomes on entry, regardless of party messages. Any failing Sparkling requirement for a currently missing party discovery reopens that species' combined check and the biome; existing catches remain recorded. This applies to every species, including Rockmite mounds found unbroken after Cavern was previously complete.

**Unique progress = capture/loot-share logs + local mode checks.** It does not listen to party biome-completion messages. Local biome-entry checks still affect progress and prevent completion while required Sparkling work remains.

**Unique announcements require current presence in that biome.** Captures received elsewhere can update progress but cannot trigger its party completion message. Revalidate on entry and announce only while still inside that biome.

For partially cleared biomes, the species minimum plus entry into its UUID-check radius and no remaining spawns establishes Full Clear; already-captured UUIDs need not be seen again. Unique/Sparkling checks retain distinct qualified UUID sightings for every species throughout the run, safely reconciling shared captures without double counting. Source conditions remain separate; checked totals persist after unloading and biome reentry. For mid-run Haunted, no floor drops + no Gimmiegolds + minimum Gimmiegold captures establishes completion; below the minimum, remain pending because a teammate may still hold coins. See the [shared rules](docs/critters/TRACKING.md#partially-cleared-biome).

A thrown capsule can miss. Only the server's `You threw a Critter Capsule at the <Critter>!` message confirms a hit/capture attempt. Escape confirms failure; retries repeat those events until a successful capture confirmation. Attempts never increment catches.

Spawn messages can precede entities. Gemzie's announcement starts a pending Sparkling check; complete it only when shared captures plus live uncaptured Gemzies in the current in-radius scan reach three. Revalidate this on Cavern entry after a party completion report; the message alone must not complete or announce Cavern.

Mid-run Forest entry can resolve missed food/feeder history inside 15 blocks of (6, 51): no remaining food-source markers or held food, plus nine accounted-for feeding opportunities from shared catches and current live birds (Macaw pairs count once), plus known extras. Keep the mode-specific catch requirements and pending extra-resource gates. See [Forest mid-run](docs/critters/TRACKING.md#forest-mid-run-birds).

Birds share a feeder: teammates' food can produce more announcements than the local food count predicts. Full Clear first requires known food used; then Bluebird and Parakeet turn green independently when none remains in their qualifying scans. Macaw completes after one shared catch even if more Macaws remain, provided all known food has been used. New food/spawns reopen evaluation. See [bird rules](docs/critters/TRACKING.md#forest-birds-shared-feeder-independent-completion).

When a player finishes their clearing biome, unused starting food/coins still held outside Forest/Haunted automatically trigger a **title alert and party notice**, with identical gates, **only if the relevant party sparkling discovery is missing**. Neither has a config toggle. Titles are **USE YOUR BIRD FOOD!** / **USE YOUR SHINING COIN!**. The notice reopens affected source checks/biome completion without adding catches or pickups. Suppress duplicates and reminders for used items, players already in the destination biome, or discoveries everyone owns. See [starting-resource reminders](docs/critters/TRACKING.md#unused-starting-foodcoin-reminders).

After an unused-resource notice, keep that biome pending until every reported item is used and its additional bird/Gimmiegold spawn is accounted for. Use spawn messages or new shared-capture + current-scan evidence, without reusing the old population. Then recheck the mode's remaining requirements; Full Clear still needs the spawned critters captured.

Standard capsules guarantee Common/Uncommon catches once hit; Rare/Epic/Legendary can escape and may return with a replacement UUID. Masterful capsules guarantee every rarity once hit. Replacement UUIDs alone must not increase population, capture targets or Sparkling opportunities. See [escape identity](docs/critters/TRACKING.md#capsule-escape-and-replacement-identity).

The retained numeric baselines come from the earlier source audit and require server confirmation. Implementation timers, ESP behavior and personal-best conditions are not evidence-based rules unless separately agreed. Party completion reports follow the explicit synchronization policy above. These notes do not claim that the current mod already implements every rule here.

When changing a rule, update its critter page and explain the server evidence and deduction. Change code separately with focused validation. Follow the requested workflow: build/test, verify behavioral changes in IntelliJ IDEA using the existing Minecraft Client/DevAuth setup, then commit and push to `v4`. Record tests actually performed; documentation edits do not certify an in-game run.

Every critter checked in Unique/Sparkling retains distinct qualified run UUIDs. Fixed checked counters, including Gazer 4/4, remain complete after unloading; reconcile shared captures without double counting previously seen critters. Gimmiegold can recover missed coin-use chat from a fresh empty inventory, resolved coin-floor evidence and the full required captured/current population; Full Clear still requires all actual captures.

Documentation revisions and source synchronization: [SOURCE_SYNC.md](docs/SOURCE_SYNC.md).

**Wumpa and Doomspiral are capture-required exceptions in Sparkling mode.** A needed boss remains unchecked until this run receives a personal capture or party loot-share confirmation. Seeing or retaining its UUID, a spawn/awoken/slain message, and a party biome-completion report cannot substitute for that capture. The confirmed catch completes its check immediately and remains valid across unloading, biome reentry and mode changes. Other species keep their existing checking rules.

A detected Sparkling of any species blocks its biome completion in **every mode**, regardless of party discoveries. Retain the obligation after unloading/reentry, require a confirmed Sparkling catch, and reopen completion if a new Sparkling appears. Party biome reports and ordinary catches cannot bypass it. See [Sparkling capture obligations](docs/critters/TRACKING.md#sparkling-captures-block-biome-completion-in-every-mode).

Safari entry protection is stage-specific: [Torrhus readiness and Safari kick/presence checks](docs/SAFARI_ENTRY_GUARD.md). Sender-specific party readiness clears only that sender's hold; unknown/loading presence stays blocked. Shift never bypasses entry protection.

## Last updated

2026-10-16 - Added stage-specific entry protection; retained all capture/check evidence rules.

# Safari logic notes

These notes describe evidence from Hypixel and the deductions used to track Safari. They are concise rules to guide implementation, not a record of every current code behavior.

- [Shared Safari evidence](docs/critters/TRACKING.md): entry, tickets/Head Start, resources, UUIDs, capture/loot-share events, variable spawns and late biome entry.
- [All 37 critters](docs/critters/README.md): one editable page per species, with its evidence and completion rules.
- [Exact Hypixel messages](docs/critters/MESSAGES.md): supplied quotations, audiences, Chat Heads variants and event meanings.
- [Typed floor drops](docs/critters/FLOOR_DROPS.md): inspected entity-order prediction and future biome/item filtering; critter requirements remain unchanged.
- [Config GUI defaults](docs/CONFIG_DEFAULTS.md): independent features on by default; Insta Sell valuation, achievement party announcements, and warp alerts off.

| Mode | Chosen goal | Required evidence |
| --- | --- | --- |
| Unique | First shared captures clear species rows; needed Honeybug checking also gates progress | Rockmite/Snoozle need a catch plus resolved safari-wide sources, or cd confirming unseen zero spawn; Honeybug needs local checking or trusted fd if its discovery is needed; no-Macaw and confirmed-Sparkling exceptions remain |
| Full Clear | Variable species require party report plus minimum shared captures | Scrappy/Gemzie/Troodon/Gazer may complete early from fixed capture totals; positive remaining critters reopen only their species. Macaw keeps its exception |
| Sparkling | Check relevant missing-discovery opportunities | Inside the species' scan radius: retained qualified UUID checks, reconciled with shared captures, reach its Sparkling minimum; special source rules still apply |

Completed missing panels say **All caught and checked!** in Unique and Full Clear; Sparkling says **All checked!**. Unique advances per species capture; Rockmite/Snoozle additionally need resolved safari-wide sources; cd confirms an unseen zero-spawn case. Honeybug is a separate progress exception when its discovery is needed: its row clears on one catch but progress waits for local nest checking or trusted fd. Exception: when all nine baseline bird foods plus known extras are used, no Macaws have spawned/been seen and no known food remains outstanding, remove Macaw from Unique's missing panel and count it complete for Forest progress without recording a catch. Other Forest requirements still apply. Sparkling checks only party-needed discoveries, subject to their spawn prerequisites.

Personal catches and party catches stay distinct. Starting grants, floor pickups, resource uses, spawns and sightings never manufacture catches. Keep catches received before visiting a biome; subtract them from its minimum once. Unknown coverage remains pending.

The [missing/check panel](docs/critters/TRACKING.md#missingcheck-panel-by-mode) is mode-specific: Full Clear lists capture shortages/live remainders; Unique lists uncaught species and Rockmite/Snoozle awaiting source resolution or cd confirmation of no spawn; Sparkling lists discoveries still needing checks. Only Scrappy, Gemzie, Troodon and Gazer have fixed-spawn X/X check suffixes; Unique shows them only for missing party discoveries.

All missing/check panels list plain critter names in species colors with only applicable gray `X/M captured` or `X/M checked` suffixes. Below that species list, retain the conditional bee-nest, Snooper-wall and Rockmite-mound footers: Full Clear shows outstanding work regardless of discoveries; Unique retains a helper before its first catch, then only needed Rockmite/Snoozle source work while those species remain incomplete; Sparkling retains it only for a needed unfinished discovery. No duplicate Gazer counter, checking prose or absence notes. Source tracking and highlights remain active independently.

Fixed-spawn counters explicitly label **X/M captured** in Full Clear (confirmed catches only) and **X/M checked** in Sparkling/Unique's needed discovery checks (retained qualified UUIDs reconciled with shared captures). Hideyho's inside/outside watch/title/chat operates only in Unique or Full Clear while the local player is in Haunted; leaving Haunted or switching to Sparkling cancels its pending watch. Sparkling mode emits no inside/outside title or chat.

**Highest-priority Icy ESP:** if anyone still needs Sparkling Wumpa, highlight every Icy species until its first shared catch, before normal mode rules. Then apply those rules independently for that species. Checking/catching Wumpa does not replace the other species' catches. See [Icy ESP priority](docs/critters/TRACKING.md#highest-priority-icy-esp-rule-missing-sparkling-wumpa).

A biome completes when every critter passes its active-mode check; send `fd`/`cd`/`id`/`hd` in party chat. Received biome reports update Full Clear/Sparkling progress. Unique ordinary species need only their own shared capture; Rockmite/Snoozle also need source resolution, with cd confirming unseen zero spawn. Reports never add catches. Unique biome completion requires a shared catch per species except proven no-Macaw and source-resolved, unseen Rockmite/Snoozle with party cd confirmation. Other unavailable species still require their catches. Full Clear trusts the report after minimum captures and reopens only positively evidenced remaining work; Sparkling revalidates on entry; Unique follows the limited Cavern reversal policy below. See [party synchronization](docs/critters/TRACKING.md#biome-completion-and-party-synchronization).

Unique trusts party checking for every ordinary species and needs no local Sparkling radius or scan. Rockmite/Snoozle complete from one shared catch plus all mounds/all five walls resolved, anywhere in Safari; cd remains trusted checking evidence if sources are unknown. With no catch, resolved sources plus cd can confirm no spawn only if no actual critter was encountered. Positively unbroken sources reopen their species regardless of discovery ownership. Loaded source observations may resolve or reopen these checks from any Safari biome. Unloaded/unknown space is not proof that a source was broken.

**Unique progress = first shared species captures**, with safari-wide source validation and the report-confirmed no-spawn exception for Rockmite/Snoozle, and needed Honeybug checking/report confirmation. The full biome does not need to be caught before each species advances. Actual detected, uncaptured Sparklings remain blockers in every mode; no captured ordinary species must wait for local discovery scans.

**Unique announcements require current presence in that biome.** Captures received elsewhere can update progress but cannot trigger its party completion message. Revalidate on entry and announce only while still inside that biome.

For partially cleared biomes, the species minimum plus entry into its UUID-check radius and no remaining spawns establishes Full Clear; already-captured UUIDs need not be seen again. Ordinary Unique/Sparkling checking retains qualified UUID history; Gimmiegold uses the special current-population rule, and birds use current feeding equivalents. Source conditions remain separate; checked totals persist after unloading and biome reentry. Gimmiegold Sparkling requires all initial coin slots collected, empty coin inventory, and captures + current rendered Gimmiegolds reaching that slot count plus known starting/party-notified coins; outside Haunted a party hd can override ordinary checking. Unique retains its catch and Full Clear its capture minimum. See the [shared rules](docs/critters/TRACKING.md#partially-cleared-biome).

A thrown capsule can miss. Only the server's `You threw a Critter Capsule at the <Critter>!` message confirms a hit/capture attempt. Escape confirms failure; retries repeat those events until a successful capture confirmation. Attempts never increment catches.

Spawn messages can precede entities. Gemzie's announcement starts a pending Sparkling check; complete it only when retained qualified UUID checks reconciled with shared captures reach three. They need not be rendered simultaneously. Revalidate this on Cavern entry after a party completion report; the message alone must not complete or announce Cavern.

Forest bird spawning uses shared capture counts plus current nearby bird counts, never accumulated distinct UUIDs. Count two Macaws as one feeding opportunity. Feeder announcements overlap that population and are not added again. Require nine feeding equivalents plus known extras, no raw Forest floor drops and no held bird food. No Macaws spawned/seen plus locally resolved food while in Forest completes Macaw in every mode without a catch. See [Forest birds](docs/critters/TRACKING.md#forest-mid-run-birds).

Birds share a feeder: teammates' food can produce more announcements than the local food count predicts. Full Clear first requires known food used; then Bluebird and Parakeet turn green independently when none remains in their qualifying scans. Macaw completes after one shared catch even if more Macaws remain, provided all known food has been used. New food/spawns reopen evaluation. See [bird rules](docs/critters/TRACKING.md#forest-birds-shared-feeder-independent-completion).

When a player finishes their clearing biome, unused starting food/coins still held outside Forest/Haunted automatically trigger a **title alert and party notice**, with identical gates, **only if the relevant party sparkling discovery is missing**. Neither has a config toggle. Titles are **USE YOUR BIRD FOOD!** / **USE YOUR SHINING COIN!**, or **USE YOUR STARTING ITEMS!** for mixed coin/food. Combine eligible items into one short party message, e.g. `[SM] Use: 1x Shining Coin + 1x Yogi Berry`; retain independent filtering and source obligations for each item. Receivers also accept old verbose notices. The notice reopens affected source checks/biome completion without adding catches or pickups. Suppress duplicates and reminders for used items, players already in the destination biome, or discoveries everyone owns. See [starting-resource reminders](docs/critters/TRACKING.md#unused-starting-foodcoin-reminders).

After an unused-resource notice, keep that biome pending until every reported item is used and its additional bird/Gimmiegold spawn is accounted for. Use spawn messages or new shared-capture + current-scan evidence, without reusing the old population. Then recheck the mode's remaining requirements; Full Clear still needs the spawned critters captured.

Standard capsules guarantee Common/Uncommon catches once hit; Rare/Epic/Legendary can escape and may return with a replacement UUID. Masterful capsules guarantee every rarity once hit. Replacement UUIDs alone must not increase population, capture targets or Sparkling opportunities. See [escape identity](docs/critters/TRACKING.md#capsule-escape-and-replacement-identity).

The retained numeric baselines come from the earlier source audit and require server confirmation. Implementation timers, ESP behavior and personal-best conditions are not evidence-based rules unless separately agreed. Party completion reports follow the explicit synchronization policy above. These notes do not claim that the current mod already implements every rule here.

When changing a rule, update its critter page and explain the server evidence and deduction. Change code separately with focused validation. Follow the current user-requested branch and workflow. Build/test and verify behavioral changes in IntelliJ IDEA using the existing Minecraft Client/DevAuth setup; commit or push only when requested. Record tests actually performed; documentation edits do not certify an in-game run.

Qualified UUID histories remain run-owned, but Gimmiegold Sparkling validation uses captured plus currently rendered population. Fixed checked counters, including Gazer 4/4, remain complete after unloading; reconcile shared captures without double counting previously seen critters. Gimmiegold Sparkling uses the initial coin-slot total plus deduplicated unused-coin contributions; all those floor slots must be collected and coin inventory empty; Full Clear still requires all actual captures.

Documentation revisions and source synchronization: [SOURCE_SYNC.md](docs/SOURCE_SYNC.md).

**Wumpa and Doomspiral are capture-required exceptions in Sparkling mode.** A needed boss remains unchecked until this run receives a personal capture or party loot-share confirmation. Seeing or retaining its UUID, a spawn/awoken/slain message, and a party biome-completion report cannot substitute for that capture. The confirmed catch completes its check immediately and remains valid across unloading, biome reentry and mode changes. Other species keep their existing checking rules.

A detected Sparkling of any species blocks its biome completion in **every mode**, regardless of party discoveries. Retain the obligation after unloading/reentry, require a confirmed Sparkling catch, and reopen completion if a new Sparkling appears. Party biome reports and ordinary catches cannot bypass it. Once all detected Sparklings of that species are caught, Unique and Sparkling complete the species immediately and remove its missing row, overriding its ordinary scan/source checks. Full Clear instead resumes its normal capture/source requirements and completes once those pass; resolved Sparkling sightings must not remain pending. See [Sparkling capture obligations](docs/critters/TRACKING.md#sparkling-captures-block-biome-completion-in-every-mode).

Starting-resource reminders cover every eligible held item/quantity independently after clearing, with paced delivery and fresh inventory/destination/discovery checks. Unsent reminders remain eligible later and create no source obligations before delivery.

Sparkling mode calls the missing/check panel **Unchecked** in all four biomes. For Honeybug, retain initial nest classifications/counts for the whole run. Full Clear completes its known bee population through shared catch messages without another local render scan. Unique needs one catch and, when its Sparkling is needed, local checking or trusted fd for progress; its species row still clears at one catch. Sparkling needs only checking/report trust. Honeybug fd trust survives later Forest entry and never fabricates captures.

Safari entry protection is stage-specific: [Torrhus readiness and Safari kick/presence checks](docs/SAFARI_ENTRY_GUARD.md). Sender-specific party readiness clears only that sender's hold; unknown/loading presence stays blocked. Shift never bypasses entry protection.

## Last updated

2026-10-17 - Rockmite/Snoozle use retained safari-wide sources: Unique catch plus source proof, report-confirmed unseen zero spawn, and Sparkling checks without ordinary captures.

2026-10-17 - Retained initial Honeybug nests and party checking trust, separated Unique row/progress behavior, and named every Sparkling checklist Unchecked.

2026-10-17 - Specified Gimmiegold Sparkling coin-slot/current-population rules and remote hd override without waiving mode capture requirements.

2026-10-17 - Completed source audit; fixed remote report contradictions and clearing-triggered starting-item reminders.

2026-10-17 - In Full Clear, local species completion advances progress in the clearing biome without a party report, retaining earned progress after travel. Other biomes without local proof retain their report/minimum requirements and existing exceptions; see docs/MISSING_PANEL.md.

2026-10-17 - Honeybug retains the initial actual nest total for run-wide Full Clear capture accounting. Unique removes its species row at one catch but retains needed nest work until all nests are personally punched or catches plus live bees account for them. Remote Forest reports fill Honeybug progress until entry; see docs/critters/honeybug.md.

2026-10-17 - Full Clear missing rows use retained local capture/source and empty-scan proof independently of party-report progress; see docs/MISSING_PANEL.md. Unique and Sparkling keep their separate checklist rules.

2026-10-16 - Full Clear progress trusts capture-backed biome reports, with fixed-count early completion and positive-evidence reopening.

2026-10-16 - Unique per-species progress no longer waits for discovery scans; Rockmite/Snoozle await cd and loaded-source observations work outside Cavern.

2026-10-16 - Updated Unique party-report trust and limited Cavern source revalidation.

2026-10-16 - Starting food/coin reminders use one compact combined party message.

2026-10-16 - Sparkling catches complete Unique/Sparkling immediately; Full Clear still requires its normal remaining captures/sources.

2026-10-16 - Bird completion uses shared captures plus current nearby counts and floor/inventory evidence, never distinct UUID history; no-Macaw exception applies in every mode.

2026-10-16 - Added Unique Forest completion and missing-panel exception for resolved runs with no Macaw spawns.

2026-10-15 - Added mandatory capture of any observed Sparkling before biome completion in every mode; retained boss capture exceptions.

2026-10-15 - Fixed complete, independently gated starting-resource reminder delivery (#55).
2026-10-16 - Added stage-specific entry protection; retained all capture/check evidence rules.

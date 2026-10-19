# Safari logic notes

These notes describe evidence from Hypixel and the deductions used to track Safari. They are concise rules to guide implementation, not a record of every current code behavior.

- [Shared Safari evidence](docs/critters/TRACKING.md): entry, tickets/Head Start, resources, UUIDs, capture/loot-share events, variable spawns and late biome entry.
- [All 37 critters](docs/critters/README.md): one editable page per species, with its evidence and completion rules.
- [Exact Hypixel messages](docs/critters/MESSAGES.md): supplied quotations, audiences, Chat Heads variants and event meanings.
- [Typed floor drops](docs/critters/FLOOR_DROPS.md): inspected entity-order prediction and future biome/item filtering; critter requirements remain unchanged.
- [Config GUI defaults](docs/CONFIG_DEFAULTS.md): independent features on by default; Insta Sell valuation, achievement party announcements, and warp alerts off.

| Mode | Chosen goal | Required evidence |
| --- | --- | --- |
| Unique | Species-specific catches plus required spawning/source proof | Birds require spawning proof plus first catches, with the no-Macaw exceptions. Rockmite/Snoozle need resolved sources or report-confirmed unseen zero spawn; needed Honeybug checking also gates progress |
| Full Clear | Actual required captures and resolved sources | Birds require expected captures inside or outside Forest, without report credit. Other species retain their local-proof/report/minimum paths; Macaw keeps its one-catch exception |
| Sparkling | Check relevant missing-discovery opportunities | Inside the species' scan radius: retained qualified UUID checks, reconciled with shared captures, reach its Sparkling minimum; special source rules still apply |

Completed missing panels say **All caught and checked!** in Unique and Full Clear; Sparkling says **All checked!**. Unique advances per species capture; Rockmite/Snoozle additionally need resolved safari-wide sources; cd confirms an unseen zero-spawn case. Honeybug is a separate progress exception when its discovery is needed: its row clears on one catch but progress waits for local nest checking or trusted fd. Exception: when all nine baseline bird foods plus known extras are used, no Macaws have spawned/been seen and no known food remains outstanding, remove Macaw from Unique's missing panel and count it complete for Forest progress without recording a catch. Other Forest requirements still apply. Sparkling checks only party-needed discoveries, subject to their spawn prerequisites.

Personal catches and party catches stay distinct. Starting grants, floor pickups, resource uses, spawns and sightings never manufacture catches. Keep catches received before visiting a biome; subtract them from its minimum once. Unknown coverage remains pending.

The [missing/check panel](docs/critters/TRACKING.md#missingcheck-panel-by-mode) is mode-specific: Full Clear lists capture shortages/live remainders; Unique lists uncaught species and Rockmite/Snoozle awaiting source resolution or cd confirmation of no spawn; Sparkling lists discoveries still needing checks. Only Scrappy, Gemzie, Troodon and Gazer have fixed-spawn X/X check suffixes; Unique shows them only for missing party discoveries.

All missing/check panels list plain critter names in species colors with only applicable gray `X/M captured` or `X/M checked` suffixes. Below that species list, retain the conditional bee-nest, Snooper-wall and Rockmite-mound footers: Full Clear shows outstanding work regardless of discoveries; Unique retains a helper before its first catch, then only needed Rockmite/Snoozle source work while those species remain incomplete; Sparkling retains it only for a needed unfinished discovery. No duplicate Gazer counter, checking prose or absence notes. Source tracking and highlights remain active independently.

Fixed-spawn counters explicitly label **X/M captured** in Full Clear (confirmed catches only) and **X/M checked** in Sparkling/Unique's needed discovery checks (retained qualified UUIDs reconciled with shared captures). Hideyho's inside/outside watch/title/chat operates only in Unique or Full Clear while the local player is in Haunted; leaving Haunted or switching to Sparkling cancels its pending watch. Sparkling mode emits no inside/outside title or chat.

**Highest-priority Icy ESP:** if anyone still needs Sparkling Wumpa, highlight every Icy species until its first shared catch, before normal mode rules. Then apply those rules independently for that species. Checking/catching Wumpa does not replace the other species' catches. See [Icy ESP priority](docs/critters/TRACKING.md#highest-priority-icy-esp-rule-missing-sparkling-wumpa).

A biome completes when every critter passes its active-mode check; send `fd`/`cd`/`id`/`hd` in party chat. Received biome reports update Full Clear/Sparkling progress. Unique ordinary species need only their own shared capture; Rockmite/Snoozle also need source resolution, with cd confirming unseen zero spawn. Reports never add catches. Unique biome completion requires a shared catch per species except proven no-Macaw and source-resolved, unseen Rockmite/Snoozle with party cd confirmation. Other unavailable species still require their catches. Full Clear trusts the report after minimum captures and reopens only positively evidenced remaining work; Sparkling revalidates on entry; Unique follows the limited Cavern reversal policy below. See [party synchronization](docs/critters/TRACKING.md#biome-completion-and-party-synchronization).

Unique trusts party checking for every ordinary species and needs no local Sparkling radius or scan. Rockmite/Snoozle complete from one shared catch plus all mounds/all five walls resolved, anywhere in Safari; cd remains trusted checking evidence if sources are unknown. With no catch, resolved sources plus cd can confirm no spawn only if no actual critter was encountered. Positively unbroken sources reopen their species regardless of discovery ownership. Loaded source observations may resolve or reopen these checks from any Safari biome. Unloaded/unknown space is not proof that a source was broken.

**Unique progress = first shared species captures**, with safari-wide source validation and the report-confirmed no-spawn exception for Rockmite/Snoozle, and needed Honeybug checking/report confirmation. The full biome does not need to be caught before each species advances. Detected Sparkling entities add no separate capture requirement; no captured ordinary species must wait for local discovery scans.

**Unique announcements require current presence in that biome.** Captures received elsewhere can update progress but cannot trigger its party completion message. Revalidate on entry and announce only while still inside that biome.

For partially cleared biomes, the species minimum plus entry into its UUID-check radius and no remaining spawns establishes Full Clear; already-captured UUIDs need not be seen again. Ordinary Unique/Sparkling checking retains qualified UUID history; Gimmiegold uses the special current-population rule, and birds use current feeding equivalents. Source conditions remain separate; checked totals persist after unloading and biome reentry. Gimmiegold Sparkling requires all initial coin slots collected, empty coin inventory, and captures + current rendered Gimmiegolds reaching that slot count plus known starting/party-notified coins; outside Haunted a party hd can override ordinary checking. Unique retains its catch and Full Clear its capture minimum. See the [shared rules](docs/critters/TRACKING.md#partially-cleared-biome).

A thrown capsule can miss. Only the server's `You threw a Critter Capsule at the <Critter>!` message confirms a hit/capture attempt. Escape confirms failure; retries repeat those events until a successful capture confirmation. Attempts never increment catches.

Gemzie's exact Hypixel chamber-opening announcement completes its Sparkling check after five seconds, without entity sightings. This run-owned proof survives Cavern reentry and supplies 3/3 checked in Sparkling mode and in Unique mode when the party needs Sparkling Gemzie. Qualified UUID/capture evidence remains an alternative checking path. The timer supplies no catches; Unique still needs its first shared catch and Full Clear keeps its capture/absence requirements.

Forest bird spawning ignores attraction chat. Inside the radius-15 bird scan circle at X/Z 6/51, shared captures plus current rendered birds must reach nine food opportunities plus known starting/party-notified food; a Macaw pair is one opportunity. Raw Forest food markers are checked Safari-wide and held food blocks bird completion. See [Forest bird completion](docs/BIRD_COMPLETION.md).

The three birds are explicit mode exceptions. Sparkling requires local spawning proof inside Forest or fd outside; Unique additionally requires each shared species catch, waiving Macaw for proven local no-spawn or outside fd when both other birds are caught. Full Clear always requires actual expected bird captures, retaining the one-Macaw exception; fd never supplies captures. Other Forest species keep their own rules. Only initial Forest clearers with at least six separate local food pickups, no held food and local mode completion may send fd. Late entrants cannot send it.

The exact server message `HOTSPOT! Your Hunting Hotspot is the <Biome> Biome!` is the maintainer-confirmed departure trigger. Two seconds later, send one concise absolute report of every witnessed starting Shining Coin, Bag of Seeds, Wriggleworm and Yogi Berry, independently of discovery loading, mode, HUD and ESP. Examples: `[SM] Starting items: 2x Bag of Seeds` and `[SM] Starting items: 1x Shining Coin; 1x Yogi Berry`; use `none` for a witnessed empty applicable grant set. See [complete protocol and limits](docs/critters/TRACKING.md#early-party-starting-resource-synchronization).

Starting quantities are shared early as described above, not when a player clears a biome. End-of-clearing reminders are **local titles only**. Keep the existing four-second trigger/spacing, current held quantities, independent discovery gates and destination suppression: food only while a bird discovery is needed and outside Forest; coins only while Gimmiegold is needed and outside Haunted. There is no new setting.

Single-category titles remain **USE YOUR BIRD FOOD!** / **USE YOUR SHINING COIN!**. A mixed reminder uses **USE YOUR STARTING ITEMS!** with one combined item/quantity/destination subtitle, so one title does not immediately overwrite the other. Recheck eligibility at display time; a temporarily suppressed item remains eligible later. Mark only local presentation state after display.

Do not send an automatic unused-item party message, local chat echo, replacement label, retry or fallback. A title never creates or appends a contribution/outstanding obligation. Existing legacy inbound formats remain accepted through the common sender/type maximum ledger. Normal `fd`/`cd`/`id`/`hd`, ticket readiness and unrelated party commands are unchanged. Additional known quantities reopen only their affected spawning requirements; they never add catches or floor pickups.

Standard capsules guarantee Common/Uncommon catches once hit; Rare/Epic/Legendary can escape and may return with a replacement UUID. Masterful capsules guarantee every rarity once hit. Replacement UUIDs alone must not increase population, capture targets or Sparkling opportunities. See [escape identity](docs/critters/TRACKING.md#capsule-escape-and-replacement-identity).

The retained numeric baselines come from the earlier source audit and require server confirmation. Implementation timers, ESP behavior and personal-best conditions are not evidence-based rules unless separately agreed. Party completion reports follow the explicit synchronization policy above. These notes do not claim that the current mod already implements every rule here.

When changing a rule, update its critter page and explain the server evidence and deduction. Change code separately with focused validation. Follow the current user-requested branch and workflow. Build/test and verify behavioral changes in IntelliJ IDEA using the existing Minecraft Client/DevAuth setup; commit or push only when requested. Record tests actually performed; documentation edits do not certify an in-game run.

Qualified UUID histories remain run-owned, but Gimmiegold Sparkling validation uses captured plus currently rendered population. Fixed checked counters, including Gazer 4/4, remain complete after unloading; reconcile shared captures without double counting previously seen critters. Gimmiegold Sparkling uses the initial coin-slot total plus deduplicated early starting-coin contributions; initial coverage must be known, no applicable coin marker may remain, and coin inventory must be empty; Full Clear still requires all actual captures.

Documentation revisions and source synchronization: [SOURCE_SYNC.md](docs/SOURCE_SYNC.md).

**Wumpa and Doomspiral are capture-required exceptions in Sparkling mode.** A needed boss remains unchecked until this run receives a personal capture or party loot-share confirmation. Seeing or retaining its UUID, a spawn/awoken/slain message, and a party biome-completion report cannot substitute for that capture. A confirmed Wumpa catch marks every Icy species checked in Sparkling mode, regardless of missing scans or other catches, and clears the Icy missing list. This proof lasts for the run, including biome reentry. Unique and Full Clear retain their normal requirements. Doomspiral completes only its own check.

Detected Sparkling entities do not add capture requirements, reopen completed species, or block reports, run-complete alerts or Unique PBs. The user reports duplicate detections during capsule capture, so detection remains presentation evidence rather than a completion gate. Confirmed Sparkling catches still complete Unique/Sparkling species immediately; Full Clear retains its normal capture/source requirements. See [Sparkling detection and completion](docs/critters/TRACKING.md#sparkling-detection-does-not-gate-completion).

Starting-resource reminders cover every eligible held item/quantity independently after clearing, with paced delivery and fresh inventory/destination/discovery checks. Unsent reminders remain eligible later and create no source obligations before delivery.

Sparkling mode calls the missing/check panel **Unchecked** in all four biomes. For Honeybug, retain initial nest classifications/counts for the whole run. Full Clear completes its known bee population through shared catch messages without another local render scan. Unique needs one catch and, when its Sparkling is needed, local checking or trusted fd for progress; its species row still clears at one catch. Sparkling needs only checking/report trust. Honeybug fd trust survives later Forest entry and never fabricates captures.

Safari entry protection is stage-specific: [Torrhus readiness and Safari kick/presence checks](docs/SAFARI_ENTRY_GUARD.md). In Safari, full current-party tab presence permits entry after a two-second pause from any verified party kick-containing message; pending message checks cannot overrule ready presence. Missing/unknown presence stays blocked. On Torrhus Canyon, sender-specific r or any ticket-containing message clears only that sender's downtime hold. Holding either Shift key bypasses the current entry interaction without clearing any readiness or kick state; releasing Shift restores normal guarding.

## Last updated

2026-10-19 (America/New_York) — Early absolute starting-resource reports at HOTSPOT+2s, explicit unknown coverage and local-only later reminders (issue68).

2026-10-18 - Made bird spawning render-based, added mode-specific report trust and blocked late-entry or food-held fd reports.

2026-10-17 - Removed detected-Sparkling capture requirements from every completion mode after reported duplicate capsule entities; retained detection and confirmed-catch evidence.

2026-10-17 - Aligned entry guards with tab-first Safari presence, two-second kick pauses, ticket-message downtime release and Shift override.

2026-10-17 - Added per-interaction Shift entry override with local guidance and background missing-member name lookup.

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

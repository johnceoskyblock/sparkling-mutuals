# Safari history, import and achievements

## Purpose

Keep local run and Sparkling histories, inspect totals/records, and award local achievements. Manual log import backfills both histories when older logs are available. Personal capture, shared capture and party species coverage remain distinct; see the [glossary](DOCUMENTATION_GUIDE.md).

## Controls and defaults

| Control | Default/effect |
| --- | --- |
| `/sm history` or **History → History** | Opens Runs; other tabs are Bests, Sparklings, Achievements, Stats and Profit. |
| `/sm achievements`, `/sm stats` | Opens the corresponding tab. |
| **Save runs** | On; gates adding a finished live run, not Sparkling history. |
| **Achievements**, **Achievement sound** | Both on; party announcements default off. |
| **Coins at insta-sell** | Off; live reward valuation uses sell-offer prices by default. |
| `/sm history import [folder]` or **Import History from Logs** | Explicitly imports both histories; no automatic startup scan/toggle. |

The History settings tab places **History** immediately before **Import History from Logs**. The import button closes settings so progress is visible above the hotbar. For another instance, quote a path containing spaces:

```text
/sm history import "C:\Minecraft Backups\logs"
```

## Implemented behavior

### Live history and UI

Run recording follows the Safari ledger's arriving/leaving run objects. Chat is tallied on the client thread; replacing/ending a run finishes the prior tally before resetting it. Save runs must be enabled and the run must not be Alpha; captures or an actual reward summary establish recordable evidence. Records include mode, elapsed time, capture counts, loot shares, species coverage, tickets, shard quantities, feathers, Macaws, Sparkling announcements, splits, hotspot and known summary shard/essence/Hunting XP totals. A bounded fifteen-second window accepts delayed summary rows after the ledger closes. The 37/37 label describes party species coverage, not Full Clear population completion or PB eligibility.

Sparkling history independently saves recognized system announcements for personal and other catchers, with the live tickets-since-Sparkling counter. It is not gated by **Save runs** or restricted to the credited-run calculation used by [`!since`](SINCE_COMMAND.md). A live unique flag requires no earlier species entry from any participating catcher and the local mod user’s pre-catch cached discovery baseline to lack the species. The catcher’s discovery status does not determine whether the local user gains a unique. Catches update participating party members locally without an API fetch. An unavailable baseline leaves the unique flag unconfirmed; the flag is not guaranteed first-discovery proof.

Runs can be sorted, filtered to Sparkling runs, and navigated by exact run number (optional `#`). Typing a number selects and scrolls to that row, centered where possible, without removing surrounding runs. A target hidden by Sparkling Runs clears that filter. The current sort order is retained; scrolling manually after jumping remains possible. Sparklings combines Mine only with case-insensitive critter-name substring filtering. All scrolling history tabs support dragging the right-hand thumb or clicking its track, as well as the mouse wheel. Stats average run time excludes unknown durations and runs lasting ten minutes or longer, while retaining those rows in other totals. The 37/37 row is named Runs with all 37 caught. Known essence is shown in Stats and run details; hovering details also shows hotspot and Hunting XP. Profit groups by local date and includes marked estimates. Unknown duration or unpriced native coins display `-`.

Bazaar valuation includes recorded shards, feathers and actual summary essence using the existing cache and three-hour throttle. Summary essence already includes ticket bonuses and is not multiplied again. The saved essence contribution prevents double addition across imports/restarts. Complete logged rewards can be valued directly; incomplete imports keep a marked median estimate plus known essence. The Profit price button follows the same rules. Changing the setting alone does not reprice old records. Reward valuation does not request prices during import. Each explicit import may make one optional profile lookup for an informational Sparkling-total comparison, using the existing profile transport cache and no automatic retries.

### Catch log links

Hover a catch in the Sparklings tab to reveal **[Log]** in its row. Clicking opens a read-only in-game log viewer centered on the original system catch announcement, highlighted with its line number. Scroll or drag the scrollbar to browse surrounding context; Shift + scroll moves sideways for long lines. **Jump to catch** recenters the message; Back/Escape returns to the same history tab, filter and scroll position.

New imports retain the source path and line. Existing matched entries gain source information on re-import; older/live entries can also locate it on demand without importing. Every location is revalidated against owner, catcher, species and timestamp, and Alpha logs and player-written detection alerts are rejected. Archived `.log.gz` files are read in memory, without extracting or modifying the original. Lookup runs off-thread and uses no API requests. Missing/moved logs show an unavailable message; history remains intact. Without a valid saved source path, search the current instance's logs near the recorded date. A JSON-only export requires an available original `.log`/`.log.gz` to open context.

### Alpha server testing

Live recording reads exact scoreboard footers: `alpha.hypixel.net` excludes Alpha; `www.hypixel.net` identifies main. The last explicit address survives temporary scoreboard loss and resets with a new connection. Once Alpha is detected during a run, exclusion sticks to that run. Before any explicit Alpha footer, the state permits history; it does not establish that an unidentified connection is main.

Excluded runs do not save; excluded Sparkling announcements do not save or reset their ticket streak. Alpha ticket confirmations do not increment history/achievement ticket counters. Live capture tracking, HUDs and PBs keep their existing behavior. Manual imports also track client `Connecting to alpha.hypixel.net` metadata and exact server footers within each verified-owner file; reconnecting to main ends Alpha exclusion. Party-written server mentions cannot change state. JSON can identify a server with a root or per-entry `server` field. Unknown server metadata alone is not grounds for deletion.

Before merging, back up both histories, then remove saved catches/runs matched to explicit Alpha evidence. Duplicate catches are matched one-to-one by catcher, species and timestamp (ten-second live/log tolerance); excess copies near that logged capture are removed. Run duplicates require the same logged start/ticket anchor within 1.5 seconds, preferring a native record's details. Two real logged catches or separate runs remain separate. No record is removed merely because its original file is missing, a file failed, or its owner could not be verified. Exact duplicates and over-thirty-minute invalid runs retain their existing cleanup rules.

A manual import optionally compares the resulting party-catch history with the active IGN's selected-profile API total. This is one ordinary cached profile lookup, no retry; failures never stop import. The API exposes a total, not a timestamped catch ledger, may lag, and may cover a different history/profile scope. It cannot identify which catch is extra or recover a missing catch's species/catcher/time. Never delete or invent records to force equality. The result reports removed records and the comparison separately.

### Manual import inputs and run boundaries

Read only files directly inside the chosen folder: `.log`, `.log.gz` and chat `.json`, including `latest.log`. Modern/older prefixes, formatting, optional Chat Heads and midnight rollovers are supported. JSON may be an array or an `entries` array; accepted entries include message/text/formattedMessage/content/line strings, numeric seconds/milliseconds or ISO dates, timestamped log strings, and dated player/critter Sparkling entries. It is chat ingestion, not arbitrary saved-run JSON restoration.

Dated archive filenames establish the start day. Undated logs infer it from modification time and time-of-day rollovers, using the local timezone. Preserve names/timestamps; changed file dates/timezones cannot be reliably repaired.

A Hunting Hotspot starts a run and associates a nearby Manager ticket confirmation; without a Hotspot, a ticket can start one. The reward-summary header is the preferred end, with server departure as fallback. Missing ends remain unknown (`length=0` internally), even when a later boundary separates records. An unfinished run in this instance's active `latest.log` is left to the live recorder. Archived partial runs can import with no known duration; ticket-only runs can have zero captures. [Messages](critters/MESSAGES.md) owns exact recognized wording.

Recognized messages rebuild personal captures, loot shares, shard rewards, feathers, Macaw bonuses, Sparkling finds and party biome-completion splits. Party species coverage is the union of personal/shared species; participant count includes only message-established participants. Failed files are reported/skipped; a damaged gzip contributes no partial events, while valid files still import.

Before accepting a file, verify its non-chat `Setting user:` owner against the active IGN. Foreign or unverifiable sessions are skipped and counted in the import result. JSON exports need an explicit root `owner`, `self` or `username`, or equivalent identifiable session metadata; a sparkling catcher is not proof of session ownership. An account switch during an import cancels merging. Earlier imports were not owner-filtered, and old mixed-account rows cannot be safely identified retroactively. See the [read-only log audit](HISTORY_LOG_AUDIT.md).

Reward summary rows provide actual essence, Hunting XP and total shards even when capture messages were hidden. Modern logger prefixes, observed mode changes, hotspot biome and elapsed times embedded in biome completion reports are retained. Missing species distribution and original ticket tier remain unknown.

### Estimates

| Imported field | Implemented estimate | Limitation |
| --- | --- | --- |
| Mode | Use an observed mode selection; otherwise Sparkling, flagged estimated. | Missing selection messages cannot establish mode. |
| Coins | Complete logged item rewards use cached quotes; otherwise use the nearest-nine median estimate. Add actual known summary essence once. | `~` identifies estimates. These are current cached valuations, not verified historical market prices. |
| Unique flag | Earliest chronological species among imported and existing Sparkling entries, across catchers, flagged estimated. | Not per-account discovery verification; first-seen estimates are recalculated after older history is imported. Repeated species cannot remain unique merely because a different player caught them. |
| Tickets between finds | Recognized confirmations since the previous known find. | Missing logs undercount. |

Matched native records retain capture/item evidence and gain missing summary, mode or split fields. Existing imports can gain more complete capture/item evidence and an exact cached reward valuation when available. Neither is inserted again. Uniqueness is reconciled across the combined chronological history: estimated flags follow first occurrence, and older catcher-based unique flags are cleared for repeated species. Verified pre-owned first catches stay non-unique. Imported flags can affect [`!since unique`](SINCE_COMMAND.md).

### Achievements

Bests, Achievements and Stats use short labels without You/Party/Dex category tags. Hover for capture scope, counting rules and data sources. Single-run records are clickable and jump to their run. Personal boss PBs show Solo and Assisted (the stored Duo category); the hover explains the actual prerequisite evidence rather than implying an exact party size.

Examples: Most types counts distinct personally caught species, excluding loot shares. Macaws counts captures plus loot shares across the relevant runs. Fastest 37/37, Any% and Speedrunner stop at the first capture/loot-share that completes all 37 types, not departure. The exact elapsed timestamp is saved live and rebuilt by log imports; repeat captures do not advance it. Re-import fills this field in matching old records. Without exact timing, a known positive whole-run duration is a conservative upper bound; unknown zero duration cannot earn a speed award. Live speed awards can unlock immediately at the 37th type. Supernova still requires 100 personal sparkling catches. Secret details remain hidden until unlocked.

The Bests Luck section derives its longest uninterrupted streaks from live-recorded runs. Sparkling dry streak resets for any party sparkling, consistent with shared run credit. Imported runs break streaks rather than joining native runs across an unobserved gap. Macaw drought counts only runs with live confirmation that bird food use was resolved, or a confirmed personal/loot-shared Macaw; Macaw catches reset it. Runs without spawning proof break the Macaw streak. Older rows do not establish a miss merely because their Macaw reward count is zero. With no eligible evidence, a row shows no value. Both labels omit trailing colons. Sparkling and new-type dry-streak achievements now follow party catches and the same native-only continuity rule. New-type resets use the participating user’s recorded discovery credit, regardless of catcher. On update, saved streak progress is recalculated from available history; already unlocked achievements remain unlocked. Personal-catch achievements such as Supernova and Back to Back keep their existing criteria.

Unlocks are local, one-time IDs with saved timestamps. On first initialization, existing history earns awards quietly, with one summary notice. Imports and API-stat refreshes also award silently; they do not send individual party announcements. Live unlocks show chat/hover details, optional sound, a title for Legendary or higher, and optional party text `Achievement unlocked: X!`. Mythic and higher use the challenge fanfare. Locked secret awards hide their details in the UI.

| Award group | Implemented measures/thresholds |
| --- | --- |
| Own Sparkling/dex | First, 10/25/50/100 own entries; 10/20/30/37 species; biome sets; own Macaw/Wumpa/Doomspiral and Gemzie events. |
| Run Sparkling events | Party 2/3 or own 3 in a run; consecutive own-Sparkling runs; an own find within 5 tickets after an earlier own entry. |
| Dry streaks | Own Sparkling drought 10/20/30/40/50 runs; own flagged-unique drought 5/10/25/50/250 runs. These differ from party-credited `!since`. |
| Macaws | 3/4/8 in a run; drought 3/5/8 runs. |
| Totals | Runs 10/50/100/500; hours 10/50; personal captures + loot shares 1,000/5,000/10,000/25,000/50,000; coins 100M/1B; contests 10/50/100. |
| Run coverage/time | Party species 37; the same under 3:00/2:30; 10 own species. |
| Other events | Own Emerald Dye recognition; a run ending 03:00–04:59 local time; 50 runs starting on one local day. |

Runs/catches use the larger of local totals and available own API statistics; own dex unions local own species with API discoveries. API lookup refreshes in SkyBlock when the configured provider is enabled, normally every thirty minutes, without an after-run refresh. Hours use saved runs or the observed `/playtime` menu's Safari minutes plus later runs, whichever is greater. Open that server menu to supply pre-mod playtime; the mod does not request it automatically.

Disabling achievements blocks unlocks/announcements and periodic/contest processing, but history and several event-driven counters continue. Reenabling can catch up history/API-derived awards; it does not reconstruct every missed event. Import reconstructs own-Sparkling and Macaw droughts, not a complete historical unique-event drought. Unknown imported duration can still satisfy speed-award predicates; see [audit D7](IMPLEMENTATION_AUDIT.md#d7).

## Lifecycle and persistence


Live history transitions are guarded while the departing run is finalized. Reentrant chat/tick callbacks cannot finish it twice or mutate its tally; tickets arriving for the next run remain pending until the old tally is saved. A second persistence guard rejects another native record with an already-saved live start timestamp, so end-of-run achievements are not awarded twice.

At startup, history removes exact repeated records with a positive start timestamp. All recorded fields must match except the display run number; nearby starts, equal capture totals, or unknown timestamps alone are insufficient. Before writing repaired history, the original file is backed up to `sparkling-mutuals-runs.json.pre-deduplication.bak`. Remaining runs are numbered oldest-first and the next number is rebuilt. Loot, coins, splits and Sparkling lists in retained records are unchanged. Imports also drop exact duplicates already saved or repeated within the same batch. Existing short-run/import matching rules otherwise remain unchanged.

Local JSON files under `config/` are `sparkling-mutuals-runs.json`, `sparkling-mutuals-sparklings.json`, `sparkling-mutuals-achievements.json` and `sparkling-mutuals-playtime.json`. They are local-instance stores, not player/profile-keyed databases. PBs use the [separate PB properties file](CONFIG_DEFAULTS.md#personal-best-storage-and-migration). Reads occur at registration; failed JSON loads start empty and log a warning. Writes use temporary replacement and atomic move where supported.

Only one manual import runs at a time. Parsing is asynchronous; merging/saving runs on the client thread. Repeated/overlapping imports deduplicate: saved-run matching considers starts/ticket times within 90 seconds, assigns closest matches first and consumes each saved record at most once. Sparkling matches use the same catcher/species within ten seconds; new repeated messages within 1.5 seconds collapse. Separate later finds remain separate. New entries are added chronologically and run numbers rebuilt; existing values and the live ticket streak counter are retained. A completed live run can replace its overlapping imported duplicate.

Before merging, existing files are copied to `sparkling-mutuals-runs.json.pre-import.bak` and `sparkling-mutuals-sparklings.json.pre-import.bak`. Each import replaces these backups with the immediately prior saved versions. Merging the two histories is sequential, not a cross-file transaction; a later failure can leave the first saved update in place. Close Minecraft before restoring a backup over its corresponding JSON file.

## Edge cases

Missing messages/dates cannot be reconstructed, and close run starts can be ambiguous. Import estimates and local/API totals should not be presented as independently verified lifetime statistics. Existing history stays intact under normal deduplication, but malformed-file recovery and failed multi-file saves need backup care. The Alpha filter does not cover unrelated PBs or every achievement event. No automatic old-log scan runs at startup.

## Source and validation references

- Registration and tick order: [SparklingMutuals](../src/main/kotlin/net/johnceo/sparklingmutuals/SparklingMutuals.kt); live state: [RunHistory](../src/main/kotlin/net/johnceo/sparklingmutuals/history/RunHistory.kt), [RunTally](../src/main/kotlin/net/johnceo/sparklingmutuals/history/RunTally.kt), [SparklingLog](../src/main/kotlin/net/johnceo/sparklingmutuals/history/SparklingLog.kt), [TicketCounter](../src/main/kotlin/net/johnceo/sparklingmutuals/history/TicketCounter.kt), [HistoryServerPolicy](../src/main/kotlin/net/johnceo/sparklingmutuals/history/HistoryServerPolicy.kt).
- Import and storage: [LogImporter](../src/main/kotlin/net/johnceo/sparklingmutuals/history/importing/LogImporter.kt), [HistoryBackfill](../src/main/kotlin/net/johnceo/sparklingmutuals/history/importing/HistoryBackfill.kt), [HistoryImportMerge](../src/main/kotlin/net/johnceo/sparklingmutuals/history/importing/HistoryImportMerge.kt), [HistoryStore](../src/main/kotlin/net/johnceo/sparklingmutuals/history/HistoryStore.kt).
- Consumers: [HistoryScreen](../src/main/kotlin/net/johnceo/sparklingmutuals/history/ui/HistoryScreen.kt), [Achievements](../src/main/kotlin/net/johnceo/sparklingmutuals/history/Achievements.kt), [OwnStats](../src/main/kotlin/net/johnceo/sparklingmutuals/history/OwnStats.kt), [PlaytimeReader](../src/main/kotlin/net/johnceo/sparklingmutuals/history/PlaytimeReader.kt), [BazaarPrices](../src/main/kotlin/net/johnceo/sparklingmutuals/history/BazaarPrices.kt); controls: [SparklingCommand](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/SparklingCommand.kt), [SafariSettings](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariSettings.kt).
- Existing secondary coverage: [HistoryBackfillImportTest](../src/test/kotlin/net/johnceo/sparklingmutuals/history/HistoryBackfillImportTest.kt), [HistoryServerPolicyTest](../src/test/kotlin/net/johnceo/sparklingmutuals/history/HistoryServerPolicyTest.kt), [HistoryTest](../src/test/kotlin/net/johnceo/sparklingmutuals/history/HistoryTest.kt), [ProfitTest](../src/test/kotlin/net/johnceo/sparklingmutuals/history/ProfitTest.kt). No local runtime suites or Minecraft session ran for this documentation audit; CI results are separate. Live validation remains needed for message order, GUI actions, settings/restart, Alpha transitions and real imported-log fidelity; parser tests alone do not prove those paths.

## Last updated

2026-10-27 — Removed scope category tags and shortened stat names; scope lives in hover help. Party dry-streak achievements recalculate from native history. Speed awards and Fastest 37/37 use first complete capture coverage, with import enrichment and safe legacy timing fallback.

2026-10-27 — Shortened descriptions across all history tabs; retained personal/party scope, estimate labels and pricing limits. Added concise table-header hover help. Achievement criteria, PB rules and statistic calculations are unchanged by this wording pass.

2026-10-27 — Corrected Luck scope to party sparkling credit and verified Macaw spawning opportunities; imports/unknown evidence break streaks, and both labels omit colons.

2026-10-27 — Added hover [Log] actions with an asynchronous, read-only catch log viewer, archived-log support and saved source locations.

2026-10-27 — Exclude Alpha sessions during import; repair log-confirmed Alpha/duplicate records with backups while retaining records whose logs are missing. Add an optional cached API-total cross-check, never a deletion target.

2026-10-27 — Clarified personal/party/profile/reward scope throughout Bests, Achievements and Stats; added wrapped hover explanations, scope tags and clickable record navigation. Bests Luck now excludes imported runs and labels Sparkling dry streak explicitly. Achievement unlock rules and existing unlocks are preserved.

2026-10-27 — Runs longer than thirty minutes are removed from saved history on load (with a pre-duration-cleanup backup), excluded from new live records and rejected on every import. Exactly thirty minutes and unknown durations remain eligible. Remaining rows are renumbered chronologically. Average duration includes only known positive durations strictly below ten minutes, labeled Average run (includes any run below 10m).

2026-10-27 — Run-number input now jumps/selects without filtering out other rows. History scrollbars are wider, accept track clicks and support thumb dragging on every scrolling tab.

2026-10-27 — Manual import always rescans all eligible log files, including previously imported files. Matched rows are enriched in place; the completion message reports both newly added and updated existing runs. Repeated unchanged scans add no duplicates and report no updated rows. Newly available prices can also replace a zero-value imported estimate.

2026-10-27 — Audited 981 logs across JohnFounder/Skyblock. Added strict import ownership checks, summary rewards and essence profit, enrichment of matched records, modern prefixes, observed modes and timed splits, history searches and AFK-filtered average duration. See HISTORY_LOG_AUDIT.md for evidence and limitations.

2026-10-26 — Prevented reentrant live finalization and repeated native start insertion; added backed-up exact-duplicate cleanup and import-batch deduplication.

2026-10-26 — Live uniques use the local participating account’s cached discovery baseline. Startup repairs legacy repeated-species unique flags with a `.pre-unique-repair.bak` backup; imports also reconcile first-seen estimates after adding older history. No new API requests.

2026-10-21 — History uniqueness uses pre-catch party cache data; personal stats refresh every thirty minutes and Bazaar attempts every three hours.

2026-10-19 (America/New_York) — Audited live history, import, UI, achievements and persistence against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md) and [consolidated discrepancies](IMPLEMENTATION_AUDIT.md).

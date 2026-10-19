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

Run recording follows the Safari ledger's arriving/leaving run objects. Chat is tallied on the client thread; replacing/ending a run finishes the prior tally before resetting it. A live record is saved only with **Save runs** on, a non-Alpha run and at least one personally/shared captured party species. It includes mode at finish, elapsed time, personal capture counts, loot shares, personal/party species coverage, ticket confirmations, shard quantities, feathers, Macaw bonuses, Sparkling announcements and observed biome splits. History's `37/37` full-run labels describe party species coverage; they are not proof of Full Clear population completion or PB eligibility.

Sparkling history independently saves recognized system announcements for personal and other catchers, with the live tickets-since-Sparkling counter. It is not gated by **Save runs** or restricted to the credited-run calculation used by [`!since`](SINCE_COMMAND.md). A live unique flag requires no earlier same-player/species entry and an asynchronous discovery lookup that still lacks that species. Lookup failure or a fresh server result racing the catch can leave it unflagged; the flag is not guaranteed first-discovery proof.

Runs can be sorted by date, duration, catches, coins or species coverage, filtered to Sparkling runs, and selected for details/splits. Sparklings offers a mine-only filter. Bests combines dedicated PBs with history-derived records; Stats and Profit aggregate saved records. The Profit tab groups by the computer's local date and includes estimated imports in totals. Unknown duration displays `-`, and unknown live coin value is `-` until prices load.

Bazaar valuation uses recorded shards/feathers and the public Bazaar endpoint, with lazy thirty-minute caching. Missing live coin values are filled when prices arrive, excluding estimated imports. **However, the Profit tab's price button explicitly reprices every saved run, including imports, and leaves their estimate flag set.** An imported median estimate can therefore be replaced with reward-based value; see [audit D6](IMPLEMENTATION_AUDIT.md#d6). Changing the settings preference alone does not run this all-record repricing handler.

### Alpha server testing

Live recording reads exact scoreboard footers: `alpha.hypixel.net` excludes Alpha; `www.hypixel.net` identifies main. The last explicit address survives temporary scoreboard loss and resets with a new connection. Once Alpha is detected during a run, exclusion sticks to that run. Before any explicit Alpha footer, the state permits history; it does not establish that an unidentified connection is main.

Excluded runs do not save; excluded Sparkling announcements do not save or reset their ticket streak. Alpha ticket confirmations do not increment history/achievement ticket counters. Live capture tracking, HUDs and PBs keep their existing behavior. This filter neither deletes older records nor infers absent server addresses from imported chat logs.

### Manual import inputs and run boundaries

Read only files directly inside the chosen folder: `.log`, `.log.gz` and chat `.json`, including `latest.log`. Modern/older prefixes, formatting, optional Chat Heads and midnight rollovers are supported. JSON may be an array or an `entries` array; accepted entries include message/text/formattedMessage/content/line strings, numeric seconds/milliseconds or ISO dates, timestamped log strings, and dated player/critter Sparkling entries. It is chat ingestion, not arbitrary saved-run JSON restoration.

Dated archive filenames establish the start day. Undated logs infer it from modification time and time-of-day rollovers, using the local timezone. Preserve names/timestamps; changed file dates/timezones cannot be reliably repaired.

A Hunting Hotspot starts a run and associates a nearby Manager ticket confirmation; without a Hotspot, a ticket can start one. The reward-summary header is the preferred end, with server departure as fallback. Missing ends remain unknown (`length=0` internally), even when a later boundary separates records. An unfinished run in this instance's active `latest.log` is left to the live recorder. Archived partial runs can import with no known duration; ticket-only runs can have zero captures. [Messages](critters/MESSAGES.md) owns exact recognized wording.

Recognized messages rebuild personal captures, loot shares, shard rewards, feathers, Macaw bonuses, Sparkling finds and party biome-completion splits. Party species coverage is the union of personal/shared species; participant count includes only message-established participants. Failed files are reported/skipped; a damaged gzip contributes no partial events, while valid files still import.

### Estimates

| Imported field | Implemented estimate | Limitation |
| --- | --- | --- |
| Mode | Sparkling, flagged estimated. | Logs do not establish selected mode. |
| Coins | Median of up to nine nearest positive-coin reference runs, weighted by captures, shares, party species, Sparkling count and feathers; zero without references. | `~` identifies the estimate; not verified historical income. Explicit Profit repricing can replace it. |
| Unique flag | Earliest chronological species among imported and existing Sparkling entries, across catchers, flagged estimated. | Not per-account discovery verification; existing saved flags stay unchanged. |
| Tickets between finds | Recognized confirmations since the previous known find. | Missing logs undercount. |

Matched existing records retain their values/flags instead of receiving import estimates. Imported flags can affect [`!since unique`](SINCE_COMMAND.md).

### Achievements

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

Runs/catches use the larger of local totals and available own API statistics; own dex unions local own species with API discoveries. API lookup refreshes in SkyBlock when the configured provider is enabled, normally every ten minutes and after saved live runs. Hours use saved runs or the observed `/playtime` menu's Safari minutes plus later runs, whichever is greater. Open that server menu to supply pre-mod playtime; the mod does not request it automatically.

Disabling achievements blocks unlocks/announcements and periodic/contest processing, but history and several event-driven counters continue. Reenabling can catch up history/API-derived awards; it does not reconstruct every missed event. Import reconstructs own-Sparkling and Macaw droughts, not a complete historical unique-event drought. Unknown imported duration can still satisfy speed-award predicates; see [audit D7](IMPLEMENTATION_AUDIT.md#d7).

## Lifecycle and persistence

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

2026-10-19 (America/New_York) — Audited live history, import, UI, achievements and persistence against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md) and [consolidated discrepancies](IMPLEMENTATION_AUDIT.md).

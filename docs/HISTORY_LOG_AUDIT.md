# Run-history log audit

## Scope

Read-only inspection on 2026-10-27 of all 981 plain and gzip log files available in the two requested Prism instances. Live configuration/history files were inspected but not modified. Counts include archived/overlapping files and are not unique-run totals.

| Instance | Log files | Recognized reward summaries | Saved runs inspected | Exact duplicate extras |
| --- | ---: | ---: | ---: | ---: |
| 26.1.2 JohnFounder | 248 | 1,977 | 2,395 | 0 |
| 26.1.2 Skyblock | 733 | 2,925 | 2,954 | 0 |

The JohnFounder folder contains 191 sessions identifying JohnFounder, 18 identifying JohnCEO, and 39 without an identified owner. Skyblock contains 495 identifying JohnCEO, 2 identifying JohnFounder, 85 identifying MindfulBenZ, 9 identifying JohnMage, and 142 without an identified owner. This confirms that the folder name alone cannot establish account ownership.

The prior importer passed the active IGN to the tally but did not check `Setting user:`. That affected interpretation of personal rewards, not file selection. The updated importer verifies the logged-in owner and skips foreign/unverifiable files. It also aborts merging if the active Minecraft UUID changes during the import. This prevents future cross-account imports; it cannot safely identify and remove already-mixed historical rows because old saved records contain no reliable owner identity.

## Recoverable data

| Evidence in logs | Run-history use | Limits |
| --- | --- | --- |
| Manager ticket confirmation and HOTSPOT server lines | Start/ticket anchor, ticket count, hotspot biome | A missing ticket line cannot prove which ticket tier was consumed. |
| Personal captures and loot-share messages | Capture counts, per-species shards, personal and shared dex coverage, participating catchers | Suppressed/missing messages cannot be recovered from a summary alone. |
| Sparkling announcements, feather reward/stash lines, Macaw bonus | Sparkling catches, observed catcher, feather rewards and first-seen estimates | First occurrence in available logs remains an estimate of discovery uniqueness. |
| `SAFARI REWARD SUMMARY`, `+88 Shards`, `+350 Safari Essence`, `+239,781 Hunting Exp` | Actual shard total, essence and Hunting XP | Shard total does not establish its species distribution or number of captures. |
| `[SM] ... mode on.` | Verified mode at that run | Without an observed mode selection, the existing Sparkle estimate remains marked estimated. |
| `Cavern completed in 3m 49.36s! (3/4)` and biome done messages | Reported biome split, using embedded elapsed time where present | A report is timing evidence, not independent proof of every population capture. |
| Summary/departure timestamps | Observed duration/end boundary | Partial sessions can retain unknown duration. |

Modern `(Minecraft) [System] [CHAT]` logger prefixes are now handled as well as older prefixes. Summary rows after the summary header are included even when they arrive in later seconds. Live recording also accepts delayed summary rows for the recently finished run within a bounded fifteen-second window.

## Profit and imports

No inspected saved run already had recorded Safari Essence. New records store the actual summary amount; ticket multipliers are not applied again. Essence is valued using the existing Bazaar cache and added once to shard/feather value. Re-importing matching logs enriches missing reward, mode and split fields without inserting another row. Existing imported capture/item evidence can improve; native capture details remain preserved.

When the logs contain complete per-species item rewards and cached prices exist, imports use reward-based values. Incomplete rewards retain the historical median estimate, marked with `~`, plus actual known essence value when a quote is available. Reference median values exclude any already-valued essence to prevent adding it twice. Repricing retains these distinctions.

Duplicate-event handling preserves same-second captures within a file while deduplicating overlapping copies. Run merging remains one-to-one by nearby start/ticket anchors, and exact saved duplicates are repaired by the existing backed-up cleanup. Changed timestamps or missing boundaries can remain ambiguous.

## UI and remaining uncertainty

Runs has exact number jump navigation (`123` or `#123`), retaining the surrounding rows. Sparklings has a case-insensitive species substring filter, combined with Mine only. Every scrolling history tab has a draggable scrollbar. Average run duration excludes unknown/zero lengths and runs lasting ten minutes or longer. Other totals retain those runs. The 37/37 statistic describes runs where all species were caught, rather than Full Clear population completion.

Live Minecraft testing is still needed for search focus/layout and server summary ordering. Missing per-species reward messages, complete party rosters, original ticket tier, historical prices and account ownership of previously mixed saved records cannot be reconstructed reliably from the available summaries alone.

## Last updated

2026-10-27 — Runs longer than thirty minutes are removed from saved history on load (with a pre-duration-cleanup backup), excluded from new live records and rejected on every import. Exactly thirty minutes and unknown durations remain eligible. Remaining rows are renumbered chronologically. Average duration includes only known positive durations strictly below ten minutes, labeled Average run (includes any run below 10m).

2026-10-27 — Initial audit and implementation notes from both requested log folders; source changes and automated validation accompany this audit.

## Alpha import audit — 2026-10-27

A read-only scan of the JohnCEO / 26.1.2 Skyblock instance found 184 saved Sparkling entries. The selected Pomegranate profile returned an API total of 183. Five saved catches are positively identified as Alpha by the client's connection metadata:

| Local time (America/New_York) | Catcher | Critter |
| --- | --- | --- |
| October 5, 18:32:24 | _cj | Solsnatcher |
| October 5, 18:35:50 | JohnCEO | Strongarm |
| October 5, 18:40:56 | _cj | Driftling |
| October 5, 18:45:30 | _cj | Litterbug |
| October 17, 02:49:41 | JohnCEO | Tepid |

The October 5 session connected to Alpha at 18:26:53 and back to main at 18:50:55 (`2026-10-05-5.log.gz`). October 17 connected to Alpha at 02:32:46 and back to main at 02:51:07 (`2026-10-17-2.log.gz`). The later Billygoat on October 5 is main-server history and remains valid.

The production parser/repair code was run against copies loaded into memory: 161 main-server catches found in currently available eligible logs, five Alpha catches, eight Alpha runs. Repair removes those five saved Alpha entries and eight saved Alpha runs, adds no new catch, and preserves 18 saved catches absent from the currently matched log events. The projected saved count is 179, four below the API total. The API does not provide catch identities/timestamps to reconstruct those four; a difference of one before repair did not mean exactly one invalid entry. Live config files were not modified during this audit. Re-import with the updated mod applies repair after creating backups.

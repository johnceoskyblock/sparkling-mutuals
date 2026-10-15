# Importing Safari history from logs

Use `/sm history import` to backfill **both run history and Sparkling history** from the current Minecraft instance's `logs` folder. In `/sm` settings, the History tab begins with **History**, followed by **Import History from Logs**, which runs the same import. The button closes settings so progress is visible above the hotbar; completion reports added runs, Sparkling finds and failed files.

For another instance or a folder of old logs, use a quoted path:

```text
/sm history import "C:\Minecraft Backups\logs"
```

Imports read matching files directly inside that folder, without scanning subfolders. Parsing runs in the background; only one manual import can run at a time. Normal sessions continue recording history live. The old launch-import toggle and automatic startup scans have been removed. Manual import is intended mainly for play before installing the mod, or recovery from available logs.

## Inputs and run boundaries

- Read `.log`, `.log.gz` and chat `.json` files, including `latest.log`, using modern or older Minecraft log prefixes. Formatting codes, optional Chat Heads and midnight rollover are supported. Compressed files are read directly.
- Chat JSON may be an array or an object with an `entries` array. Entries can contain message/text/formattedMessage/content/line strings with numeric epoch seconds, milliseconds or ISO timestamps; timestamped log strings and dated player/critter Sparkling entries are also accepted. This is chat ingestion, not a general run-history JSON restore command.
- Dated Minecraft archive filenames establish the starting day. Undated text logs infer dates from their modification time and time-of-day rollovers. Keep original names and modification times when possible: logs do not record enough information to repair an arbitrary file date or timezone change.
- Prefer the Hunting Hotspot message as a run start and associate a nearby Manager ticket confirmation. Without a hotspot, a ticket confirmation supplies the start. Prefer the reward-summary header as the end; a server departure is the fallback. Missing end times display as `-` rather than a guessed duration.
- An unfinished run in the current instance's active `latest.log` is left to the live recorder. An archived run can still be imported with partial observed statistics and no known duration. Missing chat cannot be reconstructed; ticket-only runs may contain zero captures.
- Own captures, loot shares, shard rewards, feathers, Macaw bonuses, Sparkling finds and observed biome-completion splits are reconstructed from recognized messages. The Party unique count uses the mod's normal union of own and shared species. Player count includes only participants established by those messages.
- Damaged or unsupported files are reported and skipped. A failed gzip contributes no partial events; other valid files can still import. Diagnostic details are written to `latest.log`.

## Estimates

Match the supplied HTML backfill tools' estimates where the logs cannot establish a value:

| Field | Imported behavior |
| --- | --- |
| Mode | Assume Sparkling mode and mark the mode as estimated. |
| Coins/profit | Use the median of up to nine most similar existing positive-coin runs, scored by captures, loot shares, Party species, Sparkling finds and feathers. Without reference runs, use zero. Mark the result as estimated and show `~` in the Coins column. |
| First-time Sparkling discovery | Mark the earliest known chronological find of each species as first seen, across imported and existing history. New imported flags are estimates; a tooltip identifies them. |
| Tickets between Sparkling finds | Count recognized ticket confirmations since the previous known find. Missing logs can undercount this value. |

Existing saved records retain their original mode, coin values, discovery flags and ticket counts. Imports do not overwrite them with estimates. Estimated profit is not subsequently repriced as though it were a live-recorded reward total. These estimates describe the available history, not independently verified historical discoveries or income.

## Merging and recovery

Repeated imports and overlapping log copies are deduplicated. Run matching uses a 90-second start-time window, also considering the associated ticket time. Sparkling matches require the same player and species within ten seconds of an existing entry; repeated messages within 1.5 seconds collapse. Separate finds a few minutes apart remain separate. Closest existing matches are assigned first, and each saved entry matches at most once.

Existing records are preserved; newly reconstructed entries are added chronologically and run numbers are rebuilt. The live tickets-since-Sparkling counter is preserved. Import can unlock achievements silently without sending party announcements.

Before merging, the importer copies any existing history files to:

- `config/sparkling-mutuals-runs.json.pre-import.bak`
- `config/sparkling-mutuals-sparklings.json.pre-import.bak`

Each import replaces these backups with the immediately preceding saved versions. Saves use a temporary file and atomic replacement where supported. To restore a backup, close Minecraft first and copy it over its corresponding `.json` file. Log import is best effort: missing messages, incorrect file dates and unusually close run starts can make historical reconstruction ambiguous.

## Minecraft checks

1. Open History settings. Verify the button order, absence of the startup toggle, and that each button performs its named action. Start import through both the command and the import button.
2. Import a folder containing known old plain/compressed logs, including a session across midnight. Check runs, personal/party Sparkling finds, own catches, loot shares and dates against those logs. Try a quoted folder with spaces.
3. Repeat the same import: expect zero additional entries. Include a duplicate log copy and verify it does not inflate captures. Two legitimate finds of the same species/player a few minutes apart must remain separate.
4. Keep an existing native run and Sparkling entry in the folder's time range. Check that their values and flags survive and that the live ticket counter does not jump. Import with saved positive-profit runs to inspect median estimates, `~` coins and estimated-mode/first-seen explanations.
5. Add a damaged gzip beside a valid log. Expect a failure count for that file and successful import of the valid one. Check the backup files, then restart Minecraft and confirm both histories persisted.
6. Import during an unfinished current run: it must not add that partial live run. Finish normally and verify one saved run. Restart without importing: old logs must not be scanned automatically, and normal completed runs must still save.

Automated parser, merge, settings and persistence tests accompany the implementation. The checks above require Minecraft and are separate from the automated build.

## Last updated

2026-10-15 - Implemented the two HTML backfillers as one manual native importer, with estimates, preserved existing entries, revised settings actions and recovery checks.

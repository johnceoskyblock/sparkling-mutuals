# Credited party Sparkling streak commands

Type either command in party chat (`/pc` or `/p chat`):

| Command | Reply |
| --- | --- |
| `!since sparkling` | Runs since anyone's most recent Sparkling credited while you were in that Safari run, including critter and catcher |
| `!since unique` | Runs since the most recent credited party Sparkling flagged unique in history, including critter and catcher |

Examples:

- `Farmer: 1 run since last sparkling: Foxtrot caught by OtherPlayer.`
- `Farmer: 3 runs since last unique sparkling: Wumpa caught by Friend.`

## Credit and counting

Use **any party member's qualifying find**, not only the requester as catcher. The requester receives Sparkling credit while participating in that run, so a teammate's find resets the appropriate streak. Do not filter historical catches to the current party roster: earlier parties' credited finds remain part of the requester's history.

Qualify an entry when its timestamp lies inside a saved run's start/duration or the current run's start/end bounds. A catch in an unfinished current run can immediately reset the streak; finds outside the requester's runs do not count. When an imported run has no known duration, require that run's Sparkling list to match `catcher:critter` rather than inferring credit from a date alone.

For `unique`, use the saved Sparkling entry's **unique flag**, including imported estimated flags, across all eligible catchers. Do not treat every Sparkling as unique or perform a new historical discovery lookup. This command reports the history's flagged unique events; it does not recalculate which account first unlocked a discovery.

Find the latest qualifying entry and count saved run records whose **start is later than its catch**. Exclude the run containing the catch. The unfinished current run is not an additional saved run. A party Sparkling repeat resets `sparkling`, while `unique` continues from the most recent eligible unique entry.

The message omits the word **recorded** and includes the exact critter and catcher from that last entry. The underlying count still depends on available saved history. Missing logs, disabled recording and incorrect import dates can leave incomplete totals. With no qualifying entry, say `No sparkling in history (N runs tracked).` or the unique equivalent instead of inventing a lifetime streak.

## Reply ownership

The requesting player's own mod client answers from its history, because other clients may have joined different runs. Other mod users must not answer with their own unrelated histories. Requester matching is case-insensitive. No API lookup/key is required. Both commands work in every mode and outside Safari, and appear in `!commands` and settings help. The party-command toggle, cooldown and response deduplication remain active.

Snapshot the reply on the client thread before the async reply pipeline. Match response bodies by requester and command kind so another player's result, or the normal streak, cannot cancel a unique-streak response.

## Minecraft checks

1. Have a teammate catch a Sparkling while you are in the run. `!since sparkling` should reset to zero and name their critter/IGN. A later saved run without a Sparkling increments it.
2. Use an eligible unique entry caught by a teammate: `!since unique` must use that event and name its critter/catcher. A later repeat resets only the normal streak. Compare both against history's unique flags.
3. Observe a catch outside your run, or query after changing party members: outside-run entries must not reset you, and your earlier parties' valid credit must remain.
4. Query during an active run and outside Safari, in every mode, with no API key. With multiple mod users, only the requester replies. Disable party commands and verify silence.
5. Check messages contain no `recorded` wording and do contain critter/catcher for a successful result. With no qualifying history, expect the explicit history-missing message. Restart/import and verify the merged saved records produce the expected count.

## Last updated

2026-10-16 - Corrected streaks to include any credited party catch during the requester's runs; omitted recorded in replies and added critter/catcher details.

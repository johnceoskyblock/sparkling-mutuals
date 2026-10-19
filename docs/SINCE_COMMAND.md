# Credited party Sparkling streak commands

## Purpose

Report runs since a Sparkling credited to the requesting player's saved or current run. This is a history-based streak, not an account lifetime statistic.

## Controls and defaults

Type either command in party chat with `/pc` or `/p chat`:

| Command | Reply |
| --- | --- |
| `!since sparkling` | Latest credited Sparkling, its species/catcher, and later saved runs. |
| `!since unique` | The same calculation restricted to entries whose saved unique flag is true. |

**Respond to party commands** defaults on. These commands require the requester's own mod client, a known current party and available local history; no API key is required. They work outside Safari and in every mode. `!commands` and settings help list them.

## Implemented behavior

### Credit and counting

Any catcher's eligible Sparkling can reset the requester's streak. Historical credit is not filtered to the current party roster. An entry qualifies inside the active run's start/end bounds, or inside the most recent saved run beginning no later than that entry. For a saved run with positive duration, the catch must fall within its duration. For an imported run with unknown duration, its Sparkling list must explicitly contain the same `catcher:critter`; a date alone is insufficient.

The latest qualifying entry supplies the result. Count saved runs whose start is later than that catch; exclude the catch's run and do not add the unfinished active run. `unique` uses the entry's stored flag, including [estimated import flags](HISTORY_IMPORT.md#estimates); it does not perform a new discovery lookup.

Examples: `Farmer: 1 run since last sparkling: Foxtrot caught by OtherPlayer.` and `Farmer: 3 runs since last unique sparkling: Wumpa caught by Friend.` With no eligible entry, report `No sparkling in history (N runs tracked).` or its unique equivalent.

### Reply ownership

Only the requester answers from their own history, with case-insensitive name matching. The normal pipeline applies a ten-second request-token cooldown, party/context checks and result deduplication. It snapshots the result on the client thread before the asynchronous reply task. Matching includes requester and command kind, so another player's result or a normal streak cannot cancel a unique-streak result. Pending replies are also cancelled by mode or party changes.

## Lifecycle and persistence

Commands do not modify history. Results use [persisted histories](HISTORY_IMPORT.md), including earlier parties and imported records. Disconnect clears pending replies, recent requests and response history. Imports can change subsequent results; missing logs or dates can leave incomplete totals.

## Edge cases

- A teammate's repeat resets `sparkling` while `unique` continues from its latest flagged event.
- A catch outside an eligible run cannot reset either streak.
- An active-run catch can reset the count immediately; the active run itself adds no saved run.
- The `unique` streak describes stored flags, which differ from a freshly verified account discovery history.

## Source and validation references

- Calculation and matching: [SafariSinceReply](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/SafariSinceReply.kt), `format`, `current` and `matches`.
- Parsing/ownership: [PartyCommand](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/PartyCommand.kt); registration, snapshot and sending: [PartyCommands](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/PartyCommands.kt); result history: [PartyReplies](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/PartyReplies.kt).
- Existing secondary coverage: [SafariSinceReplyTest](../src/test/kotlin/net/johnceo/sparklingmutuals/commands/SafariSinceReplyTest.kt), [PartyCommandTest](../src/test/kotlin/net/johnceo/sparklingmutuals/commands/PartyCommandTest.kt). No tests or live Minecraft validation were run in this audit. Live checks should compare both replies with own/party catches, active and imported unknown-duration runs, changed parties, disabled commands and multiple mod users.

## Last updated

2026-10-19 (America/New_York) — Audited historical credit, saved-flag semantics and reply ownership against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md).

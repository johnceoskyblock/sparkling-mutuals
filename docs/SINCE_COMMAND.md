# Safari streak commands

## Purpose

Report saved runs since a Sparkling credited to the responding player or since their latest personal Emerald Dye drop.

## Controls and defaults

Type these commands in party chat with `/pc` or `/p chat`.

| Command | Reply |
| --- | --- |
| `!since` | Latest credited Sparkling, its species/catcher, and later saved runs. |
| `!since unique` | The same calculation restricted to entries whose saved unique flag is true. |
| `!since dye` | Saved runs after the responding player's latest dated Emerald Dye drop. `!since dyes` and `!since emerald dye` also work. |

**Respond to party commands** defaults on. Any party member can request these commands, including players without the mod. Every mod user with party commands enabled replies from their own history, using a known current party and available local history; no API key is required. They work outside Safari and in every mode. `!commands` and settings help list them.

## Implemented behavior

### Credit and counting

Any catcher's eligible Sparkling can reset the responding user's streak. Historical credit is not filtered to the current party roster. An entry qualifies inside the active run's start/end bounds, or inside the most recent saved run beginning no later than that entry. For a saved run with positive duration, the catch must fall within its duration. For an imported run with unknown duration, its Sparkling list must explicitly contain the same `catcher:critter`; a date alone is insufficient.

The latest qualifying entry supplies the result. Count saved runs whose start is later than that catch; exclude the catch's run and do not add the unfinished active run. `unique` uses the entry's stored flag, including [estimated import flags](HISTORY_IMPORT.md#estimates); it does not perform a new discovery lookup. Live unique flags refer to the responding account’s discovery baseline, regardless of catcher. Startup repairs legacy unique flags on species already seen earlier; imports recompute estimated first-seen flags across the combined history.

Examples: `1 run since last shiny: Foxtrot caught by OtherPlayer.` and `3 runs since last unique shiny: Wumpa caught by Friend.` With no eligible entry, report `N runs tracked; no shiny in history.` or its unique equivalent.

### Emerald Dye

Each player reports from their own selected History account. A personal Main-server drop notice supplies the date. Use `/sm history import` to recover dated drops from older logs for that account. Vincent's Dye Guide supplies a lifetime total. It has no drop date for the streak calculation.

`3 runs since last Emerald Dye.` counts saved runs that started after the drop. With no dated drop it reports the tracked run count and says `no dated Emerald Dye drop in history.`

### Reply ownership

Every mod user who has replies enabled answers from their own history. Replies start with the run count. The last credited Sparkling can belong to anyone in their party run. The ten-second request-token cooldown and party/context checks still apply. Responders stagger replies by roster UUID rank, and each responder can send even when their streak matches another player's. The result is snapshotted on the client thread. Mode or party changes cancel pending replies.

## Lifecycle and persistence

Commands do not modify history. Results use [persisted histories](HISTORY_IMPORT.md), including earlier parties and imported records. Disconnect clears pending replies, recent requests and response history. Imports can change subsequent results; missing logs or dates can leave incomplete totals.

## Edge cases

- A teammate's repeat resets `!since` while `unique` continues from its latest flagged event.
- A catch outside an eligible run cannot reset either streak.
- An active-run catch can reset the count immediately; the active run itself adds no saved run.
- The `unique` streak describes stored flags, which differ from a freshly verified account discovery history.

## Source and validation references

- Calculation and matching: [SafariSinceReply](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/SafariSinceReply.kt), `format`, `current` and `matches`.
- Parsing/ownership: [PartyCommand](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/PartyCommand.kt); registration, snapshot and sending: [PartyCommands](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/PartyCommands.kt); result history: [PartyReplies](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/PartyReplies.kt).
- Existing secondary coverage: [SafariSinceReplyTest](../src/test/kotlin/net/johnceo/sparklingmutuals/commands/SafariSinceReplyTest.kt), [PartyCommandTest](../src/test/kotlin/net/johnceo/sparklingmutuals/commands/PartyCommandTest.kt). Automated coverage includes all-responder parsing, prefix-free replies and teammate catches resetting the streak. Live Minecraft validation remains pending. Live checks should compare both replies with own/party catches, active and imported unknown-duration runs, changed parties, disabled commands and multiple mod users.

## Party wording

Party replies use “shiny” or “unique shiny” so detection-keyword chat alerts remain reserved for actual Sparkling sightings. The old `!since sparkling` spelling is replaced by `!since`; `!since unique` is unchanged. No API requests are added.

## Last updated

2026-10-26 — Corrected catcher-based live uniqueness and added automatic legacy repeat-flag reconciliation, so another player’s new discovery cannot reset an existing local discovery’s unique streak.

2026-10-26 — Removed responder IGN prefixes; retained every mod user’s response even when results match.

2026-10-21 — All mod users now reply with their own named party-credited streak; shortened the default command to `!since` and changed party wording to shiny. Credit/counting logic is unchanged.

2026-10-19 (America/New_York) — Audited historical credit, saved-flag semantics and reply ownership against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`. See [documentation conventions](DOCUMENTATION_GUIDE.md).

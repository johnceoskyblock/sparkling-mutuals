# Safari entry guard

The guard cancels entry actions before the normal interaction packet path. It never aims, fires capsules or starts entry automatically.

| Stage | Blockers | Setting |
| --- | --- | --- |
| Safari Manager/entry tickets/menu on Torrhus Canyon (`foraging_3`) | Each party requester's outstanding downtime/readiness | Party: **!dt downtime** |
| Waiting entrance on the Safari instance (`safari`) | Kick recovery, unknown/loading roster, or missing current party members | Safari: **Guard Safari entry** |
| Verified active run or unrelated island/NPC/inventory | No entry interruption | Existing settings unchanged |

An unknown server location holds recognized entry controls while an applicable guard is enabled. Location updates and actual mapped run evidence distinguish waiting from an active run; transfer/entry chat alone does not establish an active run. Disabling one setting disables only its applicable blocker. Saved preferences are preserved. **Shift/sneak is not a bypass.** Closing the menu and ordinary inventory actions remain possible.

## Readiness and identity

`!dt [reason]` records each requester independently. In-run requests are banked; transferring out of the run activates the next-entry hold without losing the requests. Standalone party `r` or `Ticket earned!` releases only the actual sender's request, for **every reason**. Local Miria scoreboard detection resolves only the local player's contest request. One person's ready message, quoted names, Guild/private/public chat, or an unrelated scoreboard cannot release someone else's request.

Party input is read from both Fabric GAME and CHAT events through the anchored party envelope. Before applying it, match the sender to the current Mod API UUID roster. Names already known from that roster's current connection player list are reused across transfers; an unknown sender name is resolved with the existing Mojang lookup off the client thread and then checked against the current roster. Failed identity lookups remain pending and retry after ten seconds; unknown is visible as waiting, not readiness. No API key is needed for a name lookup.

The bounded inbox keeps the latest readiness state per sender, independently from kick reports, so `!dt`, `r`, `!dt` ends with a request. Ordinary membership refreshes retain unverified messages; confirmed departure prunes only that UUID's request, and confirmed party replacement/disband clears the old party. Same-party world transfer retains downtime while discarding old-instance kick context. More than 32 distinct pending sender/control entries triggers a kind-specific safe hold. After lookup/roster recovery, each current member must personally send `r` or `Ticket earned!` to clear readiness overload. Kick overload is conservative generic kick evidence and requires a new Safari instance with full presence validation; ready messages cannot clear it. Each setting disables only its own overload state. Repeated requests from normal Safari parties coalesce and do not exhaust the inbox.

## Presence and kick recovery

Compare normalized current-party UUIDs, including the local player, with this connection's **listed player-info entries**, bound to the current client world and Mod API Safari location. This is independent of entity render distance. NPC names, arbitrary player counts and online players on another connection do not satisfy the roster. Unknown roster, missing local list entry or incomplete loading blocks immediately; there is no three-second permissive grace. A verified solo roster containing the local player permits entry immediately.

This is the client-visible server player-list evidence, not an independent server-side occupancy guarantee. If Hypixel omits a real same-instance member from that list or supplies stale entries, the client may conservatively wait; use `/sm debug` to inspect the evidence. Actual Hypixel list behavior must be verified in game.

Any case-insensitive `kick` substring in a verified party message body counts, including `kicked`, `I was kicked!` and `kickstart`. A username containing `kick` alone does not count. Reports are accepted only while waiting on the Safari instance, and remain independent of readiness.

There is **no timer-only release**. Each identified self-report remains held until that UUID is observed absent after the report and then present with the full roster ready, a confirmed refreshed roster removes that UUID, or a newly verified Safari world has the full roster present. A different member returning cannot clear its hold. Multiple victims are tracked independently. Generic/ambiguous kick requests identify no reliable victim and require full revalidation in a new Safari world; repeated stale snapshots or waiting thirty seconds cannot clear them. Several different unverified kick reports from one sender are conservatively retained as a generic report. Explicitly disabling Guard Safari entry is the deliberate override.

## Interception and validation

One decision is used by `MultiPlayerGameMode.interact`, `attack`, `useItem`, `useItemOn` and `handleContainerInput` HEAD injections. This covers Manager interactions, both hands, ticket air/block use, and server entry-menu confirmation including keyboard and quick-move. Manager recognition requires its own name or a closely aligned nameplate over an unlisted NPC player; nearby ordinary entities/real players are not enough. Entry-menu checks require the exact entry title and a server-owned name-tag entry control; player inventory slots and close controls are unaffected.

Refusal reasons are local and rate-limited to once per three seconds. `/sm debug` logs settings, location/run state, roster readiness, missing UUIDs, pending identity and kick state. Interception logs also record the route, recognition and decision; no party message spam is added.

Automated tests cover sender readiness, transfers, unknown/solo/missing presence, independent kick recovery, message coalescing/overflow and the actual mixin callback handlers under controlled game observations. Tests do not claim a real Hypixel packet capture. Verify actual mixin application on all three supported targets and perform these live checks when a session is available:

- Two downtime requesters on Torrhus: each releases only their own hold; repeat with a non-contest reason and banked in-run request.
- Safari waiting: `I was kicked!`, missing/late/out-of-render-range members, unknown/loading roster, and separate recovery of two reports.
- Manager attack/use, both hands, ticket use while aiming at air/block, already-open entry GUI, Shift, keyboard/quick-move; no entry packet/menu while blocked.
- Resolve all blockers: normal entry works; unrelated NPCs, inventories and close controls remain usable. Test a generic report's fresh-instance recovery and saved disabled settings.

## Last updated

2026-10-16 - Documented issue #56's stage-specific entry blocking, evidence-based recovery and validation limits.

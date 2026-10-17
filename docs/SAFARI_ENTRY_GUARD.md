# Safari entry guard

The guard cancels entry actions before the normal interaction packet path. It never aims, fires capsules or starts entry automatically.

| Stage | Blockers | Setting |
| --- | --- | --- |
| Safari Manager/entry tickets/menu on Torrhus Canyon (`foraging_3`) | Each party requester's outstanding downtime/readiness | Party: **!dt downtime** |
| Manager/entry tickets/menu on the Safari instance (`safari`) | A two-second kick-message pause, then current party presence in tab; unknown/loading roster or missing members block entry | Safari: **Guard Safari entry** |
| Unrelated island/NPC/inventory | No entry interruption | Existing settings unchanged |

An unknown server location holds recognized entry controls while an applicable guard is enabled. Safari-instance presence is observed continuously and checked on every entry attempt. A sidebar/map change to an active run cannot release a hold or suppress kick reports. Run detection still controls banking of downtime requests. Disabling one setting disables only its applicable blocker. Saved preferences are preserved. **Hold either Shift key to bypass any blocked Manager click, ticket use or entry-menu confirmation for that interaction only.** This does not clear requests or mark missing members present; releasing Shift restores the guard. Closing the menu and ordinary inventory actions remain possible.

## Readiness and identity

`!dt [reason]` records each requester independently. In-run requests are banked; transferring out of the run activates the next-entry hold without losing the requests. Standalone party `r` or any party message containing `ticket` (case-insensitive) releases only the actual sender's request, for **every reason**. Local Miria scoreboard detection resolves only the local player's contest request. One person's ready message, quoted names, Guild/private/public chat, or an unrelated scoreboard cannot release someone else's request.

Party input is read from both Fabric GAME and CHAT events through the anchored party envelope. Before applying it, match the sender to the current Mod API UUID roster. Names already known from that roster's current connection player list are reused across transfers; an unknown sender name is resolved with the existing Mojang lookup off the client thread and then checked against the current roster. Failed identity lookups remain pending and retry after ten seconds; unknown is visible as waiting, not readiness. No API key is needed for a name lookup.

The bounded inbox keeps the latest readiness state per sender, independently from kick reports, so `!dt`, `r`, `!dt` ends with a request. Ordinary membership refreshes retain unverified messages; confirmed departure prunes only that UUID's request, and confirmed party replacement/disband clears the old party. Same-party world transfer retains downtime while discarding old-instance kick context. More than 32 distinct pending sender/control entries triggers a kind-specific safe hold. After lookup/roster recovery, each current member must personally send `r` or `Ticket earned!` to clear readiness overload. Pending kick identity/overflow cannot hold a fully present party indefinitely: verified kick reports only apply their two-second receipt-time pause, and Safari presence takes priority afterward. Each setting disables only its own overload state. Repeated requests from normal Safari parties coalesce and do not exhaust the inbox.

## Presence and kick recovery

Compare normalized current-party UUIDs, including the local player, with this connection's **listed player-info entries**, bound to the current client world and Mod API Safari location. This is independent of entity render distance. NPC names, arbitrary player counts and online players on another connection do not satisfy the roster. Unknown roster, missing local list entry or incomplete loading blocks immediately; there is no three-second permissive grace. A verified solo roster containing the local player permits entry immediately.

This is the client-visible server player-list evidence, not an independent server-side occupancy guarantee. If Hypixel omits a real same-instance member from that list or supplies stale entries, the client may conservatively wait; use `/sm debug` to inspect the evidence. Actual Hypixel list behavior must be verified in game.

Any case-insensitive `kick` substring in a verified party message body counts, including `kicked`, `I was kicked!` and `kickstart`. A username containing `kick` alone does not count. Reports are accepted on the Safari instance regardless of run detection, and remain independent of downtime readiness.

**The current tab list is the primary Safari entry decision.** A verified current party message containing `kick` starts a **two-second pause from receipt time**, covering delayed removal from tab. Another kick report starts a fresh two-second pause. After that pause, permit entry whenever the current roster (including self) is fully present, even if a kick victim never visibly disappeared, a message lookup remains pending, or an older generic kick/overflow condition exists. If any member is absent or the roster is unknown, keep blocking until presence is ready. Missing-member blocks have no timeout. Downtime requests do not override full-party presence inside Safari; they apply separately on Torrhus Canyon. Shift overrides every blocker for the current interaction without clearing it. World changes discard the old instance's kick pause.

## Interception and validation

One decision is used by `MultiPlayerGameMode.interact`, `attack`, `useItem`, `useItemOn` and `handleContainerInput` HEAD injections. This covers Manager interactions, both hands, ticket air/block use, and server entry-menu confirmation including keyboard and quick-move. Manager recognition accepts its exact name or tightly aligned player/armor-stand/interaction/text-display proxies beneath its nameplate. Remember recognized UUIDs and nameplate anchors only for the current client world so transient nameplate/player-list changes cannot bypass the guard during repeated clicks. Known party players and the local player are excluded, unrelated/distant entities do not inherit recognition, and a world change clears this memory. Guard readiness is still reevaluated on every interaction; the chat-notice cooldown never releases a click. Entry-menu checks require the exact entry title and a server-owned name-tag entry control; player inventory slots and close controls are unaffected.

Refusal reasons are local and rate-limited to once per three seconds. Keep them short: `[SM] Safari: 1 missing: JohnFounder.` or a sorted list for several missing members. Resolve display names from the current/cached verified player UUID mappings across transfers; if no name is known yet, say `name loading` rather than showing a raw UUID. Current-party UUIDs without a cached name are resolved asynchronously through Minecraft's Mojang session service, independently of tab presence or sending a control command. Cache valid names only for the current roster, retry failed lookups after ten seconds, and never let name-lookup success or failure change presence readiness. Unknown/loading presence with no specific missing UUID says `Waiting for the party to load`. Other reasons use short loading, readiness, downtime and kicked-member notices. Every refusal appends `Hold Shift to enter anyway.` The initial downtime hold notice also explains this override. Downtime local messages name the requester and remaining holders; clearing downtime must not claim that unrelated presence/kick guards are also cleared. `/sm debug` logs settings, location/run state, roster readiness, missing UUIDs, pending identity and kick state. Interception logs also record the route, recognition and decision; no party message spam is added.

Automated tests cover sender readiness, transfers, unknown/solo/missing presence, independent kick recovery, message coalescing/overflow and the actual mixin callback handlers under controlled game observations. Tests do not claim a real Hypixel packet capture. Verify actual mixin application on all three supported targets and perform these live checks when a session is available:

- Two downtime requesters on Torrhus: each releases only their own hold; repeat with a non-contest reason and banked in-run request.
- Safari entry: `I was kicked!`, missing/late/out-of-render-range members, unknown/loading roster, and separate recovery of two reports. Repeatedly click while someone is missing, including after run detection changes: every attempt must stay blocked.
- Manager attack/use, both hands, ticket use while aiming at air/block, already-open entry GUI, Shift, keyboard/quick-move; no entry packet/menu while blocked.
- Hold Shift across Manager clicks/attacks, both-hand ticket use and entry-menu confirmation: each can bypass. Release Shift while a member is still missing: every route blocks again. Verify hold/request state was not cleared.
- Before a party member appears in the current instance, wait for their name lookup and verify the missing notice names them. Repeat after a transfer and during lookup failure; failure must not release entry.
- Resolve all blockers: normal entry works; unrelated NPCs, inventories and close controls remain usable. Test kick reports while everyone remains in tab: block for two seconds, then allow if all are still present. Remove a member during the pause and verify blocking continues beyond expiry. Test a new report restarting the pause, a later departure and saved disabled settings.

In the entry GUI, holding physical Shift bypasses all entry blockers and converts a server ticket control’s quick-move into a normal selection click. Other GUIs, player inventory slots and other click types retain their normal input. Local downtime notices list every waiting player and tell the user to wait for those players to say `r` or report a ticket; holding Shift enters anyway. A leader’s `!dt contest` waits for every current party member separately.

Readiness messages accept clear confirmations (`r`, `rdy`, `ready`, `I'm ready`, `ready to go`, `ready when you are`), ignoring case and common punctuation. Short r-spam and one adjacent keyboard-key typo (`r/`, `rrr`, `rt`, `er`) also count. `nr`/`NR`, unrelated words (`party`, `red`, `brb`), questions and negative/future statements (`not ready`, `almost ready`, `I will be ready`) do not release a hold. This parser is shared by normal and contest downtime and pending identity validation. A ready message releases only its sender's outstanding request.

## Last updated

2026-10-17 - Added affirmative readiness phrases and conservative r-typo tolerance; nr remains not ready.

2026-10-17 - Added whole-party leader contest readiness, audience-correct multi-player notices, and normal ticket selection when Shift overrides entry.

2026-10-17 - Made Safari tab presence primary after a two-second kick pause; any ticket-containing party message clears its sender's Torrhus downtime request, and Shift overrides all blockers.

2026-10-17 - Added per-interaction Shift entry override with local guidance and background missing-member name lookup.

2026-10-17 - Retained per-world Manager/proxy recognition across spam clicks and transient nameplate changes; every route still checks current party presence.

2026-10-17 - Shortened all guard/downtime local notices and named missing members using retained player-name mappings.

2026-10-17 - Removed the active-run entry bypass, required continuous full-party presence, and allowed generic kick recovery after an observed full-party return.

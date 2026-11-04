# CJ’s Revenge: WebSocket-driven automatic Safari entry

## Intent and decisions already made by the human

Use the supplied unpublished SM 4.0.2 source plus the local CJ integration at `6791936e7f39798b9b8a198c028241e90cedf361`. The human requires CJ coordination to use the WebSocket entirely. In this chat they explicitly chose:

- An uncaught discovery in the current Safari room triggers entry. Remove the five-second visible-sighting requirement; no server protocol extension is required for this criterion.
- Complete entry automatically: approach the Safari Manager, interact with him, and select the configured ticket. The previous approach-only .5 behavior is no longer the requested endpoint.

Keep everything local, in the separate CJ experiment, and build only Minecraft **26.1.2**. Never read `.devAuth`. Do not push, create a PR/release, alter the backend, install a JAR, close/launch Minecraft or send a Discord message under this change. Preserve the supplied archives and prior delivered builds.

## Approach

Reuse the existing `SafariPartySync` connection and its advertised `room-sparklings-v1` atomic snapshot. This is the selected approach because it supplies exactly the human’s found-but-not-caught criterion and already serves the new SM base. A second CJ socket duplicates authentication, reconnects and room ownership. Extending the server for fresh sightings is unnecessary after the explicit removal of that requirement. CJ party-chat query/reply fallback conflicts with the WebSocket-only instruction and will be removed from the registered CJ path.

Ordinary Hypixel location/party-warp/personal-admission events and current world/input checks remain local lifecycle evidence. They do not supply positive Sparkling permission. Normal SM chat, warp/invitation timing and non-CJ features keep their existing owners.

## Discovery authority and connection lifecycle

An eligible signal requires the current world, account, canonical Safari lobby and exact verified party roster to match the shared connection. Require a validated welcome advertising atomic room snapshots and a valid atomic `sparklings` snapshot on that connection. Welcome alone, legacy per-sender `S` replay, local entity detection and CJ party messages cannot authorize entry.

Compute active species from found minus caught. Apply found and caught together before considering any input. Retain caught negatives for the current visit so an older found-only update cannot revive a capture. After reconnect, require a newly validated snapshot; retained found history alone cannot authorize an action. No observation-age cutoff applies to an outstanding current-room discovery. Species masks cannot distinguish multiple individuals of the same species, and the existing service does not prove that a discovery is still visibly loaded; those are accepted consequences of the human’s chosen criterion.

Bind an attempt to its socket generation as well as its immutable local arrival context. Process bounded queued inbound updates before START-tick input and before each interaction boundary. If the socket is unavailable, replaced, malformed, missing the capability, or has undrained work that prevents a coherent current view, no positive permission is available. Loss after an attempt starts cancels it; reconnect cannot resume or replenish that attempt. A valid fresh snapshot may start an as-yet-unspent attempt within its entry window.

Correct the supplied wire table to match all 37 upstream species, including index 33 `Fluffling` and index 34 `Hideonfloor`. This changes the shared protocol mapping, not unrelated roster presentation. Preserve the normal upstream local publication path and never republish received room facts as local observations.

## Entry sequence and action budget

Only a verified party nonleader may act, at the pre-admission entrance in a Safari world associated with a genuine current-leader party warp. Keep CJ’s existing independently verified warp/world association and current-world personal admission. A generic world replacement, toggle, roster refresh or reconnect cannot manufacture a new action budget. Keep the existing 12-second preparation/action window from world arrival; removing the five-second sighting limit does not remove this separate entry-attempt bound. Late personal admission may still be recorded after that window.

For each genuine eligible warp:

1. Wait for verified context, an active room discovery, safe input conditions, party readiness and one uniquely correlated loaded Manager body.
2. Face that body and use the existing ordinary forward/crouch approach, with at most one block cumulative travel, early release and reserved stopping distance. Preserve floor/collision/velocity checks and Hold/Toggle crouch ownership. No teleportation, reach extension, sprinting, jumping or pathfinding.
3. Release walking input and wait for ordinary coast to settle. Revalidate the same Manager, current room signal, camera/line of sight, native eye-to-hitbox interaction reach and all entry guard holds. Issue at most one normal main-hand Manager interaction through the existing guarded game-mode path. If reach is still insufficient, cancel; do not move farther or retry.
4. Wait for the exact server-owned `Critter Safari Entry` menu opened after that interaction. Tie ownership to the actual menu object/container, attempt and context. Reuse the supplied ticket parser and `safariTicketPreference` selection policy. Require all tier controls and known availability; unknown data waits within the same deadline. No ticket available means no click.
5. Immediately before selection, recheck room/context/guard/menu identity and the actual selected slot/tier/availability. Consume a one-use permit before the normal guarded container-input call. Issue at most one ticket selection, including when its outcome is uncertain.
6. Wait for existing exact personal Manager acceptance/ticket-use or personal Hotspot evidence. Sending an input or seeing someone else enter is not admission. Report completed only on personal evidence; otherwise report cancelled, expired or outcome unconfirmed.

No delayed action queue may substitute a new context for an old proposal. Cancellation is terminal for this attempt. If the last active room discovery is caught, stop before further input. If another discovery remains active, the room criterion is still satisfied; no particular individual is reserved by this protocol.

## Menu ownership and user takeover

CJ must own its complete click sequence rather than enabling the generic ticket helper as a shortcut. Refactor reusable option extraction/selection without changing the normal helper’s unrelated behavior. While CJ owns or has cancelled its attempted Manager/menu sequence, suppress the generic automatic ticket helper for those late or reopened entry menus so it cannot bypass CJ’s revoked permission. Keep that suppression for the attempted arrival even if its enable switch is turned off. A genuinely new validated warp provides the next budget; manual input remains available through ordinary guards.

The expected owned entry screen is allowed after Manager interaction. Unexpected screens, manual mouse/key input, camera takeover, Escape/closing the owned menu, focus loss, disconnect, context/roster changes, personal admission, guard holds, timeout or target replacement cancel remaining automatic actions and release only CJ-owned keys. A normal user click is never mistaken for CJ’s own dispatched click. The design retains the current requirement that Minecraft is focused; it does not add background-window control.

Keep the owned entry menu visible for this experimental build, with concise local status identifying waiting, approaching, selecting, admitted or cancelled. This supports testing and Escape; do not conceal an uncertain outcome. No claim that a rejected/uncertain packet was rolled back.

## Settings and compatibility

Retain saved CJ keys where compatible. Relabel the old status-exchange setting to explain WebSocket room observation. Add a distinct off-by-default **Automatic Safari entry** control; an existing enabled approach-test switch must not silently acquire ticket-spending behavior. The full-entry control enables the approach as part of its sequence and uses the existing preferred-ticket choice; it does not require enabling the separate generic ticket helper. Preserve an approach-only test option for users who do not enable full entry, using the same new WebSocket criterion. Keep explicit saved values and the existing normal ticket helper defaults. Document the exact switch combination in delivery instructions.

Target package: `4.0.2-cjs-revenge.6`, display name CJ’s Revenge, unchanged mod ID, developer metadata false. Normal/CJ installed profiles remain untouched. Existing .5 artifacts remain recoverable.

## Verification and delivery

Use failing regression tests first for room readiness, atomic found/caught application, no five-second expiry, all wire indices, stale callback rejection, reconnect/generation loss, before-input capture withdrawal and context/party mismatches. Exercise the composed movement-to-interaction-to-menu policy with one-use Manager/ticket dispatch, full current-frame revalidation, reach/line-of-sight failure, late/reopened/replaced menus, tier changes, unknown availability, manual takeover, switch-off, generic-helper suppression and genuine-warp reset. Test transport behavior using local fixtures; do not probe the production backend.

Run the complete 26.1.2 suite and build, then independent whole-change review. Verify packaged metadata, dependencies, expected classes/resources and SHA-256. Supply the local JAR/test kit, exact enable/test steps and concise version notes. State clearly that offline tests do not establish actual Hypixel admission, accepted NPC/menu inputs, live backend availability or exact movement under latency. No 26.2/26.3 artifacts.

## Research and status

Official source/protocol references are saved under `outputs/cjs-websocket-entry-20261104` outside this checkout. The inspected published master is `0d33d8e475e21140202a93280897575411a2cb16`; the unpublished supplied source remains the integration base. This document supersedes the .5 design’s explicit rejection of backend-positive evidence and its manual-click endpoint only for the new increment.

Status: builder check completed; human approved the written design and plan in this chat. Native implementation, 1,457 Kotlin tests, 21 helper tests and 26.1.2 build/JAR verification completed. Final review found two Important issues corrected with regressions; one scanning-overhead Minor is recorded for follow-up. Live entry remains unverified.

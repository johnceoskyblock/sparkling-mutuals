# CJ's Revenge entry dry run

## Scope

Local experiment `4.0.2-cjs-revenge.5`, Minecraft **26.1.2 only**. It observes doom-check arrivals, current Sparkling detections and party status exchanges. **Status exchange alone does not move or click.** The separate opt-in approach test can face the Manager and creep up to one block; Manager/ticket clicks remain manual. The [admission decision core](SAFARI_ENTRY_CORE.md) remains disconnected from gameplay actions.

## New SM integration (.5)

The supplied newer SM source is the base. Its backend room synchronization, returning invitations, capture filtering, shared alerts and other improvements remain intact. The backend retains discovery bits across a visit without observation timestamps; these bits never create or refresh CJ movement evidence. Current-world/server/roster-scoped capture bits can withdraw prior CJ evidence, once per species and context, including an already in-flight leader reply. Repeated snapshots do not keep retiring a genuinely new later identity.

The source’s 28-second fresh warp delay and 24-second returning-invite phase are preserved. Explicit saved timing wins. No live run has verified the remaining entry-window margin with this integrated build. The approach also respects current upstream entry/contest holds without granting Shift bypass.

Configuration files now use `config/sparkling-mutuals/`, through the new SM migration. Explicit CJ and ticket-selection choices survive. Status exchange defaults on, approach and automatic ticket selection default off. Developer-only recording/HUD features remain off in package metadata. This build has not been installed or sent automatically.

## Setup and collection

1. Use a separate Fabric 26.1.2 profile named CJ's Revenge with its own game directory. Replace Sparkling Mutuals only within that test profile, retaining required dependencies. They share a mod ID, so install one JAR, not both. Normal instances stay unchanged.
2. On the leader and joining clients, enable **`/sm` > Safari helpers > Safari entry > Safari entry status exchange**. It defaults on in this recording build and persists per instance. Existing explicit saved values are preserved; verify the switch when reusing a test profile.
3. Keep the leader's existing Auto warping/Auto-party/Leech configuration and saved delay as appropriate for the existing routine. This feature changes none of them; it does not require changing the delay to 25 seconds.
4. For passive menu recording, keep **Auto-select Safari ticket** off (the default in this recording build). That older helper can still act on a manually opened menu when enabled; the dry-run setting does not disable or reconfigure it.
5. Observe a delayed warp. Local `[CJ dry run ...]` messages describe waiting, accepted fresh evidence, expiry or cancellation. A ready diagnostic says **would check entry NPC/menu; actions disabled**. It does not certify reach or admission. `/sm debug` writes the current state and at most 64 recent timing events to `latest.log` under `CJ_ENTRY_DRY_RUN`.

No automatic instance installation, client launch, recording or upload is performed by the implementation. Collect the actual spawn-to-menu-to-admission sequence manually; it remains necessary for live integration.

## Flow and evidence

The leader's actual positive name/skin detector scans refresh current unresolved sightings. Unknown particles and old party announcements cannot create a LIVE response. An empty complete scan or send-time unloaded-identity check withdraws visibility without declaring a capture; a later real re-observation can qualify. Global capture notices withdraw requester evidence even while that requester is at the entrance. Captured known identities cannot be resurrected by the lingering model.

A joining nonleader waits for a fresh `safari` location packet associated with the currently observed world, a known party/leader and the CJ pre-admission classification (mapped boat/centre without current-world personal Manager acceptance/Hotspot evidence). It sends at most one `SMSE1` query per candidate arrival. The preparation limit is 12 seconds from observed world arrival, including time spent waiting for location/party data. A reply must arrive in under 3 seconds and have an effective observation age under 5 seconds including round-trip time. No retry or toggle can replenish a spent query. Nothing here changes the existing warp timer or proves the reported 30-second server lockout.

The leader answers only in an active Safari context, with the same claimed server/realm and a current member's verified profile name/UUID. Equal party refreshes retain continuity; membership/leadership changes and verification loss discard old queued work, even if an equal roster returns between ticks. After invitees join, the leader can establish fresh observations under the new roster and answer them.

Protocol messages use the non-overlay server GAME Party envelope, with a unique current connection-profile mapping restricted to the current roster. Normal player CHAT is not a trusted substitute. GAME supplies no signed sender UUID: this is server-envelope/name attribution, not cryptographic authentication. The experiment also trusts the leader's claim; a modified leader client can lie. Missing/ambiguous names, wrong nonce/leader/requester/server/realm and stale replies are rejected. Some other mods can block/modify the message pipeline; live delivery is not guaranteed.

Responses retain the existing bounded 1500ms pacing and strict 3-second queue lifetime. Of three simultaneous requests, the third can time out before its reply slot. This increment deliberately does not burst/retry or change those approved core limits. A NONE/timeout means **no usable fresh observation**, not proof that the entire instance contains no Sparkling.

## Limits and validation

A location packet is a **candidate destination claim**, not a unique server run ID, proof of completed party warp, ticket admission or permission to act. A packet observed before a later world replacement is discarded, and a missing fresh packet conservatively leaves the dry run waiting. The .3 Alpha recording exposed a pre-admission classification bug: the sidebar already says Critter Safari before the Manager accepts a ticket. CJ now tracks personal admission separately. Fresh-query delivery, approach execution, packet ordering and timing still need the corrected build tested live. Only loaded, positively identified creatures can be reported; this does not scan the whole Safari.

The dry-run output type contains only bounded party messages and local diagnostic notices. It has no Manager/Ticket action or permit API. Before each immediate party send, the native adapter rechecks the output's immutable origin context, current setting, destination and role. Replies retain their verified recipient UUID/name and require the same unique current profile mapping at send time. Missing, renamed, replaced or ambiguous profiles drop the proposal without lookup or retry.

A separate transport cooldown measures 1500ms from the actual completed/uncertain send attempt, including client stalls, and survives roster-context changes. Proposals blocked by this cooldown are dropped, never buffered or retried. An uncertain send also consumes the cooldown. Sending a command is logged as an attempt, never server acceptance.

Automated tests exercise context continuity, real requester/responder sessions, captures/unloading, pacing/deadlines, current-frame dispatch and config persistence. A 26.1.2 build/compile does not establish live Hypixel behavior. No 26.2/26.3 artifacts or tests are generated for this increment.

## Last updated

2026-11-04 — Recording build .3 defaults dry run on and ticket selection off; explicit saved choices remain intact. Build .2 introduced the diagnostic wiring.

## One-block approach test (.4)

Enable **Approach Manager (one-block test)** under the same Safari entry category on the joining test client. It defaults off; status exchange remains required. A genuine server party-warp notice must bind the next loaded world and match the current verified leader profile. An unrelated world replacement or respawn does not replenish the movement attempt. Fresh local/leader evidence and the existing party-entry readiness are rechecked at every START_CLIENT_TICK.

The client faces one uniquely nameplate-correlated loaded Manager body, then creeps using ordinary forward and sneak input. No jumping, sprinting, teleporting, velocity injection or reach extension is used. The one-block cumulative travel budget releases input early (0.85 blocks) for deceleration; server corrections/latency mean the exact final displacement remains a live-test limitation. Focus loss, open screens, physical controls, camera takeover, blocked/unsupported/slippery terrain, unusual movement, expired/captured evidence or changed context cancels and releases owned keys. There is no retry in the same warp attempt. Manager/ticket interaction remains manual.

For the next recording, keep automatic ticket selection off. On one leader and one joining client, verify the status exchange; on the joiner also enable the approach test before the warp. Keep the game focused and do not hold movement/mouse buttons. Record from before warp through the approach, then open the Manager menu and enter manually. Run `/sm debug` on both clients before disconnecting and provide relevant `CJ_ENTRY_DRY_RUN` and `CJ_ENTRY_APPROACH` lines. This is an Alpha-tested input sequence from .3, not certification of .4 movement or main-server behavior.

Review corrections: sneak is primed before forward input and managed idempotently for Hold/Toggle bindings, preserving user takeover. Both physical and foreign logical controls stop input. The movement policy reserves a next-step-plus-coast allowance from current movement/sneak attributes and uniform ordinary floor friction, in addition to the 0.85-block early-release threshold. Unsupported attributes/terrain cancel. Backward/cross-corridor drift cancels and path checking uses the frozen heading. START-tick evidence acquisition first withdraws unloaded/capture-resolved local identities without refreshing their age.

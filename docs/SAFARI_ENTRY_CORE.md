# CJ’s Revenge Safari entry core

The local **4.0.2-cjs-revenge.8** experiment now registers the complete WebSocket-driven entry flow on **26.1.2 only**. See [setup, actions and limits](CJS_ENTRY_DRY_RUN.md), the [approved design](superpowers/specs/2026-11-04-cjs-websocket-entry-design.md), and [implementation plan](superpowers/plans/2026-11-04-cjs-websocket-entry.md). The separate normal instance is not modified or installed by this delivery.

## Current ownership

- `PartyRoomDiscoveries`: current-socket atomic positives and visit-scoped caught negatives. `PartySyncTransport` drains bounded inbound work and synchronizes permission consumption with current generation/room readiness. `SafariPartySync` validates world/account/server/exact roster. A new welcome requires a new snapshot; old found history is insufficient. No five-second age gate or CJ party-chat fallback.
- `CjsEntryFlow`: immutable context and verified warp, 12-second attempt window (the bound warp authority uses that same arrival deadline), approach/settle/Manager/menu/ticket/admission phases, one-use Manager/ticket consumption, and retained generic-helper suppression after cancellation. Unknown context authorizes no action.
- `CjsApproach`/`CjsApproachInput`/`CjsWalkingKeys`: bounded ordinary movement, frozen Manager-facing heading, clear supported corridor, early release/reserved coast, physical/logical takeover and Hold/Toggle crouch ownership. Only a genuinely new leader warp replenishes input.
- `CjsEntryInput`: native geometry/reach, visible exact menu, current ticket options and normal guarded game-mode calls. Proposals retain emitting context/warp/socket generation and are revalidated at dispatch. No raw interaction packets, extra reach or repeated spend.
- `CjsSafariEntry`: current Hypixel location/party/warp/personal admission lifecycle and START-tick composition. No registered report-session transport or local detector forwarding remains. Screen/manual callbacks cancel owned automation; generic ticket-helper tick/hide paths honor the retained owner.

Full entry has one saved opt-in (`safariEntryAutoEnter=false` by default), shown last under **Party → Party warping** as **sparkling auto entry** with the red warning **USE AT YOUR OWN RISK!!!!**. Separate CJ discovery/approach-only gates are removed; obsolete saved keys cannot activate or block entry. Preferred ticket and other explicit existing choices are preserved. Passive genuine-warp observation continues while off so the previous arrival’s cancelled-menu ownership can retire correctly; being off never grants input.

## Historical pure report contracts

`SparklingEntryProtocol`, `EntryReportExchange`, `EntryDryRunSession`, `EntryDryRunTransport` and `SafariEntryController` remain as tested historical pure components. Their SMSE1 query/nonce, three-second reply and five-second sighting rules are **not** the registered .6 entry path. The previous `SafariEntryController` does not execute game inputs; the new `CjsEntryFlow` owns the current WebSocket-based sequence. Historical design/plan documents describe those superseded increments, not extra active gates.

## Evidence and limits

The reported spawn `(-50,68,24)` is out of Manager interaction reach; the human approved movement toward the Manager `(-52,68,21)`. The adapter checks the live correlated body and actual native eye-to-hitbox reach after bounded movement rather than treating those centre coordinates as proof of reach. Exact personal acceptance confirms admission; inputs alone remain uncertain and are never retried.

Automated policies, real loopback transport and offline geometry/tests/builds do not certify live Hypixel admission, backend availability, server input acceptance or timing. The selected room criterion deliberately accepts untimestamped outstanding discoveries. No game/session changes, installation, server changes or GitHub publication are part of implementation.

Last updated: 2026-11-04 (Asia/Dubai).

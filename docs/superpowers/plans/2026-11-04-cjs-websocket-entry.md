# CJ WebSocket automatic entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Native execution is the human’s preserved choice. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A verified unticketed nonleader automatically approaches the Manager and selects the configured ticket when the current WebSocket Safari room has an uncaught discovery.

**Architecture:** Reuse the one existing shared socket. Separate current-connection discovery authority, a pure once-per-warp entry flow, and native movement/interaction/menu adapters. Reuse ticket parsing and normal packet guards while preventing the generic helper from completing a cancelled CJ attempt.

**Tech Stack:** Kotlin, Fabric 26.1.2, Java 25, JUnit 5, existing Gradle/Stonecutter harness, Java HttpClient/WebSocket.

**Spec:** `docs/superpowers/specs/2026-11-04-cjs-websocket-entry-design.md`, explicitly approved in this chat on 2026-11-04.

## Global Constraints

- Local only; no GitHub pushes/PRs/releases, backend changes, Discord send, installation or Minecraft process changes. Never read `.devAuth`.
- Build and test Minecraft **26.1.2 only**, version **4.0.2-cjs-revenge.6**, CJ’s Revenge display name, existing mod ID, developer metadata false.
- Start from local .5 integration `6791936e7f39798b9b8a198c028241e90cedf361`; preserve its branch, delivered artifacts and supplied archives. Reuse the current linked worktree on `experiment/cjs-revenge-websocket`.
- Positive authority comes only from a validated current atomic room snapshot. No five-second sighting age, CJ party exchange, local-sighting fallback or extra socket.
- Genuine leader warp, 12-second attempt window, personal admission and the existing one-block movement/safety budget remain independent requirements.
- Full entry is a new off-by-default switch; existing approach-only settings do not acquire ticket-spending behavior.
- At most one Manager interaction and one ticket input per attempt, with complete immediate revalidation and cancellation latches. Unknown or uncertain outcomes never retry.

## Review Focus

1. Socket callbacks racing the START/input boundary: known queued capture/loss must invalidate permission; old-generation work must not dispatch in a new room. Pin with Task 1 loopback transport and Task 4 dispatch tests.
2. Existing menu helper and mouse callbacks after switch-off or cancellation: no independent automatic spend or hidden late menu. Pin with Task 2 ownership tests and Task 4 composed helper tests.
3. Toggle-crouch release and ordinary coast between approach and interaction: no held movement during Manager/menu input and no forced release of physical takeover. Pin with Task 3 key-driver and movement-to-interaction tests.
4. World/location/party data settling in different orders: only a genuine verified nonleader arrival gets an attempt; reconnect or equal-roster restoration does not replenish it. Pin with Task 2 arrival tests and Task 4 runtime-session tests.
5. Menu loading/replacement or preferred-tier changes immediately before input: do not select a stale slot or consume twice. Pin with Task 2 permit tests and Task 3 parsed-menu tests.

## Execution commands and records

Use `C:/Python314/python.exe ../../outputs/cjs-websocket-entry-20261104/run-gradle.py` as command prefix `G`. Adapt the prior receipt wrapper with explicit UTF-8 and an owned temporary test directory under the new output directory. Java home remains `work/jdk25/jdk-25.0.4.1+1`, Gradle cache `work/gradle-home`. No downloads or version-matrix expansion are needed. Redirect full command output to named receipts and inspect the exit code and JUnit XML totals.

Before Task 1, verify clean .5 tests with `G :26.1.2:test`. Expected: 1,435 tests, zero failures/errors/skips. Preserve delivered .5 artifacts. Each task records its BASE, tests and decisions in this plan’s execution ledger; use the executing-plans task scripts. Documentation synchronization is read-only, without `-Record`.

## File responsibilities

- `sync/PartyRoomDiscoveries.kt` (new): connection-scoped atomic room snapshot and visit-scoped caught negatives; no Minecraft/input dependencies.
- `sync/PartySyncTransport.kt`: bounded inbound draining and synchronized current-room permission boundary.
- `sync/SafariPartySync.kt`: validate current native visit/roster, expose read-only room snapshots and guarded input dispatch; preserve existing shared feature owners.
- `sync/PartySyncProtocol.kt`: correct all 37 wire positions using official upstream order.
- `safari/automation/entry/CjsEntryFlow.kt` (new): pure complete-sequence state machine, once-per-warp budgets, exact-menu ownership and one-use permits.
- `safari/automation/entry/CjsApproach.kt` and `CjsApproachInput.kt`: WebSocket signal instead of timestamped sightings, explicit approach outcome, safe release/settling and unique Manager evidence.
- `safari/automation/entry/CjsEntryInput.kt` (new): native normal Manager/container input, screen/manual takeover and current-menu evidence.
- `safari/automation/entry/CjsSafariEntry.kt`: event/tick composition, settings, diagnostics, session lifecycle; remove registered SMSE1/query/reply path.
- `safari/automation/SafariTicketSelect.kt`: reusable read-only options and preference selection; honor CJ-owned-menu suppression in tick, hiding and mouse callbacks.
- `config/ConfigManager.kt`, `config/SafariSettings.kt`, `safari/SafariAssist.kt`, `gradle.properties`: opt-in control, accurate labels, remove unused CJ detector hooks and bump version.
- Tests mirror each pure/native adapter under `src/test/kotlin/net/johnceo/sparklingmutuals`; new fixture utilities stay test-only.

### Task 1: Current-connection discovery authority and wire compatibility

**Files:** Create `sync/PartyRoomDiscoveries.kt`; modify `sync/PartySyncTransport.kt`, `sync/PartySyncProtocol.kt`, `sync/SafariPartySync.kt`; test `sync/PartyRoomDiscoveriesTest.kt`, `sync/PartySyncTransportEntryTest.kt`, existing protocol tests.

**Interfaces:**
- Produce immutable `PartyRoomSnapshot(generation: Long, active: Set<String>)` from `PartyRoomDiscoveries.snapshot(generation: Long): PartyRoomSnapshot?`.
- `PartyRoomDiscoveries.welcome(generation: Long, atomic: Boolean)` clears current positive readiness; `receive(generation: Long, payload: String): Boolean` validates/applies atomic facts; `reset()` clears visit facts. Preserve caught negatives across welcome changes in the same visit.
- Produce `PartySyncTransport.drainIncoming(): RoomStatus`, synchronized `roomStatus(): RoomStatus` and `withCurrentRoom(generation: Long, action: () -> Boolean): Boolean`. `RoomStatus` includes generation, immutable context, readiness, atomic capability and empty/backlogged inbound state. Draining is bounded to the queue capacity of 128 per call.
- Produce `SafariPartySync.entryRoom(client: Minecraft): PartyRoomSnapshot?` and `withEntryRoom(client: Minecraft, expected: PartyRoomSnapshot, action: () -> Boolean): Boolean`; both verify current world/account/canonical lobby/exact roster and process known inbound updates. The latter also confirms expected generation and an active discovery at the normal input boundary.

- [ ] Write failing tests: welcome without atomic data returns null; payload `3,0,1,0` yields only `Flitter`; no age limit at 6,000ms; caught bits survive stale found-only updates; reconnect requires a new snapshot and cannot revive caught bits; malformed/outdated frames yield no permission; all 37 bits map to the official species list, with indices 33/34 Fluffling/Hideonfloor.
- [ ] Run `G :26.1.2:test --tests '*PartyRoomDiscoveriesTest' --tests '*PartySyncTransportEntryTest' --tests '*PartySyncProtocolTest'`. Expected: missing-feature test failure before implementation, captured in Task 1 RED receipt.
- [ ] Implement the stated interfaces. Use one transport lock for current-room checks and inbound delivery admission; callbacks still never mutate game state. Preserve existing publication, sequence, reconnect/backoff and feature callbacks. Invalid atomic input invalidates CJ authority rather than retaining a positive permission. Keep legacy S handling for normal SM only.
- [ ] Use a test-only local HTTP/WebSocket fixture through existing endpoint constructor overrides to exercise real welcome, capture-before-dispatch, disconnect and stale generation. Bounded waits only; no live backend requests or test hooks in product classes.
- [ ] Run the same targeted tests, then `G :26.1.2:test`. Expected: all selected and full-suite tests pass.
- [ ] Commit the component and ledger evidence as `feat: expose current WebSocket room discovery authority`.

### Task 2: Complete-entry policy and one-use action ownership

**Files:** Create `safari/automation/entry/CjsEntryFlow.kt`, test `CjsEntryFlowTest.kt`; adapt approach frame/tests only where necessary for the signal contract.

**Interfaces:**
- Consume Task 1 snapshots through `CjsRoomSignal(context: EntryContext, generation: Long, active: Set<String>)`.
- Produce `CjsEntryFlow.tick(frame: CjsEntryFrame, now: Long): CjsEntryEffect?`, `consumeManager(frame: CjsEntryFrame, now: Long): Boolean`, `consumeTicket(frame: CjsEntryFrame, now: Long): Boolean`, `suppressesAutomaticMenus: Boolean`, and explicit phase/status.
- `CjsEntryFrame` carries immutable context, signal, warp generation/verification, arrival time, full-entry enable, pre-admission/admitted state, guard readiness, manual/unexpected-screen cancellation, current Manager UUID/reach/stationary evidence, approach outcome, and owned-menu evidence. Default evidence grants no action.
- `CjsTicketMenu` carries the actual menu identity reference, container ID, exact-title/readiness and selected slot/tier. Compare object identity as well as ID; dispatch-time selected tier/slot must match the proposal.
- Effects propose approach, one Manager action or one ticket action with the emitting context/warp/socket generation. Reservation precedes input; successful consumption is one-use even if the subsequent call is uncertain.

- [ ] Write failing tests for success through approach/settle/Manager/menu/ticket/personal admission; no action on initial empty/default frames; exactly one Manager/ticket; no spend from approach-only setting; active discovery remains valid past five seconds within the 12-second window.
- [ ] Add cancellation tests for loss/replacement of socket, context, roster, Manager, last discovery, input/focus, menu close/reopen/replacement, disabled setting and deadlines. Retain generic-helper suppression after a claimed attempt is cancelled; toggling/reconnect/world replacement alone never replenishes the warp budget. A genuinely verified new warp does.
- [ ] Add permit tests where slot/tier/availability/guard/context changes between proposal and consumption. Unknown availability waits; no available ticket cancels; changed actual menu identity fails even with the same ID.
- [ ] Run `G :26.1.2:test --tests '*CjsEntryFlowTest'`. Expected: missing-policy failure in Task 2 RED receipt.
- [ ] Implement the small pure flow. Reuse `EntryClock`, immutable context and existing personal-admission semantics; do not activate or retrofit the old report-based `SafariEntryController`. Terminal admission evidence can still be recorded after cancellation/expiry without authorizing another input.
- [ ] Run targeted tests and `G :26.1.2:test`. Expected: all pass.
- [ ] Commit as `feat: add one-use automatic Safari entry flow`, with ledger results.

### Task 3: Native approach, Manager reach and exact ticket adapter

**Files:** Modify `CjsApproach.kt`, `CjsApproachInput.kt`, `SafariTicketSelect.kt`; create `CjsEntryInput.kt`; add/extend `CjsApproachTest.kt`, `CjsWalkingKeysTest.kt`, `CjsEntryInputTest.kt`, ticket parser tests.

**Interfaces:**
- Replace `CjsApproachFrame.sighting` with `signal: CjsRoomSignal?`; bind started movement to its signal generation. Add `CjsApproachOutcome` to actions so completion is not inferred from text.
- `CjsApproachInput.tick(...)` returns its explicit outcome/current Manager evidence while retaining the existing input owner. Expose a release operation for handoff; no automatic interaction while keys or coast are active.
- Extract `SafariTicketSelect.options(client: Minecraft): List<Option>` (or equivalent menu/player arguments) without changing option parsing or `choose(options, preference)` semantics.
- `CjsEntryInput.frame(...)` supplies native evidence for Task 2; `dispatch(client, effect, currentFrameSupplier, now): Boolean` checks the emitting attempt and consumes the respective permit inside Task 1’s current-room boundary before normal game-mode interaction/container input.

- [ ] First write/run failing tests that WebSocket evidence older than five seconds can start the approach, generation changes cancel it, and completion releases keys before interaction. Keep all prior stopping-distance, Hold/Toggle crouch, corridor and takeover tests.
- [ ] Write/run failing tests for one matching Manager body, current native eye-to-hitbox reach and line of sight, no interaction when still moving/out of reach, exact menu/reference ownership and parsed available-tier changes. Prefer pure extracted evidence/dispatch composition with real policies over source-string assertions or mocks of assertions.
- [ ] Implement the adapter using actual mapped 26.1.2 methods inspected from local Minecraft sources. Manager interaction uses the normal guarded game-mode main-hand path; ticket input uses normal PICKUP. No raw packets, velocity changes, extra reach or fallback movement.
- [ ] Keep the CJ-owned menu visible. Manual input cancels automation without blocking ordinary guarded manual use; dispatch’s own synchronous input must not be classified as user takeover.
- [ ] Run `G :26.1.2:test --tests '*CjsApproachTest' --tests '*CjsWalkingKeysTest' --tests '*CjsEntryInputTest' --tests '*SafariTicketSelectTest'`, then the full suite. Expected: all pass; zero interaction/ticket attempts when permission is absent.
- [ ] Commit as `feat: connect guarded Manager and ticket input to CJ flow` with ledger evidence.

### Task 4: Runtime composition, configuration, regression delivery and review

**Files:** Modify `CjsSafariEntry.kt`, `SafariAssist.kt`, `SafariTicketSelect.kt`, `SafariManagerMixin.kt` only if boundary composition requires it, config owners, `gradle.properties`, relevant feature docs. Add `CjsWebsocketSessionTest.kt`, configuration/helper integration tests and final output receipts.

**Interfaces:**
- `CjsSafariEntry` composes fresh native frames, verified warp, Task 1 room signal, Task 2 flow and Task 3 adapters at START and END boundaries. Registered callbacks receive no CJ SMSE1 messages or direct detector positives.
- Produce `CjsSafariEntry.suppressesAutomaticMenu(client: Minecraft): Boolean` for all generic helper tick/hide/mouse paths, including late menus after cancellation/switch-off. This gate retains the attempt tombstone independently of the enable switch.
- Add persisted `ConfigManager.safariEntryAutoEnter = false` and matching native settings binding. Full-entry enable owns its approach; old approach-only remains non-spending. Reuse saved status key with accurate WebSocket label and the existing preferred-ticket key.

- [ ] Write/run failing composed-session tests: current-room signal drives the full sequence; no room signal plus local/party evidence never acts; captures delivered before START/ticket prevent input; switch-off and legacy generic-helper enable cannot spend from a late menu; reconnect and roster/world ordering do not rearm. Verify config fresh defaults and existing saved choices.
- [ ] Wire native lifecycle and helper suppression. Remove unused `SafariAssist` CJ observation forwarding and `entryVisibleIdentities` hooks while preserving normal upstream discovery publication. Keep legacy pure report tests/classes unregistered rather than deleting unrelated tested contracts.
- [ ] Bump .6 before build tasks that could overwrite generated .5 outputs. Update `docs/CJS_ENTRY_DRY_RUN.md`, `docs/SAFARI_ENTRY_CORE.md`, `docs/SAFARI_ENTRY_GUARD.md`, relevant defaults/docs links and concise local setup instructions; identify superseded historical behavior clearly. Reconcile read-only doc synchronization without recording a false full-doc baseline.
- [ ] Run `G :26.1.2:test :26.1.2:build` and required scoped helper checks from documented build guidance. Expected: full suite/build/target-JAR verification pass, actual test totals parsed from XML; no 26.2/26.3 artifacts generated. Record failures honestly and resolve material failures before delivery.
- [ ] Package a .6 JAR and concise README/validation/hash receipts under `outputs/cjs-websocket-entry-20261104`. Verify exact game/version/name/developer/dependency metadata, expected classes/resources and SHA-256; retain .5 artifacts. Document required full-entry enable, ticket preference, focused client, current backend room and limitations.
- [ ] Commit implementation/docs. Create the skill whole-change review package from base `6791936` to final HEAD, and dispatch one fresh final reviewer using `requesting-code-review` on the strongest allowed model. Include this plan/spec, Review Focus and ledger decisions. Correct Critical/Important findings with failing regressions and one fix pass; record deferred Minors. Rerun the full suite/build after fixes and regenerate final artifacts/receipts if bytes changed.
- [ ] Archive this plan’s ledger/review package outside the temporary skill workspace, remove only its verified workspace, retain the local branch, and deliver the downloadable local JAR with behavior, validation and explicit live-game limitations. No install/send/push.

## Self-review and approval record

Spec coverage: authority/connection lifecycle and wire order → Task 1; attempt/menu ownership/settings protection → Tasks 2/4; movement/reach/normal input → Task 3; lifecycle/settings/docs/package/review → Task 4. Interface dependencies are 1→2 signal, 1+2→3 dispatch, and 1+2+3→4 composition. Retained caught negatives and new-generation positive readiness are deliberately separate. Native/API details are builder choices, not new human scope questions.

The human approved both written design and this plan. Native implementation and final review fixes are validated; execution details and the one deferred scanning-overhead Minor are recorded in the plan ledger and local delivery receipts. No new execution-method question is needed.

# Safari Entry Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Build the approved coordinate-independent Safari sighting/report/admission decision core locally, without enabling gameplay automation.

**Architecture:** Pure Kotlin objects parse bounded messages and accept explicit verified context snapshots. A requester/responder and admission controller return proposed effects to a future adapter; no runtime registration, network sender, NPC interaction or ticket input is added. The existing live ticket helper remains untouched because this increment cannot initiate an admission attempt.

**Tech Stack:** Kotlin/JVM, Java 25, existing SafariRoster/PartyChat, JUnit 5, Gradle three-target verification.

**Spec:** Local approved design: `../../outputs/safari-entry-research-20261104/coordinate-independent-design.txt` from the worktree root. Human approval: "Approved", after the consolidated design and 51-case scenario matrix. Spawn `(-50,68,24)` and Manager `(-52,68,21)` are observations, not reach proof.

## Global Constraints

- Everything remains local; no push, merge, tag, release, install or Minecraft interaction.
- Never read `.devAuth`; preserve unrelated work and the published release.
- One request per arrival, no retry; context preparation 12 seconds, response 3 seconds, effective observation age under 5 seconds including RTT.
- Protocol at most 1024 characters, exact anchored single-line grammar; UUID-verified current leader response, intended requester IGN, matching realm/server claim and local context.
- Server name is not a run ID; destination association/reach/admission are explicit future-adapter evidence, unavailable by default.
- Cancelled Manager attempts suppress automatic entry menus for that arrival; uncertain ticket input cannot be automatically resent after re-arm/toggle/menu refresh.
- No active state persisted. No runtime settings/hook changes. All three configured targets must build and pass before handoff.

## Review Focus

- Mutable roster sets or ordinary revision refreshes must not silently change an accepted context or replenish budgets (Tasks 1/2).
- Bounds, multiline/Unicode input, forged sender claims and arithmetic/clock rollback must fail closed (Tasks 1/2).
- A fresh reply can still be old at consumption; expiry must be checked at each proposed action (Tasks 2/3).
- Cancellation/re-arm cannot let a late server menu regain automatic ticket selection (Task 3).
- New world alone is not proven new arrival; no method may accidentally reset uncertainty latches (Task 3).

## File map

All new production classes live in `src/main/kotlin/net/johnceo/sparklingmutuals/safari/automation/entry/`:
- `EntryContext.kt`: immutable context/peer/sighting types and monotonic deadline helper.
- `SparklingEntryProtocol.kt`: exact message grammar, canonical species tokens, party sighting hints.
- `EntryReportExchange.kt`: one-use requester, bounded live sighting cache and leader responder queue.
- `SafariEntryController.kt`: pure effect/state machine and arrival-wide ticket ownership gates.
Tests mirror these names under `src/test/kotlin/.../automation/entry/`, with shared `EntryFixtures.kt`.
Documentation: `docs/SAFARI_ENTRY_CORE.md`, small navigation/entry-guard notes. Do not describe this core as a usable auto-entry feature.

### Task 1: Context and bounded protocol

**Interfaces:** `EntryContext(account: UUID, selfName: String, connection: Long, world: Long, arrival: Long, realm: String, server: String, party: EntryParty)`; `EntryParty(leader: UUID, members: Set<UUID>, continuity: Long)` defensively snapshots membership. `SparklingEntryProtocol.parse(body: String): EntryMessage?`, `encode(message): String`, `hint(rawPartyMessage: String): Pair<String,String>?`. `EntryMessage.Query`, `.None`, `.Live` have the exact design fields.

- [x] Write protocol/context regression tests: exact round trips including Mantis Shrimp; wrong/missing fields, CR/LF, oversize, malformed age/token/server/requester; catch/lookup/public text rejection; canonical collision and immutable membership.
- [x] Run `:26.1.2:test --tests '*automation.entry.*ProtocolTest'`; expect unresolved new types before code.
- [x] Implement minimal context and parser types; reject public constructor values outside the same parser contract.
- [x] Re-run focused tests; expect success. Commit only Task 1 files locally.

### Task 2: Fresh report exchange

**Consumes:** Task 1 immutable context and message types.
**Interfaces:** `EntryReportRequester.beginArrival(context, now)`, `request(context, now): Query?`, `receive(context, sender: EntryPeer, reply, now): EntrySighting?`; `EntrySightings.observe(context, entity: UUID, species, now)`, `captured(context,species)`, `freshest(context, now)`; `EntryReportResponder.offer(context,peer,query,now,enabled,activeRun,destinationAssociated)`, `poll(context,now,enabled,activeRun,destinationAssociated): EntryReply?` (send-time evidence rechecked explicitly).

- [x] Write failing tests for missed original notices, one query/no retry, intended requester/nonce/leader/context/realm/server checks, strict expiry/RTT, rollback, NONE consumption, capture/new identity, mutable roster and bounded queues.
- [x] Run focused requester/responder tests; expect missing types.
- [x] Implement exchange with injected nonce supplier, defensive clocks and at most one reply per current peer/arrival. Current local observations alone feed the responder; queue revalidates freshness before emitting. Bounded response pacing consumes deadline budget.
- [x] Run all entry tests; expect success. Commit Task 2 locally with result recorded.

### Task 3: Admission controller and ownership

**Consumes:** immutable context, `EntrySighting` and report requester.
**Interfaces:** `SafariEntryController.beginArrival(context, now, confirmedNewArrival)`, `observe(sighting)`, `tick(frame: EntryFrame, now): List<EntryEffect>`, `cancel()`, `rearm()`, `allowsAutomaticTicket(context,containerId,now): Boolean`, `consumeTicketPermit(frame,now): Boolean`. Frames explicitly supply destination, guard, current Manager reach/identity, exact menu, selected available slot and personal admission evidence; they perform no action. Effects describe query/Manager/ticket intent only; the complete current frame must atomically consume a one-use ticket permission at the future packet boundary.

- [x] Write failing tests for guarded flow, no action with unavailable evidence, one Manager and ticket effect, precise menu ownership, expiry between stages, cancel/late menu, re-arm/toggle locks, uncertain result, context/world mismatch, and proven-new-arrival requirements.
- [x] Run controller tests; expect missing class.
- [x] Implement controller with arrival-wide suppression/spent latches; context changes cancel before any effect, but do not reset latches. Generic re-arm has no authority to replenish budgets. Only explicit validated new arrival resets them. No real adapter or initializer changes.
- [x] Run all entry tests; expect success. Commit Task 3 locally.

### Task 4: Documentation and final verification

- [x] Document implemented pure contracts, proposed defaults, effect-only boundary and unverified live adapter requirements. Link the approved spec/research and scenario matrix through local receipts; maintain concise repository navigation.
- [x] Run complete configured validation `clean buildAllTargets --offline`, then Python research-helper tests if affected (not affected by this core). Inspect target XML totals and distribution manifest, without claiming a live test.
- [x] Fresh independent whole-branch review against approved design. Correct Important findings using failing regression tests first; record Minor findings and material limits.
- [x] Reconcile docs read-only and verify no runtime registration/network/NPC/input code was added. Keep branch/worktree locally; no push or installation. Record final local commit and checks.

## Execution and handoff

Native execution in the current session follows the human's explicit approval to implement the reviewed core. The plan refines that approved scope; it does not add another approval requirement for routine file/type choices. Local ledger and logs are retained in the output folder after final review. Actual NPC interaction, transport registration, target selection, movement and admission detection remain a later evidence-dependent increment.

# Capture-Shot Recorder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver an opt-in local capsule recorder, reproducible replay/diagnostic tools and a usable collection guide before attempting live shot prediction.

**Architecture:** A client-thread coordinator feeds immutable, allowlisted observations into a pure bounded session model. A single worker writes versioned JSONL; recorder-owned observations remain independent of ESP/render visibility. Offline Python tools reconstruct the same evidence and evaluate diagnostic visual baselines without claiming authoritative server physics.

**Tech Stack:** Kotlin 2.4.20, Java 25, existing Gson, Fabric/Mixin and Stonecutter; JUnit 5.11.4; Python 3.11+ standard library and `unittest`. No additional mod runtime dependency.

**Spec:** [Approved recorder-first design](../specs/2026-10-16-capture-shot-recorder-design.md). Read it completely before this plan. Written-specification approval is recorded in the implementation chat; this plan still awaits review and an execution choice on issue #51.

## Global Constraints

- Branch `feature/capture-shot-projection`; independent base `eaf6e17807d620af2cb305bcd97f32d991bac6f9`; specification commit `206643676bb5872306ef20d28fdacad979e89c34`.
- Exact Minecraft targets **26.1.2, 26.2 and 26.3**; preserve Java 25, internal mod version **3.3.0**, pinned dependencies and active editor target 26.1.2.
- Recording is off at startup; default 180 seconds, start range 10–600 seconds; no persistent recording-on preference or automatic resume.
- Session limits: 600 seconds, 300 local inputs, 100,000 events, 10 MiB encoded output. Writer queue plus pre-input ring: at most 4 MiB of encoded data, including reserved termination capacity.
- Maximum 16 simultaneous/256 session capsule tracks, 64 target tracks, 256 entity inspections per tick; observation/aim radius 64 blocks; pre-input ring 0.5 seconds; samples once per client tick until eight seconds after latest input.
- Association heuristic: 1.5 seconds after input and within 8 blocks of input eye; feedback window at most 60 seconds. These are collection rules, not game constants.
- JSONL schema version 1; model version null. Session-relative monotonic microseconds, client tick index, session-local entity generations and relative positions; no raw UUID/IGN/profile/server/wall-clock/auth/chat/NBT exports.
- Do not read `.devAuth`. No automatic upload, aiming, firing, target selection, live renderer, calibrated model, history/completion mutation, PR, merge or release.
- All issue-originated questions/reviews go on still-open issue #51. Apply the actual-login allowlist and durable source fingerprints. Preserve unrelated branch/worktree changes.
- Read current maintained docs and applicable AGENTS instructions; read-only DocumentationSync before/after. Do not record its shared baseline while unrelated proposals remain unresolved.

## Review Focus

1. Packet/tick observations share one client clock but can repeat timestamps; preserve arrival sequence, and never relabel interpolated positions as raw updates (Tasks 1, 3).
2. A stalled or failed writer must stop recording within the same memory/file limits while gameplay continues, including a world change with an outstanding worker callback (Tasks 2, 4).
3. A display can be identified only after metadata arrives, or reuse an old entity ID; retain the gap/generation and never manufacture ownership or count it as a new critter (Tasks 1, 3).
4. Imported tokens, malformed JSONL and output aliases must not cause path traversal, overwrite an input, accept non-finite coordinates or turn an incomplete file into success (Task 5).
5. The same session copied/renamed into validation, or a capsule variant lacking fit evidence, must not yield a misleading independent accuracy result (Task 6).

## File and interface conventions

In the task file lists, `K/` means `src/main/kotlin/net/johnceo/sparklingmutuals/` and `T/` means `src/test/kotlin/net/johnceo/sparklingmutuals/`. Files below each abbreviation are exact package-relative paths. Kotlin DTOs are ordinary immutable data classes/sealed types, with no Minecraft objects or arbitrary maps in the export contract. Named observation variants carry the fields in the spec's record table, and enums carry wire values explicitly.

Execution is sequential: **1 → 2 → 3 → 4 → 5 → 6 → 7**. Tasks 5/6 consume Task 1's schema/fixtures, not private Kotlin implementation. Keep each task focused and commit only its listed files. Do not mark a task complete from compilation failures caused only by missing symbols: after creating the minimal interface, demonstrate a failing behavioral assertion, then implement the behavior.

### Task 1: Typed protocol and bounded shot evidence

**Files:** Create `K/safari/shots/ShotData.kt`, `ShotLimits.kt`, `ShotCodec.kt`, `ShotSession.kt`; create `T/safari/shots/ShotSessionTest.kt`, `ShotProtocolTest.kt`, `ShotTestFixtures.kt`; create `docs/schemas/capture-shots-v1.schema.json` and `src/test/resources/shots/{isolated,ambiguous,partial}.jsonl`.

**Interfaces:**

- Define `ShotStamp(micros: Long, tick: Long)`, `ShotVector(x: Double, y: Double, z: Double)`, `ShotBounds(min: ShotVector, max: ShotVector)`, `ShotLimits`, `ShotMetadata` and `ShotStatus`.
- Define `ShotBufferBudget.tryReserve(bytes: Int): Boolean`, `release(bytes: Int): Unit`, `usedBytes: Int` as a small interface; the fixture uses a fake and Task2 supplies the real shared budget. Shared `ShotInputToken`, `ShotExportResult` and typed feedback kind/observation DTOs also live in `ShotData.kt`; later tasks add behavior, not duplicate DTOs.
- Enum names shared by later tasks: `ShotVariant.ORDINARY/MASTERFUL` map to the two supported item IDs; `ShotHand.MAIN/OFF`; `ShotUseRoute.AIR/BLOCK`; `ShotUseResult.PASS/SUCCESS/FAIL/OTHER`. Status exposes `recording: Boolean`, `stopReason: ShotStopReason?`, `inputCount: Int`, `eventCount: Int`, `encodedBytes: Long`, `queuedBytes: Int`, session token and safe export result. `ShotTestFixtures.session(limits: ShotLimits = ShotLimits()): ShotSession` uses a fake in-memory sink, and `input(): ShotObservation.Input` supplies a known ordinary local input with a fresh token on every call.
- Define sealed `ShotObservation` variants `Input`, `Continuation`, `Capsule`, `Target`, `Packet`, `DisplayState`, `Obstruction`, `Feedback`, `Annotation`, `Gap`, with typed scalar/geometry fields from the approved spec. No payload is an arbitrary JSON map, raw packet, NBT, Entity or chat string. Internal runtime identity keys are never part of these DTOs.
- `ShotCodec.encode(record: ShotRecord): ByteArray` returns one complete UTF-8 JSONL line; `ShotRecord` is Header, Event or Footer. Reject non-finite geometry. Footer includes completeness, stop reason, counters and unresolved tokens, not invented outcomes.
- Define `ShotSink.offer(line: ByteArray): Boolean`, `finish(footer: ByteArray): Unit`, `queuedBytes(): Int`; false means observation cannot be accepted. Task 2 supplies the bounded implementation.
- `ShotSession(metadata: ShotMetadata, limits: ShotLimits, sink: ShotSink, budget: ShotBufferBudget)` exposes `accept(stamp: ShotStamp, event: ShotObservation): Boolean`, `advance(stamp: ShotStamp): Unit`, `stop(stamp: ShotStamp, reason: ShotStopReason): Unit`, `status(): ShotStatus`. Coordinates supplied here are already relative to the session origin. `ShotStopReason` includes user stop/export, duration/input/event/byte/queue limit, world change, Safari exit, disconnect, shutdown and error.

- [ ] **Step 1 — Write behavioral tests.** Pin exact defaults/caps, enum/nullable wire fields and privacy allowlist. Test both variants/hands, reused IDs/generations, delayed discovery versus observed spawn, a pre-existing capsule, rapid/overlapping inputs, independent feedback, manual annotation and an escaped replacement UUID. Assertions must include `association == ambiguous` for two qualifying tracks, `candidate_unique` never becoming verified ownership, shared capture never settling local input, and expiry never creating `miss`.

```kotlin
@Test fun `input cap stops without counting the rejected input`() {
    val session = ShotTestFixtures.session(ShotLimits(maxInputs = 2))
    repeat(3) { session.accept(ShotStamp(it * 10_000L, it.toLong()), ShotTestFixtures.input()) }
    assertEquals(2, session.status().inputCount)
    assertFalse(session.status().recording)
    assertEquals(ShotStopReason.INPUT_LIMIT, session.status().stopReason)
}
```
- [ ] **Step 2 — Run red.** `./gradlew.bat :26.1.2:test --tests '*shots.ShotSessionTest' --tests '*shots.ShotProtocolTest' --no-parallel`; after type scaffolding, require the relevant behavioral assertions to fail, not just unresolved symbols.
- [ ] **Step 3 — Implement the pure session and codec.** Assign increasing sequence at observation time; equal microseconds are legal, backward time stops with error. A pre-input ring flushes only not-yet-committed sequences, so overlapping windows cannot duplicate/reorder old samples. Reserve **64 KiB and one final record** within file/event limits for termination; bound footer tokens by the spec's track/input caps. Stop incomplete on `offer(false)`; no best-effort success. Schema distinguishes raw relative deltas, absolute observations, interpolated tick state, missing bases and typed outcome provenance.
- [ ] **Step 4 — Run green and inspect fixtures.** Same command, zero failures; parse each complete fixture's lines and verify header/footer counters. Test exact byte boundaries with multibyte UTF-8 and an injected monotonic clock. Add Review Focus 1/3 regressions: repeated timestamps preserve sequence; later metadata cannot backdate a fabricated spawn.
- [ ] **Step 5 — Commit.** Commit protocol, session, schema and fixtures together as `Add bounded capture-shot evidence protocol`.

### Task 2: Local exporter that cannot block gameplay

**Files:** Create `K/safari/shots/ShotExportWriter.kt`, `ShotByteBudget.kt`; create `T/safari/shots/ShotExportWriterTest.kt`, `ShotByteBudgetTest.kt`.

**Interfaces:**

- Consumes Task 1's `ShotRecord`, `ShotCodec` and `ShotSink`.
- `ShotByteBudget(maxBytes: Int = 4 * 1024 * 1024) : ShotBufferBudget` implements `tryReserve(bytes: Int): Boolean`, `release(bytes: Int): Unit`, `usedBytes: Int`. The ring/writer transfer a reservation's ownership without copying/re-reserving the same bytes. Carve the 64 KiB termination reserve out of this same budget; Task4 passes this same instance to the session and writer.
- `ShotExportWriter.open(directory: Path, sessionToken: String, budget: ShotByteBudget, completed: (ShotExportResult) -> Unit): ShotExportWriter` creates a writer implementing `ShotSink`; `shutdown(timeoutMillis: Long): Unit` has a **250 ms** bound. `ShotExportResult` contains path, complete/incomplete/error, counts and safe error category; no payload dump. The callback is marshalled to the client thread by Task 4.

- [ ] **Step 1 — Write failing writer tests.** Use a temporary directory and injected blocked/failing output stream. Assert queue plus ring stays at/below 4 MiB, encoded bytes include newlines/header/footer, cap reserves work, old exports remain unchanged, a second create cannot overwrite, partial lines recover conservatively, and failures do not write a fallback log. Inject shutdown timeout and assert `.part` remains incomplete.

```kotlin
@Test fun `combined byte budget refuses one byte beyond its limit`() {
    val budget = ShotByteBudget()
    assertTrue(budget.tryReserve(4 * 1024 * 1024))
    assertFalse(budget.tryReserve(1))
    budget.release(1)
    assertTrue(budget.tryReserve(1))
    assertEquals(4 * 1024 * 1024, budget.usedBytes)
}
```
- [ ] **Step 2 — Run red.** `./gradlew.bat :26.1.2:test --tests '*shots.ShotExportWriterTest' --tests '*shots.ShotByteBudgetTest' --no-parallel`; reproduce queue/full/I/O assertions after minimal interface setup.
- [ ] **Step 3 — Implement one worker.** Fixed `<gameDir>/sparkling-mutuals/shots`, random token/create-new `.jsonl.part`, bounded nonblocking producer queue, one reserved termination slot, then close and rename without replace. Workers never retain Minecraft objects. No per-frame I/O, retries forever, export deletion or log fallback. Prefer same-directory atomic rename; if unsupported, same-directory move without overwrite, reporting completion only after success. Directory/open errors report stopped/error without starting a live session.
- [ ] **Step 4 — Run green.** Same tests; include Review Focus 2: reserve/queue rejection is immediately visible to the producer and draining still emits an honest incomplete footer when possible. Verify worker writes an immutable snapshot and never accesses a live mutable list.
- [ ] **Step 5 — Commit.** `Add bounded local shot export writer`.

### Task 3: Independent observations and three-version packet translation

**Files:** Create `K/safari/shots/ShotObservationAdapter.kt`, `ShotObservationHooks.kt`, `ShotEntityCache.kt`, `ShotPacketAdapter.kt`, `ShotInputCycles.kt`, `K/compat/ShotPacketCompat.kt`, `K/mixin/ShotPacketMixin.kt`, `ShotInputMixin.kt`, `ShotUseMixin.kt`; modify `src/main/resources/sparkling-mutuals.mixins.json`; create `T/safari/shots/ShotEntityCacheTest.kt`, `ShotPacketAdapterTest.kt`, `ShotInputCyclesTest.kt`, `ShotObservationBoundaryTest.kt`.

**Interfaces:**

- `ShotObservationAdapter(origin: ShotVector, emit: (ShotStamp, ShotObservation) -> Unit)` exposes `tick(client: Minecraft, stamp: ShotStamp): Unit`, `packet(client: Minecraft, update: ShotPacketUpdate, stamp: ShotStamp): Unit`, `reset(): Unit`. Copy only allowlisted immutable fields; translate all world geometry to origin-relative values before emitting. Velocity, direction and display-local translations are vectors and must not have the world origin subtracted.
- Adapter input methods are `input(client: Minecraft, token: ShotInputToken, hand: ShotHand, variant: ShotVariant, route: ShotUseRoute, stamp: ShotStamp): Unit` and `continuation(token: ShotInputToken, result: ShotUseResult, stamp: ShotStamp): Unit`. `ShotObservationHooks.bind(adapter: ShotObservationAdapter?, clock: () -> ShotStamp, failed: (Throwable) -> Unit): Unit` installs the passive callback bridge and owns/reset its input cycles. It starts unbound, so Task3 compiles without a future ShotRecorder; Task4 binds/unbinds it for active sessions only. Hooks return immediately while unbound and route observation exceptions to `failed` without altering gameplay results. Emit one Input per token; air/block fallthrough adds route/continuation evidence rather than a second counted input.
- `ShotEntityCache` owns session-local tokens/generations, bounded bootstrap/load/metadata discovery and a recorder-owned packet-position base. Produce typed `ShotPacketUpdate` (`Spawn`, `Move`, `PositionSync`, `Teleport`, `Motion`, `MetadataChanged`, `Removed`) plus explicit availability/base status. Unknown/unrelated display metadata is never exported wholesale.
- `ShotPacketCompat` converts the target packet into `ShotPacketUpdate` or null for irrelevant entities. Raw delta/path observations stay separate from reconstructed absolute and tick state. Never mutate the entity's own packet-position codec.
- `ShotInputCycles.begin(): Unit`, `observe(hand: ShotHand, variant: ShotVariant, route: ShotUseRoute): ShotInputToken`, `returned(token: ShotInputToken, result: ShotUseResult): Unit`, `end(): Unit`. One `Minecraft.startUseItem` cycle coalesces air/block fallthrough for the same hand/variant; distinct hands and distinct invocations remain distinct. Calls outside a known cycle get standalone tokens with explicit provenance, not timestamp-based deduplication.

- [ ] **Step 1 — Write failing observation tests.** Pin token generations, late metadata/spawn distinction, pre-input discovery, bounded cache work, frame-independent samples and both hidden/visible capsule settings with ESP display disabled. For input, assert block→air fallthrough in one cycle returns one token with two continuation records; two rapid cycles yield two tokens; both hands/variants survive. No continuation claims server acceptance.

```kotlin
@Test fun `block fallthrough shares an input but a second cycle does not`() {
    val cycles = ShotInputCycles()
    cycles.begin()
    val block = cycles.observe(ShotHand.MAIN, ShotVariant.ORDINARY, ShotUseRoute.BLOCK)
    assertEquals(block, cycles.observe(ShotHand.MAIN, ShotVariant.ORDINARY, ShotUseRoute.AIR))
    cycles.end(); cycles.begin()
    assertNotEquals(block, cycles.observe(ShotHand.MAIN, ShotVariant.ORDINARY, ShotUseRoute.AIR))
}
```
- [ ] **Step 2 — Run red.** `./gradlew.bat :26.1.2:test --tests '*shots.ShotEntityCacheTest' --tests '*shots.ShotPacketAdapterTest' --tests '*shots.ShotInputCyclesTest' --tests '*shots.ShotObservationBoundaryTest' --no-parallel`.
- [ ] **Step 3 — Implement pure cache/input rules and target compatibility.** Primary API evidence is saved outside Git at `outputs/capture-shots-2026-10-16/api/`. The seven ClientPacketListener handlers are present on all targets: `handleAddEntity`, `handleMoveEntity`, `handleEntityPositionSync`, `handleTeleportEntity`, `handleSetEntityMotion`, `handleSetEntityData`, `handleRemoveEntities`. Use after-thread-handoff/TAIL observation; removal uses packet IDs even though the entity is already gone. Validate exact descriptors and applied injections on each target, with required mixins/defaultRequire retained.

  For 26.1.2/26.2 move packets, copy `getXa/getYa/getZa` and decode only against a known recorder base using the installed codec semantics. For 26.3 use `getPositionDelta()` and a recorder-owned `VecDeltaCodec`; preserve `PositionPath` linear/stepped points and each `PositionStep.tickOffset()`. Absolute sync changed from `values(): PositionMoveRotation` to `position(): PositionPath` on 26.3. Copy `endPosition()` and bounded stepped points with their timing. Do not replace raw observations with the already-interpolated entity position or reconstruct an unknown absolute base from it. If the base is missing, export relative delta/path with missing-base provenance until an observed absolute anchor arrives. Handle teleport relative flags and motion separately.

- [ ] **Step 4 — Implement passive game adapters.** Observe `Minecraft.startUseItem` cycle HEAD/RETURN and `MultiPlayerGameMode.useItem`/`useItemOn` HEAD/RETURN without cancellation, using actual local player, hand and item IDs. Do not add another `EspCaptureMemory.throwing` call. Copy changed display transformation/interpolation fields; do not use rendering as the data source. Bound load/bootstrap/refresh inspections at 256/tick, active capsules 16, total 256, targets 64; label overflow/gaps. Clip only loaded block shapes along the 64-block aim ray and observed capsule segments; unknown chunks produce unavailable evidence, never loads. No all-entity-per-frame scan.
- [ ] **Step 5 — Run green on all targets.** Repeat the focused task tests for 26.1.2, 26.2 and 26.3. Include Review Focus 1/3: packet-relative bases versus smooth entity samples and a reused entity ID after removal. Boundary tests must inspect the actual compiled observation callbacks with controlled immutable observations, and prove they preserve use return values; do not merely test a duplicate policy helper.
- [ ] **Step 6 — Commit.** `Record capsule inputs and versioned entity observations`.

### Task 4: Controls, lifecycle and feedback without tracking side effects

**Files:** Create `K/safari/shots/ShotRecorder.kt`, `ShotFeedback.kt`, `K/commands/ShotCommands.kt`; modify `K/SparklingMutuals.kt`, `K/commands/SparklingCommand.kt`; create `T/safari/shots/ShotRecorderTest.kt`, `ShotFeedbackTest.kt`, `ShotCommandsTest.kt`, `T/safari/integration/ShotRecordingIsolationTest.kt`; create `docs/CAPTURE_SHOT_RECORDING.md`.

**Interfaces:**

- `ShotRecorder.register(): Unit`, `start(seconds: Int = 180): ShotCommandResult`, `stop(reason: ShotStopReason): ShotCommandResult`, `status(): ShotStatus`, `label(scenario: ShotScenario, split: ShotSplit): ShotCommandResult`, `mark(shotToken: String, outcome: ShotManualOutcome): ShotCommandResult`, `tick(client: Minecraft): Unit`, `disconnect(): Unit`, `shutdown(): Unit`.
- Scenario enum wire values: `unspecified`, `stationary_level`, `stationary_up`, `stationary_down`, `walking_forward`, `walking_sideways`, `moving_target`, `solid_obstruction`, `edge_obstruction`, `rapid_inputs`, `nearby_thrower`, `hiding_comparison`. Split: `fit`, `validation`, `unassigned`. Initial scenario/split is unspecified/unassigned.
- `ShotFeedback.parse(text: String): ShotFeedbackObservation?` uses `SafariRules.strip`, anchored existing thrown/escaped species parsers and `SafariCatch.parse`; discard catcher identity/shards/raw text immediately. `ShotFeedbackObservation` has `kind: ShotFeedbackKind`, canonical `species: String`, nullable known capsule variant and typed source provenance; kinds include HIT, ESCAPE, PERSONAL_CAPTURE, SHARED_CAPTURE, STARTLE. Recognize exact Shyworm startle as typed evidence, not a local confirmed miss. Subscribe once to canonical GAME messages (`!overlay`); CHAT/party/public messages are not a second feedback source. Provenance is canonical GAME dispatch plus per-dispatch sequence, so real identical repeated messages remain separate. No long text-based dedup timeout.
- `ShotCommands.register(root: LiteralArgumentBuilder<FabricClientCommandSource>): Unit` installs `/sm shots` and all approved subcommands, returning local notices only. `export` calls stop when recording and reports the last completed path otherwise. Use enumerated suggestions and reject paths/free text.

- [ ] **Step 1 — Write failing coordinator/feedback/command tests.** Test off/start range10–600/default180/repeated start/writer-finalizing/export/status; unsupported item, unloaded or non-Safari start; enum suggestions and invalid mark tokens. Verify both hands/variants, manual versus typed feedback and two identical real hit messages. Neither shared loot, timeout nor startle settles a local capture. Snapshot existing ledger/ESP state before and after replaying observations; assert no recorder mutation.

```kotlin
@Test fun `escape is distinct feedback and quoted party text is rejected`() {
    val escaped = ShotFeedback.parse("The Gemzie escaped your Critter Capsule!")!!
    assertEquals(ShotFeedbackKind.ESCAPE, escaped.kind)
    assertEquals("Gemzie", escaped.species)
    assertNull(ShotFeedback.parse("Party > Farmer: You threw a Critter Capsule at the Gemzie!"))
}
```
- [ ] **Step 2 — Run red.** `./gradlew.bat :26.1.2:test --tests '*shots.ShotRecorderTest' --tests '*shots.ShotFeedbackTest' --tests '*shots.ShotCommandsTest' --tests '*ShotRecordingIsolationTest' --no-parallel`.
- [ ] **Step 3 — Implement coordinator and docs with one epoch per session.** Stop old world before any new-world observation, ignore stale worker callbacks by epoch, and never resume automatically. Clear transient objects while retaining only safe export result/status. Marshal worker notifications to client thread; report failures once locally without data dumps. Add command registration and disconnect/shutdown cleanup without changing existing feature order/side effects. Update the guide with real path, controls, caps and honest readiness-to-collect status. No saved recording preference/config toggle.
- [ ] **Step 4 — Run green.** Include Review Focus 2: late worker callback during world transfer and start while finalizing. Exact assertions: new-world input cannot appear in old file; disconnect/restart is off; worker I/O error preserves previous export and existing capture totals. Validate a codec fixture contains no input chat/UUID/auth/path secrets and all world-space bounds/camera/ray fields are relative.
- [ ] **Step 5 — Commit.** `Expose opt-in shot recording controls and lifecycle`.

### Task 5: Deterministic replay and export validation

**Files:** Create `tools/capture_shots/{protocol.py,replay.py,__init__.py}`, `tools/capture_shots/tests/{test_protocol.py,test_replay.py}`; extend `src/test/resources/shots/` with invalid/nonfinite/reused-token/UTF8-tail fixtures and `docs/CAPTURE_SHOT_RECORDING.md`.

**Interfaces:**

- Python `read_session(path: Path) -> Session` in `protocol.py`; immutable dataclasses `Session(header, events, footer, complete, issues, input_sha256)` and `Track`/`Shot` reconstructed from the shared schema. Reader limits file10MiB, events100,000 and one line64KiB; reject duplicate JSON keys/non-finite JSON numbers. A missing/partial final line yields incomplete with a reason; malformed complete interior lines are errors.
- `replay(session: Session) -> ReplayResult` and `write_replay(result: ReplayResult, output: Path) -> None` in `replay.py`. Result contains deterministic summary plus per-shot rows with the original observation kinds. Paths use generated internal indexes, not imported tokens. Refuse an output containing the input or an existing output report; never overwrite. CLI is the exact replay command in the spec; exit0 for clean complete, exit2 for recoverable incomplete, exit1 for invalid/error, with explicit messages.

- [ ] **Step 1 — Write failing tool tests.** Use Task1 fixtures plus a truncated UTF-8 tail. Assert exact sequence/timing/token reconstruction, availability and outcome provenance, CSV raw/tick separation and byte-identical replay output across repeats to fresh directories. Test unsupported schema, missing footer, duplicate keys, NaN/Infinity, late event after footer, duplicate sequence, reused entity generation and raw privacy-key rejection.

```python
def test_partial_file_does_not_become_a_complete_session(self):
    session = read_session(FIXTURES / "partial.jsonl")
    self.assertFalse(session.complete)
    self.assertIn("missing_footer", session.issues)
    self.assertTrue(all(event.kind != "miss" for event in session.events))
```
- [ ] **Step 2 — Run red.** `python -m unittest discover -s tools/capture_shots/tests -p 'test_*.py' -v`; require a meaningful assertion failure after minimal interfaces.
- [ ] **Step 3 — Implement standard-library reader/replay.** Use bounded binary reads and JSON parsing, verify enums/allowed fields/finite numbers against the protocol contract, keep missing values/reasons, and do not infer outcomes. Write CSV through `csv.writer` and fixed field names; reject imported token path separators even though output names are generated. Resolve input/output locations and refuse alias/symlink overwrite. No imports/plugins/network from input content; no external content rendering.
- [ ] **Step 4 — Run green and cross-check producer/consumer.** Include Review Focus4 tests for path traversal, input/output alias, oversized line/file and partial tail. Replay actual Kotlin-produced synthetic fixture bytes, verify counts against producer expectations; do not claim synthetic fixture replay establishes live packet fidelity.
- [ ] **Step 5 — Commit.** `Add deterministic local capture-shot replay`.

### Task 6: Diagnostic visual analysis with genuine held-out data

**Files:** Create `tools/capture_shots/{analyze.py,models.py}`, `tools/capture_shots/tests/{test_analysis.py,test_models.py}`; add synthetic linear/accelerating/irregular and ambiguous fixtures under `src/test/resources/shots/`; extend `docs/CAPTURE_SHOT_RECORDING.md`.

**Interfaces:**

- `fit_track(samples: Sequence[Sample], degree: int) -> VisualFit | None` and `predict(fit: VisualFit, seconds: float) -> Vec3` in `models.py`, with degree1/2 only. `Sample` has observation time in seconds and relative position; reject missing bases and mixed raw/tick sample sources. The analysis default uses reconstructed raw updates; tick-only diagnostics are separately labelled, never pooled.
- `analyze(fitting: Sequence[Session], validation: Sequence[Session], curation: Curation | None, provisional: bool = False) -> AnalysisResult`. `Curation` references each source SHA-256 and explicit session/shot/track choices with a user rationale; unknown fields cannot execute code. Default excludes uncurated ownership, ambiguous/unmatched/incomplete tracks. `--provisional` gives separately labelled diagnostic output, not calibrated scores.
- CLI accepts repeatable `--fit`, `--validation`, optional `--curation`, `--provisional`, `--predictions` and required `--output`. `read_predictions(path: Path) -> PredictionSet` validates CSV columns `schemaVersion,session,shot,track,micros,x,y,z,model,coordinates`; schemaVersion must be1 and coordinates must equal the session-relative game-axis convention. Match points only at supplied observation timestamps; report uncovered times rather than invent interpolation/collision.

- [ ] **Step 1 — Write failing math/isolation tests.** A synthetic trajectory `p(t)=(2t, 3t-0.5t², 0)` yields acceleration `(0,-1,0)` and zero residual within tolerance on held-out exact data; irregular times give the same fit. A stationary trace with enough distinct times yields finite zero motion; too-few/distinct-time and rank-deficient cases return unavailable. Assert known RMSE/nearest-rank p50/p95/max and last-observed error; never use the word impact without confirmed evidence.

```python
def test_quadratic_fit_predicts_held_out_positions(self):
    training = [Sample(t, Vec3(2*t, 3*t-0.5*t*t, 0)) for t in (0, 0.2, 0.7)]
    fit = fit_track(training, degree=2)
    self.assertIsNotNone(fit)
    actual = predict(fit, 1.0)
    self.assertAlmostEqual(2.0, actual.x, places=9)
    self.assertAlmostEqual(2.5, actual.y, places=9)
}
```
- [ ] **Step 2 — Run red.** `python -m unittest discover -s tools/capture_shots/tests -p 'test_*.py' -v`.
- [ ] **Step 3 — Implement diagnostics.** On fit sessions, train on the first `floor(n/2)` observations (minimum3 for acceleration), score only the rest. Freeze per-variant component-wise median acceleration from finite fitting coefficients. For validation, seed from first two distinct-time points, subtract the fixed acceleration contribution when deriving initial velocity, exclude seeds from scores and never refit/tune on validation. CV uses zero acceleration. Reject fit/validation overlap by session token **or source digest**, missing variant fit evidence and mixed coordinate/sampling bases. Clearly distinguish visual diagnostic from before-firing prediction.
- [ ] **Step 4 — Run green.** Add Review Focus5: same content renamed, same session with edited filename, unavailable Masterful fit evidence, validation seed excluded, and changing only later validation points cannot change fitted parameters. Report input/feedback delays, update gaps, counts/exclusion reasons and variant summaries separately; compare optional external predictions deterministically. Input files remain byte-for-byte unchanged.
- [ ] **Step 5 — Commit.** `Add held-out visual trajectory diagnostics`.

### Task 7: Verified collection kit and delivery

**Files:** Modify `docs/CAPTURE_SHOT_RECORDING.md`, `README.md`, `docs/README.md`, `docs/IMPLEMENTATION_TEST_CHECKLIST.md`, `.github/workflows/build.yml`; add `tools/capture_shots/README.md` and `T/safari/integration/ShotRecordingExportIntegrationTest.kt` if a whole-path fixture is not covered by Task4. Keep temporary client smoke helpers and gameplay exports outside tracked source.

**Interfaces:** Consume public recorder controls and stable tools only. Add a read-only CI Python tool test job ([`actions/setup-python@v7`](https://github.com/actions/setup-python) with `python-version: '3.11'`, no pip dependencies), and make aggregate verification require it alongside build-logic/target jobs. Retain all3JAR artifact names and existing validation; no release workflow edits. Add a `capture-shot-tools` Actions ZIP preserving the repository paths for `tools/capture_shots/` sources/tests, `docs/schemas/capture-shots-v1.schema.json`, `docs/CAPTURE_SHOT_RECORDING.md` and synthetic `src/test/resources/shots/` fixtures so a player need not clone the repository. Use an explicit archive allowlist, excluding `__pycache__`; do not bundle gameplay data, caches or development authentication.

- [ ] **Step 1 — Pin whole-path regressions before final wiring.** Feed controlled local input, changed packet/display state, one target and typed feedback through the real service/codec to a temp file; replay it and compare expected relative positions, provenance/counts and unknown outcomes. Test recorder off/on and hidden/ESP-disabled inputs leave existing captures/history/check evidence unchanged. If this exposes a missing boundary, show the failing assertion before fixing it in the owning task.
- [ ] **Step 2 — Run local verification.** Java25: `./gradlew.bat clean buildAllTargets testBuildLogic verifyDistributions --no-parallel`; Python: `python -m unittest discover -s tools/capture_shots/tests -p 'test_*.py' -v`. Verify actual final JAR metadata and exclude all smoke tools/development dependencies. Record test counts from XML, not estimates.
- [ ] **Step 3 — Verify actual clients.** Start each of `:26.1.2:runClient`, `:26.2:runClient`, `:26.3:runClient` with `-PclientSmoke --no-parallel` and authentication disabled for offline checks. Confirm required packet/input mixins apply, controls register and a controlled local fixture can finalize. Inspect transformed classes/actual callbacks where needed; startup alone does not prove Hypixel packet fields. Keep no-auth fixtures separate from the shipped mod and restore any displaced test helpers.
- [ ] **Step 4 — Finish the collection guide and reconcile docs.** Include exact commands, scenario/split/mark syntax and the complete spec checklist: small ordinary-capsule sanity block first; then both variants, approximate5/15/30block level/up/down samples and independent fit/validation sessions, five feasible repetitions, movement/obstructions/interference and hiding comparison. Do not request rare-capsule expenditure before the kit is usable. List missing/unavailable live checks and explain how each block distinguishes models. Update Last updated using America/New_York; read-only DocumentationSync and relevant rule diff, never blanket `-Record`.
- [ ] **Step 5 — Independent final review and corrections.** Review actual diff against spec, all five Review Focus cases, privacy/export bounds, applied observation hooks, Python isolation and tracking noninterference. Correct findings with a demonstrated regression, then repeat only affected checks and the final clean matrix when code changes. An optional live Hypixel session must test field fidelity, both hands/variants, hiding on/off, movements/targets and transfer; if unavailable, label collection/validation awaiting gameplay.
- [ ] **Step 6 — Commit/upload only the issue branch.** Immediately recheck issue51 open; post the authorized implementation-stage reply without claiming Actions success, record IDs/fingerprints, commit focused files and push `feature/capture-shot-projection`. Do not create a PR/merge/release. Verify every required Actions job on the exact final uploaded SHA, including the Python job; download/verify all3JARs and the tools ZIP locally. Recheck state immediately before the final source-linked confirmation and collection request on51. Retain durable handled-report and confirmation history. Deliver concise chat notes and downloadable artifacts, explicitly calling this recorder-ready/awaiting calibration, not a completed aiming projection.

## Self-review and execution handoff

Every approved spec section maps to a task: protocol/evidence/privacy/association1; bounded files/errors2; input/entities/raw/display/obstruction3; controls/lifecycle/feedback/isolation4; schema/replay5; fitting/validation/error analysis6; guide/runtime checks/CI/artifacts7. No live model, renderer, unrelated branch integration or gameplay-mechanics guess is included. Exact values remain in Global Constraints rather than differing per task.

The preferred execution method is **Native** for this plan: the seven tasks share one evolving evidence schema, byte budget and observation lifecycle, so one implementer avoids repeated interface handoffs; an independent final review still checks the whole branch. **Subagent-driven** is also available, with an implementer and review gates per task, at higher context cost. The maintainer must review this plan and choose the method on issue51 before Task1 starts. A previous subagent choice for issue52 does not silently choose execution for this plan.

## Last updated

2026-10-15 (America/New_York) - Prepared the seven-task recorder/replay implementation plan after written-spec approval; product implementation awaits plan review and execution choice.

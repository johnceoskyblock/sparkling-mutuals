# Capture-shot Terrain and Critter Contacts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver an opt-in 26.1.2 guide with distinct estimated terrain/critter contact markers, readable geometry and reproducible local validation support.

**Architecture:** Extend the fixed spatial curve through a separate, explicitly uncalibrated hypothesis. Keep bounded scene acquisition, pure contact ordering, rendering and versioned diagnostic export separate; the renderer works with recording off. Existing tracking/ESP and raw recorder observations keep their meanings.

**Tech Stack:** Kotlin/JVM, Java 25, Fabric/Minecraft 26.1.2, existing Stonecutter/Gradle setup, JUnit 5, Gson, Python 3.11+ standard library.

**Spec:** [Approved written specification](../specs/2026-10-17-capture-shot-contacts-design.md). The human approved it directly in chat after review request 6232201364. The human approved this plan directly in chat after review question 6232286774. Preserve **Native** execution; do not ask the method question again.

## Global Constraints

- Work stays on `feature/capture-shot-projection`; inspect and preserve unrelated changes. Establish the isolated existing checkout using the worktree skill at execution time, without blindly creating another checkout or changing master.
- Build and deliver **Minecraft 26.1.2 only**. Do not run `buildAllTargets`, `verifyDistributions`, release tasks or target 26.2/26.3 builds during this increment.
- Never read `.devAuth`; no credentials, unrelated chat, runtime entity IDs/UUIDs or raw field recordings in commits/public replies. No automatic uploads of field evidence.
- No PR, merge, release, version bump or global documentation-baseline recording. Scheduled issue checks remain paused.
- Existing path default off; **Allow uncalibrated estimates** default off, enables expanded path and contacts. Width 1 through 5, default 3. Ordinary main-hand capsules, stationary grounded first-person player, two stable ticks, both hiding options off; preserve remaining spec exclusions.
- Fixed coefficients remain `0.8482482249942948` and `-0.020085553311180723`. Preserve original model identity and files; new wrapper identity `contact-guide-center-extrapolation-v1`. Parameter is not server time.
- Expanded curve starts at ordinal 0; cap at 64 accumulated blocks, ordinal 128 or 256 segments. Maximum segment chord 0.5 blocks; maximum parabolic chord deviation 0.01 blocks. Zero collision radius is a hypothesis, not a server claim.
- Candidate cache at most 64; discovery and revalidation share 256 inspected entity slots per client tick, including null slots. Never load chunks or turn missing coverage into a miss.
- Schema v2 / recorder policy v3 adds typed contact probes. Read v1 and v2 strictly; retain 64 KiB line, 10 MiB session, shared 4 MiB queue/ring and 300-input caps. Probes do not settle ownership, hit, capture or endpoint truth.

## Review Focus

1. A near-eye wall inside the formerly omitted launch prefix must not permit a marker on a critter behind it (Task 1 numeric regression, Task 6 client fixture).
2. Entity-map clearing, ID reuse and cache turnover must not crash, retain a stale target or exceed the shared scan budget (Task 2 boundary regressions).
3. An unloaded chunk, fluid boundary or unsupported proxy before the apparent target must produce uncertainty, not an attractive but false hit marker (Tasks 1–3).
4. Large UTF-8 probes near the line/queue cap must not corrupt raw capsule evidence or silently become complete measurements (Task 4 real writer tests, Task 5 strict-reader tests).
5. Turning the camera, changing worlds or opening a screen between tick and submission must not retain the old marker or change existing through-wall ESP (Task 3 immutable-frame tests, Task 6 real client checks).

## File and interface map

Kotlin base `K` below means `src/main/kotlin/net/johnceo/sparklingmutuals/`; test base `T` means `src/test/kotlin/net/johnceo/sparklingmutuals/`. Expand these literal bases when editing. New pure files live together under `K/safari/shots/preview/contacts/`; client adapters remain there but do not leak Minecraft classes into pure math/solver interfaces.

| Unit | Files and responsibility |
| --- | --- |
| Curve and pure contacts | New `ContactData.kt`, `ContactCurveSampler.kt`, `ContactIntersections.kt`, `CapsuleContactSolver.kt` under the contacts package |
| Scene acquisition | New `ContactTargetCache.kt`, `ContactTargetProxies.kt`, `CapsuleContactScene.kt`; extract `K/safari/esp/EspBodyBounds.kt` only for existing unpadded body placement |
| Appearance and integration | New `ContactGuideGeometry.kt`; modify existing `CapsulePreviewRules.kt`, `CapsulePreviewFrameBuilder.kt`, `CapsulePreview.kt`, `CapsulePreviewRenderer.kt`, config files and additive `WorldRenderCompat.kt` support |
| Probe producer | New `K/safari/shots/ShotContactProbe.kt`; modify `ShotData.kt`, `ShotCodec.kt`, `ShotSession.kt`, `ShotRecorder.kt`, `ShotObservationAdapter.kt` and minimal lifecycle wiring |
| Consumer | New `tools/capture_shots/contacts.py`, schema v2 and synthetic fixtures; update strict protocol reader, replay and package allowlist |
| Field guide and delivery | Update recording/config/manual-checklist/package documentation and complete target-only checks, independent review, Actions receipts and issue confirmation |

### Task 1: Bounded extrapolated curve and pure first-contact solver

**Files:** Create the four pure contacts files above; tests `T/safari/shots/preview/contacts/ContactCurveSamplerTest.kt`, `ContactIntersectionsTest.kt`, `CapsuleContactSolverTest.kt`. Read existing `CapsulePathModel.kt` and its tests; do not refit or modify frozen research artifacts.

**Interfaces produced:**

```kotlin
data class ContactNode(val parameter: Double, val position: ShotVector, val observedRegion: Boolean)
enum class ContactStop { CONTACT, RANGE_LIMIT, ORDINAL_LIMIT, WORK_LIMIT, INVALID, UNLOADED, FLUID, TARGET_COVERAGE }
data class ContactCurve(val nodes: List<ContactNode>, val stop: ContactStop, val hypothesis: String)
data class ContactTarget(val key: String, val generation: Int, val species: String, val bounds: ShotBounds, val proxy: String)
data class ContactCandidate(val kind: ContactKind, val parameter: Double, val position: ShotVector,
    val normal: ShotVector?, val targetKey: String?, val proxy: String?)
enum class ContactKind { TERRAIN, CRITTER }
data class ContactBoundary(val fraction: Double, val reason: ContactStop)
data class ContactSegmentScene(val terrain: ContactCandidate?, val boundary: ContactBoundary?)
data class ContactCoverage(val complete: Boolean, val reasons: Set<String>)
data class ContactTrace(val nodes: List<ContactNode>, val primary: ContactCandidate?,
    val stop: ContactStop, val warnings: Set<String>)
object ContactCurveSampler { fun sample(eye: ShotVector, aim: ShotVector, pitch: Double): ContactCurve }
object ContactIntersections { fun entry(from: ShotVector, to: ShotVector, bounds: ShotBounds): Double? }
object CapsuleContactSolver {
    fun trace(curve: ContactCurve, targets: List<ContactTarget>, coverage: ContactCoverage,
        scene: (ContactNode, ContactNode) -> ContactSegmentScene): ContactTrace
}
```

`key` is an opaque local cache key, not a UUID. `entry` returns the earliest segment fraction in [0,1], including 0 for an inside start. Terrain candidates returned by `scene` carry the same curve parameter basis as nodes. Use `Double` throughout; ties within `1e-7` parameter prefer terrain and add a tie warning. Boundary comparisons use that same tolerance; unknown boundaries suppress markers at/beyond them. Incomplete target coverage yields no primary marker. `warnings` uses a closed allowlist of constants, not arbitrary client text.

- [ ] **Write failing arithmetic/ordering tests.** Include `prefixWallPrecedesCritter`: eye `(0,0,0)`, aim `(0,0,1)`, a wall slab `z=0.5..0.6` and target `z=2..3`; assert terrain at z=.5, parameter below 2 and no critter primary. Include inside starts, negative direction, axis-parallel/vertical rays, degenerate boxes, wall/target reversal, ties and boundary-before-target. Pin the independent intersection arithmetic:

```kotlin
assertEquals(0.25, ContactIntersections.entry(
    ShotVector(0.0, 0.0, 0.0), ShotVector(0.0, 0.0, 2.0),
    ShotBounds(ShotVector(-1.0, -1.0, 0.5), ShotVector(1.0, 1.0, 0.6)))!!, 1e-12)
```

- [ ] **Write failing bounded-sampling tests.** Assert original n=2 numeric point, finite unit-normalized translation invariance, `nodes.size <= 257`, first parameter 0, total length <=64+`1e-8`, each chord <=.5+`1e-8`, exact parabola-midpoint deviation <=.01+`1e-8`, ordinal <=128, outside-angle nodes unobserved, invalid/nonfinite aim unavailable. Keep original 64-point short-path tests unchanged.
- [ ] **Run red tests:** `./gradlew.bat :26.1.2:test --tests '*ContactCurveSamplerTest' --tests '*ContactIntersectionsTest' --tests '*CapsuleContactSolverTest'`. New APIs/tests must fail before product changes; keep a local red receipt.
- [ ] **Implement the pure interfaces.** Bound subdivision attempts as well as accepted segments. Clamp the final chord to the remaining arc-length budget; never return a cap as contact. Interpolate contact positions/parameters on a segment, trim trace nodes at the earliest accepted contact/unknown boundary and preserve launch/evidence flags. No Minecraft/world calls here.
- [ ] **Run green tests** with the same command and existing `*CapsulePathModelTest`; all assertions pass. Commit focused source/tests as `feat: add bounded experimental capsule contact solver`.

### Task 2: Loaded terrain shapes and bounded current-position critter proxies

**Files:** Create `K/safari/shots/preview/contacts/ContactTargetCache.kt`, `ContactTargetProxies.kt`, `CapsuleContactScene.kt`; create `K/safari/esp/EspBodyBounds.kt` and modify `SafariEsp.kt` only if sharing its current unpadded shape placement. Tests `T/safari/shots/preview/contacts/ContactTargetCacheTest.kt`, `ContactTargetProxiesTest.kt`, `ContactSceneBoundaryTest.kt`, and `T/safari/esp/EspBodyBoundsTest.kt`.

**Consumes:** Task 1 types and `trace`; existing `SafariEsp.describe`, `SafariEspRules.identify`, model/capture validity and `ShotVector`/`ShotBounds`.

**Interfaces produced:**

```kotlin
data class ContactSceneSnapshot(val trace: ContactTrace, val targets: List<ContactTarget>,
    val coverage: ContactCoverage, val sourceTick: Long)
class ContactTargetCache {
    fun reset()
    fun tick(world: Any, slots: Iterator<ContactDiscovery?>, inspect: (ContactDiscovery) -> ContactTarget?)
    fun snapshot(): Pair<List<ContactTarget>, ContactCoverage>
}
object CapsuleContactScene {
    fun tick(client: Minecraft, enabled: Boolean)
    fun reset()
    fun snapshot(client: Minecraft, eye: ShotVector, aim: ShotVector, pitch: Double): ContactSceneSnapshot
}
```

Define `data class ContactDiscovery(val key: String, val generation: Int)` in the cache file. Retain discoveries for existing cache entries; `inspect` resolves their current identity, range and bounds and returns null when invalid. Charge calls for existing entries before reading new slots. Use at most 240 slots in the tick cache pass, reserving 16 of the shared 256-slot budget for selected-contact identity checks during rendered frames/input probes. `CapsuleContactScene.snapshot` returns immutable data and validates its selected target using that remaining allowance; exhaustion or removal suppresses the marker until refreshed. Do not revalidate all 64 targets on every rendered frame. Maximum retained candidates is 64. A disabled scene with no active recorder performs no discovery; the recorder can request it independently when explicitly active. Call the shared scene tick only once per client tick when preview and recording are both active.

- [ ] **Write failing cache boundary tests:** real fastutil linked entity-map clear between batches, null slots consume allowance, ID reuse changes generation, stale/out-of-range entries release slots, close entrants replace obsolete entries, 65 candidates/unfinished sweeps flag coverage, and total inspections never exceed 256 including 64 cached revalidations. World identity change clears all records. The empty-cache budget assertion is:

```kotlin
val cache = ContactTargetCache()
var inspected = 0
cache.tick(Any(), List(400) { ContactDiscovery("local_$it", 1) }.iterator()) {
    inspected++; null
}
assertEquals(240, inspected)
assertFalse(cache.snapshot().second.complete)
```

- [ ] **Write failing scene/proxy tests:** exact existing ESP body placement before padding, native finite boxes, zero-sized display fallback only for identified visual bodies, unsupported labels rejected, capsule/player exclusions, partial stair/slab shapes, inside-solid origin and exact crossed-chunk preflight. A fluid or unloaded boundary returns an unknown boundary, not a terrain hit or an empty safe path. Preserve old ESP padded boxes in regression fixtures.
- [ ] **Run red tests:** `./gradlew.bat :26.1.2:test --tests '*ContactTargetCacheTest' --tests '*ContactTargetProxiesTest' --tests '*ContactSceneBoundaryTest' --tests '*EspBodyBoundsTest'`.
- [ ] **Implement scene adapters.** Query only loaded collision shapes and fluid boundaries; count/block bound all per-segment work and never force a chunk load. Use native/visual proxy provenance strings `client_body_v1`, `display_body_v1`, `armor_stand_body_v1`; unavailable models remain unsupported. Keep live tracking/model state read-only and independent of ESP switches. Do not add velocity extrapolation or ESP padding to contact bounds.
- [ ] **Run green tests** plus existing `*ShotObservationBoundaryTest`, `*ShotTargetSamplingTest` and ESP tests affected by extraction. Verify target-only compilation against actual mapped 26.1.2 APIs. Commit as `feat: acquire bounded capsule contact scene`.

### Task 3: Opt-in controls, readable path and distinct contact markers

**Files:** Modify `K/config/ConfigManager.kt`, `SafariSettings.kt`; existing preview rules/frame/adapter/renderer; `K/compat/WorldRenderCompat.kt` only for additive geometry submission. Create `K/safari/shots/preview/contacts/ContactGuideGeometry.kt`. Tests existing preview tests, `T/config/ConfigTest.kt`, and new `T/safari/shots/preview/contacts/ContactGuideGeometryTest.kt`, `ContactGuideLifecycleTest.kt`.

**Consumes:** Task 2 `CapsuleContactScene.snapshot`, Task 1 trace and existing terrain-occluded pipeline.

**Interfaces produced:** Saved properties `ConfigManager.allowUncalibratedCapsuleEstimates: Boolean = false`, `ConfigManager.capsulePathWidth: Float = 3f`; add `allowUncalibratedEstimates: Boolean = false` to `CapsulePreviewState` without changing existing default construction. Add `ContactGuideGeometry.build(trace: ContactTrace, camera: ShotVector, coreWidth: Float): ContactGuideMesh`. Define `ContactGuideMesh(core: List<ContactLine>, outline: List<ContactLine>, marker: List<ContactLine>)` and `ContactLine(from: ShotVector, to: ShotVector, color: Int, width: Float)`. Every position in the mesh is world-space; camera is used only to orient the critter marker, with a deterministic fallback for degenerate view axes. Existing WorldRenderCompat performs the sole camera subtraction. Marker lists contain at most 24 segments. Preserve `CapsulePreview.register/tick/reset/renderHud` entry points. A single immutable frame object supplies line, marker and HUD semantics for the same aim/world snapshot.

- [ ] **Write failing mode/config tests:** old files retain both toggles off, width defaults to 3, clamps to 1..5 and persists; expanded mode accepts pitch -90/+90 only when all other stationary/item/view checks pass. Turning expanded mode off restores the original angle/15-block path and no marker. All exclusions, two stable ticks, screens and HUD rules remain.
- [ ] **Write failing geometry/lifecycle tests:** terrain ring versus critter diamond with radius 0.25 and shape distinction independent of color; core width 3 plus outline width 5, widths finite/clamped; observed curve solid and extrapolated portions dashed; exactly one camera subtraction; no stale marker after aim/world/item changes; deferred submission owns immutable arrays. `UNLOADED`, `FLUID`, `TARGET_COVERAGE` and range-limit states have no misleading endpoint. Pin the absent-endpoint case:

```kotlin
val mesh = ContactGuideGeometry.build(
    ContactTrace(emptyList(), null, ContactStop.RANGE_LIMIT, emptySet()),
    ShotVector(100.0, 10.0, 100.0), 3f)
assertTrue(mesh.marker.isEmpty())
assertTrue(mesh.core.isEmpty())
```
- [ ] **Run red tests:** `./gradlew.bat :26.1.2:test --tests '*CapsulePreview*' --tests '*ContactGuide*' --tests '*ConfigTest'`.
- [ ] **Implement settings and rendering.** UI labels/help match the spec. Use dark outline `0xFF151515`, amber terrain/path `0xFFFFB347`, cyan critter `0xFF55DDEE`; marker radius is appearance only. Both outline/core use existing `WorldDepthMode.OCCLUDED`, no depth writes. Keep all existing default renderType callers through-terrain. Current interpolated aim drives bounded frame solve; never reuse an endpoint across different aim/world data. HUD includes `Uncalibrated` in expanded mode, a contact kind or specific unavailable reason, no repeated chat.
- [ ] **Run green tests** plus `*WorldLineGeometryTest` and relevant config/preview regressions. Commit as `feat: render experimental terrain and critter contacts`.

### Task 4: Versioned per-input contact probes without changing raw evidence

**Files:** Create `K/safari/shots/ShotContactProbe.kt`; modify `ShotData.kt`, `ShotCodec.kt`, `ShotSession.kt`, `ShotRecorder.kt`, `ShotObservationAdapter.kt` and `SparklingMutuals.kt` only as required to enable scene discovery during active recording. Create `docs/schemas/capture-shots-v2.schema.json`; preserve v1 schema. Tests new `T/safari/shots/ShotContactProbeTest.kt`, updates to protocol/recorder/target-sampling/budget tests. Add synthetic real-writer fixture `src/test/resources/shots/contact-probe-v2.jsonl`.

**Consumes:** `ContactSceneSnapshot`; existing accepted `ShotObservation.Input`, session-relative origin, recorder clock, bounded `ShotSession.accept` and existing input-cycle deduplication.

**Interfaces produced:** `ShotObservation.ContactProbe(val probe: ShotContactProbeData)` and the following DTOs in `ShotContactProbe.kt`:

```kotlin
data class ShotProbeTerrain(val fromParameter: Double, val toParameter: Double,
    val position: ShotVector?, val normal: ShotVector?, val block: String?,
    val shapes: List<ShotBounds>, val boundary: ContactStop?)
data class ShotContactProbeData(val shot: String, val model: String, val hypothesis: String,
    val eye: ShotVector, val aim: ShotVector, val pitch: Double, val sourceTick: Long,
    val coverage: ContactCoverage, val nodes: List<ContactNode>, val targets: List<ContactTarget>,
    val terrain: List<ShotProbeTerrain>, val primary: ContactCandidate?, val stop: ContactStop,
    val truncated: Boolean = false, val omittedNodes: Int = 0, val omittedTargets: Int = 0,
    val omittedTerrain: Int = 0)
```

`ShotContactProbe.capture(client: Minecraft, input: ShotObservation.Input, origin: ShotVector): ShotContactProbeData` acquires one input-time snapshot. All positions/bounds are converted to the existing session-relative origin; aim/normal vectors stay vectors. Remap every target key and matching primary reference to `p1`..`p64`, scoped to that probe's shot; these are not raw track tokens and never encode runtime identities. Schema validates closed reason/source values and array limits. New default metadata is schema 2 / recorder 3. Existing `ShotCodec.encode(record: ShotRecord): ByteArray` signature stays unchanged and compacts only oversized contact-probe detail before its 64 KiB check.

- [ ] **Write failing producer tests:** one probe after each accepted ordinary input, not duplicate block-to-air continuation; probe runs with preview off / recorder on, never recorder off; Masterful and unsupported player states do not receive fabricated ordinary predictions. Probe coordinates are relative, runtime identity canaries absent, prediction data cannot alter association/feedback/annotations.
- [ ] **Write failing real-writer tests:** maximum 64 proxies / 256 segments, adversarial UTF-8 detail, finite values, v2 schema, deterministic truncation that preserves shot/result/reasons but marks detail incomplete; no encode error for a compactable probe. In `oversizedProbeCompactsBeforeEncoding`, assert `encoded.size <= 64 * 1024`, parsed `data.probe.truncated == true`, original shot token unchanged and runtime-identity canaries absent. Repeat established 180-second / 20-throw and 600-second / nine-throw populated workloads with one probe per input: both named workloads must still fit the unchanged cap and retain every raw capsule packet. Separate adversarial cap tests must stop explicitly incomplete; do not weaken old raw-retention assertions.
- [ ] **Run red tests:** `./gradlew.bat :26.1.2:test --tests '*ShotContactProbeTest' --tests '*ShotProtocolTest' --tests '*ShotRecorderTest' --tests '*ShotTargetSamplingTest' --tests '*ShotByteBudgetTest'`.
- [ ] **Implement versioned producer.** Capture at most once after input acceptance using the current world/pose; no new gameplay callback return behavior. Extend reducer validation for known shot token and one probe per input. Encoder removes optional terrain/proxy/segment detail deterministically until it fits, retaining explicit omitted counts/truncation; if minimal data cannot encode, normal original-exception diagnostics apply. Do not use broad exception swallowing or silently discard raw events. V1 fixture generation uses explicit schema 1 / recorder 2 metadata.
- [ ] **Run green tests** and produce the v2 fixture through the actual Kotlin controller/session/codec/writer. Commit as `feat: record bounded versioned capsule contact probes`.

### Task 5: Strict v1/v2 replay and offline contact diagnostics

**Files:** Create `tools/capture_shots/contacts.py`, `tools/capture_shots/tests/test_contacts.py`; modify `protocol.py`, `replay.py`, `package_tools.py`, protocol/replay/package tests and tools README. Include both checked schemas and the new synthetic fixture in the package allowlist. Existing `analyze.py` visual-baseline semantics remain unchanged.

**Consumes:** Task 4 checked v2 schema/fixture and legacy v1 fixtures; current immutable `Session`, `plain`, strict finite/duplicate-key/privacy validators and `read_session(path: Path) -> Session`.

**Interfaces produced:** `contacts.summarize(session: Session) -> dict`; CLI `python tools/capture_shots/contacts.py session.jsonl --output new-contact-directory`. Output includes input SHA-256, model/proxy versions, per-shot predicted contact/coverage, raw terminal evidence availability, and separate typed/manual feedback. `endpoint_error` stays null in this increment because the existing outcomes do not supply independently observed endpoint coordinates. Do not infer such coordinates from removal or the last sample. Replay adds a separately named prediction layer; v1 files show no contact probes.

- [ ] **Write failing strict-reader tests:** real Kotlin v2 fixture reads; all valid v1 fixtures still read; wrong header/event combinations, unknown versions, invalid probe tokens, arrays exceeding 64/256 entries, unknown fields, duplicate keys, non-finite values, mismatched shot references and malformed interior lines fail. Truncated probes/files remain flagged.
- [ ] **Write failing contact-report tests:** a capture message does not prove predicted geometric contact; no feedback is not a miss; last observed point/removal is not ground impact; a target behind wall remains excluded; v1 has `contact_prediction_available=false`; output directories are create-new and input hashes unchanged. Package test extracts and runs both schema fixtures with no network/dependencies. For the one-shot real-writer fixture:

```python
session = read_session(Path('src/test/resources/shots/contact-probe-v2.jsonl'))
report = contacts.summarize(session)
self.assertEqual(session.header['metadata']['schemaVersion'], 2)
self.assertIsNone(report['shots'][0]['endpoint_error'])
self.assertEqual(report['input_sha256'], session.input_sha256)
```
- [ ] **Run red tests:** `python -m unittest tools.capture_shots.tests.test_contacts tools.capture_shots.tests.test_protocol tools.capture_shots.tests.test_replay tools.capture_shots.tests.test_package -v`.
- [ ] **Implement version-aware schema dispatch.** Select checked schema by validated integer header version, not global mutation of `SCHEMA`; recursive references resolve against the selected schema. Use new `validate_record(value: dict, schema_version: int) -> None` internally so alternating v1/v2 reads cannot contaminate each other. Keep parser byte/event/line limits and strictly separate probes from tracks/observations.
- [ ] **Run green tests** and `python -m unittest discover -s tools/capture_shots/tests -p "test_*.py" -v`. Package to a new local output directory, run downloaded/extracted tools tests against both versions, and commit as `feat: replay and inspect experimental contact probes`.

### Task 6: Documentation, client validation, review and verified delivery

**Files:** Update `docs/CAPTURE_SHOT_RECORDING.md`, `docs/CONFIG_DEFAULTS.md`, `docs/IMPLEMENTATION_TEST_CHECKLIST.md`, `docs/README.md`, `README.md`, tools README, approved spec/plan status. Keep release workflow/full matrix unchanged; current feature branch CI already targets 26.1.2. Temporary client fixtures and validation receipts belong in workspace outputs, not installable artifacts or the user's instance.

**Consumes:** Tasks 1–5 complete; all earlier regression receipts and spec requirements. Produces exact-final-commit Actions/artifact receipts, concise issue confirmation and a working collection kit with honest pending field validation.

- [ ] **Update user docs and collection commands.** Explain the two toggles, 1..5 width, first estimated contact, dashed launch/wider curve, terrain ring/critter diamond, current-position proxy, unknown fluid/coverage/range and excluded Masterful/movement. Give exact existing label commands: `/sm shots label stationary_up fit`, `stationary_down fit`, `solid_obstruction fit`, `edge_obstruction fit`, `moving_target fit` and corresponding validation labels. Include the spec's six-shot sanity test, fitting matrix and frozen independent validation sequence; do not request all field work before supplying the working build.
- [ ] **Run required target-only checks** with Java 25: `./gradlew.bat :26.1.2:clean :26.1.2:verifyTargetJar`, `./gradlew.bat -p buildSrc test`, and full Python suite. Package tools via `python tools/capture_shots/package_tools.py --output ../../outputs/issue51-contacts-20261017/tools-local`. Expected: successful target build/tests/metadata verification, helper tests and Python tests; inspect actual totals instead of copying historical counts.
- [ ] **Run controlled offline 26.1.2 client checks.** Verify 13 required hooks, defaults/save/reopen and unsupported/disabled states. Use deterministic loaded fixtures for a near-eye wall with target behind, target before wall, inside origin, stairs/slabs, fluid/unloaded boundary, ring/diamond, old ESP depth behavior and actual camera/GUI/FOV/screen/world changes where controllable. Capture screenshots/receipts. Profile the 256-segment / 64-candidate worst case and verify the 256-slot shared tick budget; report measured timings and conditions locally. No claim of live Hypixel accuracy from synthetic fixtures. Remove temporary helpers afterward.
- [ ] **Perform one independent whole-branch review under preserved Native execution.** Use requesting-code-review and the most capable available reviewer as required by the execution workflow, covering the exact implementation range and this spec/plan. Resolve Critical/Important findings with regression evidence and rerun only affected checks. Record deferred minors explicitly; do not redispatch historical deferred minors from prior increments.
- [ ] **Reconcile docs and stage focused changes.** Rerun read-only DocumentationSync, re-read introduced revisions, `git diff --check`, inspect actual branch/worktree and final changed files. No `-Record`. Commit validated code/docs; immediately recheck #51 open before an implementation-stage reply and record its ID/fingerprint. Push only the issue branch.
- [ ] **Verify final uploaded commit and deliver.** Confirm feature branch CI runs only 26.1.2 plus helpers/tools, all applicable jobs succeed on exact HEAD. Download JAR/tools/test artifacts locally, verify GitHub archive SHA-256/JAR metadata, run extracted tools and inspect downloaded tests. Do not substitute an earlier commit's run. If #51 still open, reply linking source proposal, implementation commit, run and downloads; state implemented behavior and pending Hypixel field tests. Save reply ID/body fingerprint and durable history. Provide the 26.1.2 JAR link and concise version notes in chat. Leave the issue open for field calibration; no PR/merge/release.

## Execution setup and review completion

Before Task 1, read the approved spec and this plan fully, current applicable instructions and relevant changed Markdown. Use the existing named feature checkout if clean and suitably isolated; preserve unrelated changes and never reset them. Establish Java 25, target 26.1.2 and workspace Gradle cache without reading authentication files. Start a local Native execution ledger outside the repository; track red/green receipts and task commits.

Plan self-review maps every spec section to Tasks 1–6: settings/lifecycle/appearance→3; curve/launch/ordering→1; shapes/fluid/loaded coverage/proxies→2; reproducible producer→4; strict consumer/diagnostics→5; collection/verification/delivery→6. All five Review Focus risks have owning regressions. Required interfaces are defined above; runtime-only DTO details must preserve these contracts rather than invent cross-task names.

The human has already selected Native execution. After this plan's review approval, use `superpowers:executing-plans`; do not begin product implementation before that review. Questions and review requests go on the still-open #51 under the latest routing instruction. Automatic monitoring remains paused.

## Last updated

2026-10-17 (America/New_York) — Prepared the six-task Native implementation plan after direct written-specification approval. Approved directly in chat and implemented natively; software validation and field calibration are tracked separately.

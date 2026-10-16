# Experimental Capsule Path Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a default-off ordinary-capsule world-path preview limited to the approved stationary first-person experiment.

**Architecture:** A pure fixed model generates a bounded spatial curve; separate eligibility rules control availability and stationary stability. A passive client adapter connects configuration, tick/world lifecycle, depth-tested world rendering and one quiet HUD status. Existing tracking, ESP and recorder behavior remain independent.

**Tech Stack:** Existing Kotlin 2.4.20, Java 25, Fabric/Stonecutter and JUnit setup; Minecraft 26.1.2, 26.2 and 26.3. No new runtime dependency or recording format.

**Spec:** [Approved preview specification](../specs/2026-10-16-capture-shot-preview-design.md), initially committed at `130f731f0f40051a82882a444f8a4ec1c4d68f6a`. The human approved it in chat. The human approved this plan in chat and chose **Native** implementation.

## Global Constraints

- Work on `feature/capture-shot-projection`; preserve unrelated changes. Do not modify master, merge, create a PR, publish a release or bump version 3.3.0.
- Exact targets 26.1.2, 26.2 and 26.3; Java 25; pinned dependencies; active editor target 26.1.2 before commits.
- No `.devAuth` access, automatic recording/upload, raw observation fixtures, credentials, unrelated logs, model refitting, aiming, firing or target selection.
- Preview is off by default. Main-hand ordinary capsule only, loaded Safari, first person, on-ground, two stable client ticks, pitch inclusive `[-10, +1]` degrees; both capsule-hiding settings off.
- Stable position change is at most `0.001` blocks per axis and horizontal speed at most `0.001` blocks/tick. Suppress sneak, sprint, swimming, flight, riding, jumping/falling, screens and hidden HUD.
- Fixed parameters: speed `0.8482482249942948`, vertical acceleration `-0.020085553311180723`. Parameter `n` is not server time. Sample exactly 64 finite points from `n=2` through `n=15/speed`, at most 63 segments.
- Default amber ARGB `0xFFFFB347`, fixed line width `2f`. Omit the optional color editor in this first delivery; no exposed physics controls.
- Own depth-tested pipeline, no depth writes. Preserve existing ESP pipeline defaults. No collision clipping, endpoint/impact marker, target highlighting or capture success claim.
- World/connection changes clear transient stability immediately. Setting persistence does not start the recorder or reset existing tracking.
- All approvals go to this chat until the human changes that instruction. Scheduled monitoring remains paused. Keep raw observation evidence local.
- Read-only documentation sync before/after; do not record its shared baseline while unrelated revisions/proposals remain unresolved.

## Review Focus

1. A camera teleport/world change or delayed render submission must not draw stale geometry in a new frame/world (Tasks 2–3).
2. An unsupported held item, view, pitch or movement state must suppress the line immediately, even between tick updates (Tasks 2–3).
3. Turning the preview on must not change existing see-through ESP pipelines, capture handling or recorder operation (Tasks 3–4).
4. Invalid/non-finite input, zero aim, coincident points or float conversion overflow must not submit invalid vertices/normals (Tasks 1 and 3).
5. Missing/invalid saved config must default off, while a deliberate saved toggle survives reopening/settings restart (Task 2).

## Files and interface conventions

`K/` means `src/main/kotlin/net/johnceo/sparklingmutuals/`; `T/` means its mirrored `src/test/kotlin/net/johnceo/sparklingmutuals/`. All listed paths below those roots are exact. New preview code lives together in `safari/shots/preview/`. Reuse the existing immutable `safari/shots/ShotVector` and `ShotVariant` types; do not reuse or alter recorder session state.

Sequence: **1 → 2 → 3 → 4**. Tasks 1–3 each end with a focused testable deliverable; Task 4 verifies and delivers their combined runtime behavior.

### Task 1: Fixed spatial model with bounded finite geometry

**Files:** Create `K/safari/shots/preview/CapsulePathModel.kt`; create `T/safari/shots/preview/CapsulePathModelTest.kt`.

**Interfaces:** `object CapsulePathModel` exposes `const val MODEL_ID = "ordinary-stationary-visual-ordinal-candidate-v1"`, `const val SPEED`, `const val VERTICAL_ACCELERATION`, `const val POINT_COUNT = 64`, `val endParameter: Double`, `fun point(eye: ShotVector, aim: ShotVector, parameter: Double): ShotVector?`, and `fun points(eye: ShotVector, aim: ShotVector): List<ShotVector>`. Normalize a finite nonzero aim. Reject parameters outside `[2, endParameter]`, non-finite inputs/results and zero aim; invalid paths return an empty list. No clipping or time simulation.

- [ ] **Step 1 — Write failing model tests.** Use literal independently calculated examples, translated origins and scaled nonzero aim. Check exactly 64 points, both endpoints, all finite, monotonically increasing parameter/range and invalid inputs. Review Focus 4: NaN/infinity/zero aim cannot reach geometry; a huge finite origin that cannot remain safely renderable must be rejected at the render boundary in Task 3.

```kotlin
@Test fun `first observed parameter matches fixed spatial curve`() {
    val p = CapsulePathModel.point(ShotVector(1.0, 2.0, 3.0), ShotVector(1.0, 0.0, 0.0), 2.0)!!
    assertEquals(2.6964964499885896, p.x, 1e-12)
    assertEquals(1.9799144466888192, p.y, 1e-12)
    assertEquals(3.0, p.z, 1e-12)
}
```
- [ ] **Step 2 — Run red.** `./gradlew.bat :26.1.2:test --tests '*preview.CapsulePathModelTest' --no-parallel`; expect missing model/API or failed new assertions.
- [ ] **Step 3 — Implement the pure model.** Copy the exact reviewed constants/formula. Generate uniform parameter samples including both endpoints; no model/file load or world access. Keep returned point lists detached from mutable scratch state.
- [ ] **Step 4 — Run green.** Repeat the focused suite, independently check the last point's nominal range is 15, and verify local frozen coefficients still match the constants. Never add private raw recordings to tests.
- [ ] **Step 5 — Commit.** Commit only model and its tests as `Add fixed experimental capsule path model`.

### Task 2: Availability, stability and persistent opt-in control

**Files:** Create `K/safari/shots/preview/CapsulePreviewRules.kt`; modify `K/config/ConfigManager.kt` and `K/config/SafariSettings.kt`; create `T/safari/shots/preview/CapsulePreviewRulesTest.kt`; extend `T/config/ConfigTest.kt`.

**Interfaces:**

- Immutable `CapsulePreviewState` contains `enabled`, `inSafari`, `ordinaryMainHand`, `firstPerson`, `onGround`, `sneaking`, `sprinting`, `swimming`, `flying`, `riding`, `hideFlying`, `hideGround`, `screenOpen`, `hudHidden` booleans, `pitch: Double`, `position: ShotVector` and `velocity: ShotVector`.
- `enum class CapsulePreviewAvailability` has `HIDDEN`, `STAND_STILL`, `TESTED_ANGLE_ONLY`, `VIEW_UNAVAILABLE`, `HIDING_UNAVAILABLE`, `AVAILABLE`. `HIDDEN` covers disabled/outside Safari/unsupported item/missing state/screens/hidden HUD and produces no status.
- `class CapsulePreviewRules` exposes `tick(worldIdentity: Any?, state: CapsulePreviewState?): CapsulePreviewAvailability`, `evaluate(state: CapsulePreviewState?): CapsulePreviewAvailability`, and `reset(): Unit`. Tick compares world identity and positions, advances at most one stability observation per client tick, and stores no Minecraft entity. `evaluate` never advances stability; it immediately rechecks all current restrictions.
- Add `ConfigManager.experimentalCapsulePath = false` to the existing boolean flag map (therefore using existing load/save). Add `SafariSettings.Safari.experimentalCapsulePath` with `ConfigOption` name **Experimental capsule path**, help **Experimental visual guide. Terrain collision and capture targeting are unverified.**, and include it in `SafariSettings.apply`'s changes list.

- [ ] **Step 1 — Write failing rule/config tests.** Pin pitch `-10`/`+1` inclusive and neighboring values unavailable; default/invalid config off and true survives save/reload. Assert two stable tick observations are required and `evaluate` cannot advance them. Standing vertical gravity velocity alone does not reject an on-ground player. Review Focus 1/2/5: world replacement/null state/reset clears stability, changing view/item/movement between ticks suppresses immediately, and a saved toggle does not turn on recording.

```kotlin
@Test fun `render evaluation cannot create stationary history`() {
    val rules = CapsulePreviewRules()
    val state = eligibleState() // Test fixture explicitly sets every eligibility field.
    repeat(20) { assertNotEquals(CapsulePreviewAvailability.AVAILABLE, rules.evaluate(state)) }
    val world = Any()
    rules.tick(world, state)
    assertNotEquals(CapsulePreviewAvailability.AVAILABLE, rules.evaluate(state))
    rules.tick(world, state)
    assertEquals(CapsulePreviewAvailability.AVAILABLE, rules.evaluate(state))
}
```
- [ ] **Step 2 — Run red.** `./gradlew.bat :26.1.2:test --tests '*preview.CapsulePreviewRulesTest' --tests '*config.ConfigTest' --no-parallel`.
- [ ] **Step 3 — Implement rules and binding.** Define eligibility precedence as hidden conditions, view/hiding restrictions, stationary restrictions, angle restriction, then available. Reset stable history on world/state loss and every physical ineligible state; GUI/status changes may suppress the frame but must never count render evaluations as ticks. Add only the one boolean control; no optional color or physics controls.
- [ ] **Step 4 — Run green.** Repeat the suites; cover every restriction and threshold, position drift on each axis, horizontal velocity, stable same-world reset and settings application. Restore shared config values in tests to avoid suite order dependence.
- [ ] **Step 5 — Commit.** `Add conservative capsule preview availability and setting`.

### Task 3: Isolated depth-tested renderer and passive integration

**Files:** Create `K/compat/WorldLineGeometry.kt`, `K/safari/shots/preview/CapsulePreview.kt`, `CapsulePreviewRenderer.kt`, `CapsulePreviewFrameBuilder.kt`; modify `K/compat/WorldRenderCompat.kt` and `K/SparklingMutuals.kt`; create `T/compat/WorldLineGeometryTest.kt` and `T/safari/shots/preview/CapsulePreviewFrameTest.kt`.

**Interfaces:**

- `enum class WorldDepthMode { THROUGH_TERRAIN, OCCLUDED }`. Extend `WorldRenderCompat.renderType(name, filled, cull, depth: WorldDepthMode = THROUGH_TERRAIN)` additively; existing calls retain their behavior. Map OCCLUDED to the target API's ordinary less/equal depth comparison with depth writes disabled. Inspect actual mapped API names before compiling; do not guess names across 26.3's package split.
- `data class RenderLine(val from: Vec3, val to: Vec3, val normal: Vec3)`; pure `WorldLineGeometry.segments(worldPoints: List<Vec3>, camera: Vec3): List<RenderLine>` snapshots no more than 64 points, subtracts the camera once, rejects non-finite or unsafe float results, skips coincident segments, and calculates a finite unit direction/normal for every emitted segment.
- `WorldRenderCompat.polyline(context: LevelRenderContext, type: RenderType, worldPoints: List<Vec3>, color: Int, width: Float = 2f): Unit` obtains camera state once, builds detached camera-relative segments before deferred submission, then uses the existing private geometry/flush lifecycle. A closure must not read later-frame points/camera. No second camera subtraction inside emission.
- `data class CapsulePreviewFrame(val points: List<ShotVector>, val status: String?)`; pure `CapsulePreviewFrameBuilder.build(availability: CapsulePreviewAvailability, eye: ShotVector, aim: ShotVector): CapsulePreviewFrame` maps HIDDEN to empty/null, unavailable states to empty points plus the stated reason, and AVAILABLE to Task 1's detached bounded points plus **Experimental path**. Invalid available geometry instead returns empty points with **Capsule path unavailable**.
- `object CapsulePreview` exposes `register(): Unit`, `tick(client: Minecraft): Unit`, `reset(): Unit`, `renderHud(graphics: GuiGraphicsExtractor): Unit`, `internal fun currentState(client: Minecraft): CapsulePreviewState?`, and `internal fun availability(state: CapsulePreviewState?): CapsulePreviewAvailability`; it owns the rules and a narrow immutable state snapshot adapter. `object CapsulePreviewRenderer` exposes `render(context: LevelRenderContext): Unit`, uses the current frame's interpolated eye/look, rechecks current eligibility, builds the frame, and passes its points to the isolated OCCLUDED render type.
- Add one feature registration, one end-tick call and one reset call in `SparklingMutuals`. Call `renderHud` in its existing HUD callback. No item-use/mixin/recorder callback is added or changed.

- [ ] **Step 1 — Write failing geometry/frame tests.** Review Focus 1/4: translated-camera inputs produce exactly one subtraction; zero-length segments vanish; NaN/infinity/overflow produces no invalid vertices; 64 points yield at most 63 segments. Mutating the caller's list after capture must not change captured segments. World change/ineligible current view produces no preview frame. Test every availability-to-status mapping and invalid geometry fallback. Review Focus 3's actual depth/ESP preservation check is the controlled-scene regression in Task 4, rather than a test merely repeating enum constants.

```kotlin
@Test fun `camera subtraction happens once`() {
    val lines = WorldLineGeometry.segments(listOf(Vec3(101.0, 2.0, 3.0), Vec3(102.0, 2.0, 3.0)), Vec3(100.0, 0.0, 0.0))
    assertEquals(Vec3(1.0, 2.0, 3.0), lines.single().from)
    assertEquals(Vec3(2.0, 2.0, 3.0), lines.single().to)
    assertEquals(Vec3(1.0, 0.0, 0.0), lines.single().normal)
}
```
- [ ] **Step 2 — Run red.** `./gradlew.bat :26.1.2:test --tests '*compat.WorldLineGeometryTest' --tests '*preview.CapsulePreviewFrameTest' --no-parallel`.
- [ ] **Step 3 — Implement the compatibility helper and integration.** Reuse `WorldRenderCompat`'s existing version-specific registration/submission hooks. Register one preview pipeline, use fixed amber/2f, no depth writes, and avoid per-frame render-type creation. Current player state must be rechecked without caching a stale available result. Draw the short status horizontally centered 18 GUI pixels below the crosshair, with font shadow, only when the spec permits status. Map enum reasons to the spec's copy; add **Disable capsule hiding for preview** for HIDING_UNAVAILABLE. Use existing `smScreen`, `smHideGui`, `smCamera` and mapped interpolation APIs.
- [ ] **Step 4 — Run green and integration checks.** Repeat focused suites plus existing `ShotRecordingIsolationTest` and `ShotRecorderTest`. Verify renderer code has no target/entity scan, file I/O, input callback or capture-memory mutation. Ensure `register()` is invoked once; ticks/reset/HUD calls are separate and no path can survive a new world.
- [ ] **Step 5 — Commit.** `Render experimental capsule path with isolated depth-tested geometry`.

### Task 4: Three-target verification, documentation and reviewable delivery

**Files:** Update `docs/CAPTURE_SHOT_RECORDING.md`, `docs/CONFIG_DEFAULTS.md`, `docs/IMPLEMENTATION_TEST_CHECKLIST.md`, `docs/README.md` and the current preview specification/plan status when achieved. Save local check/client/artifact receipts under workspace `outputs/`, outside the repository. Do not add release notes to repository docs.

**Interfaces:** Consumes Tasks 1–3's complete preview. Produces tested three-target artifacts, a concise enable/test guide and an honest list of remaining Hypixel visual/server limitations. The issue remains open for broader mechanics and endpoint work.

- [ ] **Step 1 — Reconcile docs and verification cases.** Document the option, exact scope, initial unobserved segment, experimental status, movement/view/angle/hiding restrictions and no-impact-marker meaning. Add live checks for terrain depth occlusion, camera turn/FOV/GUI size, HUD/screens, holding/hand changes, stop/start movement, world transfer and disabled setting. Preserve prior recorder evidence distinctions.
- [ ] **Step 2 — Run the required matrix.** With Java 25, run `./gradlew.bat clean buildAllTargets` and `python -m unittest discover -s tools/capture_shots/tests -p "test_*.py" -v`. Require all target tests/build/distribution checks and helper tests to pass. Inspect actual test results; do not copy historical counts.
- [ ] **Step 3 — Check clients on all three targets.** Use the documented `-PclientSmoke` setup without reading authentication files. Verify required hooks/startup, setting discovery/save/reopen, no geometry while disabled, and controlled finite geometry. In a local scene, put the preview line and an existing ESP outline behind the same solid obstacle: the line must be occluded and the original ESP must retain its prior through-terrain behavior. This is Review Focus 3's regression; if a scene cannot be exercised, record it as unverified rather than substituting an enum-value assertion. Recheck recording/ESP before and after toggle; perform live Hypixel visual tests only when available. Record startup/controlled-scene checks separately from server accuracy.
- [ ] **Step 4 — Request independent review.** Native implementation ends with one independent whole-change review under the review skill. Address material findings with regression coverage and rerun affected checks; repeat the full matrix only if changes justify it. Confirm only approved scope is implemented and preview-off tracking is unchanged.
- [ ] **Step 5 — Finish delivery.** Recheck docs and branch/tree, commit the final corrections/docs, push only the issue branch when authorized delivery is ready, verify Actions on that exact commit, download/verify all three JARs and relevant tools/test artifacts. Do not upload raw observations or private analysis. Provide version notes, download links and a small runtime preview test guide in this chat. Recheck issue #51 is open before any permitted issue reply; route approval requests here. Do not merge, create a PR or publish a release. Keep scheduled monitoring paused.

## Self-review and handoff

The four tasks cover model math, every eligibility/config restriction, immutable bounded rendering and isolated depth behavior, lifecycle/HUD integration, all-target verification and delivery. Review Focus cases map to explicit tests above. No new world scan, collision solver, time-based flight animation, packet observer, endpoint marker or Masterful extrapolation is part of this plan.

The spec's optional color control is intentionally omitted; fixed amber and 2f width are sufficient for this experiment. The HUD placement and exact unavailable copy are implementation decisions fixed here for review. Existing public ESP defaults and saved flags are preserved. Execution is **Native**, explicitly approved in chat. Tasks 1–3 are implemented; final runtime checks, independent review and delivery are tracked in the execution ledger.

## Last updated

2026-10-16 (America/New_York) — Recorded plan approval and Native implementation of the model, availability, opt-in control and renderer. Three-target clean builds and replay tests pass; runtime/review/delivery checks are in progress.

## Human execution update

The human subsequently directed capture-shot development to build and deliver **26.1.2 only** until the feature is finished. This overrides the three-target production/check steps above for ongoing development. Preserve the compatibility sources and full release matrix; do not produce 26.2/26.3 artifacts now. The earlier clean three-target matrix completed before this instruction.

# Experimental ordinary-capsule path preview

**Status: written specification approved by the human in the implementation chat.** After reviewing this document, the human answered **Approve the written specification**. The implementation plan is the next review stage; product implementation has not started. Approval requests stay in the chat until the human changes that instruction. Execution preference is Native.

## Purpose and evidence boundary

Show a short world-aligned guide following the player's current look direction before an ordinary capsule throw. This first preview is opt-in and limited to observed stationary first-person conditions. It helps compare the predicted capsule-center curve with visible gameplay. It does not choose a target, aim, fire or promise a capture.

The existing recorder/replay delivery remains independent. A separately frozen local model has been compared with new stationary ordinary-capsule observations using an agreed numeric criterion: 95th-percentile 3D position error at most 0.25 blocks and no observed error above 0.5 blocks in the nominal 5–15-block region. Passing that narrow observation check does not establish server timing, collision, target selection, impact or rendered appearance. Raw association flags remain unchanged; the human's statement that they alone were throwing is separate research curation context.

No raw recordings, logs, player identities or credentials belong in the repository. Keep the local analysis and source reading history in the existing workspace outputs. Compile the reviewed fixed model parameters into pure Kotlin; never open a recording or fit a model during gameplay. Do not present the sample count as a statistical accuracy guarantee.

Work remains on `feature/capture-shot-projection`, initially based on `693058d1f9a74b8d0466faaf6c66f81b03f69dd3`. No PR, merge, master change, release, version bump or global documentation-baseline recording is included.

## User-facing behavior

- Add **Experimental capsule path**, default **off**, to Safari helper settings. Persist the toggle through the existing properties/config conventions. An additional resettable color option may use the existing color editor; use a restrained amber default and a fixed readable width. Do not add arbitrary curve-strength, launch-speed or gravity controls.
- With the option enabled, draw only in a loaded Safari world while the local player holds an ordinary capsule in the main hand, uses first person, is standing still on the ground, and looks within the conservative preview angle interval **10 degrees upward to 1 degree downward** (`pitch` from `-10` through `+1`, inclusive).
- Require two consecutive stable client ticks before showing a path after movement stops. Stability requires position change at most `0.001` blocks on every axis and horizontal velocity at most `0.001` blocks/tick. Clear stability on world replacement, disconnect or an ineligible player state. Ordinary standing gravity velocity must not itself make an on-ground player ineligible.
- Suppress the path while walking, jumping, falling, flying, swimming, riding, sneaking, sprinting, using third person, holding a Masterful/unsupported item, or using the off hand alone. Both capsule-hiding settings must remain off for this initial preview. These restrictions express the current evidence boundary.
- Suppress world geometry while a screen is open or the HUD is hidden. No path survives a disconnect or world replacement. The option itself remains saved.
- When enabled, in Safari, and holding a supported ordinary main-hand capsule, a quiet local HUD status reads **Experimental path** while available. If otherwise ineligible, show one useful reason, such as **Stand still for capsule path**, **Look within tested angle**, or **Capsule path unavailable in this view**. Render status through the existing HUD event path and honor hidden HUD/screens. Do not emit repeated chat messages, expose model internals or add party/network traffic.

The path ends at its short display limit. There is **no endpoint circle, impact marker, target highlight, success color or capture probability**. A curve meeting a target or terrain is not advertised as a hit. Settings help states: **Experimental visual guide. Terrain collision and capture targeting are unverified.** The player continues to control every aim and throw.

## Model and geometry

Use the fixed ordinary stationary visual-ordinal candidate, versioned independently from the recorder schema:

```text
speed = 0.8482482249942948
verticalAcceleration = -0.020085553311180723
p(n) = eye + speed * n * normalizedAim
       + (0, 0.5 * verticalAcceleration * n * (n - 1), 0)
```

`n` parameterizes the observed position-update sequence. It is **not server time**. The first position-sync observation corresponded to ordinal two; the spawn observation was excluded from the curve fit. Do not convert the coefficients into advertised blocks/second, gravity or a tick-based flight-time animation.

Draw from **`n = 2`** to **`n = 15 / speed`**. Thus the unseen spawn-to-first-update segment is not invented, the far limit is nominal 15 blocks, and the human-approved accuracy region remains nominal 5–15 blocks. The guide before five blocks is a visual connection supported by observations, not an extension of the agreed accuracy claim. Do not extrapolate to a longer range or wider angle when unavailable.

Sample at most 64 points, including both ends, at uniform parameter intervals. Produce only finite coordinates. Reject an invalid/non-finite input or degenerate aim before geometry submission. Use the same interpolated eye position and current interpolated look direction for one frame. Normalize once. World coordinates are converted relative to the current render camera exactly once; avoid combining a camera-relative origin with another camera subtraction.

The model is a spatial guide, not a moving projectile simulation. It must not use recent observed capsule locations, capture messages, candidate target positions, or recorder state to initialize or tune the path. Existing ESP capture memory remains untouched.

## Rendering and visibility

Introduce a small prediction package under `safari/shots/` with three separate responsibilities:

| Unit | Responsibility |
| --- | --- |
| Pure model and bounded path builder | Fixed coefficients, finite inputs, normalized aim, range-limited point generation and model identity |
| Pure eligibility/state rules plus client adapter | Main-hand capsule recognition, stationary stability, world/view/angle/settings checks and a single meaningful status |
| Preview renderer | Submit the bounded camera-relative line strip and status using existing rendering/HUD compatibility hooks |

Reuse the target-specific `WorldRenderCompat.register` hook and its existing submission lifecycle. Add an additive polyline/segment helper if needed. Preserve existing boxes, labels, render-type defaults and ESP visibility. Avoid putting prediction logic into `SafariEsp`, recorder callbacks or target scanning.

Existing ESP pipelines deliberately render through terrain. The preview needs its **own depth-tested line pipeline** using the relevant normal depth comparison and no depth writes. Add an explicit opt-in parameter/helper for that pipeline rather than changing existing `renderType` callers' behavior. Verify the actual 26.1.2, 26.2 and 26.3 APIs before selecting enum names and submission arguments.

World depth occlusion hides portions behind visible terrain. It is **not collision simulation or path clipping**: do not stop the mathematical curve at a block ray intersection, infer an impact location, or load chunks. Do not draw the guide through terrain with a see-through ESP pipeline. Respect normal camera/frustum clipping; do not apply a screen-space projection to world vertices or create behind-camera HUD points.

Bound work to at most 63 line segments and one status string per eligible frame. No entity/target scan, world traversal, block-shape intersection loop, file access, packet mutation or recording is required. Capture immutable point data for delayed submission and avoid sharing mutable per-frame arrays with a later render callback.

## Integration and lifecycle

- Register preview rendering once through feature registration, without registering another item-use callback or changing any callback result.
- Route the status through existing HUD registration. Use the config and localization style already present; status is independent from recorder on/off.
- Add a saved config property and settings binding with backward-compatible default off. Reset transient preview state on session reset/world identity change. Do not reset existing tracking, ESP, capture memory or recorder state.
- Recognize the ordinary custom item ID through existing pure item classification where possible. Main-hand eligibility must not mutate the stack or copy/export unrelated item data.
- Restore the active editor target to 26.1.2 before committing. Keep compatibility branches small and limited to actual API differences.

## Failures and unsupported conditions

Invalid input or missing world/player/render data yields unavailable geometry and an appropriate status where one can be rendered. It must not crash the client, leave a stale line from a previous frame or silently fall back to an untested straight ray. Catch only expected boundary failures; do not conceal renderer programming errors behind broad exception handling.

The fixed coefficients and model ID are immutable at runtime. A change requires separate fitting evidence, a new frozen model version and fresh validation. Current observations do not authorize widening the preview scope, adding Masterful constants or inheriting player movement.

## Required verification

1. Pure Kotlin model tests use independently calculated numeric examples, including translated origins, normalization, first/last parameters, bounded point count, invalid inputs, and the frozen model coefficients. Synthetic tests prove arithmetic, not Hypixel behavior.
2. Eligibility tests cover movement/stability transitions, every restricted state, pitch boundaries, main/off hand, supported/unsupported capsule IDs, settings defaults/persistence, hide settings, missing player/world, world replacement and disconnect. Existing recorder and capture/ESP behavior remains unchanged.
3. Render geometry tests cover one camera subtraction, finite nondegenerate segments, unit normals where required, stable captured submissions, point/segment bounds and isolated depth-test selection. Preserve old ESP pipeline defaults with a meaningful regression check.
4. Run the documented `clean buildAllTargets`, Python tools and build-helper checks. Verify packaged target metadata, required hooks and startup on 26.1.2, 26.2 and 26.3. After upload, verify Actions on the exact final implementation commit and download/check the matching artifacts. No claims of new test counts are made by this specification.
5. Offline clients on all three targets verify option discovery/persistence, feature registration, no path when disabled, no startup exceptions and controlled geometry where possible. These do not certify Hypixel appearance.
6. Provide the working opt-in preview for live visual comparison. Check camera turn/FOV/window/GUI scale, HUD hide/screens, stationary-to-moving transitions, angle boundary, world transfer, terrain occlusion and comparison with ordinary visible capsule centers. Explicitly report which runtime checks remain unavailable. Neither a clean build nor the numeric raw-position check certifies display interpolation or collision.

## Delivery and approval boundaries

This specification proposes a first experimental geometry preview. It does not complete the full capture-shot issue or its target/endpoint requirement. Broader angles, moving players, Masterful capsules, hiding-enabled fidelity, off-hand/third-person behavior and collision/target-selection models remain separate evidence work.

After the human reviews this written specification in chat, prepare a concrete Native implementation plan for review. Do not begin product implementation before the required plan review. Retain the original recordings and fixed model unchanged; no upload of recordings/logs is included. Keep scheduled checks paused unless the human explicitly resumes them.

## Last updated

2026-10-16 (America/New_York) — Defined the first conservative, opt-in path preview and recorded written-specification approval. Implementation-plan review remains pending.

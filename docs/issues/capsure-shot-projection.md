# Feature: capsure shot projection

## Goal and branch

Investigate how Critter Capsule shooting works and implement a live aiming projection showing where the player's current shot is expected to travel and hit. The guide should update as the player looks around, before firing, while holding a supported capsule in Safari.

Create and work on branch `feature/capsure-shot-projection`, following the requested `feature/<feature>` naming convention. The requested feature title is **Feature: capsure shot projection**; Git does not permit its spaces or colon in a branch name, so use the valid branch name above. Preserve unrelated work and do not reset the working tree. Follow applicable repository instructions.

This issue includes investigation and implementation. If the mechanics cannot be determined from available code and observations, implement tools to collect the missing data and ask the maintainer for specific measurements. Do not invent a trajectory and present it as accurate, or stop at asking for videos without providing collection support.

## Repository starting points

Verify these paths and behavior on the implementation branch:

- `src/main/kotlin/net/johnceo/sparklingmutuals/safari/CritterCapsuleHider.kt` and `CritterCapsuleRules.kt`: recognize flying capsules as `Display.ItemDisplay`, dropped capsules as `ItemEntity`, and item IDs `CRITTER_CAPSULE` / `MASTERFUL_CRITTER_CAPSULE`. Recognition for hiding does not establish that the display entity represents authoritative shot physics.
- `safari/SafariEsp.kt`: existing item-use callback, critter observations, and world rendering hooks.
- `safari/SafariEspRules.kt`: `EspCaptureMemory` freezes aim at item use and associates later server messages with capture attempts. Preserve its behavior; do not equate that heuristic association with proven projectile ownership or collision.
- `mixin/SafariCapsuleMixin.kt`: hides capsule rendering. Recording must work when capsule hiding is enabled.
- `safari/EspRenderWork.kt`, `safari/BeeNests.kt`, and `safari/TeammateEsp.kt`: existing rendering patterns.
- `SparklingMutuals.kt`: feature registration, tick, HUD, and disconnect lifecycle.
- `config/ConfigManager.kt` and `config/SafariSettings.kt`: persistent settings and configuration UI.

Except for the first full path, paths above are relative to `src/main/kotlin/net/johnceo/sparklingmutuals/`. Check current Minecraft/Fabric versions and any existing multi-version build setup before choosing APIs.

## Phase 1: determine the mechanics

Trace item input, capsule identification, relevant entity updates, and throw/capture/escape messages. Establish what the client can actually observe and what remains server-side. Determine whether shots use ballistic motion, a straight ray, homing, target selection at use time, a cosmetic animation, or another mechanism. Do not assume vanilla snowball/arrow physics or assume the server-visible animation determines capture logic.

Investigate, where relevant:

- Shot origin versus camera/eye position, yaw/pitch, launch speed, gravity, drag, range, lifetime, and inherited player motion.
- Differences between ordinary and Masterful capsules and any other verified modifiers.
- Item-display interpolation/transforms versus raw entity positions and network update timing.
- Block obstruction, critter collision/selection rules, and whether visible models match actual capture hitboxes.
- Delay between input, first observable shot, target feedback, and outcome; effects of latency and server ticks.
- Reliable association of a locally fired shot with its observed entities and messages, including rapid shots and other players firing nearby.

Document confirmed facts, fitted parameters, uncertainty, and unsupported cases separately. Cite any external research used and respect upstream licenses. Choose the simplest model supported by evidence; a straight targeting ray is valid if that is how the mechanics work.

## Phase 2: facilitate manual data collection when needed

Build an opt-in local calibration recorder before requesting measurements that require it. Provide discoverable start/stop/status/export controls using the existing command/config conventions. Export structured JSON or CSV plus a schema/model version to a documented local directory. Bound recording duration, sampling, and file size; capture relevant shot data rather than dumping all traffic or chat.

Record, where observable:

- Session/shot identifiers, mod/game version, capsule type, and timestamp/tick/time basis.
- Player position, eye/launch origin, yaw/pitch, movement, and item-use time.
- Candidate capsule entity identifiers, raw position updates and their times, relevant display transforms/interpolation settings, and removal time/reason when available.
- Candidate target species, positions, motion and bounds, relevant obstruction information, and attributable throw/capture/escape feedback.
- Observed outcome versus predicted path/impact, association confidence, and a manual annotation for uncertain or missing outcomes.
- Available latency context, explicitly identifying estimates. Do not claim a client timestamp is the authoritative server firing time.

Do not collect credentials, tokens, unrelated chat, or unnecessary player identity. Provide a way to inspect/export the relevant session without uploading automatically. Unknown ownership or lost entities must remain unknown rather than become fabricated misses or captures.

If user input is needed, request an exact collection checklist through the implementation conversation and issue notes: the missing question, recorder command, setup, capsule types, distances, aim angles, movement states, obstruction cases, repetitions, output location, and files to return. Include flat and upward/downward shots, stationary and moving player/target cases, and multiple ranges as relevant to the hypothesis. Explain how the measurements distinguish competing models and define what is sufficient to proceed. Minimize manual transcription; collect available values automatically.

Provide a reproducible analysis/replay tool that reads exported observations, compares predicted versus observed trajectories/endpoints, and reports error and excluded/ambiguous samples. Use separate fitting and validation shots. Agree on a useful accuracy tolerance in blocks and relevant distance ranges based on measured mechanics before declaring the model validated. If measurements require maintainer gameplay, leave a concrete usable recorder and instructions, and report the feature as awaiting calibration rather than fully complete.

## Phase 3: render the aiming projection

- While enabled, in Safari, and holding a supported capsule, continuously draw the predicted path for the current aim and an endpoint/impact marker when an endpoint can be estimated. Use world-aligned rendering projected through the current camera so the path stays aligned on screen while turning or moving.
- Update with appropriate interpolation. Handle camera/eye versus launch origin correctly and document third-person behavior. Clip or suppress points behind the camera and handle GUI scale, FOV, resizing, and HUD visibility correctly where screen-space elements are used.
- Show the earliest predicted obstruction or target intersection only where the collision/selection model supports it. If the server mechanic selects a target independently of the visible flight, represent that distinction honestly.
- Indicate uncertain, uncalibrated, out-of-range, or unavailable predictions clearly; do not label a predicted intersection as a guaranteed capture. Target motion prediction is optional unless required by the verified mechanics; label its limitations.
- Add an enable setting and practical appearance/range controls using existing UI conventions. Avoid unbounded world scans or per-frame file I/O. Keep recording opt-in and separate from normal rendering.
- Clear transient shot/target state on world changes and disconnects. Respect existing capsule hiding, ESP, capture tracking, and mode behavior.
- Keep the feature an aiming aid controlled by the player's current look direction; firing and aiming remain player actions.

## Acceptance criteria and validation

1. Work is on the specified valid branch, with a documented mechanics investigation and evidence supporting the chosen prediction model.
2. A live projection follows current aim while holding a supported capsule and stops when disabled, outside Safari, or holding an unsupported item.
3. Projection behavior covers tested aim angles, distances, movement, and obstacles, with explicit uncertainty for unverified cases. No unsupported assumption of vanilla projectile physics or guaranteed capture.
4. Recording can be started/stopped/exported and captures useful shot observations even when flying capsule visuals are hidden. Ambiguous nearby-player shots, rapid shots, and incomplete observations are handled conservatively.
5. If data is missing, the maintainer receives a precise collection request and working recorder/replay tooling; unresolved calibration is clearly identified.
6. Automated tests cover model math and applicable collision bounds, shot association/expiry, recorder export, and lifecycle behavior. Use measured fixtures when available. Existing tests continue to pass.
7. Run the repository's documented tests/build for supported targets. Perform in-game visual and shot validation when a session is available, including camera movement, different FOVs, obstacles, and capsule hiding. Report unavailable checks explicitly; compilation alone does not verify aim accuracy.
8. Document controls, data location/schema, calibration/replay steps, accuracy results and tested operating range, model limitations, and performance bounds.

Deliver code and documentation, summarize the evidence and validation results, and distinguish implemented functionality from anything blocked on manual measurements. Do not publish a release as part of this issue.

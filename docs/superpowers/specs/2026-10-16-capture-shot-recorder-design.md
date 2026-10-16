# Capture-shot recorder and replay design

**Status:** written specification approved by the maintainer in the implementation chat following [its review request on issue #51](https://github.com/johnceoskyblock/sparkling-mutuals/issues/51#issuecomment-6207382463). No recorder or prediction is implemented by this document. The implementation plan and execution method are the next review stage.

The maintainer approved the [recorder-first direction](https://github.com/johnceoskyblock/sparkling-mutuals/issues/51#issuecomment-6199770401) in the implementation chat: “I approve of the recorder-first direction.” After this specification was posted for review, the maintainer replied: “Capture shot request is approved.” That approves this written specification; implementation follows review of the written plan and its execution choice.

## Purpose and delivery boundary

Give a player a working, opt-in way to collect capsule observations, then reproduce and inspect those observations without replaying gameplay. The immediate deliverable is a recorder, local replay/analysis tools, a schema and an exact collection guide. It supports ordinary and Masterful Critter Capsules on Minecraft **26.1.2, 26.2 and 26.3**.

The eventual goal remains a guide showing the player's expected shot path and endpoint before firing. This first delivery does not draw a prediction, aim, fire or select a target. It must leave uncertain ownership and outcomes unknown. Successful recorder delivery means **ready to collect calibration data**, not a completed or accurate aiming projection.

Work stays on `feature/capture-shot-projection`. Its independent starting point is the verified three-target build commit `eaf6e17807d620af2cb305bcd97f32d991bac6f9`. Other issue branches, including the entry guard and achievement rebalance, remain independent. No PR, merge, release, version bump or source-baseline recording is part of this work.

## Evidence and unknowns

Verified repository observations:

- `safari/esp/CritterCapsuleHider.kt` recognizes capsule item displays using custom item IDs `CRITTER_CAPSULE` and `MASTERFUL_CRITTER_CAPSULE`. `mixin/SafariCapsuleMixin.kt` suppresses rendering only. That does not establish authoritative flight or ownership.
- `SafariEsp.register` observes local item use and freezes recent aim in `EspCaptureMemory`. Its species/UUID association serves existing ESP capture memory; it is not proof of projectile ownership or a server collision. Preserve that behavior.
- `SafariTracking`, `SafariEspRules`, `SafariCatch` and [the message reference](../../critters/MESSAGES.md) distinguish a confirmed hit/attempt, an escape, personal capture, shared loot and resource events. A missing hit message is not proof of a miss. Shard quantity is not a shot or capture count.
- Existing compatibility/build adapters support three exact Minecraft targets. Packet/display access must be checked against each target's installed mapped classes; compilation alone cannot establish correct live observations.

Unresolved mechanics include the launch origin, authoritative timing, motion law, player-momentum inheritance, collision/selection rule, display interpolation, ordinary/Masterful differences and reliable attribution during overlapping shots. No vanilla projectile constants are assumed. Observed display motion may be cosmetic or differ from server target selection.

The recorder does not change species completion, retained checks, counts or source resolution. Honeybug nest/spawn accounting and Gemzie's three-entity check remain separate from shot observations. An escape with a replacement UUID is neither a new population nor an automatically matched target.

## Components and integration

New Kotlin code belongs under `safari/shots/`, with mirrored tests. Keep the components small and independent:

| Component | Responsibility |
| --- | --- |
| `ShotRecorder` | Client-thread lifecycle, controls and connection/world binding |
| `ShotSession` | Pure ordered event reduction, bounds, provisional associations and summaries |
| `ShotObservationAdapter` | Copy permitted local input, capsule, target and collision observations into immutable records |
| `ShotFeedback` | Convert recognized capture feedback to typed records, without retaining raw chat |
| `ShotExportWriter` | Single bounded writer, JSONL encoding, finalization and error reporting |
| `tools/capture_shots/` | Python 3.11+ standard-library replay and diagnostic analysis, independent of a running game |

Register the recorder once through `SparklingMutuals`; add the command group through `SparklingCommand`. Use narrow, target-verified observation hooks for entity packet updates where tick observations cannot expose the original update. A packet observer copies only permitted fields on the client thread after Minecraft's threading handoff. Do not change packets, install a broad traffic logger or retain network buffers.

The record path never depends on `shouldRender`, ESP visibility, active mode panels or `EspCaptureMemory` retiring a captured model. Identify target candidates through the existing pure species rules using a recorder-owned bounded observation cache; do not enable ESP or change its scan cadence. Reuse pure descriptions where practical, without exposing or mutating ESP's private state.

All callbacks return the existing result and remain passive. Recorder exceptions terminate recording with a local error; they must not cancel item use, alter capture processing or escape into the game event loop. File operations run off the render/client thread. Existing history and completion consumers receive exactly the same events as before.

## Controls and lifecycle

Commands give local feedback only; they never send party chat or upload data:

| Command | Behavior |
| --- | --- |
| `/sm shots` or `/sm shots help` | Show controls, local-only recording notice and output directory |
| `/sm shots start [seconds]` | Start in a loaded Safari world; default 180 seconds, valid range 10–600 |
| `/sm shots status` | State, session token, elapsed/remaining time, shot/event counts, bounds and output/error status |
| `/sm shots stop` | Stop observation immediately; finish pending tracks as incomplete and finalize the file asynchronously |
| `/sm shots export` | Finalize the active session, equivalent to stop; when stopped, show the completed file path without overwriting it |
| `/sm shots label <scenario> <split>` | Label subsequent inputs; scenario is an enumerated collection case, split is `fit`, `validation` or `unassigned` |
| `/sm shots mark <shot-id> <outcome>` | Add an explicit user annotation to a shot in the active session; outcome is `hit`, `capture`, `escape`, `miss` or `unknown` |

`mark` records **manual annotation**, never server confirmation. No free-form text, arbitrary paths or player names are accepted. The command help lists valid scenarios. A new session starts with scenario `unspecified` and split `unassigned`.

Recording is off at startup and never automatically resumes after reconnect or restart. Do not add a persistent recording-on preference. A local start/stop/limit/error notice and status command make its state discoverable without adding a normal gameplay HUD.

Starting twice reports the current session; it does not replace it. A new start waits for the previous writer to finish. Leaving Safari, disconnecting, replacing the client world, reaching a cap or closing the client stops the session with a specific reason. A world transition is processed before a new-world event can enter the old session. Inputs, tracks and feedback cannot cross worlds. Crash recovery reads complete JSONL lines from an unfinished file and labels it incomplete; a missing footer is never reported as a clean stop.

## What is recorded

Each event has a strictly increasing sequence, a session-relative monotonic timestamp in microseconds and a client tick index. Use the monotonic clock for intervals; timestamps are **client observations**, not server launch times. No wall-clock time, server address, profile ID, IGN or raw UUID is exported. Start metadata includes mod/Minecraft/Fabric versions, schema version, recorder version, dimension registry ID, configured bounds and capsule-hiding settings.

Positions use a session-local coordinate origin at the player's eye position when recording starts. Store its convention, not the absolute origin. Preserve game axes and block-sized units. This retains relative geometry without exporting a player's exact world coordinates.

| Record | Allowed fields and interpretation |
| --- | --- |
| Local input | Shot token, hand, supported item ID, position/eye position, yaw/pitch and normalized aim, velocity, on-ground/sneak/sprint state, first/third-person camera position, current entity/block crosshair observation and estimated connection latency if available |
| Input continuation | Whether the ordinary client use path was observed to proceed; unavailable/aborted remains explicit. Input alone does not prove server acceptance or that a capsule spawned |
| Capsule lifecycle | Session-local entity token plus generation, capsule item ID, discovery source/time, add/remove observations, position samples, velocity when exposed and removal reason when exposed |
| Decoded capsule update | Update kind and permitted decoded position, relative delta, rotation or motion fields; indicate absolute/relative encoding and whether an earlier base is known. Keep it separate from entity tick state |
| Display state | Item-display context, transformation translation/scale/quaternions, interpolation start/duration and position interpolation duration when exposed; emit on change, never the whole metadata/NBT payload |
| Target candidate | Session-local token/generation, identified species, position, motion estimate, entity AABB, and identification source; unknown target identity stays unknown. Entity bounds are client observations, not proven server capture hitboxes |
| Obstruction sample | Loaded-world status and the first client block-shape intersection on the 64-block aim ray at input, and on successive observed capsule segments; relative hit position, face, block registry ID and sampled shape bounds only |
| Feedback | Typed `hit`, `escape`, `personal_capture`, `shared_capture` or narrowly recognized `startle`; canonical species and relevant capsule variant only when actually known. Shared catches never settle a local shot |
| Annotation | Shot token, enumerated manual outcome, scenario/split and annotation provenance |
| Completeness | Start/stop reasons, missing predecessors, unavailable fields, observation gaps, candidate overflow and cap/error indicators |

Do not record credentials, authentication files, API keys, full item data, raw chat, chat senders, skins, textures, inventories, unrelated entities, scoreboard dumps or automatic screenshots. Do not read `.devAuth`. Nearby players need only an anonymous count within the observation radius to flag possible interference; it does not prove nobody outside view fired a capsule. Latency is an estimate or null with a reason.

Target samples cover loaded, identifiable candidates within 64 blocks of the local player or a tracked capsule, with a maximum of 64 target tracks. Capsule tracks are limited to 16 simultaneously and 256 over a session. Bootstrap discovery and ongoing cache work use bounded batches (maximum 256 entity inspections per tick); backlog and truncation are visible. No chunk loads, whole-world traversal, arbitrary packet dumps or per-frame file writes are permitted. Ignore unsupported item IDs; ground item capsules are explicitly marked ground observations and cannot become flying tracks merely by proximity.

Keep a half-second bounded in-memory pre-input ring while recording. Once a supported local input occurs, attach available pre-input observations and sample local/capsule/target state once per client tick for up to eight seconds after the latest input. Relevant packet and feedback events retain their own sequence/time. Long-lived tracks finish as `window_expired`, not misses. Data outside a shot window is limited to bootstrap/discovery needed to identify already-existing tracks; do not continuously export unrelated movement.

## Association and outcome rules

Local inputs get stable session-local shot tokens. Each entity lifetime gets a separate token; a numeric entity ID reused after removal creates a new generation. A capsule present before input remains a pre-existing candidate. A track discovered after input is not necessarily newly spawned: preserve whether its spawn was observed or it was discovered later through metadata/bootstrap.

For diagnostics, attach a candidate relation when a supported capsule is first observed within 1.5 seconds after input and within 8 blocks of that input's eye position. These are **collection heuristics**, not inferred game speed/range constants. Store all qualifying relations; never pick the nearest to manufacture a unique owner. Entity and shot records remain available even when no relation qualifies. Overlapping input windows, multiple candidates, caps or observation gaps mark the relation ambiguous/incomplete.

Use symbolic association labels `unmatched`, `candidate_unique`, `ambiguous` and `manual`; no invented numeric confidence percentage. Even `candidate_unique` means one candidate under the declared heuristic, not proven ownership. A manual outcome does not turn a candidate relation into verified ownership. A separate analysis curation file may select a shot/track pair with an explicit human rationale; keep original exported observations unchanged.

Feedback is kept as a separate timeline event with all temporally/species-compatible shot candidates (maximum 60 seconds since input). It may refine a provisional relation but cannot prove a UUID or capsule association from a species name alone. Retain hit, escape and personal capture separately; repeated delivery of the same server event must not create a second outcome. Deduplicate known paired event delivery using provenance, rather than suppressing two real identical feedback lines by a long text timeout.

A disappearance can mean removal, unloading, capture or another unobserved event. It is not an impact. An explicit escape is a failed **capture attempt after hit**, not a missed throw. An elapsed window, missing hit message, packet gap, shared loot or Shyworm startle cannot independently become a confirmed local miss/capture. Existing capture/ESP ledgers remain authoritative for their own purposes and are not rewritten from the recorder.

## Export format and resource limits

Use UTF-8 JSON Lines: one header, ordered typed events, then a footer. Include `schemaVersion: 1` and `modelVersion: null`; the latter means no calibrated model is included. Each line is a complete JSON object. A checked-in JSON Schema and small synthetic fixtures document required fields, units, nullable availability reasons and enums. Model code must not silently reinterpret a different schema version.

Write to `<Minecraft game directory>/sparkling-mutuals/shots/<random-session-token>.jsonl.part`, then rename to `.jsonl` after a valid footer and closed stream. Use a random token plus create-new semantics, a fixed directory and no overwrite. The command prints the actual path (development clients have separate `run/<target>` directories). Exports remain local until the player deliberately shares them. Nothing scans or uploads this directory in normal play.

Limits per session: **600 seconds, 300 local inputs, 100,000 events, 10 MiB encoded output**, whichever comes first. Reserve space within those limits for a footer/termination marker. The writer queue and pre-input ring together are capped at **4 MiB**; count encoded bytes, not a guess based on object count. Bounded active entity caches and scalar input records have separate enforced caps. A full queue stops recording as incomplete, without blocking gameplay or silently discarding individual points while claiming a complete trace.

On I/O failure, stop immediately and retain any existing partial file. Report the failure once locally and via `status`; do not fall back to dumping observations into `latest.log`. A completed previous export remains intact. Do not delete older exports automatically. Shutdown has a bounded writer drain; a timeout leaves a recoverable partial file, not a false completed export.

## Replay and diagnostic analysis

Provide a Python 3.11+ tool with no third-party dependencies or network access:

```text
python tools/capture_shots/replay.py session.jsonl --output replay-directory
python tools/capture_shots/analyze.py --fit fit-session.jsonl --validation validation-session.jsonl --output analysis-directory
```

The replay command validates the schema, sequence, time monotonicity, token lifetimes and footer/bounds. It reconstructs each shot/track timeline deterministically and writes a summary JSON plus per-shot CSV (raw decoded updates, tick positions, display state, target candidates and feedback kept distinguishable). Incomplete sessions and unavailable fields survive the round trip. It never executes fields from input, renders remote content or follows input-specified output paths.

The analysis command reports usable/excluded shot counts and reasons, association status, update/sample intervals, apparent visual speed/acceleration, input-to-first-observation/feedback delays and per-variant summaries. Default calibration summaries exclude ambiguous, unmatched, incomplete or uncurated ownership. A deliberate option can show provisional diagnostic samples, labelled as such. Original exports are immutable; any curation is a separate file with input hashes and chosen shot/track pairs.

Include two explicitly **diagnostic visual baselines** for comparing observations: constant velocity and constant acceleration. On fitting-session tracks, fit only the first half of a sufficiently sampled track and evaluate its remaining half without refitting. Report these within-track diagnostics separately from independent validation.

For independent validation, freeze each variant's acceleration estimate from fitting-session tracks (component-wise median of finite fitted accelerations, in the recorded game-axis convention). Constant velocity always uses zero acceleration. Initialize a validation track's position and velocity from only its first two distinct-time observations; mark those as conditioning samples and exclude them from residual scoring. Score later observations with the frozen model; never refit acceleration, choose a model or tune parameters on validation data. If a variant lacks fitting evidence or enough later samples, report unavailable rather than borrow another variant's parameters. This diagnoses visible motion after an observed initial segment; it is **not** a before-firing, aim-conditioned prediction model or proof of server mechanics. Reject rank-deficient/insufficient fits and non-finite values.

Reports include position residual count, RMSE, p50, p95 and maximum in blocks, plus last-observed-position error where a comparison exists. Call the latter **last observed point**, not impact error. Capsule variants, sampling gaps and curation exclusions remain separate. To compare later models, accept a versioned optional prediction CSV keyed by session/shot/track/time with coordinate convention and model identifier; compare supplied points at observed times and report uncovered intervals. Confirmed hit/impact comparisons are reported only when the data actually contains that evidence, not from the final display sample.

No baseline is selected for gameplay by this release. There is no calibrated model file, accuracy guarantee, live renderer or enabled prediction setting. The later model must explain input-conditioned behavior, independent shot validation and collision/target-selection evidence, with an approved useful tolerance and operating range.

## Exact first collection guide

Ship the guide with the working recorder before asking the maintainer for measurements. Explain each condition and print/export automatically collected values rather than requesting manual coordinate transcription.

1. Install the matching Actions JAR and matching Fabric dependencies. In Safari, select an ordinary capsule, use `/sm shots start 180`, then `/sm shots status`. Confirm recording and the output location. Stop/export after a manageable block; limits are deliberate, so use additional sessions rather than one oversized file.
2. Establish a quiet, loaded area without another nearby player throwing. Collect ordinary and Masterful variants in separate labelled blocks. Begin with capsule hiding disabled, then repeat a representative block with it enabled. Other relevant settings stay unchanged; both hiding states are recorded.
3. Baseline block: stationary player, stationary or visibly slow target, approximately 5, 15 and 30 blocks away; level, upward and downward aim, five isolated attempts per feasible range/angle/variant. Wait at least eight seconds between attempts so observation windows can separate. Annotate only outcomes actually observed. A target's own movement is recorded and can invalidate a stationary case.
4. Independent validation block: a second session with the same conditions and five new isolated attempts per feasible case. Set split `validation` before firing; do not relabel an inspected fitting sample as held out. Reserve these sessions before model selection.
5. Diagnostic blocks: five attempts each while walking forward and sideways, against moving targets, and with an intervening solid block/near edge. Label these cases separately; do not force an unsafe or unavailable geometry. They distinguish momentum, target following and obstruction hypotheses from simple visual flight.
6. Interference block: rapid consecutive local inputs and another cooperating player firing nearby (five sequences each where available). This tests ambiguity handling, not trajectory fitting. Repeat a small hidden-capsule block to verify observations persist while visuals are suppressed. Briefly test world transfer/disconnect to verify a clearly stopped/incomplete session.
7. Stop/export. Inspect summary/CSV locally and share only chosen `.jsonl` files plus any deliberate curation file. No credentials, logs or authentication setup are requested. Provide target game version and collection condition through the recorded metadata/scenario controls.

These are collection targets, not proof that a sample count is statistically sufficient or a request to consume rare capsules before the kit exists. Start with inexpensive ordinary capsules and a small sanity-check export; request the larger/Masterful matrix only after the recorder demonstrates useful fields. All gameplay requests and questions go on issue #51. If some cases are impractical, record exclusions and request a focused replacement there.

## Verification and acceptance

Meaningful automated coverage must include:

- Off/start/stop/export/error/cap states; repeated starts; transfer-before-input ordering; reconnect/restart remaining off; complete/partial-file recovery and writer failure without game mutation.
- Both capsule variants and hands; unsupported items; pre-existing displays, delayed item metadata, reused entity IDs, rapid throws, two senders' capsule ambiguity and unmatched feedback.
- Typed privacy allowlists; sanitized identifiers/relative positions; no raw NBT/chat/auth/profile leakage; schema/version/finite-number checks, byte/event/memory caps and create-new/no overwrite behavior.
- Packet updates versus interpolated/tick positions, changed display transforms, missing relative-position predecessors, removal versus outcome, bounded bootstrap/scan work and observations with hiding and ESP display disabled.
- Personal/shared feedback separation, repeated identical real events versus paired delivery, escapes and replacement UUIDs remaining distinct, and timeout never inventing misses.
- Deterministic replay fixtures; manually checkable linear/accelerating synthetic traces, irregular times, insufficient fits, explicit fitting/validation separation, ambiguous/excluded counts and prediction-CSV residuals. Synthetic motion proves tool arithmetic, not Hypixel behavior.

Run documented clean tests/builds and distribution verification on all three targets, plus the Python standard-library test suite. Verify required observation hooks and startup on each actual Minecraft target. Compare recorder-off versus recorder-on capture tracking/ESP, settings persistence, both hiding settings, both hands, export and transfer during a suitable live session. If live Hypixel access is unavailable, provide the working kit and say which checks await gameplay; do not claim runtime field fidelity from synthetic tests or startup.

Push only the issue branch. Verify Actions at the final uploaded implementation commit, download and verify the three matching artifacts, then post the implementation confirmation and collection request on the still-open issue. Give concise notes and artifact links in the implementation chat. Do not publish release notes to a Release or create a PR/merge as part of monitoring.

Recorder-phase acceptance is a usable bounded local recorder plus reproducible tools, privacy/schema documentation, exact collection instructions, tests/build evidence and honest runtime limitations. Issue #51 remains **awaiting calibration/model validation** until independent observations support a useful input-conditioned prediction and a separately reviewed renderer implementation.

## Last updated

2026-10-15 (America/New_York) - Defined recorder evidence, controls, privacy, bounds, replay, collection and validation; recorded subsequent written-specification approval. No product implementation is claimed.

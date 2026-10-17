# Capture-shot collection kit

This opt-in kit records local observations for ordinary and Masterful Critter Capsules on **26.1.2, 26.2 and 26.3**. A separate, default-off experimental ordinary-capsule path is available within the narrow scope below. Endpoints, terrain collision and capture targeting remain unverified. Visible capsule motion may differ from server capture rules.

## Experimental capsule path

Capture-shot development builds currently target **26.1.2 only**, by maintainer direction; the other two builds resume when this work is finished.

In `/sm` settings, open **Safari helpers → Experimental capsule path**. It starts **off** and saves independently from the recorder. Turn both **Hide capsules on ground** and **Hide flying capsules** off. Hold an ordinary capsule in your main hand in Safari, use first person and stand on the ground for two stable client ticks. With **Allow uncalibrated estimates** off, the original -10 through +1 degree gate, short path and **Experimental path** status remain; no endpoint is shown.

Walking, jumping/falling, swimming, flight, riding, sneaking/sprinting, unsupported items (including Masterful), off-hand-only use and third person suppress the path. Screens and hidden HUD suppress both the path and status. A quiet status explains supported-state restrictions; there are no repeated chat notices. Disconnects/world changes clear stability without changing the saved option or starting recording.

Enable **Allow uncalibrated estimates** (also default off) for broader first-person angles and estimated contact. The guide extends the same fixed spatial formula from the eye, including the formerly omitted launch prefix. This is the versioned `contact-guide-center-extrapolation-v1` hypothesis, not measured launch physics. It stops at the first of 64 accumulated blocks, ordinal 128 or 256 segments. Ordinal is **not server flight time**. Launch/wider regions are dashed; the previously observed region is solid. **Capsule path width** saves from 1–5, default 3, with a dark outline. Both passes remain terrain-occluded; existing ESP visibility stays unchanged.

One primary marker clips the path: an amber surface ring for **Estimated terrain contact**, or a cyan diamond with center mark for **Possible critter contact**. Terrain wins indistinguishable ties, which are labelled ambiguous. Terrain uses loaded collision shapes, including partial blocks; critters use current client-body or versioned unpadded visual-body proxies, independent of ESP toggles. Neither those proxies nor the zero-radius curve is a verified server hitbox. No target-motion prediction is performed. **Uncalibrated** remains in the HUD. Fluids, unloaded geometry and incomplete/unsupported critter coverage suppress farther markers; a range/work limit displays no landing or miss claim.

For the first visual test, leave recording off, enable the option as above and compare a few ordinary throws in the tested range. Check camera turns/FOV, GUI scale, screen/HUD suppression, movement stop/start, item/hand changes, angle limits and world transfer. Put the line behind a solid obstacle and confirm occlusion while existing ESP keeps its normal visibility. Live Hypixel appearance and server mechanics require gameplay testing; a passing build or numeric observation check cannot establish them. See the [manual checklist](IMPLEMENTATION_TEST_CHECKLIST.md#experimental-capsule-path).

## Controls

### Contact collection: start small

The working 26.1.2 build and updated tools support this sequence. Use ordinary main-hand capsules, a stationary grounded first-person player, both hiding options off, one thrower and at least eight seconds between throws. Use separate 180-second recordings; stop explicitly in the same world. Recording collects an input-time probe even with the preview off when those conditions and two stable ticks hold. It never starts automatically.

1. **Six-throw sanity:** two near-level into open terrain, two downward at empty nearby ground, two at a small ground critter. Start `/sm shots start 180`; label the applicable scenario `unassigned`, then stop. Inspect probes, raw paths and typed feedback before collecting more. No message means unknown, not miss.
2. **Wider-angle fitting:** twelve throws, three near each of -30, +15, +35 and +60 degrees. Use `/sm shots label stationary_up fit` or `/sm shots label stationary_down fit`. Recorded pitch defines the bins.
3. **Close terrain fitting:** twelve throws, four toward feasible visible ground/wall points around 1, 2 and 4 blocks. Use `/sm shots label solid_obstruction fit`. If partial shapes need investigation, use `/sm shots label edge_obstruction fit` in a separate session.
4. **Proxy fitting:** twelve attempts, six small ground targets and six larger/airborne targets, initially slow or briefly stationary, both unobstructed and wall-adjacent. Use `/sm shots label moving_target fit` only for moving targets; otherwise use the actual stationary scenario. Keep intended species in a separate local note.
5. **Independent validation:** freeze the candidate/proxy hypothesis and agree tolerances from fitting evidence first; then collect fresh twelve terrain and twelve critter attempts in new sessions, using corresponding `/sm shots label stationary_up validation`, `stationary_down validation`, `solid_obstruction validation`, `edge_obstruction validation` or `moving_target validation`. Do not reuse fitting throws or select validation examples after seeing residuals. No Masterful throws are needed for ordinary calibration.

Schema v2 / recorder policy v3 adds one typed contact probe per eligible ordinary input. Target keys are local to that probe; positions are session-relative. Detail can be compacted below 64 KiB with omitted counts and `truncated=true`. Raw capsule observations keep their original meanings and caps. The tools separate raw-file completeness from incomplete prediction detail:

```text
python tools/capture_shots/contacts.py session.jsonl --output new-contact-report
```

The report separates predicted contact, coverage, typed/manual outcomes and raw removals. Endpoint error stays null until an independent terminal position exists; removal/last position/capture feedback cannot supply it. The existing collection matrix below is historical/general recorder coverage; the bounded ordinary contact sequence above is the current next collection.

Recording starts off and never resumes automatically. In a loaded Safari world:

```text
/sm shots start 180
/sm shots label stationary_level fit
/sm shots status
/sm shots mark s1 unknown
/sm shots stop
/sm shots export
```

`start` defaults to 180 seconds; accept 10â€“600 seconds. `status` shows opening/recording/finalizing/off, session, elapsed/remaining time, input/event/byte bounds and last completed export. Starting again cannot replace an active or finalizing session. `export` stops an active recording or prints the last completed path. `mark` accepts existing shot IDs and `hit`, `capture`, `escape`, `miss`, `unknown`; these are **manual annotations**, never server confirmations. IDs start at `s1`; block-to-air fallthrough in one client use is one input with separate continuations. Input is not proof the server accepted a throw.

`label <scenario> <split>` affects subsequent inputs. Splits are `fit`, `validation`, `unassigned`. Scenarios: `unspecified`, `stationary_level`, `stationary_up`, `stationary_down`, `walking_forward`, `walking_sideways`, `moving_target`, `solid_obstruction`, `edge_obstruction`, `rapid_inputs`, `nearby_thrower`, `hiding_comparison`. Help and suggestions list the choices. Each session resets to unspecified/unassigned.

Files stay under `<Minecraft game directory>/sparkling-mutuals/shots/`. Development clients use separate `run/<target>` game directories. A random token names a create-new `.jsonl.part` file; a closed file with a footer is renamed `.jsonl` without replacing older files. A crash or disk error can leave a recoverable partial file. Nothing automatically uploads or deletes exports.

## Evidence and bounds

The recorder copies local aim/movement/camera, supported inputs and continuations, capsule lifetime/packet/tick observations, display transforms, identifiable nearby target bounds, loaded-block intersections and typed capture feedback. It works independently of ESP display and capsule hiding. Header hiding settings describe session start; keep them unchanged within a session and start a new session for the comparison.

Coordinates are relative to the starting eye position, with game axes and block units. The origin itself is omitted. Velocities, aim and display-local translations remain vectors. Monotonic microseconds are client arrival times, not server launch times. Packet `observedMicros` preserves original arrival when identification metadata arrives later. `base` and raw relative path points remain separate from smooth tick samples. Missing bases, truncated paths and cache gaps stay explicit. Informational `scan_pending` means a bounded sweep continues next tick; actual dropped pending evidence remains a lossy cache gap. If the live Java entity map clears entries between batches, empty iterator slots are skipped and still count against the scan allowance. Exceptions from inspecting a real entity still stop recording with local diagnostics. A pre-existing or metadata-discovered capsule is not automatically a new local spawn; reused numeric IDs get separate tokens/generations.

One candidate within 1.5 seconds and 8 blocks is labelled `candidate_unique`, which is a collection heuristic, not verified ownership. Multiple candidates/overlapping inputs remain ambiguous. Shared captures and startles cannot settle a local shot. Escapes follow a hit; removal, unload, an eight-second sample-window expiry or absent feedback cannot establish a miss or impact. Existing capture/completion/ESP records remain independent. Terminal records survive the sampling window. A track ending at expiry, unload or recording stop is incomplete; stopping the file does not establish that a capsule finished its flight. An expired entity lifetime cannot become a fresh track after another input.

Recorder policy **v2** (header `recorderVersion: 2`, unchanged JSONL schema v1) keeps raw capsule packets, capsule tick/display/lifecycle observations, player state, inputs and typed feedback. Targets have fresh full snapshots at each local input, broad full keyframes every two seconds, and change-sensitive samples up to 5 Hz for at most eight candidates near the current aim/crosshair or observed capsules. The focused cadence is shared across candidates, so turning quickly cannot multiply its rate. Between explicit keyframes, unchanged target states are suppressed. Each batch rechecks the 64-block distance from the player eye or observed capsules. Still-loaded targets leaving that range release their cache slots before the next bounded discovery pass, allowing newly nearby candidates to enter. Target raw packets and display transforms are not exported; their internal packet bases still update. Version 1 recordings used per-tick target samples and target packet/display traffic; do not interpret the two policies as equivalent-rate evidence. Priority, omission and sparse context never establish ownership, collision or absence.

The historical recorder-v2 deterministic populated-area regression used 64 changing targets at 20 ticks/second plus 64 unrelated target packet updates per tick, changing player yaw, fresh input snapshots and forty raw capsule updates per throw. Its 180-second workload with 20 isolated throws writes 18,424 events / 7,328,547 bytes; its 600-second sparse workload with nine throws writes 8,326 events / 3,316,366 bytes. Both used the real sampler, session, codec and file writer and retain all 800 / 360 capsule packet observations. These are software budget checks, not live Safari measurements; arbitrary traffic or more frequent throws may still reach a cap. For the initial sanity collection, use a few isolated throws in one 180-second session, stop explicitly, and inspect locally before expanding the matrix.

Caps: 300 inputs, 100,000 events, 10 MiB encoded output, 600 seconds; encoded queue plus pre-input ring share 4 MiB. A half-second ring supplies pre-input context and sampling continues eight seconds after the latest input. Cache work inspects at most 256 entities per tick, up to 16 simultaneous/256 session capsule tracks and 64 target tracks. Queue/cap/error termination is incomplete. Some footer summaries may be condensed at the line limit; retained timeline events remain the evidence. No chunks are loaded to collect intersections.

Leaving Safari, changing worlds, disconnecting, shutting down or reaching a cap stops recording. An export may finish later; wait for its local completion notice. Failures retain the operation/stage, session token, relative time/tick where available, and original exception/cause/stack trace once in the local client `logs/latest.log`. Status gives a concise diagnostic reference without exception messages or observation dumps. Writer errors have a stage/session reference but no client tick. Error termination remains incomplete; previous completed exports remain intact. If an unexplained error occurs, share only the recorder-related error excerpt and chosen JSONL after checking them for private information. The previously reported independent early error cannot be diagnosed without its session/log evidence.

Only allowlisted fields are exported. No authentication files, credentials, server address, player names, UUIDs, skins, raw chat, whole item data, inventory dumps, screenshots or automatic uploads. Anonymous nearby-player counts flag possible interference without claiming no unseen player fired.

## First collection checklist

1. Install the Actions JAR matching your game and Fabric dependencies. Start with inexpensive ordinary capsules and a small sanity-check export; inspect the fields before a larger or Masterful collection.
2. Find a quiet, loaded area. Keep capsule hiding disabled for the first block. Start recording, check status and label the condition. Keep other relevant settings fixed. Separate ordinary and Masterful blocks.
3. Baseline: stationary player and stationary/slow target, approximately 5, 15 and 30 blocks away; level, upward and downward aim; five isolated attempts per feasible range/angle/variant. Wait at least eight seconds between attempts. Target movement is recorded and may invalidate a stationary case. Annotate only observed outcomes.
4. Reserve a **second session** for independent validation before inspecting/tuning models. Label it `validation`; collect five new isolated attempts per feasible condition. Do not relabel a fitting sample after inspecting it.
5. Diagnostic blocks: five attempts each while walking forward, walking sideways, against moving targets, behind a solid obstruction and near an edge. Label separately; skip impractical geometry and record the exclusion when sharing selected files.
6. Interference: five rapid-input sequences and five cooperating nearby-thrower sequences where practical. Repeat a representative block in a **new session with capsule hiding enabled**. These check ambiguity and observation continuity, not trajectory accuracy. Briefly test world transfer/disconnect and confirm recording stays off.
7. Stop/export, wait for completion and inspect locally. Deliberately share only chosen `.jsonl` files and any separate curation file. Do not share credentials or authentication setup. Ask gameplay/collection questions on [issue #51](https://github.com/johnceoskyblock/sparkling-mutuals/issues/51).

The sample matrix is a collection target, not a claim of statistical sufficiency. Server trajectory, launch timing, momentum, target selection and useful prediction tolerance remain unverified until gameplay evidence supports them. Automated synthetic tests validate software behavior and tool arithmetic; startup checks alone cannot establish Hypixel field fidelity.

## Format

UTF-8 JSON Lines: one versioned header (`modelVersion: null`), typed ordered events and a footer. New recorder policy v3 uses [schema v2](schemas/capture-shots-v2.schema.json); tools also read unchanged [schema v1](schemas/capture-shots-v1.schema.json) files. Legacy raw event meanings are preserved. A footer describes raw export completeness; unknown outcomes remain unknown. Do not edit raw exports to curate ownership.

## Local replay

Use Python 3.11 or newer, with no extra packages or network access:

```text
python tools/capture_shots/replay.py session.jsonl --output replay-directory
```

Use a new output directory. Replay refuses to overwrite reports or an input. It writes `summary.json`, `timeline.csv` and generated `shot-0001.csv` views; imported tokens never choose filenames. CSV rows keep the original kind and typed JSON data, so raw packet updates, smooth samples, display state, targets, feedback and manual annotations remain distinguishable. The full timeline includes unmatched evidence.

The reader checks the shared schema, allowed fields/enums, finite values, sequence/time order, token lifetimes, footer counts/bytes and file/line/event bounds. Complete files exit `0`, recoverable incomplete files exit `2`, and invalid files exit `1`. A truncated final UTF-8/JSON line preserves earlier complete records and remains incomplete; malformed complete interior lines are rejected. Do not repair a partial file by inventing a footer. Synthetic fixtures under `src/test/resources/shots/` exercise both accepted and deliberately invalid examples.

## Visual diagnostics and independent validation

```text
python tools/capture_shots/analyze.py --fit fit-session.jsonl --validation validation-session.jsonl --curation choices.json --output analysis-directory
```

Repeat `--fit`/`--validation` for additional sessions. `analysis.json` reports exclusions, counts, input/feedback delays, arrival intervals/gaps, apparent visual speed and separate variant results. Raw arrival endpoints are the default sample source. Tick-only descriptions stay separate; stepped packet offsets remain in replay and are not promoted to authoritative launch timing.

Default analysis excludes uncurated, ambiguous, unmatched, incomplete and incorrectly labelled samples. A finalized file can contain incomplete tracks: tracks without an observed server removal, including window expiry, unload or recorder stop, are excluded from default scoring. `--provisional` retains those traces for inspection without treating them as complete flights. Curate ownership separately, using the SHA-256 printed in replay's summary and a rationale for each selected shot/track pair:

```json
{
  "schemaVersion": 1,
  "choices": [
    {"sha256": "COPY_THE_64_CHARACTER_INPUT_HASH", "session": "COPY_SESSION_TOKEN", "shot": "s1", "track": "c1", "rationale": "Explain why this isolated pair was selected."}
  ]
}
```

Keep raw exports unchanged. Hashes must match the selected files. `--provisional` deliberately includes provisional candidate diagnostics and labels the report accordingly; it does not verify ownership or create a gameplay model. Useful for a first sanity check:

```text
python tools/capture_shots/analyze.py --fit src/test/resources/shots/accelerating-fit.jsonl --validation src/test/resources/shots/accelerating-validation.jsonl --provisional --output synthetic-analysis
```

Constant velocity and constant acceleration are **diagnostic visual baselines**. On fitting tracks, only the first half trains the models; later points score them. Independent validation freezes each variant's component-wise median fitting acceleration, initializes position/velocity from the first two distinct-time observations, excludes those conditioning points from scoring and never refits on validation. A variant without its own fitting evidence remains unavailable. Reusing the same session token or source bytes across splits is rejected, even after renaming files.

Residual reports give count, RMSE, nearest-rank p50/p95, maximum and **last observed point** error in blocks. The last visible capsule position is not a proven impact. No model is selected for live aiming by this kit.

Optional `--predictions predictions.csv` compares a separate model's supplied coordinates at exact observed times only. Required columns, in order:

```text
schemaVersion,session,shot,track,micros,x,y,z,model,coordinates
```

Use schema version `1`, a short model identifier, and coordinates `session_relative_game_axes`. Uncovered observations/unmatched predictions are reported; the tool does not invent interpolation, collision or capture outcomes. Outputs always require a fresh directory and input files remain unchanged.

## Validation boundary

Automated suites exercise the actual coordinator/writer/codec, producer-to-reader fixtures, passive compiled callbacks, bounds, errors, privacy and deterministic arithmetic. Preview suites check the fixed spatial model, conservative availability and finite camera-relative geometry separately. Offline client smoke checks do not establish Hypixel observation fidelity, rendered interpolation, both-hands/variants gameplay, hiding comparisons or capture rules. The contact guide remains an uncalibrated estimate; broader trajectory, endpoint and server target-selection validation remains pending. The legacy raw obstruction event uses its earlier loaded predicate and is not an independent proof of complete terrain coverage; new predicted contact probes use explicit nonloading client-chunk checks.

## Last updated

2026-10-16 (America/New_York) - Added the opt-in stationary ordinary-capsule preview guide and its visual/server validation limits.

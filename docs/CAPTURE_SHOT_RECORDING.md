# Capture-shot collection kit

This opt-in kit records local observations for ordinary and Masterful Critter Capsules on **26.1.2, 26.2 and 26.3**. It is ready for collecting calibration evidence after the matching build is verified. Live aiming paths and endpoints await measurements and independent model validation. Visible capsule motion may differ from server capture rules.

## Controls

Recording starts off and never resumes automatically. In a loaded Safari world:

```text
/sm shots start 180
/sm shots label stationary_level fit
/sm shots status
/sm shots mark s1 unknown
/sm shots stop
/sm shots export
```

`start` defaults to 180 seconds; accept 10–600 seconds. `status` shows opening/recording/finalizing/off, session, elapsed/remaining time, input/event/byte bounds and last completed export. Starting again cannot replace an active or finalizing session. `export` stops an active recording or prints the last completed path. `mark` accepts existing shot IDs and `hit`, `capture`, `escape`, `miss`, `unknown`; these are **manual annotations**, never server confirmations. IDs start at `s1`; block-to-air fallthrough in one client use is one input with separate continuations. Input is not proof the server accepted a throw.

`label <scenario> <split>` affects subsequent inputs. Splits are `fit`, `validation`, `unassigned`. Scenarios: `unspecified`, `stationary_level`, `stationary_up`, `stationary_down`, `walking_forward`, `walking_sideways`, `moving_target`, `solid_obstruction`, `edge_obstruction`, `rapid_inputs`, `nearby_thrower`, `hiding_comparison`. Help and suggestions list the choices. Each session resets to unspecified/unassigned.

Files stay under `<Minecraft game directory>/sparkling-mutuals/shots/`. Development clients use separate `run/<target>` game directories. A random token names a create-new `.jsonl.part` file; a closed file with a footer is renamed `.jsonl` without replacing older files. A crash or disk error can leave a recoverable partial file. Nothing automatically uploads or deletes exports.

## Evidence and bounds

The recorder copies local aim/movement/camera, supported inputs and continuations, capsule lifetime/packet/tick observations, display transforms, identifiable nearby target bounds, loaded-block intersections and typed capture feedback. It works independently of ESP display and capsule hiding. Header hiding settings describe session start; keep them unchanged within a session and start a new session for the comparison.

Coordinates are relative to the starting eye position, with game axes and block units. The origin itself is omitted. Velocities, aim and display-local translations remain vectors. Monotonic microseconds are client arrival times, not server launch times. Packet `observedMicros` preserves original arrival when identification metadata arrives later. `base` and raw relative path points remain separate from smooth tick samples. Missing bases, truncated paths and cache gaps stay explicit. A pre-existing or metadata-discovered capsule is not automatically a new local spawn; reused numeric IDs get separate tokens/generations.

One candidate within 1.5 seconds and 8 blocks is labelled `candidate_unique`, which is a collection heuristic, not verified ownership. Multiple candidates/overlapping inputs remain ambiguous. Shared captures and startles cannot settle a local shot. Escapes follow a hit; removal, unload, an eight-second sample-window expiry or absent feedback cannot establish a miss or impact. Existing capture/completion/ESP records remain independent.

Caps: 300 inputs, 100,000 events, 10 MiB encoded output, 600 seconds; encoded queue plus pre-input ring share 4 MiB. A half-second ring supplies pre-input context and sampling continues eight seconds after the latest input. Cache work inspects at most 256 entities per tick, up to 16 simultaneous/256 session capsule tracks and 64 target tracks. Queue/cap/error termination is incomplete. Some footer summaries may be condensed at the line limit; retained timeline events remain the evidence. No chunks are loaded to collect intersections.

Leaving Safari, changing worlds, disconnecting, shutting down or reaching a cap stops recording. An export may finish later; wait for its local completion notice. Disk errors are reported locally once; previous completed exports remain intact.

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

UTF-8 JSON Lines: one schema-v1 header (`modelVersion: null`), typed ordered events and a footer. [The checked schema](schemas/capture-shots-v1.schema.json) specifies fields, enums and bounds. A footer describes export completeness; unknown outcomes remain unknown. Do not edit raw exports to curate ownership.

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

Default analysis excludes uncurated, ambiguous, unmatched, incomplete and incorrectly labelled samples. Curate ownership separately, using the SHA-256 printed in replay's summary and a rationale for each selected shot/track pair:

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

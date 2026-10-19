# Capture-shot recording and projections

## Purpose

Collect opt-in local observations for ordinary and Masterful Critter Capsules, inspect them with offline tools, and optionally display an experimental aiming guide. Supported build targets are **26.1.2, 26.2 and 26.3**. Visible capsule motion, launch physics, collision and server target selection remain distinct evidence questions; the guide does not promise a capture.

## Controls and defaults

### Experimental capsule path

In `/sm → Safari helpers`, **Capture projections** is the single enable switch and defaults **off**. **Capsule path width** saves from 1–5, default 3; **Show guide through terrain** defaults on. Its off state restores terrain-occluded rendering without changing predicted collision stops. The option uses the existing `experimentalCapsulePath` key; obsolete second-gate/status-HUD properties are ignored. There is no projection text overlay or HUD-editor status entry.

The visual guide works independently of recording, ESP toggles and either capsule-hiding option. It uses an ordinary or Masterful capsule in the main hand in Safari, with finite aim across pitch −90° through +90°. Walking, sprinting, sneaking, jumping, falling, standing/swimming in water and third-person views are supported as uncalibrated estimates. Flying, riding, lava, unsupported items and off-hand-only use suppress it. Screens and hidden HUD suppress geometry. Expanded display does not require two stationary ticks.

The route starts from the player's interpolated eye and current player aim. Camera position only displays it: detached-camera angles and the front-third-person crosshair do not retarget the character's route. This does not validate server throwing through a camera mod. No momentum correction or Masterful-specific curve is applied.

### Recorder commands

Recording starts off and never resumes automatically. In a loaded Safari world:

```text
/sm shots start 180
/sm shots label stationary_level fit
/sm shots status
/sm shots mark s1 unknown
/sm shots stop
/sm shots export
```

| Command | Implemented result |
| --- | --- |
| `/sm shots` or `help` | Lists commands, enumerated choices and local output directory. |
| `start [seconds]` | Defaults to 180 seconds; accepts 10–600. Cannot replace an opening, recording or finalizing session. |
| `status` | Shows phase, session, elapsed/remaining time, input/event/byte/queue bounds, stop/error reference and last export. |
| `stop` | Stops observation and asynchronously finalizes. |
| `export` | Stops an active session, or prints the last completed path when off. |
| `label <scenario> <split>` | Labels subsequent inputs; resets to unspecified/unassigned each session. |
| `mark <shot-id> <outcome>` | Annotates an existing input in the active session; never creates server confirmation. |

Splits: `fit`, `validation`, `unassigned`. Scenarios: `unspecified`, `stationary_level`, `stationary_up`, `stationary_down`, `walking_forward`, `walking_sideways`, `moving_target`, `solid_obstruction`, `edge_obstruction`, `rapid_inputs`, `nearby_thrower`, `hiding_comparison`. Manual outcomes: `hit`, `capture`, `escape`, `miss`, `unknown`. Shot IDs begin at `s1`; block-to-air fallthrough is one input with separate continuations. A client input is not proof that the server accepted a throw.

## Implemented behavior

### Spatial guide and evidence boundary

The compiled model is `ordinary-stationary-visual-ordinal-candidate-v1`:

```text
speed = 0.8482482249942948
verticalAcceleration = -0.020085553311180723
p(n) = eye + speed * n * normalizedAim
       + (0, 0.5 * verticalAcceleration * n * (n - 1), 0)
```

`n` is an observed position-update ordinal, **not server time**. The historical first position-sync observation was ordinal 2; spawn was excluded from fitting. A separately frozen stationary ordinary model was historically compared with independent observations using p95 3D error ≤0.25 blocks and maximum ≤0.5 blocks in the nominal 5–15-block region, within pitch −10° through +1°. This recorded narrow observation criterion is not a new validation result, sample-sufficiency guarantee, or evidence for launch/collision/target selection. [Historical provenance](https://github.com/johnceoskyblock/sparkling-mutuals/blob/93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee/docs/superpowers/specs/2026-10-16-capture-shot-preview-design.md) retains the experiment's original boundary.

Current contact display extends that same formula from ordinal 0 at the eye, versioned `contact-guide-center-extrapolation-v1`. The prefix is launch extrapolation. Sampling stops at the first of 64 accumulated blocks, ordinal 128 or 256 segments; chord length is at most 0.5 blocks and parabolic chord deviation at most 0.01 blocks, subject to bounded work. Launch/wider regions are dashed; only the old pitch/ordinal region is solid. Masterful, moving and water routes are entirely dashed. A solid segment means visual-curve evidence coverage, not verified collision.

### Contacts and visibility

| Result | Display | Meaning/limit |
| --- | --- | --- |
| Earlier loaded terrain collision shape | Amber surface ring; path clips there. | Estimated terrain contact with a zero-radius curve, including partial blocks. |
| Earlier supported critter proxy | Cyan diamond with center mark; path clips there. | Possible critter contact at its current observed body; no motion prediction or verified server hitbox. |
| Recognized supported Rockmite mound model hull | Amber mound-surface estimate. | Spawn-source visual surface, never a cyan critter; released silverfish remain critter contacts. |
| Unsupported fluid/unloaded geometry | Stops known route; no marker beyond boundary. | Unknown continuation, not a miss. |
| Range/work limit without contact | Bounded path, no terminal marker. | No landing or miss claim. |

Terrain wins indistinguishable ties. Client-body or unpadded `client_body_v1`, `display_body_v1` and `armor_stand_body_v1` visual proxies are independent of ESP visibility; padded ESP outlines and empty native display bounds are not substituted as server hitboxes. Incomplete/unsupported target coverage can retain a known contact, but an unchecked target could precede it. Restriction/provisional/tie reasons stay in diagnostics rather than an on-screen text status.

Water uses the same uncalibrated curve, based on a supplied observation that capsules move similarly there; it is not a measured water model. Solid waterlogged geometry still blocks. The guide does not solve through solid terrain merely because through-terrain visibility is on.

The mound's `mound_surface_v1` hull uses loaded item-model bounds and display transforms, independent of its ESP outline. Unsupported, singular, overlarge or pending geometry yields no surface estimate. The 0.25-block amber ring wraps a small connected loaded face patch across seams/steps/model edges, with at most 24 segments and 32 nearby surface boxes from a 27-cell block neighborhood. Missing geometry/budget omits unsupported portions instead of bridging air. This bends the marker, not the route or a bounce.

On **26.1.2**, optional Iris integration draws the flat guide after shader composition while a pack is active; width and terrain-visibility choices still apply. With Iris absent/shaders off it uses the normal renderer. Toggles/reloads are detected without another setting. This is not certification of every shader pack; later-target Iris composition remains unvalidated.

### Bounded target discovery and Shyworm bodies

Discovery retains progress through routine metadata updates, marks coverage incomplete until a fresh sweep finishes, and restarts for structural entity-map changes. Up to 64 targets are ranked near the current estimated curve as aim changes; this is bounded selection, not proof that every relevant critter was checked. Each tick shares 256 inspections: 160 cache, up to 64 Shyworm connection refreshes, sixteen metadata checks and sixteen selected-dependency validations. Repeated frames can reuse unchanged validation within the tick; movement, metadata, equipment, passenger or capture/model changes invalidate affected checks. Each tick begins fresh.

Four supplied local snapshots informed provisional Shyworm body support: seven separate marker armor-stand head-items, including a raised tail/low head. They use unpadded head-item geometry without filling gaps. A body part needs an unambiguous connection to a current head in at most seven links, each ≤1.75 blocks. This is a fitting hypothesis, not server ownership. Ambiguous/disconnected pieces, unsupported poses/small stands or stale heads are omitted.

A separate connection map retains up to 64 heads/body pieces, including connectors outside the solver's 64 targets; more than 64 nodes or sixteen heads withholds body estimates. Changed unknown equipped stands enter a coalesced 64-identity queue with up to sixteen checks per tick; pending checks withhold body contacts, overflow requires a fresh sweep, and empty-head labels wait for later equipment changes. Unrelated target overflow keeps coverage provisional without permanently preventing body support. Body geometry affects only contact estimates, never sightings, capture identity, completion or ESP tracking.

`/sm debug` emits bounded `SM_SHYWORM_BODY` snapshots; `contact_scene` adds cached discovery/queue/coverage diagnostics without another scan. This explicit action does not start recording. Inspect only these prefixed lines before sharing excerpts. Collection fields and provisional-association limitations belong to [Shyworm implementation and validation](critters/shyworm.md#implementation-and-validation).

### Recorded evidence, policy and bounds

The recorder copies allowlisted aim/movement/camera, supported use/continuation, capsule packet/lifecycle/tick/display state, nearby identified target bounds, loaded-block intersections and typed capture feedback. It works independently of ESP and capsule hiding. Start-header hiding settings describe only session start; keep them fixed within a comparison block.

Positions are relative to the starting eye, preserving game axes and block units; the absolute origin is omitted. Velocities, aim and local display translations remain vectors. Monotonic microseconds are client arrival times. Delayed metadata preserves original packet `observedMicros`; raw bases/path points remain separate from smooth tick samples. Missing bases, truncated paths and cache gaps stay explicit. Informational `scan_pending` means work continues, while dropped pending evidence is lossy. Empty iterator slots count toward scan limits; real inspection exceptions stop the recorder with local diagnostics. Preexisting/metadata-discovered capsules are not automatically new local spawns; reused numeric IDs get new lifetime tokens/generations.

`candidate_unique` means one candidate within 1.5 seconds and 8 blocks of an input, a collection heuristic, not verified ownership. Overlapping inputs/multiple candidates stay ambiguous. Typed shared captures and startles cannot settle a local shot; escape is after a hit. Removal, unload, eight-second expiry, recorder stop or absent chat cannot establish a miss/impact. Expired lifetimes cannot become fresh tracks after another input. Terminal records remain evidence even after sampling ends.

Recorder policy v3/schema v2 adds one contact probe per eligible **ordinary main-hand** input. Probe eligibility remains strict: dry, grounded, stationary first-person player, both hiding options off, two stable ticks, finite pitch −90° through +90°, no sprint/sneak/flight/riding/screens/hidden HUD. Stability uses ≤0.001 horizontal blocks/tick and ≤0.001 position change per axis. Preview can be off. Probes retain the old unsupported-fluid boundary and omit display-only mound hulls/wrapped markers, while supported typed Shyworm body proxies can remain. Probe-local `p1`… target keys and session-relative coordinates do not prove identity. Detail can compact below 64 KiB with explicit omitted counts/`truncated=true`; raw export completeness and probe completeness are separate.

The v2 raw sampling policy continues in v3: fresh full target snapshots at each input, broad keyframes every two seconds, change-sensitive focused updates up to 5 Hz shared across at most eight candidates. Each batch rechecks 64-block range from eye/observed capsules; out-of-range targets release slots. Target raw packets/display transforms are not exported, though internal bases update. Recorder v1 used per-tick target samples plus target packet/display traffic; do not equate policy sampling rates or use sparse context as absence proof.

Caps: **300 inputs, 100,000 events, 10 MiB encoded output, 600 seconds**; queue and half-second pre-input ring share **4 MiB**. Sampling continues eight seconds after the latest input. Raw cache discovery uses a nominal 256-slot allowance minus the pre-discovery cache size, followed by sampling the updated cache; it is not a strict whole-tick 256-inspection bound (see [audit D11](IMPLEMENTATION_AUDIT.md#d11)). Raw lifetime caps remain 16 simultaneous/256 session capsule tracks and 64 target tracks. Queue/cap/error termination is incomplete. Condensed footer summaries do not replace retained timeline evidence. No chunks are deliberately loaded for contact probes; the older raw obstruction event uses its earlier loaded predicate and is not proof of complete terrain coverage.

### Collection procedure

Start with the matching Actions JAR, inexpensive ordinary capsules, one thrower and a small 180-second recording. For strict contact probes, use a stationary grounded first-person dry player, both hiding switches off and at least eight seconds between throws; stop explicitly in the same world. Confirm probes/raw paths/typed feedback before expanding. The guide can stay off.

1. **Six-throw sanity:** two near-level into open terrain, two downward at empty nearby ground, two at a small ground critter. Label the relevant scenario `unassigned`.
2. **Wider-angle fitting:** twelve throws, three near −30°, +15°, +35° and +60°; label `stationary_up fit` or `stationary_down fit`. Recorded pitch defines bins. The current probe angle gate permits this wider range while retaining its other strict collection conditions.
3. **Close terrain fitting:** twelve throws, four toward feasible ground/wall points about 1, 2 and 4 blocks away; use `solid_obstruction fit`. Put partial-shape investigation in a separate `edge_obstruction fit` session.
4. **Proxy fitting:** twelve attempts, six small ground and six larger/airborne targets, initially slow/stationary, both unobstructed and wall-adjacent. Use `moving_target fit` only when moving; keep intended species in a separate local note.
5. **Independent validation:** freeze hypothesis/proxies and agree tolerances from fitting evidence first; then collect fresh twelve terrain and twelve critter attempts in new sessions with corresponding `validation` labels. Do not reuse fitting shots or select validation examples after residual inspection. Ordinary calibration does not require scarce Masterful throws.

For general raw-motion diagnostics after the sanity check, keep ordinary/Masterful blocks separate. Target roughly 5/15/30-block level/up/down cases, five isolated attempts per feasible combination; reserve a second session for five fresh validation attempts. Separate five-attempt walking-forward/sideways, moving-target, solid/edge obstruction blocks; where practical add five rapid-input/nearby-thrower sequences and a new hidden-capsule session. Record exclusions instead of forcing impossible geometry. These sample targets do not establish statistical sufficiency. Transfer/disconnect should stop observation; deliberately share only chosen exports/curation. Collection questions and remaining model work belong to [issue #51](https://github.com/johnceoskyblock/sparkling-mutuals/issues/51).

### Local replay and analysis

Use Python **3.11+**, standard library only, from the repository or extracted tools root. Keep [tool package layout](../tools/capture_shots/README.md) intact; every report needs a new output directory and leaves inputs unchanged.

```text
python tools/capture_shots/replay.py session.jsonl --output replay-directory
python tools/capture_shots/contacts.py session.jsonl --output contact-directory
python tools/capture_shots/analyze.py --fit fit-session.jsonl --validation validation-session.jsonl --curation choices.json --output analysis-directory
```

Replay emits `summary.json`, `timeline.csv`, generated `shot-0001.csv` views and separate contact predictions. Imported tokens never choose filenames; rows preserve typed JSON and unmatched evidence. Contacts reports prediction/coverage, typed/manual outcomes and raw removals separately. `predicted_contact_provisional` means partial coverage/truncated detail; endpoint error stays null without independent terminal coordinates. Removal/last position/capture feedback cannot supply them.

The strict reader checks the [v1](schemas/capture-shots-v1.schema.json)/[v2](schemas/capture-shots-v2.schema.json) contract, closed fields/enums, finite numbers, sequence/time/token lifetimes, footer counts/bytes and bounds. Complete files exit 0, recoverable incomplete files 2, invalid files 1. A truncated final UTF-8/JSON line retains earlier complete records as incomplete; malformed complete interior lines fail. Never invent a footer.

Default analysis excludes uncurated, ambiguous, unmatched, incomplete or incorrectly labelled pairs. A finalized file may have incomplete tracks without observed server removal; expiry/unload/recorder-stop tracks are excluded by default. `--provisional` keeps them as labelled diagnostics. Ownership curation is separate from raw files and uses the input SHA-256 from replay:

```json
{"schemaVersion":1,"choices":[{"sha256":"COPY_THE_64_CHARACTER_INPUT_HASH","session":"COPY_SESSION_TOKEN","shot":"s1","track":"c1","rationale":"Explain the isolated pair selection."}]}
```

Repeat `--fit`/`--validation` for more sessions. Reports separate variants, exclusions, input/feedback delays, arrival gaps and apparent visual speed. Raw arrival endpoints are the default sample source; tick-only descriptions and stepped packet offsets do not become server timing. Constant velocity/acceleration are diagnostic baselines. Fitting trains on each track's first half and scores later points. Independent validation freezes component-wise median fitting acceleration per variant, initializes from its first two distinct-time points, excludes those conditioning samples and never refits. Missing variant evidence stays unavailable; same-session/source-byte split overlap is rejected even after renaming.

Residuals give count, RMSE, nearest-rank p50/p95, maximum and **last observed point** error in blocks, not impact error. No offline baseline is selected for live aiming. For external predictions, `last_observed_error` can belong to the last matched observation when the final observation is uncovered; inspect `uncovered_observations` ([audit D10](IMPLEMENTATION_AUDIT.md#d10)). Optional `--predictions predictions.csv` compares supplied coordinates only at exact observed times, with ordered columns:

```text
schemaVersion,session,shot,track,micros,x,y,z,model,coordinates
```

Use schema 1, a short model identifier and `session_relative_game_axes`. Uncovered observations/predictions remain reported; no interpolation/collision/outcome is invented. For synthetic arithmetic only:

```text
python tools/capture_shots/analyze.py --fit src/test/resources/shots/accelerating-fit.jsonl --validation src/test/resources/shots/accelerating-validation.jsonl --provisional --output synthetic-analysis
```

## Lifecycle and persistence

Files stay under `<Minecraft game directory>/sparkling-mutuals/shots/`; development instances use separate `run/<target>` directories. Random-token create-new `.jsonl.part` files are renamed to `.jsonl` only after a closed footer, without overwriting prior exports. UTF-8 JSON Lines includes a versioned header (`modelVersion: null`), ordered typed events and footer. Old files are never rewritten. Crash/disk failure can leave recoverable partials; nothing uploads or deletes exports automatically.

Leaving Safari, changing worlds, disconnecting, shutdown or caps stops observation. Wait for local finalization notice before starting another session. Exceptions log one recorder-related original cause/stack with operation/stage, session and relative time/tick when available. Status uses a concise reference; writer errors lack a client tick. Error termination stays incomplete and completed prior exports remain intact. Diagnose an unexplained early error from its own chosen session/error excerpt, not an invented cause.

Only allowlisted fields export: no authentication files, credentials, server addresses, names/UUIDs/skins, raw chat, whole item/inventory data, screenshots or automatic uploads. Anonymous nearby-player counts flag possible interference without proving no unseen thrower. Check chosen files/excerpts before deliberate sharing.

Saved projection settings survive world/reset; transient geometry and pending shader submission do not. Enabling projections never starts recording. Target discovery can run for the guide or an explicitly active recorder, once per tick through shared scene ownership.

## Edge cases

Raw evidence, prediction probes, visual markers, manual annotations and capture/completion ledgers have separate meanings. A visible mound estimate is not recorded collision evidence; a body diamond is not server ownership; a complete raw export can still have truncated probe detail. Crowded scenes can hit independent raw-target/guide limits.

The supplied [solo run](https://www.youtube.com/watch?v=9QvWx2mRHW0), [routes/roles guide](https://www.youtube.com/watch?v=1Z_DmFsVl20) and [Haunted run](https://www.youtube.com/watch?v=uvsl8I40_YE&t=116s) are representative scene references, not frame-by-frame calibration. The routes guide's older bird/bestiary advice is not current tracking authority. The Haunted samples at 1:56 (downward Litterbug) and 5:24 (Masterful Doomspiral feedback) motivate cases but cannot set coefficients/hitboxes or merge Masterful with ordinary evidence. Generated summaries had timeline inconsistencies and are not mechanics evidence.

## Source and validation references

- Startup/tick/reset: [SparklingMutuals](../src/main/kotlin/net/johnceo/sparklingmutuals/SparklingMutuals.kt); controls: [ShotCommands](../src/main/kotlin/net/johnceo/sparklingmutuals/commands/ShotCommands.kt), [SafariSettings](../src/main/kotlin/net/johnceo/sparklingmutuals/config/SafariSettings.kt), [ConfigManager](../src/main/kotlin/net/johnceo/sparklingmutuals/config/ConfigManager.kt).
- Recorder/lifecycle: [ShotRecorder](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotRecorder.kt), [ShotSession](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotSession.kt), [ShotObservationHooks](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotObservationHooks.kt), [ShotObservationAdapter](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotObservationAdapter.kt), [ShotTargetSampler](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotTargetSampler.kt), [ShotContactProbe](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotContactProbe.kt), [ShotExportWriter](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotExportWriter.kt). Actual input/use/packet hooks are registered in [mixins](../src/main/resources/sparkling-mutuals.mixins.json).
- Guide owners: [CapsulePreview](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/CapsulePreview.kt), [CapsulePreviewRules](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/CapsulePreviewRules.kt), [CapsulePathModel](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/CapsulePathModel.kt), [CapsulePreviewRenderer](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/CapsulePreviewRenderer.kt), [CapsuleContactScene](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/CapsuleContactScene.kt), [ContactCurveSampler](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/ContactCurveSampler.kt), [CapsuleContactSolver](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/CapsuleContactSolver.kt), [ShywormContactParts](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/ShywormContactParts.kt), [MoundContactSurface](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/MoundContactSurface.kt), [ContactSurfaceRing](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/ContactSurfaceRing.kt), [IrisCapsuleCompat](../src/main/kotlin/net/johnceo/sparklingmutuals/compat/IrisCapsuleCompat.kt).
- Existing secondary suites: [recorder](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotRecorderTest.kt), [probes](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotContactProbeTest.kt), [guide lifecycle](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/ContactGuideLifecycleTest.kt), [contact solver](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/contacts/CapsuleContactSolverTest.kt), [Python tools tests](../tools/capture_shots/tests). These locate software checks; this documentation audit did not run them, clients or new gameplay measurements.

Historical recorder-v2 budget regression used 64 changing targets at 20 ticks/second, 64 unrelated target packet updates/tick, changing aim, input snapshots and forty capsule updates/throw. Its 180-second/20-throw workload exported 18,424 events / 7,328,547 bytes and retained 800 capsule updates; the 600-second/nine-throw workload exported 8,326 events / 3,316,366 bytes and retained 360. These are retained prior software-budget observations, not new audit results or live Safari traffic guarantees.

Broader trajectory, endpoint, collision/proxy, hidden-capsule fidelity and Hypixel visual checks remain pending. Builds/synthetic arithmetic/offline smoke checks cannot establish them. Live visual checks should cover camera turns/FOV/GUI scale, hidden HUD/screens, item/hand/motion/water changes, world transfer, terrain-visibility modes, shader toggles, contact ordering and recording independence. [Issue #51](https://github.com/johnceoskyblock/sparkling-mutuals/issues/51) retains unfinished calibration/model work.

## Last updated

2026-10-19 (America/New_York) — Audited recorder, projection, contact/probe separation, tools and lifecycle against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; consolidated historical experiment evidence and retained live-validation limits. See [documentation conventions](DOCUMENTATION_GUIDE.md).

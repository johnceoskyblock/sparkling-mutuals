# Experimental terrain and critter contact guide

## Status and agreed intent

The human approved the proposal on issue #51 in the implementation chat: **"Ok, I approve of the proposal"**, after requesting both terrain and critter contact and reviewing the video-informed refinements. After reviewing this written specification, the human answered **"I approve of it"** in the same chat. The written specification is approved; the six-task Native implementation plan is also approved.

The player needs a visible aiming guide with a useful estimated contact point, including downward and close-range aiming. The current short line and narrow pitch gate do not meet that eventual goal. Deliver an opt-in experimental extension that distinguishes terrain contact from possible critter contact, explains unsupported estimates, and supplies reproducible validation support.

Work stays on `feature/capture-shot-projection`, starting from `a7c462648fbd91e714339e2eae6e5987bf2447f8`. Build and deliver **Minecraft 26.1.2 only** during this work. Use Native execution after plan approval. Preserve tracking, ESP, capture memory, ordinary item-use results and recorder independence. No PR, merge, release, master change or version bump. Scheduled issue checks remain paused.

## Evidence and what it does not establish

The existing fixed ordinary-capsule curve was checked against separate stationary observations within a narrow angle/range envelope. Its parameter is an observed update ordinal, not flight time. Keep its coefficients and identity unchanged. Neither that check nor a passing build establishes collision, launch origin, target-selection rules or visual alignment in every condition.

The human supplied these public references:

- [Solo Safari run](https://www.youtube.com/watch?v=9QvWx2mRHW0): representative sections show varied terrain and dense overlays.
- [Routes and roles guide](https://www.youtube.com/watch?v=1Z_DmFsVl20): chapters include Cave Top 1:35, Cave Bottom 3:46, Forest 7:45, Haunted 12:03 and Ice 15:57. Its description explicitly notes older bestiary/bird-feeder rules; do not import that advice as current tracking requirements.
- [Haunted run](https://www.youtube.com/watch?v=uvsl8I40_YE&t=116s): the sampled 1:56 view shows downward aiming at a Litterbug near furniture. The sampled 5:24 feedback identifies a Masterful throw at Doomspiral; keep it separate from ordinary-capsule calibration.

These were representative source checks, not an exhaustive frame-by-frame measurement. Generated scene summaries had timeline inconsistencies and are not mechanics evidence. Do not derive coefficients, hitbox dimensions, collision priority or guaranteed outcomes from them. They motivate tests for small targets, close downward shots, stairs, ledges, furniture and walls. Keep local recordings, unrelated logs, identities and analytical payloads outside the repository and public comments.

## Controls and supported conditions

Retain **Experimental capsule path**, default off. Add **Allow uncalibrated estimates**, also default off, subordinate to it. The second option explicitly enables the broader curve and contact markers. This is necessary because launch extrapolation and collision themselves are uncalibrated, even at the previously tested pitch. Help text must say: **Experimental terrain and critter contact estimates. Launch, collision and target bounds are unverified.**

With the second option off, preserve the current stationary ordinary-capsule path from ordinal 2 to nominal 15 blocks, including the -10 through +1 degree gate and existing unavailable reasons. Appearance controls apply, but no contact marker appears. Do not silently reinterpret the current short curve's last point as a landing point.

With both options on, allow finite pitch across the normal first-person look range, including steep upward and downward views. Keep all other eligibility restrictions: loaded Safari world, ordinary capsule in the main hand, first person, grounded and stationary for two stable ticks, no sneaking/sprinting/swimming/flying/riding, both capsule-hiding options off, no open screen and visible HUD. Masterful, moving-player, off-hand-only and third-person predictions remain unavailable. Starting or enabling the preview never starts recording.

Add a saved line-width control from 1 through 5, default 3; retain the existing fixed model without user-editable speed/gravity/radius controls. Existing configurations receive safe defaults. Reset transient state on world replacement, leaving Safari, disconnect, item/view changes and invalid inputs.

## Curve, launch gap and limits

Preserve the model arithmetic:

```text
speed = 0.8482482249942948
verticalAcceleration = -0.020085553311180723
p(n) = eye + speed * n * normalizedAim
       + (0, 0.5 * verticalAcceleration * n * (n - 1), 0)
```

The first preview omitted approximately the first 1.7 nominal blocks because its first included observation was ordinal 2. In contact mode, extending this formula from `n = 0` at the eye is an explicit **launch extrapolation**, not newly measured launch physics. Version this wrapper hypothesis as `contact-guide-center-extrapolation-v1`, referencing the original fixed curve identity rather than overwriting the frozen model. Include that prefix in contact ordering so a nearby obstacle is not silently skipped. Mark the prefix uncalibrated; all reported contacts remain conditional on this launch assumption. A future correction requires separate fitting and validation evidence and a new hypothesis/model identity.

Bound the extrapolated curve by the first of: 64 blocks of accumulated polyline length, ordinal 128, or 256 segments. Include each segment's start and end; finite nonzero aim is required. Choose subdivisions with chord length at most 0.5 blocks and maximum parabolic chord deviation at most 0.01 blocks. If any bound prevents further work, mark the result range/work limited; a range cap is not a landing.

The observed-region style applies only at the existing pitch envelope and ordinals 2 through `15 / speed`; other segments, including the launch prefix and wider-angle curves, are dashed. This styling means evidence coverage for the visual curve, not validated collision. Do not animate flight time, inherit player velocity, add drag, fit at runtime or claim new accuracy from extrapolation.

## Terrain contact

Intersect each bounded curve segment with actual loaded block collision shapes. Stairs, slabs, fences, furniture and other partial shapes must not become full-block cubes. Check every crossed chunk before any shape query; do not load chunks. Keep world/render coordinates separate and convert to camera-relative coordinates once at submission.

Use a zero-radius center-curve intersection as the explicit first hypothesis. Do not borrow a vanilla projectile radius or inflate shapes to make examples pass. Capture the hit position, surface normal, segment parameter and block-shape provenance. A point inside a solid shape is an immediate geometric contact, not an instruction to move the origin outside it.

Fluid behavior is not established. Detect a fluid boundary separately; if it occurs before a candidate contact, stop the estimate there with **Fluid behavior unavailable**, rather than treating water as a confirmed wall or projecting a certain contact beyond it. Unloaded geometry similarly ends the known segment and suppresses farther contact markers.

## Critter contact and target bounds

Use the existing pure species identification rules and fresh loaded entities, independently from ESP visibility, mode filters and recorder on/off. Exclude players, capsule entities, decorative/name-only labels, removed/replaced entities and objects whose current species/model identity cannot be resolved. Read existing capture/model validity where needed without changing its state or allowing a visual toggle to alter gameplay tracking.

Keep at most 64 candidate identities within the 64-block preview area. Each client tick has a shared maximum of 256 entity slots for discovery and revalidation; null entries count toward it. Range checks and identity checks apply before geometry use. Cache turnover, world identity and sweep completeness are explicit. Incomplete discovery or candidate overflow must not be presented as proof that a nearer critter is absent.

For ordinary entities with finite nondegenerate native bounds, use those bounds as a **client-body proxy**. Display/armor-stand species may need a visual-body proxy based on the existing unpadded ESP body placement: extract only the required pure placement calculation, preserving current ESP padding and rendering. Do not use an empty display box, label box, padded ESP outline or a guessed universal box as an authoritative capsule hitbox. Record each proxy's source and version. If a species lacks a meaningful identified proxy, mark it unsupported rather than borrowing another species' dimensions.

The first implementation intersects the curve with the critter proxy at its currently observed position. It performs no target-motion prediction. An indicator means **Possible critter contact**, never guaranteed hit/capture. Moving targets and stale/incomplete discovery increase uncertainty; unavailable target coverage suppresses a definitive nearest-contact marker and produces an explicit status. Candidate list absence alone cannot establish a miss.

## Ordering and visible result

Compute terrain and critter candidates against the same curve and coordinate snapshot. Order intersections by increasing curve parameter, not straight-line eye distance. Show one primary endpoint: a terrain ring if terrain comes first, or a critter diamond if a supported critter proxy comes first. A critter behind an earlier terrain intersection is not an eligible primary endpoint. On coincident or indistinguishable contacts, prefer the terrain obstruction and label the ambiguity; do not promise that this matches server target selection.

Clip the displayed path to that estimated primary contact. If no eligible contact is found before a range/work cap, show the bounded path with **No contact within preview range**, not a terminal ring or a miss claim. If required geometry/coverage is unknown, show only the known path and its unavailable reason. Do not display a hypothetical terrain landing beyond a critter as a second guaranteed outcome.

Use a dark contrasting line outline plus the selected core width, with no depth writes. Both passes remain terrain-depth-tested; preserve existing through-wall ESP defaults. Terrain uses an amber ring oriented to the surface; critters use a cyan diamond with a center mark. Shape and wording, not color alone, identify the result. Marker radius is a visual size of 0.25 blocks, not a collision radius. Bound marker geometry to 24 segments. Keep a concise HUD status below the crosshair; include **Uncalibrated** in expanded mode and **Possible critter contact** or **Estimated terrain contact** when relevant. Do not obscure the crosshair or flood chat.

Evaluate immutable frame data against the current player/world and render camera. Aim changes must not retain a marker from a previous aim. Target discovery/revalidation runs on client ticks; the bounded curve/contact solve uses current aim for the rendered frame. Recheck target identity and missing-world conditions before submission. No file access, fitting or world mutation in rendering.

## Component boundaries

| Component | Responsibility |
| --- | --- |
| Pure curve sampler | Fixed model, evidence flags, launch extrapolation and bounded subdivision |
| Pure contact solver | Segment/shape intersections, contact ordering, ties, unknown coverage and result reasons |
| Client scene adapter | Loaded block shapes, fluid boundaries, bounded candidate discovery, identity and proxy provenance |
| Frame builder/renderer | Fresh immutable result, outlined line/dashes, marker geometry and HUD wording |
| Optional diagnostic bridge | Snapshot a contact probe for an existing explicitly started recording; never control throwing or tracking |
| Offline contact diagnostics | Read saved probes and observations, preserve uncertainty, compare separate curated validation shots |

The contact engine does not depend on recording. Recorder input hooks keep their original return values and association semantics. The bridge samples a probe only for an already active recording and an ordinary input; the preview setting may be off during deliberate collection. It does not add per-frame exports or intercept packets.

## Reproducible contact evidence

Existing JSONL v1 stores raw targets and observed-path obstruction checks but cannot faithfully identify the new predicted curve/proxy result at input. Add a versioned **contact probe** event under JSONL schema v2 and recorder policy v3; preserve legacy event meanings. New tools strictly read both v1 and v2. Old files are never rewritten. Unknown versions and malformed/interior events remain errors; a truncated file remains incomplete.

A probe records only allowlisted typed data: input token, model/proxy versions, relative eye/aim, evidence flags, limits, target coverage, selected candidate proxies and the predicted result. At most 64 proxy entries and 256 predicted segments are allowed. Keep coordinates session-relative; use recorder-local tokens without runtime entity IDs or UUIDs. Include the queried terrain intersections/unknown boundaries needed to explain the estimate, with at most 32 shape boxes for each retained contacted block. If the 64 KiB line limit would be exceeded, omit diagnostic detail with an explicit truncated-probe flag; do not silently drop raw capsule observations or raise hard caps.

The existing 10 MiB session, shared 4 MiB queue/ring, 300-input, duration and event limits remain unchanged. Test the added per-input probes under the established populated sampler workloads; reaching any cap remains incomplete. Probes are predictions, not observed hits, misses, ownership or capture events. Replay displays them in a separate layer from raw positions, client samples and typed/manual outcomes.

Offline diagnostics report numerical path residuals, geometric predicted contact, observed terminal evidence and coverage separately. Removal, a last observed position, a capture animation, absent chat or a stopped recording cannot establish ground impact or a miss. Do not infer collision error where terminal evidence is unavailable. Freeze any revised candidate/proxy hypothesis before independent validation; never choose validation examples after seeing their residuals.

## Collection checklist

Supply the working 26.1.2 build and updated local tools before requesting this collection. Keep both capsule-hiding options off initially. Use ordinary capsules, first person, stationary player, one nearby thrower and at least eight seconds between throws. Record in separate 180-second sessions; stop explicitly while still in the same world. The recorder captures actual pitch/position, so no manual coordinate transcription is required.

1. **Sanity, six throws:** two near-level into open terrain, two downward at empty nearby ground and two at a small ground critter. Use `/sm shots start 180`, label the relevant scenario with `unassigned`, then `/sm shots stop`. Confirm probes, raw paths and typed feedback survive before expanding collection. Unknown outcomes stay unknown; do not mark a miss merely because no message appears.
2. **Wider-angle fitting, twelve isolated throws:** three each near -30, +15, +35 and +60 degrees, using open geometry with visible terminal behavior. Use `stationary_up`/`stationary_down` with `fit`. Actual recorded angles define the bins. These are fitting data only.
3. **Close launch/terrain fitting, twelve throws:** aim at visible empty ground/wall points roughly 1, 2 and 4 blocks away, four repetitions per range, including both steep downward and wall-facing cases. Use `solid_obstruction fit`. Include steps/partial shapes in a separate `edge_obstruction fit` session if the first examples suggest a different collision assumption. Do not force nominal range/angle combinations that cannot meet the chosen surface.
4. **Critter proxy fitting, twelve throws:** six small ground-target and six larger or airborne-target attempts, initially against slow or briefly stationary targets, with unobstructed and wall-adjacent examples. Use `moving_target fit` only when the target actually moves. Name the intended species in a local curation note; server feedback and raw association remain independent.
5. **Independent validation after freezing:** fresh sessions with at least twelve terrain and twelve critter attempts spanning the above cases, labelled `validation`; use different shots and preferably a new run. Retain earlier accuracy criteria only within their original trajectory envelope. Agree on endpoint/proxy tolerances using the fitting evidence before evaluating these validation sessions.

Do not request scarce Masterful throws for ordinary fitting. Masterful, player movement, underwater prediction and broader target-selection behavior are later work. If a field omits launch/terminal evidence or the sampler/probe loses coverage, report the missing evidence and adjust the working collection support before requesting a large repeat. Selected exports stay local unless the human explicitly authorizes sharing them.

## Verification and acceptance

- Pure regression fixtures cover launch-prefix obstacles, inside-shape starts, steep/vertical aim, chord bounds, translated origins, finite inputs, caps and one camera subtraction. Independently calculated examples check ordering, ties, terrain-before-critter, critter-before-terrain, degenerate/native/visual proxies, moving-target limitations, unknown chunks/fluids and incomplete candidate coverage.
- Geometry tests cover partial blocks, outlined/dashed line bounds, ring/diamond distinction, camera turns, immutable delayed submissions and unchanged ESP depth defaults. Eligibility/default/persistence/lifecycle tests preserve prior restrictions and verify the new explicit opt-in mode.
- Codec/schema tests cover real Kotlin-produced v2 probes consumed by Python, unchanged v1 fixtures, strict privacy fields/tokens, probe truncation, shared queue/session limits and raw-evidence retention. Downloaded tools must read both versions.
- Run relevant Kotlin/Python/build-helper tests and the documented target-qualified 26.1.2 build/package verification. Do not produce 26.2/26.3 development artifacts. Perform an independent review before delivery and resolve significant findings.
- Offline 26.1.2 client fixtures exercise a wall in front of a target, target before wall, stairs/furniture, close prefix obstruction, fluid/unloaded boundary, line visibility and disabled states. Confirm required hooks and existing tracking/ESP behavior. Profile the maximum bounded frame/discovery workload; structural bounds alone do not certify acceptable gameplay performance.
- Verify Actions on the final uploaded implementation commit; download and verify the 26.1.2 JAR, tools and reports. Provide clear enable/test instructions and material limitations. Documentation-only specification review does not require product builds or claim a fix is ready.
- Live Hypixel collection validates broader-angle flight, launch/terminal contact and target proxies separately. Until then the delivered contact guide is explicitly experimental and uncalibrated, not completion of all server-mechanics requirements in #51.

## Review and delivery boundaries

This specification intentionally makes uncalibrated mode the opt-in for contact markers as well as broader angles: launch and collision are unverified even when the visual curve lies inside the old angle envelope. Review that behavior together with the first-contact ordering, proxy policy and collection checklist.

After written-specification approval, prepare a concrete Native implementation plan for review. The human's proposal approval is recorded; do not ask the earlier terrain-versus-critter scope question again. Post specification/plan questions on the still-open issue under the latest routing instruction. Scheduled checks remain paused until explicitly resumed.

## Last updated

2026-10-17 (America/New_York) — Wrote the terrain/critter contact specification after direct proposal approval, including the reviewed video-informed refinements. Recorded subsequent direct written-specification approval; no product implementation is included.

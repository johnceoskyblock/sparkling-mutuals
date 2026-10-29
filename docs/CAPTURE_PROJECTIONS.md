# Capture projections

## Controls

In `/sm → Safari helpers → Capture tools`, **Capture projections** is the single enable switch and defaults **on** for missing/invalid settings; existing saved off choices are preserved. **Capsule path width** saves from 1–5, default 3; **Show guide through terrain** defaults on. Its off state restores terrain-occluded rendering without changing predicted collision stops. The option uses the existing `experimentalCapsulePath` key; obsolete second-gate/status-HUD properties are ignored. There is no projection text overlay or HUD-editor status entry.

The visual guide works independently of ESP toggles and either capsule-hiding option. It uses an ordinary or Masterful capsule in the main hand in Safari, with finite aim across pitch −90° through +90°. Walking, sprinting, sneaking, jumping, falling, standing/swimming in water and third-person views are supported as uncalibrated estimates. Flying, riding, lava, unsupported items and off-hand-only use suppress it. Screens and hidden HUD suppress geometry. Expanded display does not require two stationary ticks.

The route starts from the player's interpolated eye and current player aim. Camera position only displays it: detached-camera angles and the front-third-person crosshair do not retarget the character's route. This does not validate server throwing through a camera mod. No momentum correction or Masterful-specific curve is applied.

## Behavior

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

`/sm debug` emits bounded `SM_SHYWORM_BODY` snapshots; `contact_scene` adds cached discovery/queue/coverage diagnostics without another scan. This explicit action only writes local diagnostics. Inspect only these prefixed lines before sharing excerpts. Collection fields and provisional-association limitations belong to [Shyworm implementation and validation](critters/shyworm.md#implementation-and-validation).

## Lifecycle and validation

The live guide owns a bounded contact cache. Typed entity updates refresh its target positions and metadata after vanilla applies each packet. World transfer, disconnect, disabling the feature and invalid view state clear the guide. Capture history and completion tracking remain independent.

The development recorder, `/sm shots` commands, offline capture-shot tools, JSONL schemas, recording fixtures and their CI job have been removed. Existing files previously exported by users are untouched. Automated regression tests for the live curve, collision ordering, target geometry, cache invalidation and rendering lifecycle remain under `src/test`; they run during development/build checks and are not included in the playable mod.

The feature retains the existing curve, contacts, visibility rules and saved settings. It does not automatically throw capsules or guarantee a capture.

## Last updated

2026-10-29 — Retired capture-shot collection/calibration tooling and kept production projection regression coverage.

# Sparkling identification and presentation

## Scope

Authoritative reference for named/skin detection, particle locations, aliases and notifications. Detection is presentation evidence; [SAFARI_LOGIC](../SAFARI_LOGIC.md) owns completion overrides and [tracking](critters/TRACKING.md) owns ordinary checks and captures.

## Definitions

An **identified detection** has a canonical species from a Sparkling name or registered skin texture. An **anonymous particle location** is a last-seen packet origin, without a species/entity identity. Neither is an ordinary capture or a distinct-population count.

A model and matching nearby labels can be **aliases** of one detection. Notification memory, pending identities and confirmed catch evidence are separate run-owned states.

## Implemented rules

### Positive name and skin evidence

Every five client ticks during Safari, scan currently loaded nonremoved entities. An exact Sparkling species name or known texture hash is positive evidence. Read current metadata again each scan; there is no permanent negative skin cache or invented startup delay.

Player models use current player-info profiles when available. Armor-stand head/item-display profiles can also carry skins. Local/listed real players and known party players are excluded. Skin-only positives must be in the species' expected Safari quadrant. A recognized name takes priority when skin metadata disagrees.

A matching body is associated with nearby labels within three blocks; matching skin is preferred, then distance/entity ID. Alias identities feed the same alert, glow, detected panel and capture-presentation state. A resolved known alias is excluded from the detected HUD and ordinary live-population consumers while it remains loaded. A genuinely new identity remains detectable.

All loaded positives across Safari can appear, without an 80-block/current-biome detection cap. The Detected sparklings panel shows species, biome and distance, with up to six rows plus an overflow count. Ordinary check circles are unchanged. No client filter removal can discover entities or metadata the server did not send.

### Particle locations

SafariParticleMixin observes WAX_ON packet origins on the client thread after the handler thread check, even when vanilla particle visibility/distance would suppress rendering. Processing requires active Safari; Torrhus/entrance/other islands are excluded.

There is no player-distance/current-biome filter, but origins must be finite, Y 0–160 and inside a recognized Safari quadrant. Nearby origins in the same biome within three blocks coalesce and update last-seen position/time. At most 128 stored locations are accepted per run. These are approximate packet origins, not tracked moving entities or reliable counts.

After the next name/skin scan, an unidentified location may show SPARKLING! / Critter Unknown locally. Unknown detections never send party chat; a party notice is eligible only after the species is identified. The world label and HUD retain last-seen coordinates through unloading/travel. Debug output identifies source=wax_on; source terminology does not appear in title/party text.

Only an independent name/skin positive within three blocks retires an anonymous waypoint. It also clears its unknown banner. Known detections take priority in the scan and can send the one named notice for that species in the run. Unrelated catches/reports cannot identify an anonymous marker.

### Notification controls and retention

| Channel | Gate | Memory |
| --- | --- | --- |
| Identified GUI/sound alert | Sparkling alert enabled; sound uses its own volume/sound values | Once per species per run |
| Identified party notice | Sparkling party announcer enabled | Once per species per run; queue paced at least 1.5 seconds |
| Anonymous GUI/sound | Same independent alert controls | One shared unknown notification per run across all locations |
| Anonymous party notice | Never sent | Unknown detections do not consume a named species notice |
| Glow, detected panel, anonymous world labels | Sparkling detection display toggle and rendering/UI gates | Current named entities; retained anonymous locations |

Switching alert/ESP toggles does not stop the underlying detector. Capsule transitions, escapes, replacement identities, travel and mode changes do not reset notification histories. A new run/world reset does. Disabling party announcements or leaving Safari clears queued party notices; a notice already reserved in notification memory is not automatically requeued on reenable.

Anonymous locations continue tracking after their one notification. Spatial clustering can merge nearby critters or produce multiple locations for a moving one. Identified locations remain in the bounded internal list but are absent from unknown presentation.

### Capture reconciliation

Confirmed Sparkling reward/global announcement pairs within five seconds, for matching species and compatible catcher attribution, reconcile as one Sparkling proof. That state resolves one pending species identity; aliases remain bound to their canonical identity. It is not an arbitrary UUID-to-catch proof and does not guarantee every duplicate identity disappears.

Pending detections never add mandatory capture requirements, reopen species, block reports/alerts, or block Unique PBs. They may coexist with valid completion. Ordinary positive remainder observations still follow their own Full Clear rules. Confirmed Sparkling catches override ordinary Unique/Sparkling completion; birds are evaluated first and retain spawning requirements.

### Skin reference

Provenance: [user-supplied Sparkling skin sheet](https://docs.google.com/spreadsheets/d/1BV0F_beJTcUCS99-5mIBqVx4tDJwcAV-gDAO39YH54U/edit?gid=0), retrieved 2026-10-15 in the earlier audit. The bundled [registry](../src/main/resources/assets/sparkling-mutuals/data/sparkling_skins.json) has 36 species texture hashes; Tepid has no supplied skin and uses name fallback. Sheet spelling Arieta was normalized to roster Areita.

The prior inspection decoded supplied base64 textures, compared texture links and found no overlap with ordinary identifiers. This is recorded provenance, not fresh verification or proof of live server skin use. The registry stores hashes/provenance, not account names, signed payloads or a runtime sheet download. No image recognition or player-name lookup is involved.

## Exceptions

A name can identify Tepid without a supplied skin. Skin positives require the expected biome; named positives follow the name path. Ordinary Honeybug/structure/boss checks remain independent. Anonymous particles never assign a guessed species, ordinary UUID check or catch.

## Examples

- A loaded body has a registered skin before its name arrives: it can alert. A later matching label joins its aliases without another species notification.
- WAX_ON packets arrive from another loaded biome: retain an unknown waypoint. No packet is expected for an area the server never sends.
- Several anonymous locations produce one unknown notification. Identifying one as Macaw can still produce one named Macaw notification.
- A new detected Sparkling after ordinary completion adds presentation only; capture/source rules independently decide any progress change.

## Source and validation references

Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee.

- [SafariAssist](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariAssist.kt): five-tick scan, current profiles, particle processing, glow, HUD and diagnostics; registered by [initializer](../src/main/kotlin/net/johnceo/sparklingmutuals/SparklingMutuals.kt).
- [SparklingDetector](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingDetector.kt), [SparklingParticleLocations](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingParticleLocations.kt), [SparklingEncounters](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingEncounters.kt), [SparklingCaptureState](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingCaptureState.kt): identity, bounded locations, notification memory and reconciliation.
- [SafariParticleMixin](../src/main/kotlin/net/johnceo/sparklingmutuals/mixin/SafariParticleMixin.kt) is listed in [mixins registration](../src/main/resources/sparkling-mutuals.mixins.json); [SafariRun](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt) composes completion separately.
- Existing checks: [SparklingDetectorTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingDetectorTest.kt), [SparklingParticleLocationsTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingParticleLocationsTest.kt), [SparklingMemoryTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingMemoryTest.kt), [SparklingCaptureRequirementTest](../src/test/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingCaptureRequirementTest.kt). Not run locally in this audit.

## Open questions

User observation, 2026-10-15: Duplico at (-4, 71, -72) unloaded near player (-73, 65, 25), approximately 119.19 blocks in 3D / 119.04 horizontally. Its Sparkling status was unspecified; this is one reported unload observation, not a universal limit or confirmed Sparkling range.

Live checks remain pending for delayed skin updates, real-player exclusion, cross-biome received particles, low vanilla particle settings, clustering near moving/multiple Sparklings and replacement aliases. The user's wax-on association and duplicate-capsule entity explanation are observations requiring confirmation. No whole-Safari coverage is guaranteed.

## Party wording

Only the actual `SPARKLING <Critter>! (<Biome> <Coordinates>)` detection notice uses the detection keyword in mod-authored party text. Lookup results use Shiny/Shinies, streak replies use shiny, and completion-time supply reminders use Shiny. Party-only achievement names and echoed downtime reasons replace sparkling/sparkle with shiny while their local wording remains unchanged. Command help lists `!since` and `!since unique`. Legacy incoming discovery-result headings remain recognized for compatibility.

## Last updated

2026-10-21 — Reserved party detection keywords for actual sightings; changed non-detection party wording without changing local text or API requests.

2026-10-21 — Unknown detections retain local alerts only; party announcements wait for identification and remain once per species per run.

2026-10-19 (America/New_York) — Audited detector/particle wiring, notification limits and completion separation; preserved skin/range provenance. Reviewed source: 93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee; static source/caller review, no live Minecraft validation.

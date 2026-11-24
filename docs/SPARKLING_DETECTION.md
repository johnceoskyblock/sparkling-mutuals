# Sparkling identification and presentation

## Identified sightings

The client matches sparkling names and known skins to a critter and its displayed model, then reconciles the label and body as one sighting. Capture labels and models riding a capture capsule are excluded from new alerts. The **Detected sparklings** HUD lists identified critters with biome and distance, up to six rows plus an overflow count.

- **Standard** gates highlights, HUD rows, and identified alerts on local visibility and rechecks visibility before a queued alert plays. Once started, the seven-second alert finishes even if the critter leaves view; highlights and nearby rows still require current visibility. A recognised sparkling skin can identify a visible body without a visible nametag. It uses local identified sightings for the alert.
- **Full** and **Full No Auto Update** can display loaded detections and accept a named discovery from another compatible client in the same Safari run. Local and shared detections use the same per-species alert memory.

## Particle evidence

The Full builds can show a single **Critter Unknown** alert from eligible wax-on packets. Capture markers and outstanding shared sightings in that biome suppress redundant particle alerts. Particle hints stay out of the Detected sparklings HUD and do not create last-seen waypoints or an identified party notice. Standard can retain particle evidence internally and alerts once the critter is identified and visible.

## Alerts and completion

The settings for sparkling alerts, nearby sightings, and party announcements are independent. Eligible party announcements name the critter and location. Alerts use the shared alert queue, and the encounter resets on a new run. Confirmed captures clear stale sightings and queued notices.

**Include sparkling catch count** sends the species total from your saved party catch history. A confirmed first discovery also says `NEW!`.

Sightings do not add capture requirements. Confirmed sparkling catches are evaluated separately from ordinary checks, source completion, and personal bests. [Safari modes and completion](../SAFARI_LOGIC.md) · [Party sync](PARTY_SYNC.md)

## Source and skin provenance

- [Full encounter presentation](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingEncounters.kt) · [Standard encounter presentation](../src/standard/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingEncounters.kt)
- [Full SafariAssist](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariAssist.kt) · [Standard SafariAssist](../src/standard/kotlin/net/johnceo/sparklingmutuals/safari/SafariAssist.kt)
- The bundled registry contains the 36 supplied skin hashes reviewed in October 2026. Tepid uses a name fallback. [Bundled skin registry](../src/main/resources/assets/sparkling-mutuals/data/sparkling_skins.json) · [Original skin sheet](https://docs.google.com/spreadsheets/d/1BV0F_beJTcUCS99-5mIBqVx4tDJwcAV-gDAO39YH54U/edit?gid=0)

## Last updated

2026-11-23 (America/New_York) — Reconciled visible, shared, and particle-only sightings with the two gameplay editions.

Standard critter highlighting uses a depth-tested box fallback when terrain partially blocks a model or the model cannot use glow. Fully hidden models stay unmarked; covered box edges remain hidden by terrain.

## Standard detection integration

Standard combines named sightings and known skins with repeated particle corroboration on the existing critter-body scan. It requires three matching WAX_ON packets (count 5, spreads 0.30/0.50/0.30, speed 1.00), associates them within 1.5 blocks with a live identified body, rejects equidistant ambiguity and capture candidates, and expires evidence after 2.5 seconds. Evidence is bounded to 256 identities and resets on departure/world change. Anonymous packet locations no longer drive Standard alerts.

The first alert requires body visibility for Hideyho, Hideonwall, Duplico, Bloodbat and Hideonfloor. Other species can qualify by visible body or a vanilla-rendered custom name, including through terrain. Duplico accepts direct targeting or a ray reaching its nearby disguise surface; paintings remain blockers. Started alerts keep their normal duration; moving highlights remain visibility-gated. This does not add personal catches or replace run completion rules.

Behavior reference inspected: SafariUtils commit 12525e676f44299ebbe117c665351b8dc4650a9b, SparklingWatch, ParticleDiagnostics, VisibilityCheck and HideyhoSolver. This integration reuses Mutuals' own detector, alert queue, skin registry and capture tracking rather than importing their Java implementation.

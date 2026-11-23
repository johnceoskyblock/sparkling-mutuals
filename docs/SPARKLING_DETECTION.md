# Sparkling identification and presentation

## Identified sightings

The client matches sparkling names and known skins to a critter and its displayed model, then reconciles the label and body as one sighting. Capture labels and models riding a capture capsule are excluded from new alerts. The **Detected sparklings** HUD lists identified critters with biome and distance, up to six rows plus an overflow count.

- **Standard** gates highlights, HUD rows, and identified alerts on local visibility and rechecks visibility before a queued alert plays. It uses local identified sightings for the alert.
- **Full** and **Full No Auto Update** can display loaded detections and accept a named discovery from another compatible client in the same Safari run. Local and shared detections use the same per-species alert memory.

## Particle evidence

The Full builds can show a single **Critter Unknown** alert from eligible wax-on packets. Capture markers and outstanding shared sightings in that biome suppress redundant particle alerts. Particle hints stay out of the Detected sparklings HUD and do not create last-seen waypoints or an identified party notice. Standard can retain particle evidence internally and alerts once the critter is identified and visible.

## Alerts and completion

The settings for sparkling alerts, nearby sightings, and party announcements are independent. Eligible party announcements name the critter and location. Alerts use the shared alert queue, and the encounter resets on a new run. Confirmed captures clear stale sightings and queued notices.

Sightings do not add capture requirements. Confirmed sparkling catches are evaluated separately from ordinary checks, source completion, and personal bests. [Safari modes and completion](../SAFARI_LOGIC.md) · [Party sync](PARTY_SYNC.md)

## Source and skin provenance

- [Full encounter presentation](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingEncounters.kt) · [Standard encounter presentation](../src/standard/kotlin/net/johnceo/sparklingmutuals/safari/sparkling/SparklingEncounters.kt)
- [Full SafariAssist](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariAssist.kt) · [Standard SafariAssist](../src/standard/kotlin/net/johnceo/sparklingmutuals/safari/SafariAssist.kt)
- The bundled registry contains the 36 supplied skin hashes reviewed in October 2026. Tepid uses a name fallback. [Bundled skin registry](../src/main/resources/assets/sparkling-mutuals/data/sparkling_skins.json) · [Original skin sheet](https://docs.google.com/spreadsheets/d/1BV0F_beJTcUCS99-5mIBqVx4tDJwcAV-gDAO39YH54U/edit?gid=0)

## Last updated

2026-11-23 (America/New_York) — Reconciled visible, shared, and particle-only sightings with the two gameplay editions.

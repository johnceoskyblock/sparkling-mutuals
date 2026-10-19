# Sparkling detection and mode progress

## Shared positive evidence

The detector runs every five client ticks during Safari and scans entities the client currently has loaded. A positive identification comes from either an exact `Sparkling <species>` name or a matching known Sparkling skin texture. The same identification feeds the Sparkling alert, glow, detected-critters HUD and run-owned identity tracking. Detection never adds a mandatory capture requirement, independently of alert/ESP toggles.

Detected Sparkling identities do not block or reopen species/biome completion in any mode. The user reports duplicate entities during capsule capture; those detections must not hold normal completion, reports, alerts or PBs hostage. The underlying entity cause remains unverified. A positive name remains authoritative when old skin metadata disagrees.

A model and all its nearby matching Sparkling name tags are aliases for one critter. Resolved aliases are omitted from the detected HUD and live-remainder/population scans, even while a captured model or tag remains loaded. A genuinely new UUID remains eligible for detection, but adds no separate completion requirement.

Retain detection identity when skin and label packets arrive in different orders. Unloading an entity is not a catch. Capture/loot-share and global Sparkling catch announcements still reconcile as one confirmed catch; unresolved detection identities do not gate completion.

## Safari-only wax-on particle locations

While the player is inside Safari, inspect server `WAX_ON` particle packets in addition to the existing name/skin detector. Observe each packet's origin on the client thread, independently of vanilla particle visibility/distance settings. Do not process particles in Torrhus Canyon, the entrance, or other islands. This rule comes from the user's report that Safari Sparkling critters emit wax-on particles; it still needs live validation.

There is no current-biome or player-distance filter on received Safari particle packets. The server may only send them when their biome/entity becomes loaded; this does not establish whole-map coverage. Local particle settings cannot recover a packet that the server never sent.

A packet alone identifies a **location**, not a species, entity UUID, or distinct critter count. After the next name/skin scan, alert only for still-unidentified locations: the title shows **SPARKLING!** with **Critter Unknown**, and the optional party message is **SPARKLING Critter Unknown!** with coordinates. Never put particle-source wording such as wax-on in the title or party message. Keep the last-seen world waypoint and Detected sparklings row with coordinates. Waypoint visibility follows the existing Sparkling detection toggle and ESP text scale/color. `/sm debug` reports `source=wax_on`, coordinates, biome and last-seen time.

Coalesce origins within three blocks to avoid alerting on every burst. Retain the last observed location for the run, including after unloading or changing biomes, and reset it with a new run. These are approximate last-seen locations, not tracked moving entities. Spatial clustering can combine nearby critters or leave multiple locations for one moving critter; never use these markers to infer population totals or fabricate species checks/catches. Unknown markers remain until an independent name/skin positive matches within three blocks or the run ends. Unrelated catches/reports cannot reliably identify which anonymous marker was caught.

When a name/skin positive independently identifies a nearby body or label, retire the matching anonymous waypoint, cancel its still-queued unknown party message and unknown banner, and use the identified alert/HUD/capture ledger. If the unknown party message was already sent, a later identified species may still send its named announcement. Known detections take priority over same-tick particle packets. Particle-only evidence does not assign a guessed species or add an ordinary UUID check; species-specific completion still uses identified evidence and the documented source/check rules.

## Once-per-species notifications

For an identified critter, show the Sparkling GUI/sound alert at most once per species per run, and queue its party announcement at most once per species per run. These two notification channels retain their independent toggles. Capsule transitions, escapes, replacement UUIDs, unloading/reentry and mode changes must not repeat the species notification. A new run resets both notification histories. Other species can still alert independently.

This is notification memory only: keep confirmed catches, live identities and positions separate. A new detection neither repeats a species announcement nor blocks completion. Unknown particle-only waypoints retain location-based deduplication because their species is not known.

## Ordinary checking remains separate

The user's selected policy keeps the existing species scan circles. Ordinary critters count toward checks under their existing qualifying-area, UUID, spawn-count and source rules. A skin/name positive adds no capture obligation or survey credit. Wumpa and Doomspiral still require confirmed shared captures for their checks.

An alert says a Sparkling was detected; the check counter tracks documented survey evidence. Detection does not change completion. Confirmed Sparkling catches still complete Unique/Sparkling species, while Full Clear retains its ordinary capture/source rules.

## Skin reference

Source: [user-supplied Sparkling skin sheet](https://docs.google.com/spreadsheets/d/1BV0F_beJTcUCS99-5mIBqVx4tDJwcAV-gDAO39YH54U/edit?gid=0), retrieved **2026-10-15**. The read-only snapshot contains **36 distinct skin hashes** marked Sparkling. **Tepid has no supplied skin** and keeps name-tag detection. The sheet's `Arieta` spelling is normalized to the roster's `Areita`.

The bundled registry is `src/main/resources/assets/sparkling-mutuals/data/sparkling_skins.json`. It stores texture hashes and provenance, not account names, timestamps or signed profile payloads. Every supplied base64 texture was decoded and checked against the sheet's texture link; no supplied hash occurs in the mod's existing ordinary model identifiers. This verifies the table's consistency, not the server's live use of every skin.

Compare exact texture hashes in currently loaded NPC-player profiles, armor-stand head profiles or item-display profiles. Exclude the local player, listed real players and known party players. A skin-only match must be in its expected Safari biome. Name tags remain the fallback, including missing/new skins. No image recognition, player-name lookup or runtime sheet download is involved.

Read current metadata on every scan. For player models, prefer the current player-info profile over the entity's original profile when available. The user reports that Sparkling skins are applied **after the run starts**, so an initial ordinary/missing skin must never become a permanent negative cache for that UUID. A later positive skin/name updates detection without reopening completion. This does not invent a startup delay or claim that pre-start skin data proves a non-Sparkling result.

## Range and observed unload point

Remove the previous **80-block / current-biome presentation limit** from positive Sparkling detection. Alerts, glow and the **Detected sparklings** HUD use every currently loaded positive across Safari biomes; the HUD shows biome and distance. Ordinary survey circles remain unchanged.

The server must still send the entity and its metadata to the client. Increasing render distance or removing an 80-block client filter cannot discover entities absent from the client world. Skin matching may find a loaded body before its separate name tag arrives, but it does **not guarantee whole-Safari coverage** or one universal maximum range.

User observation, 2026-10-15: Duplico at **(-4, 71, -72)** unloaded when the player was approximately **(-73, 65, 25)**, about **119.19 blocks in 3D** (119.04 horizontally). Its Sparkling status was not specified. Record this as one observed Duplico tracking distance, not a verified limit for every species/model/server. Repeat with a confirmed Sparkling before treating it as a Sparkling skin-range measurement.

`/sm debug` includes each current positive's name/skin evidence source, species, biome, body/label UUIDs and distance. Use these diagnostics for the live checks below.

## Minecraft checks

- Inside Safari, approach a confirmed Sparkling with its name/skin initially unavailable: wax-on packets should show an unknown-location alert, coordinates in the Detected sparklings panel, and a last-seen world label. Repeated bursts at that spot must not repeatedly alert. Bring the name/skin into view and confirm the anonymous waypoint retires.
- Test from another biome with its particles loaded, then unload the area: accept received packets across the boundary and retain the last location. No detection is expected for particles the server has not sent. Repeat with reduced vanilla particle settings.
- Trigger wax-on sources in Torrhus Canyon and outside Safari: no particle alerts or waypoints. Start a new run and confirm old particle locations are absent. Test nearby simultaneous Sparklings and a moving one to assess the three-block grouping in practice.


1. In each mode, find a named Sparkling and confirm the alert, glow and Detected sparklings row refer to the same species. Detection must not add a missing-panel target or revoke otherwise valid completion. Repeat with alert/ESP toggles off.
2. Observe a known skin while its name tag is absent or beyond the old 80-block range. Expect a skin-based positive and correct biome/distance. Bring its name tag into range afterward: no duplicate alert. Capsule replacements and duplicate rows must not add completion requirements.
3. Watch a critter across run start: ordinary appearance first, then Sparkling skin on the same UUID. Later metadata must detect it without reopening progress. Inspect `/sm debug` for `source=skin` when there is no Sparkling name.
4. Keep ordinary critters outside their scan circles and check that they do not gain ordinary checked progress from the new detector. Boss, resource and population gates must remain intact.
5. Wear a reference skin as a real player, or put it on a party member: no critter alert. Test a normal skin, an unrelated head and missing Tepid skin; only a valid Sparkling name should identify Tepid.
6. Repeat the Duplico coordinates with a confirmed Sparkling, slowly crossing 80–120 blocks and moving across a biome boundary. Record the last loaded body/name UUID and actual unload point; loaded range must never be advertised as whole-map coverage. Unloading and replacement detections must not alter completion.

After a confirmed Sparkling capture, Unique and Sparkling mark an ordinary species complete even if other detected identities remain. Birds additionally retain their shared spawning/source rules in [Forest bird completion](BIRD_COMPLETION.md). Full Clear still checks its normal captures/sources. New or lingering detection identities never reopen completion.

## Last updated

2026-10-18 - Retained the birds' shared spawning/source requirement after confirmed Sparkling captures.

2026-10-17 - Removed detected-entity capture requirements from completion after reported duplicate capsule detections; detection and alerts remain enabled.

2026-10-17 - Deduplicated identified Sparkling alerts/party announcements by species for the full run, including capsule/escape UUID changes.

2026-10-17 - Retained all matching label aliases and excluded resolved Sparkling entities from HUD/live-remainder accounting after capture.

2026-10-17 - Added Safari-only wax-on packet locations, anonymous last-seen waypoints and cross-biome received-particle handling without claiming unloaded coverage.

2026-10-16 - Added post-capture completion behavior for all three modes and stale-obligation checks.

2026-10-15 - Unified positive name/skin evidence with alerts and completion, added 36 reference skins and delayed-metadata checks, removed the 80-block presentation cap, and preserved ordinary scan circles.

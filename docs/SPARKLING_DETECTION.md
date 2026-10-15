# Sparkling detection and mode progress

## Shared positive evidence

The detector runs every five client ticks during Safari and scans entities the client currently has loaded. A positive identification comes from either an exact `Sparkling <species>` name or a matching known Sparkling skin texture. The **same identification** feeds the Sparkling alert, glow, detected-critters HUD and the current run's mandatory Sparkling capture ledger. Disabling alerts, sound, glow or HUD does not disable that ledger.

Any detected Sparkling blocks its species/biome completion in **all modes** until confirmed caught, even when everybody owns the discovery. This also reopens earlier completion. Spawn messages, ordinary same-species catches, retained ordinary UUID checks and party biome reports cannot substitute for the Sparkling capture. A positive name remains authoritative when old skin metadata disagrees.

A model and its nearby matching name tag are aliases for one critter. Retain that identity when skin and label packets arrive in different orders, so the same critter does not produce two alerts or two capture obligations. Retain unresolved obligations after unloading; no longer rendering an entity is not a catch. Capture/loot-share and global Sparkling catch announcements still reconcile as one confirmed catch.

## Ordinary checking remains separate

The user's selected policy keeps the existing **species scan circles**. Ordinary critters count toward Sparkling/Unique's check totals only under their existing qualifying-area, UUID, spawn-count and source rules. A skin/name positive is an additional capture obligation, not permission to complete the ordinary survey from anywhere. Wumpa and Doomspiral still require confirmed shared captures for their checks.

Consequently, an alert and a progress counter do not mean the same thing: the alert says a Sparkling has been found; that find blocks completion until caught, while the check counter tracks the species' documented survey evidence. The two paths share every positive Sparkling detection, without treating an absence of name/skin data as proof that a critter is ordinary.

## Skin reference

Source: [user-supplied Sparkling skin sheet](https://docs.google.com/spreadsheets/d/1BV0F_beJTcUCS99-5mIBqVx4tDJwcAV-gDAO39YH54U/edit?gid=0), retrieved **2026-10-15**. The read-only snapshot contains **36 distinct skin hashes** marked Sparkling. **Tepid has no supplied skin** and keeps name-tag detection. The sheet's `Arieta` spelling is normalized to the roster's `Areita`.

The bundled registry is `src/main/resources/assets/sparkling-mutuals/data/sparkling_skins.json`. It stores texture hashes and provenance, not account names, timestamps or signed profile payloads. Every supplied base64 texture was decoded and checked against the sheet's texture link; no supplied hash occurs in the mod's existing ordinary model identifiers. This verifies the table's consistency, not the server's live use of every skin.

Compare exact texture hashes in currently loaded NPC-player profiles, armor-stand head profiles or item-display profiles. Exclude the local player, listed real players and known party players. A skin-only match must be in its expected Safari biome. Name tags remain the fallback, including missing/new skins. No image recognition, player-name lookup or runtime sheet download is involved.

Read current metadata on every scan. For player models, prefer the current player-info profile over the entity's original profile when available. The user reports that Sparkling skins are applied **after the run starts**, so an initial ordinary/missing skin must never become a permanent negative cache for that UUID. A later positive skin/name reopens progress even if the old appearance was already checked. This does not invent a startup delay or claim that pre-start skin data proves a non-Sparkling result.

## Range and observed unload point

Remove the previous **80-block / current-biome presentation limit** from positive Sparkling detection. Alerts, glow and the **Detected sparklings** HUD use every currently loaded positive across Safari biomes; the HUD shows biome and distance. Ordinary survey circles remain unchanged.

The server must still send the entity and its metadata to the client. Increasing render distance or removing an 80-block client filter cannot discover entities absent from the client world. Skin matching may find a loaded body before its separate name tag arrives, but it does **not guarantee whole-Safari coverage** or one universal maximum range.

User observation, 2026-10-15: Duplico at **(-4, 71, -72)** unloaded when the player was approximately **(-73, 65, 25)**, about **119.19 blocks in 3D** (119.04 horizontally). Its Sparkling status was not specified. Record this as one observed Duplico tracking distance, not a verified limit for every species/model/server. Repeat with a confirmed Sparkling before treating it as a Sparkling skin-range measurement.

`/sm debug` includes each current positive's name/skin evidence source, species, biome, body/label UUIDs and distance. Use these diagnostics for the live checks below.

## Minecraft checks

1. In each mode, find a named Sparkling and confirm the alert, glow, Detected sparklings row and missing/progress capture requirement refer to the same species. Repeat with every alert/ESP toggle off: tracking must still block completion.
2. Observe a known skin while its name tag is absent or beyond the old 80-block range. Expect one skin-based positive, the correct biome/distance and a pending capture. Bring its name tag into range afterward: no duplicate alert or capture requirement. Confirm one Sparkling catch resolves it.
3. Watch a critter across run start: ordinary appearance first, then Sparkling skin on the same UUID. The later metadata must detect it and reopen progress. Inspect `/sm debug` for `source=skin` when there is no Sparkling name.
4. Keep ordinary critters outside their scan circles and check that they do not gain ordinary checked progress from the new detector. Boss, resource and population gates must remain intact.
5. Wear a reference skin as a real player, or put it on a party member: no critter alert. Test a normal skin, an unrelated head and missing Tepid skin; only a valid Sparkling name should identify Tepid.
6. Repeat the Duplico coordinates with a confirmed Sparkling, slowly crossing 80–120 blocks and moving across a biome boundary. Record the last loaded body/name UUID and actual unload point; absence must retain the capture obligation, and loaded range must never be advertised as whole-map coverage.

## Last updated

2026-10-15 - Unified positive name/skin evidence with alerts and completion, added 36 reference skins and delayed-metadata checks, removed the 80-block presentation cap, and preserved ordinary scan circles.

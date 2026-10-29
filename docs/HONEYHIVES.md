# Torrhus Canyon Honeyhives

## Purpose

Show personal loot-ready Honeyhives during an incomplete Miria contest. Out of hologram range, saved personal loot/corrected deadlines determine readiness, including across relogs. Waypoints return automatically at expiry. Nearby current status holograms override that saved state and persist the correction for later distant viewing. Block honey levels never determine readiness.

## Controls and defaults

**Miria contest → Honeyhive beacons** defaults on in the general [configuration](CONFIG_DEFAULTS.md). The presentation range is 256 blocks. `/sm debug` writes nearby hive state and raw label-carrier diagnostics locally. This helper is independent of Safari Honeybug nests and their [completion rules](critters/honeybug.md).

## Implemented behavior

### Readiness evidence

| Evidence | Implemented result | Limit |
| --- | --- | --- |
| A `HIVE!`-prefixed system response correlated with a recent eligible hive click | Saves the local receipt timestamp; hides the beacon until that timestamp + 3,600,000 ms. | Five-second ordered click/chat correlation; not a server timestamp. |
| A correlated `QUEEN BEE!`-prefixed response | Clears cooldown/correction and marks the hive full immediately. | Supports Queen-before-loot ordering; unmatched responses select no hive. |
| Unambiguously associated `Click to loot!` | Confirms availability and clears stale saved cooldown/loot state. | Requires a current loaded nest and unambiguously matched personal carrier. |
| Unambiguously associated `Refill in: ...` | Corrects and persists the deadline even when an old loot timestamp or Queen Bee flag exists. | Parsed total must be 0–3,600,000 ms; round-off jitter is coalesced to avoid repeated writes. |
| No recognized label, unloaded data or honey level 0–5 | Retains the saved timer/full state. | A hive without a deadline is provisionally available. |

The [message reference](critters/MESSAGES.md) owns exact server wording. Recognition here is prefix-based for loot/Queen chat, not a requirement for one exact reward suffix. Party quotes do not satisfy the system prefix.

Click observers watch local attack/use on bee nests at bundled hive coordinates without consuming the interaction. Arbitrary tree/decorative nests cannot register a new waypoint. Chat tracking requires the helper enabled, Canyon detection and SkyBlock. Canyon detection uses the confirmed `foraging_3` island mode, falling back to the Torrhus Canyon/Springs scoreboard area only while mode is unknown. Every 20 client ticks, maintenance expires deadlines and, when display gates permit, checks loaded holograms and refreshes beacons. Automatic terrain discovery is disabled; the bundled catalogue is the location source of truth.

Holograms can be text displays or custom-name carriers, with formatting stripped per line. Select the unambiguous nearest hive by squared 3D distance to its label anchor; horizontal X and Z offsets must each be at most 1.5 blocks, with label Y from hive Y − 1 through hive Y + 4. This is a square horizontal bound, not a 1.5-block radius. Only a currently loaded bee nest can receive the correction. Contest completion suppresses display and hologram correction until a later incomplete window; timestamp expiry maintenance continues.

### Positions and display

The bundled `hive_7` position is **(-705, 92, 225)**. Saved **(-704, 92, 225)** keys migrate there; duplicate legacy fallback deadlines retain the later deadline. The nonexistent **(-730, 132, 184)** hive is excluded. Confirmed loot/Queen events refresh the visible list immediately. Available hives get green beacons and outlines; if the beacon renderer fails, columns provide a fallback. Both render paths require a catalogued coordinate. If its chunk is loaded and that block is not a bee nest, no marker renders. Unloaded known hives can retain their timer-based waypoint. This checks block presence only: honey level still never determines fullness, and a missing block does not alter the saved cooldown.

## Lifecycle and persistence

`config/sparkling-mutuals-honeyhives.properties` stores the bundled positions and per-player/profile state. On upgrade, saved positions outside the catalogue and their associated state are removed, with the original file backed up as `sparkling-mutuals-honeyhives.properties.pre-waypoint-cleanup.bak`. Valid known-hive timers and scopes remain intact. On relog, only that player's last confirmed profile scope can resume while waiting for the explicit profile-ID line. With no known profile, fresh observations use a provisional scope; confirmation promotes correlated personal loot/Queen evidence, not arbitrary provisional hologram corrections.

Version-2 fallback `refill` deadlines remain usable. Exact `looted` timestamps rebuild one-hour deadlines on load, then saved `observedRefill` hologram corrections take precedence. Explicit `queenFull` markers from correlated Queen Bee messages bypass the timer until a new loot or current nearby hologram corrects it. Legacy `full` markers mixed hologram observations and actual procs, so they no longer override cooldowns; the pre-migration file is backed up as `.pre-timer-priority.bak`. A fresh confirmed loot/Queen event clears old corrections, and a confirmed ready hologram clears stale loot timestamps. Loading and periodic maintenance prune expired deadlines; losing loaded visuals does not reset an unexpired timer. World changes clear visible/click-correlation state, not saved cooldowns. Disconnect forgets the current profile, retaining its saved scope for resumption.

## Edge cases

- Multiple eligible clicks use ordered correlation; Queen-before-loot requires a single pending click when no recent loot exists. This is a client heuristic, not verified server ownership.
- Honey level 5 cannot clear a saved cooldown; honey level 0 cannot extend an expired one.
- Loot/Queen Bee chat wins over a possibly stale hologram for two seconds, allowing the server label to update. Afterwards the current hologram is authoritative. Without a current label, saved deadlines return waypoints automatically at expiry. A new confirmed loot starts a fresh hour and discards older corrections.
- Hologram layout, personal visibility and server response ordering remain live-server uncertainties. No API refill lookup is wired into this helper's current readiness path.

## Source and validation references

- Registration, click/chat gates, scans, renderers and persistence: [HoneyHives](../src/main/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHives.kt).
- Correlation and corrections: [HoneyHiveTracker](../src/main/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveTracker.kt); association: [HoneyHiveLabels](../src/main/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveLabels.kt); migration/scope/load precedence: [HoneyHiveRules](../src/main/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveRules.kt); positions: [bundled hives](../src/main/resources/assets/sparkling-mutuals/honeyhives.txt).
- Existing secondary coverage: [HoneyHiveTrackerTest](../src/test/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveTrackerTest.kt), [HoneyHiveRulesTest](../src/test/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveRulesTest.kt), [HiveObservationAndHudPreviewTest](../src/test/kotlin/net/johnceo/sparklingmutuals/honeyhives/HiveObservationAndHudPreviewTest.kt). No tests or live session ran in this audit. Live validation should compare relog, Queen-first response, entrance level-0 hives, persisted hologram corrections, scope changes and the migrated hive position.

## Last updated

2026-10-29 — Fixed recorded loot timestamps permanently suppressing hologram corrections. Saved times remain the distant fallback; current nearby labels override and persist, including across relogs. Added a two-second post-chat stale-label grace and coalesced countdown jitter.

2026-10-26 — Recorded loot + one hour is authoritative across relogs; automatic readiness returns at expiry. Separated confirmed Queen Bee markers from legacy visual full state and prevented holograms from changing known personal timers.

2026-10-26 — Removed arbitrary click/terrain waypoint discovery, added backed-up cleanup of unlisted saved positions, and suppressed loaded non-nest blocks in both render paths while preserving timer/hologram fullness rules.

2026-10-19 (America/New_York) — Audited timestamp/hologram precedence, scope, gates and migration against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; removed superseded honey-level narratives. See [documentation conventions](DOCUMENTATION_GUIDE.md).

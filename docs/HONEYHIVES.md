# Torrhus Canyon Honeyhives

## Purpose

Show personal loot-ready Honeyhives during an incomplete Miria contest. Readiness is based on saved personal timestamps and matched status holograms; block honey levels are diagnostic only.

## Controls and defaults

**Miria contest → Honeyhive beacons** defaults on in the general [configuration](CONFIG_DEFAULTS.md). The presentation range is 256 blocks. `/sm debug` writes nearby hive state and raw label-carrier diagnostics locally. This helper is independent of Safari Honeybug nests and their [completion rules](critters/honeybug.md).

## Implemented behavior

### Readiness evidence

| Evidence | Implemented result | Limit |
| --- | --- | --- |
| A `HIVE!`-prefixed system response correlated with a recent eligible hive click | Saves the local receipt timestamp; hides the beacon until that timestamp + 3,600,000 ms. | Five-second ordered click/chat correlation; not a server timestamp. |
| A correlated `QUEEN BEE!`-prefixed response | Clears cooldown/correction and marks the hive full immediately. | Supports Queen-before-loot ordering; unmatched responses select no hive. |
| Unambiguously associated `Click to loot!` | Clears cooldown/correction and marks full. | Requires current loaded hive and recognized personal status carrier. |
| Unambiguously associated `Refill in: ...` | Saves a separate deadline from the displayed minutes/seconds. | Parsed total must be 0–3,600,000 ms. Countdown wins if both label types are present. |
| No recognized label, unloaded data or honey level 0–5 | Retains the saved timer/full state. | A hive without a deadline is provisionally available. |

The [message reference](critters/MESSAGES.md) owns exact server wording. Recognition here is prefix-based for loot/Queen chat, not a requirement for one exact reward suffix. Party quotes do not satisfy the system prefix.

Click observers watch local attack/use on bee nests without consuming the interaction. Chat tracking requires the helper enabled, Canyon detection and SkyBlock. Canyon detection finds a known bee nest at its expected loaded position. Every 20 client ticks, maintenance expires deadlines and, when display gates permit, checks loaded holograms and refreshes beacons. Every 40 ticks the helper can survey loaded chunks around a 96-block radius for nests over oak fences under oak trapdoors.

Holograms can be text displays or custom-name carriers, with formatting stripped per line. Select the unambiguous nearest hive by squared 3D distance to its label anchor; horizontal X and Z offsets must each be at most 1.5 blocks, with label Y from hive Y − 1 through hive Y + 4. This is a square horizontal bound, not a 1.5-block radius. Only a currently loaded bee nest can receive the correction. Contest completion suppresses display and hologram correction until a later incomplete window; timestamp expiry maintenance continues.

### Positions and display

The bundled `hive_7` position is **(-705, 92, 225)**. Saved **(-704, 92, 225)** keys migrate there; duplicate legacy fallback deadlines retain the later deadline. The nonexistent **(-730, 132, 184)** hive is excluded. Confirmed loot/Queen events refresh the visible list immediately. Available hives get green beacons and outlines; if the beacon renderer fails, columns provide a fallback.

## Lifecycle and persistence

`config/sparkling-mutuals-honeyhives.properties` stores surveyed positions and per-player/profile state. On relog, only that player's last confirmed profile scope can resume while waiting for the explicit profile-ID line. With no known profile, fresh observations use a provisional scope; confirmation promotes correlated personal loot/Queen evidence, not arbitrary provisional hologram corrections.

Version-2 fallback `refill` deadlines remain usable. Exact `looted` timestamps rebuild one-hour deadlines on load; separate `observedRefill` corrections apply afterward, followed by saved `full` markers. A fresh confirmed loot/Queen event clears old corrections, while the original loot timestamp remains available for reference. Loading and periodic maintenance prune expired deadlines; losing loaded visuals does not reset an unexpired timer. World changes clear visible/click-correlation state, not saved cooldowns. Disconnect forgets the current profile, retaining its saved scope for resumption.

## Edge cases

- Multiple eligible clicks use ordered correlation; Queen-before-loot requires a single pending click when no recent loot exists. This is a client heuristic, not verified server ownership.
- Honey level 5 cannot clear a saved cooldown; honey level 0 cannot extend an expired one.
- A genuine matched full/countdown label can correct even a saved timestamp or Queen-full state.
- Hologram layout, personal visibility and server response ordering remain live-server uncertainties. No API refill lookup is wired into this helper's current readiness path.

## Source and validation references

- Registration, click/chat gates, scans, renderers and persistence: [HoneyHives](../src/main/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHives.kt).
- Correlation and corrections: [HoneyHiveTracker](../src/main/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveTracker.kt); association: [HoneyHiveLabels](../src/main/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveLabels.kt); migration/scope/load precedence: [HoneyHiveRules](../src/main/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveRules.kt); positions: [bundled hives](../src/main/resources/assets/sparkling-mutuals/honeyhives.txt).
- Existing secondary coverage: [HoneyHiveTrackerTest](../src/test/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveTrackerTest.kt), [HoneyHiveRulesTest](../src/test/kotlin/net/johnceo/sparklingmutuals/honeyhives/HoneyHiveRulesTest.kt), [HiveObservationAndHudPreviewTest](../src/test/kotlin/net/johnceo/sparklingmutuals/honeyhives/HiveObservationAndHudPreviewTest.kt). No tests or live session ran in this audit. Live validation should compare relog, Queen-first response, entrance level-0 hives, persisted hologram corrections, scope changes and the migrated hive position.

## Last updated

2026-10-19 (America/New_York) — Audited timestamp/hologram precedence, scope, gates and migration against source `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; removed superseded honey-level narratives. See [documentation conventions](DOCUMENTATION_GUIDE.md).

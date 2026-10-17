# Torrhus Canyon Honeyhives

Personal honeyhive waypoints show a green beacon and outline when a hive is available. **Loaded honey level 5 establishes full; other levels use a matching status hologram when available.** The user's 2026-10-17 diagnostics demonstrate that these block states remain readable well beyond the range of nearby status holograms.

## Evidence and state transitions

| Evidence | Effect on the matched hive |
| --- | --- |
| Loaded bee-nest `honey_level=5` | Full. Clear a stale cooldown/remembered empty state, permit looting, and show the beacon/outline. |
| Loaded bee-nest `honey_level=0`, without recognized text | Empty. Hide the beacon/outline; do not fabricate a loot timestamp or new deadline. |
| Missing/unloaded block, a non-nest block, or levels 1–4 without recognized text | No new readiness conclusion. Retain previously observed state and saved fallback evidence. |
| Matching `Click to loot!` text when level is not 5 | Full. Clear stale cooldown/empty state and show the beacon under the existing contest visibility rules. |
| Matching `Refill in: ...` text when level is not 5 | Empty until the displayed countdown ends; hide the beacon. A visible level 5 still wins over text. |
| Own confirmed `HIVE!` loot response following a hive click | Save the exact loot time and one-hour fallback deadline. A click alone does not prove a loot. |
| `QUEEN BEE! The Honeyhive instantly refilled with loot!` | Correlate to the loot/click, clear its cooldown, persist full readiness, and refresh its beacon immediately. The next actual loaded level remains authoritative. |
| Saved deadline reached without an observed block state | Available fallback; never use the timer to override a loaded level 0. |

Inspect loaded known hive blocks once per second on Torrhus Canyon. Level 5 needs no hologram or extra distance threshold. For other levels, read current non-removed text-display/custom-name status carriers, normalize formatting, and associate the status unambiguously to its nearest hive. Allow carrier origins from hive Y minus 1 through Y plus 4, supporting the observed armor-stand origin below the nest. If both status types are present for one lower-level hive, prefer the countdown. No API request is required. Preserve the existing 256-block presentation range, contest-complete suppression and beam-renderer fallback. No chunk is requested solely for a hive check.

Keep click ordering and the five-second response window. Queen Bee applies to the preceding confirmed loot, or a single recent clicked hive if Queen Bee arrives before the loot response. In Queen-first ordering, consume the delayed HIVE response without starting another cooldown. A fresh click starts a new loot. Multiple unmatched clicks remain ambiguous. Unmatched/expired or quoted party messages cannot select an arbitrary nearby hive; unmatched Queen Bee messages are logged.

## Positions and saved-data correction

The corrected `hive_7` position is **(-704, 92, 225)**, replacing **(-705, 92, 225)**, whose diagnostic block was `void_air`. Correct both the bundled position and previously saved position/deadline/Queen Bee/loot-timestamp keys. Merge duplicate cooldown entries conservatively using the later deadline. Save only the corrected coordinate. Continue excluding the separate nonexistent position `-730,132,184`.

Survey nearby loaded chunks for nest-on-fence/trapdoor structures to learn additional positions. Only actual loaded bee-nest blocks supply readiness; an old waypoint pointing at air is not a full/empty hive observation.

## Player/profile ownership and persistence

Use `config/sparkling-mutuals-honeyhives.properties`. Cooldowns belong to the local player and SkyBlock profile. Before a profile join message arrives, resume only that player's last confirmed saved scope; another player's scope cannot be reused. An explicit profile ID selects its independent records. Unknown-profile observations remain provisional, with only confirmed provisional loot/Queen Bee events promoted into a known profile.

Save each confirmed loot's exact epoch-millisecond timestamp as `looted.<player.profile>.<x,y,z>` and its one-hour deadline as `refill.<player.profile>.<x,y,z>`. Preserve version-2 deadlines across upgrades and relogs. `state.<player.profile>.<x,y,z>` records descriptive full/empty state; it does not override loaded block evidence. Queen Bee full markers remain fallback information. The next confirmed loot replaces its timestamp/deadline.

On natural timer expiry, remove the deadline and save the resulting state during one-second maintenance, even outside Canyon or with beacon display disabled. On restart, prune expired deadlines. Retain the last loot timestamp for reference. A block level observation never pretends that a new loot occurred or that its exact refill time is known.

## Diagnostics and Minecraft checks

`/sm debug` includes loaded block states/honey levels, saved deadlines, Queen Bee overrides, and nearby raw text-display/armor-stand holograms. Keep the label text for diagnosing disagreements and for fallback readiness when the honey level is not 5.

1. From far enough away that holograms are absent, verify loaded level-5 hives have beacons and level-0 hives without full-text exceptions do not. Your diagnostic examples at (-724, 93, 211), (-721, 92, 207) and (-707, 92, 220) must show full despite their stale deadlines.
2. For the exceptional hives, verify level 0 plus Click to loot renders a beacon and a refill label hides it. Level 5 must remain full even with a countdown label. Check that neighboring hives do not borrow each other's status. Missing labels retain existing fallback behavior.
3. Loot a full hive, proc Queen Bee, then loot again. Expect immediate chat-based refresh followed by authoritative loaded level updates, with correct replacement timestamps/deadlines and no effect on adjacent hives.
4. Quickly restart after looting before one hour. Verify saved fallback deadlines survive; once blocks load, their levels correct any stale fallback. Check natural expiry outside Canyon.
5. Verify exactly one waypoint at (-704, 92, 225) and none at (-705, 92, 225), including with an old saved properties file.
6. Unload/reload the area and switch profiles: no absent-block or hologram-only event may invent a personal loot timestamp or overwrite another profile's stored records.

## Last updated

2026-10-17 - Restored status-text fallback below honey level 5 for exceptional hives and accepted observed armor-stand carrier origins slightly below the block.

2026-10-17 - Made actual loaded honey level the authority, removed hologram decisions, and corrected/migrated hive_7 to (-704, 92, 225).

2026-10-17 - Prioritized loaded status labels and added actual honey-level 5/0 fallback, retaining a short chat grace and avoiding invented loot timers.

2026-10-17 - Added immediate Queen Bee beacon refresh and Queen-first response handling, restored saved profile scope on relog, and persisted exact loot timestamps and natural-expiry full state.

2026-10-15 - Documented personal hive evidence, exact Queen Bee refill behavior, hologram association, provisional profile handling and diagnostics; implemented the corresponding observation fixes. Live Minecraft validation remains pending.

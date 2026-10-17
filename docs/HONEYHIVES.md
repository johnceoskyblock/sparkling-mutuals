# Torrhus Canyon Honeyhives

The saved personal loot timestamp plus **one hour** is the default source for honeyhive beacon readiness. The user reports inconsistent block levels, including entrance hives that always have honey level 0. **Block honey levels never affect readiness.**

## Readiness priority

1. A confirmed own `HIVE!` response matched to a hive click saves its exact epoch-millisecond loot timestamp and sets its deadline to loot time plus 3,600,000 ms. Hide its beacon until that deadline.
2. A matched `QUEEN BEE! The Honeyhive instantly refilled with loot!` clears that deadline and shows the beacon immediately. Keep support for Queen Bee arriving before the loot response, without letting the delayed loot response empty it again.
3. Only a current, unambiguously matched personal status hologram can correct the saved state: **Click to loot!** clears the cooldown and shows the beacon; **Refill in: ...** sets the deadline from the displayed countdown and hides it. These corrections apply regardless of honey level.
4. Without a recognized hologram, retain the stored deadline/full state. Unloading, block levels 0–5, render delays and mode changes cannot replace that timestamp. Once the deadline passes, show the beacon under the existing contest display rules.

A hive with no saved deadline is provisionally available, unless a matched refill hologram supplies one. Visual observations never invent an actual loot timestamp. Inspect loaded holograms once per second, preserving the existing presentation range, contest-complete suppression and renderer fallback. Confirmed loot/Queen Bee chat refreshes the visible beacon list immediately.

Read text displays and ordinary custom-name/armor-stand carriers, stripping formatting and normalizing each line. Associate a status unambiguously to its nearest hive within 1.5 blocks horizontally, from hive Y minus 1 through Y plus 4. If both status types are loaded for one hive, prefer the countdown. Unknown or unloaded labels do not change state. Blocks/honey levels remain useful in diagnostics only.

## Hive positions

`hive_7` is **(-705, 92, 225)**. The previous change to (-704, 92, 225) was incorrect. Correct the bundled position and automatically migrate saved positions, deadlines, full flags, observed corrections and loot timestamps from -704 back to -705. Merge duplicate fallback deadlines conservatively, retaining the later deadline. Continue excluding the separate nonexistent hive at -730,132,184.

## Ownership and persistence

Use `config/sparkling-mutuals-honeyhives.properties`, scoped to the local player and SkyBlock profile. Resume only that player's saved scope while awaiting the profile join message; an explicit profile ID selects its independent records. Preserve provisional click/loot/Queen Bee correlation with the existing five-second window. Unmatched, expired and quoted party messages cannot choose an arbitrary nearby hive.

Keep the original confirmed timestamp in `looted.<scope>.<position>`. Rebuild its exact deadline on load rather than trusting an old visual-derived deadline. Store a hologram correction separately as `observedRefill.<scope>.<position>`, and persist confirmed-full/Queen Bee state in the existing full marker. Apply hologram corrections after the base timestamp on load. A new confirmed loot or Queen Bee proc clears stale corrections. Keep the exact loot timestamp for reference even when a hologram corrects readiness.

Preserve valid version-2 deadlines for older users without exact loot timestamps. Persist natural expiry/full state during one-second maintenance, even outside Canyon or with rendering disabled. Loading prunes expired deadlines. No restart may reset a known unexpired cooldown to provisionally full.

## Minecraft checks

- Loot a hive, move beyond hologram range and quickly relog: retain its exact original one-hour deadline. Level 5 must not clear it; level 0 must not prolong it after expiry.
- Proc Queen Bee: restore its beacon immediately, including before the loot response. Block level 0 must not undo it. A real full/refill hologram can still correct it.
- Test an entrance hive that always has level 0: use the saved timer at a distance and its matched status text up close. Conflicting block data must have no effect.
- Check that Click to loot and refill corrections survive restart without rewriting the original loot timestamp. Re-loot and verify a fresh timestamp replaces it and clears old corrections.
- With an old properties file, verify one waypoint at (-705, 92, 225), none at -704, and correctly migrated deadlines/full state.
- Use `/sm debug` to compare scope, deadline, full flag, block and raw label text when a beacon disagrees. Automated tests do not establish live server message ordering or hologram layout.

## Last updated

2026-10-17 - Restored hive_7 to (-705, 92, 225); made saved one-hour loot deadlines the default, allowed only matched hologram corrections, and removed all honey-level decisions.

2026-10-17 - Restored status-text fallback below honey level 5 for exceptional hives and accepted observed armor-stand carrier origins slightly below the block.

2026-10-17 - Made actual loaded honey level the authority, removed hologram decisions, and corrected/migrated hive_7 to (-704, 92, 225).

2026-10-17 - Prioritized loaded status labels and added actual honey-level 5/0 fallback, retaining a short chat grace and avoiding invented loot timers.

2026-10-17 - Added immediate Queen Bee beacon refresh and Queen-first response handling, restored saved profile scope on relog, and persisted exact loot timestamps and natural-expiry full state.

2026-10-15 - Documented personal hive evidence, exact Queen Bee refill behavior, hologram association, provisional profile handling and diagnostics; implemented the corresponding observation fixes. Live Minecraft validation remains pending.

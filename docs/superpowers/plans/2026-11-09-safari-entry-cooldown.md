# Safari entry cooldown implementation plan

> Execute inline with superpowers:executing-plans and test-driven-development.

**Goal:** Show a 30-second Safari entry countdown after the exact server denial `You cannot join this instance!`.

**Design:** Independently implement the message-triggered countdown pattern reviewed in NoammAddons WarpCooldown; use existing Sparkling Mutuals HUD/editor/settings infrastructure. User specified Safari -> Helpers -> Safari entry cooldown toggle. Default on is an implementation choice. Ignore repeats while active, expire at exactly 30 seconds using monotonic time, clear on disconnect/disable, reject player chat and overlays. Keep countdown over world transfers within one connection. No automatic retry, guard or entry behavior changes. Match only on Hypixel with Safari/Torrhus location evidence. All three targets supported, local only.

**Reference:** https://github.com/Noamm9/NoammAddons/blob/multiversion/src/main/kotlin/com/github/noamm9/features/impl/visual/WarpCooldown.kt (independent implementation); Odin main tree has no identically named cooldown file.

## Tasks
- [x] Add failing state tests for exact/formatted message, false/player/overlay/context messages, repeats, expiry, reset and rearm.
- [x] Implement monotonic countdown and client listener/rendering, existing config persistence toggle, movable HUD/editor/right-click settings.
- [x] Run focused tests then all three builds/distribution checks, review integration and update owning Safari entry documentation.

## Review focus
Do not count party/player text as server evidence. Repeated failed clicks cannot extend the cooldown. World transfer must not clear a connection-scoped countdown. Disabled settings must suppress rendering and clear state. New HUD must have appearance/editor/config targets without exhaustive enum failures.

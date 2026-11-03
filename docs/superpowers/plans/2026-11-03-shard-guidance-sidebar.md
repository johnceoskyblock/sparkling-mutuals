# Saved Hunting Box Guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with Native execution, subject to human confirmation. Steps use checkbox syntax for tracking.

**Goal:** Show a persistent cross-page list of supported owned shards and fresh HOLD/SELL guidance beside the Hunting Box.

**Architecture:** A pure bounded ledger remembers per-profile ownership observations independently of price advice. The existing session supplies cached advice and bounded demand; a native screen adapter renders/scrolls a cached projection. Reuse the current worker, atomic cache writer and saved-choice switch.

**Tech Stack:** Kotlin 2.4.20, Fabric 26.1.2, existing Gson/atomic persistence, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-03-shard-guidance-sidebar-design.md` (approved in voice).

## Global Constraints

- Minecraft 26.1.2 only; no GitHub push, release, merge or other-target artifacts.
- Existing 37 supported Safari-shard products; no forecast, trading, order-alert or network-cap changes.
- Ownership may be stale, market advice may not bypass existing freshness/coverage gates.
- Keep both the NoFrills field-access fix and short Hunting Box labels. Preserve existing uncommitted fixes in this isolated feature worktree.
- Four verified account/profile scopes, 37 entries each. Unknown profile gets current-connection observations only.
- No user-session closure or instance replacement without applicable authorization.

## Review Focus

1. Server page transitions/filtering must not delete unseen ownership (Task 1).
2. A late disk load/profile change must not mix private lists or overwrite newer observations (Tasks 1–2).
3. A stale saved owner row must not resurrect stale SELL/HOLD advice (Task 2).
4. Small windows/GUI scaling and scrolling must preserve inventory interaction (Task 3).
5. Multiple screen initializations, disables and reconnects must not duplicate hooks, demand or workers (Tasks 2–3).

### Task 1: Bounded ownership ledger and persistence

**Files:** Create `SavedHuntingBoxLedger.kt`, `SavedHuntingBoxStore.kt` alongside existing market classes; modify `HuntingBoxShardParsing.kt`; test in `SavedHuntingBoxLedgerTest.kt`, `SavedHuntingBoxStoreTest.kt`, `HuntingBoxLabelCompatibilityTest.kt` in the matching test package.

**Interfaces:** `SavedBoxShard(productId: String, amount: Long, observedAt: Long)`; `SavedBoxRow(shard: SavedBoxShard, refreshedThisOpening: Boolean, advice: Recommendation)`.

`SavedHuntingBoxLedger(allowedIds: Set<String>)` exposes `select(scope: MarketScope?, connection: Any?)`, `beginOpening()`, `observe(shards: List<BoxShard>, now: Long)`, `rows(): List<Pair<SavedBoxShard, Boolean>>`, `export(): String`, `restore(raw: String, now: Long)`. Repeated observations replace quantities; zero removes that product. Alphabetical rows use `SaleProducts.names`. Restore merges only into untouched scopes; current observations win.

`HuntingBoxShardParsing.observe(title, slots): List<BoxShard>` accepts explicit zero counts with the same identity, slot and ambiguity guards. Existing `parse` continues returning positive-count shards for slot guidance.

`SavedHuntingBoxStore(file: Path)` exposes bounded `read(): String`/`write(raw: String): Boolean`, using `BoundedAtomicCacheFile`. File: `sparkling-mutuals-box-guidance.json`; 64 KiB each final/staging file, 128 KiB combined, four scopes maximum. No saved item stacks, quotes or recommendations.

- [ ] Write failing tests: revisit 243 then 200 yields 200; another page adds a second product; omission/filter retains first; explicit zero removes; ambiguous/negative counts ignored; new opening marks old rows unrefreshed.
- [ ] Add isolation/restore tests: account/profile switches, unknown-profile connection replacement, four-scope bound, corrupt/oversized/unknown-product JSON, future observations rejected, restore cannot overwrite a newly observed quantity.
- [ ] Run `./gradlew.bat :26.1.2:test --tests '*SavedHuntingBox*' --tests '*HuntingBoxLabelCompatibilityTest'` and confirm RED for missing behavior.
- [ ] Implement ledger, bounded store and zero-aware observation parser; run those tests to GREEN, then all market tests.
- [ ] Commit only verified Task 1 files locally using the existing Codex author identity; never push. Keep unrelated working changes separate.

### Task 2: Lifecycle, advice and demand integration

**Files:** Modify `SaleGuidanceSession.kt`, `ShardSaleGuidance.kt`; test `SaleGuidanceSessionTest.kt`, new `SavedBoxGuidanceSessionTest.kt`.

**Interfaces:** Session constructor accepts a ledger with a default empty instance. Existing `tick(...)` slot-map contract remains. Add `sidebar(now: Long): List<SavedBoxRow>`, `restoreBoxes(raw: String, now: Long)`, `exportBoxes(): String`. Ledger rows join current recommendations; missing or stale advice becomes UNKNOWN. The foreground integration exposes a cached sidebar snapshot only for the current recognized Hunting Box.

- [ ] Write RED tests for cached cross-page rows, new opening markers, unseen rows joining fresh advice, stale quotes becoming UNKNOWN, explicit zero removal and closing/disabling retaining private observations without box demand.
- [ ] Add lifecycle tests for connection/profile invalidation and late restore. Unknown profile never restores another scope; verifying a profile clears unknown-scope observations before current page recapture.
- [ ] Implement session integration. Treat a two-second gap outside the box as a new opening, allowing brief page transitions. While box is open, demand remembered supported products plus existing personal watches; outside retain existing watch-only behavior. Keep existing request throttles and limits.
- [ ] Load orders and box cache on the existing worker. Coalesce ownership saves with `WatchSaveQueue`; save only changed data, retry failures, and keep network/file work out of rendering. Scope/disable/reset paths invalidate UI immediately.
- [ ] Run session/market tests to GREEN; commit verified Task 2 files locally.

### Task 3: Right-side panel and delivery

**Files:** Create `ShardSaleSidebar.kt`, `ShardSaleSidebarLayout.kt`; modify `ShardSaleGuidance.kt` registration and `docs/SHARD_SALE_GUIDANCE.md`; tests `ShardSaleSidebarLayoutTest.kt`, extend the isolated native fixture in outputs only.

**Interfaces:** Pure `ShardSaleSidebarLayout.bounds(screenWidth, screenHeight, inventoryRight, rowCount)` returns panel bounds, scale and visible-row capacity; exposes clamped scroll/hit testing. Renderer consumes only cached `SavedBoxRow` values, existing font/color helpers and native `ScreenEvents.afterExtract`/`ScreenMouseEvents.allowMouseScroll` callbacks.

- [ ] Write RED layout tests for standard/small windows, GUI scaling, zero/37 rows and scroll clamping; scrolling is consumed only inside panel bounds. Hide the panel when there is no usable right-side space, rather than overlap inventory slots or draw outside the window.
- [ ] Render a compact alphabetical list anchored right of inventory with green SELL, red HOLD, gray unavailable text; align statuses and truncate long names. Show previous-observation marker/hover age and a prompt to visit pages when no rows exist. Reset scroll per opening and clamp after row changes.
- [ ] Register one hook set per screen; use current identity/switch/SkyBlock gates. Disable/close hides immediately. Do not revive the shelved valuation UI or change existing slot borders/tooltips.
- [ ] Run complete `:26.1.2:test :26.1.2:verifyTargetJar`; run the native offline fixture with installed NoFrills/owo-lib to verify tooltip compatibility, panel placement, all row states, reopening, scrolling and inventory interaction. Never modify or close the user's live client for fixtures.
- [ ] Obtain one fresh whole-change review under Native execution. Fix important findings, update concise docs, run read-only documentation sync and relevant checks again, then commit the final verified local change.
- [ ] Deliver a verified 26.1.2 JAR and local validation receipt in chat, with remaining live Hypixel limitations. Installation is a separate authorized step; no GitHub writes.

## Self-review

All approved requirements map to the three tasks. Cross-page persistence, scope isolation, freshness, rendering/input bounds and lifecycle races have explicit tests. Fixed limits not supplied by the design (64 KiB cache and two-second page-transition grace) are builder choices using existing repository patterns. Native execution is recommended because these three tasks share one session/ledger interface and the existing feature was implemented natively.

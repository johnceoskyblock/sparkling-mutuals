# Bazaar shard unlist guidance

Status: approved by the human in this chat on 2026-11-03.

## Intent and selected approach

Identify the player's active Safari shard sell offers on visible Bazaar Orders pages. A shard qualifies when it is in the current cached /profit top ten OR has a positive remembered Hunting Box count in the current profile/connection. The human chose Safari shards first; extending price guidance beyond the37Safari roster is deferred. Reuse the existing confirmed range engine, shared quotes, history bounds and order watcher.

Show a bright cyan-blue outline around qualifying owned offers whose confirmed recommendation is HOLD. Leave SELL, UNKNOWN, unsupported, ineligible, other-owner, buy-order and completed rows unmarked. The existing Hunting Box presentation and quick display-only advice stay unchanged.

## Ownership and visible menus

- Read only Your Bazaar Orders and Co-op Bazaar Orders pages the player opens. The Bazaar home page does not expose all offer rows. No automatic menu navigation or order actions.
- Require the existing explicit SELL marker, unambiguous Safari shard identity, offer quantity and unit-price evidence. Keep conflicting/duplicate fields excluded.
- When a By seller line is present, require a single valid name matching the local Minecraft username, case-insensitively.
- As explicitly approved, absent By is accepted only on personal Your Bazaar Orders. Shared co-op pages require a matching owner. This follows SkyHanni's documented solo-profile omission.
- Personal tracking/markers require the existing fresh verified account/profile scope. Reconnect/profile/connection changes clear personal baselines until revalidated; saved profiles never establish current identity.

## Eligibility and data

Use ProfitableCritters.cachedSpecies() for the existing ticket/price-preference ranking; do not run commands or scrape chat output. Combine it with remembered Safari ownership from the current scoped Hunting Box tracker. Quantities refresh on visible page observations, and explicit zero removes ownership eligibility.

Use the broad ownership tracker as the shared source for this eligibility, observing box pages when guidance is enabled even if the separate value header is hidden. This avoids stale competing copies of counts and does not make the header visible. Keep the roster-limited advice list and existing saved data compatible.

Filter offer watches, history demand and markers to the qualifying product union. Eligibility loss clears the recommendation baseline; becoming eligible while already on HOLD cannot invent a transition. The feature remains bounded by37Safari products and the existing quote/history/provider/cache limits. No new collector or price source.

## Change-only notifications

- The human chose chat only when guidance changes to HOLD. Remove the former initial/already-HOLD notice.
- Require an observed confirmed SELL followed by confirmed HOLD under the same rule, with an actual ask decline. Keep two distinct server quotes at least sixty seconds apart, existing hysteresis and freshness/coverage checks.
- Opening an already-HOLD offer may show its blue outline but sends no message. UNKNOWN recovery, profile/rule changes and new eligibility cannot manufacture a change.
- Group duplicate offers for one shard and simultaneous changed shards into a concise local chat suggestion. Preserve the ten-minute per-product cooldown and require a confirmed return to SELL before another decline alert.
- Example: **Bluebird changed to HOLD. Consider unlisting your last observed sell offer if it is still active.** Recommendations use experimental reference ranges, not validated forecasts.
- As with the existing watcher, revalidated qualifying offers can continue receiving change alerts after the menu closes while guidance remains enabled and scope remains valid. Wording refers to last observed offers because they may have filled. Blue outlines apply only to currently visible qualifying rows.

## Rendering and controls

Reuse the existing Shard sale guidance switch and passive container outline hook. Use a bright cyan-blue outline, with no mark or added order tooltip for rows to leave listed or with unavailable advice. Cache menu/revision/item identity so page changes cannot reuse stale slot markings. No render-time parsing, HTTP or disk work; no click interception.

The recently simplified Hunting Box total and top-ten panel styling remain as delivered. This increment builds26.1.2 only and stays local: no GitHub writes, release, live-instance replacement or game-session closure.

## Acceptance and implementation latitude

Tests cover the approved owner fallback, explicit wrong owners, co-op ambiguity, duplicate offers, filled/buy rows, top-ten OR owned eligibility, zero counts, selection/profile changes, initial HOLD silence, real confirmed transitions, grouped cooldowns, UNKNOWN recovery and stale slot invalidation.

Full26.1.2 tests/build and an isolated native NoFrills/owo fixture must verify blue own-HOLD outlines, no SELL/UNKNOWN/other-owner marks, chat transition behavior, page changes and unchanged Hunting Box overlays. Live Hypixel menu fields remain subject to gameplay confirmation.

Builder choices: use a dedicated parsed slot-evidence record rather than changing price semantics; share confirmed recommendations with watches and order markers; preserve legacy watch-cache readability without reviving old initial notices. Exact class decomposition, notification formatting for multiple names and regression fixtures belong to the implementation plan.

## Source context

Current GitHub repository Hunting Box documentation and current local market/runtime files were reconciled before design. Primary reference inspected: SkyHanni BazaarOrderApi.kt at a168f0089f0e8aec70539683f897744a22fef716; it documents missing owner lines on solo profiles and matches By names otherwise. Current local base is129302f.

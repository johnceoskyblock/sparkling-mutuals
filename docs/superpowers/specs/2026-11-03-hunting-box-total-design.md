# Hunting Box sell-offer total

Status: approved by the human in this chat on 2026-11-03.

## Intent and selected approach

Show one combined sell-offer value above the Hunting Box inventory, covering recognized shards across visited pages. No individual shard-name/value list and no generic hovered-item valuation. The human selected sell-offer prices and agreed to cross-page tracking with incomplete-scan/older-count notes.

Use the existing shared Bazaar book and profile-safety patterns, extending ownership recognition beyond the 37-species Safari guidance roster for this valuation only. A current-page subtotal cannot meet the whole-box request; restoring the archived value list would add the unwanted individual rows. The selected approach is a compact cumulative total.

## Behavior

- Display a compact line such as **Estimated box value: 123.45M coins** above the inventory, with **Sell offers** identified as its basis.
- Value every recognized owned Hunting Box shard, independently of the top-ten profit ranking. Keep the existing top-ten HOLD/SELL sidebar, borders, tooltips and alert rules unchanged.
- Multiply the last observed owned quantity by the current lowest valid competing sell-offer ask from the shared Hypixel book. This is an estimate before fees, not a promise that every shard sells at that price.
- Visiting a page replaces the observed quantities; repeat visits and overlapping pages cannot double-count. Explicit zero removes ownership. Unvisited or filtered-out items cannot prove zero ownership.
- Retain remembered counts between openings and save them only under a freshly verified account/profile. Until profile evidence is available, use connection-local observations. Mark older counts clearly.
- State visited-page coverage when reliable metadata is available. Never call a partial/filtered scan a confirmed complete box. Unknown coverage remains explicitly partial.
- Missing prices produce a labeled known-value subtotal; if none are usable, show unavailable. Stale live quotes cannot silently remain current valuations.
- Use the inventory's actual bounds and existing style. Scale or hide the header when there is no readable space above it; never cover item slots or ordinary tooltips.
- Add an independent **Hunting Box total value** switch under SkyBlock Helpers, initially enabled for this requested feature. Turning it off hides the total and stops valuation-only demand. The HOLD/SELL guidance switch remains independent.

## Constraints and builder choices

Minecraft 26.1.2 only, local source/builds; no GitHub writes or instance changes. No automatic page navigation, order actions, private API credentials, per-shard price requests or restored general item/chest valuation. Reuse the shared one-minute quote throttle; no historical-range requests are needed for totals.

Builder choices: keep parsing, aggregation and bounded profile persistence testable separately from rendering; reuse the established worker/save protections; isolate expanded identities from the narrow Safari advice allowlist. Choose bounded cache limits and safe layout constants in the implementation plan.

## Acceptance

Tests cover multi-page totals, repeats/overlap, quantity changes/zeros, supported non-Safari shards, controls/unrelated items, missing/stale prices, partial/filter coverage, profile/connection isolation and saving. Native 26.1.2 checks with NoFrills/owo verify header placement, combined total, no added per-shard valuation, and coexistence with the existing advice panel. Deliver a verified local JAR; live gameplay confirmation remains a separate check.

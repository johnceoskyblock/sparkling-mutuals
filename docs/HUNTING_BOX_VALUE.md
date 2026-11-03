# Hunting Box total value

## Current local 26.1.2 feature

**SkyBlock → Helpers → Hunting Box total value** shows one combined estimated sell-offer value above the inventory, without a shard-value list or generic item-value tooltips. It reuses the existing saved huntingBoxValue preference (default on), independently of [HOLD/SELL guidance](SHARD_SALE_GUIDANCE.md). General item/chest valuation remains shelved.

For every recognized owned shard, multiply the last observed count by the lowest valid current competing ask from the shared Hypixel Bazaar book. This is an estimate before fees, not guaranteed fills. It includes non-Safari shards and products outside the top-ten advice list. Visit pages to collect quantities; repeats replace, overlaps deduplicate and explicit zero removes ownership. Missing or filtered entries never prove zero.

At the human's presentation amendment, the header shows only **Hunting Box value: <coins>**, using the existing Safari Progress HUD's saved panel background, border and transparency. Page counts, sell-offer basis, subtotal wording, session/profile hints and other helper lines are hidden. Internally the scan still tracks incomplete coverage, older counts, missing prices, unresolved identities and limits; the displayed amount sums usable observed holdings and may be partial. No usable prices means unavailable. Empty loading packets cannot claim a zero box. Quotes expire after three minutes, including between ticks. The header uses actual container bounds, renders before ordinary tooltips, scales no lower than 75%, and hides when there is insufficient readable space above the inventory.

Only exact box titles, inner slots and unambiguous Owned lore count. Native SHARD_ IDs use bounded grammar. Generic short/long labels require shard-footer evidence and an exact match against the shared all-product catalogue. Conflicting metadata, duplicate slots, controls and player inventory are excluded. Unresolved labels remain internally unrecognized rather than guessed.

Private local sparkling-mutuals-box-total.json holds four freshly verified account/profile scopes, 512 products each, 96-character IDs, 512 KiB final plus512 KiB atomic stage (1 MiB combined). Only positive ownership counts persist; prices and identity claims do not. Bounded in-memory zero tombstones reject older observations. Late restore cannot overwrite live edits. Until fresh server profile confirmation establishes scope, use connection-local observations. Enabled shard sale guidance automatically requests confirmation and recognizes current clickable replies; the value-only feature still accepts the legacy Profile ID line. Reconnect/profile changes clear visible state pending fresh evidence.

The existing worker and coalescing save queue handle disk work. Valuation reuses the shared public Bazaar response with its existing one-minute minimum, with no per-shard/page requests, history demand, private credentials or uploads. Rendering reads cached summaries only. Its toggle ends valuation-only demand immediately.

Unit tests cover aggregation, identities/lore, coverage, stale/missing prices, overflow, profiles, save bounds/recovery, late restore, settings and layout. Native NoFrills/owo fixtures cover combined totals, independent toggles, no valuation-only history demand, actual bounds, tooltip layering and resize. Live Hypixel menu/profile behavior remains pending.

Primary reference: SkyHanni HuntingBoxValue.kt and AttributeShardsData.kt at a168f0089f0e8aec70539683f897744a22fef716. The former totals one inventory at a time; this implementation follows the human's compact cumulative-total design.

Updated 2026-11-03. The historical description below concerns the archived individual-value list.

## Archived individual-value list

> Shelved as of 2026-11-02. This feature is not included in the mod or config UI. Sources, tests and supporting data are retained under `tools/shelved/item-values` for a future release. The behavior below is archival, not an implementation requirement.

## Controls

Advanced → SkyBlock → Helpers → **Hunting Box value** defaults on. The former Advanced → Endgame page moves to Advanced → SkyBlock → Endgame. Phantomleaf Solver keeps its existing saved preference.

## Display

While viewing the Hunting Box in SkyBlock, show a scrollable list beside the inventory, sorted by total instant-sell value. Each entry has its item icon, name, owned amount and instant-sell total. Hover for per-shard and total instant-sell/instant-buy prices and highlight its slot on the currently open page. The footer shows cumulative shard count and instant-sell/buy totals. Missing prices are explicitly marked; quoted subtotals are lower bounds, not complete totals.

This is an independent implementation of SkyHanni's Hunting Box valuation behavior using our native screen events and existing cache. Reference inspected: SkyHanni `HuntingBoxValue.kt` and `AttributeShardsData.kt`, local commit `9cfab4c88`.

## Cumulative pages

Read only the inner shard slots (9–44, excluding border columns), with a shard item identity and exact `Owned: … Shards` lore. Item stack counts do not establish ownership. Support native `SHARD_` identities and legacy attribute-shard items whose displayed shard name identifies the Bazaar product.

A stable page snapshot replaces its previous quantities. Accumulate all visited pages, deduplicating each shard identity even when views overlap. Use explicit page metadata where supplied; otherwise recognize pages by their shard identities, independent of amounts and ordering. Show scanned-page coverage, with a total page count when available. Unvisited pages are not inferred or fetched.

Keep the scan while browsing, allowing brief menu transitions. Leaving the box for over two seconds, changing filters/sort/search, switching accounts/profiles, or disconnecting clears it so stale pages cannot silently remain a whole-box total. The next opening starts a fresh scan. Nothing is written to run, sparkling or PB history.

## Prices and traffic

Reuse the existing public, keyless Hypixel Bazaar request and three-hour throttle, as selected by the user. The shared cache retains every Bazaar product, including all `SHARD_` products, Rainbow Feathers and Safari Essence. No per-shard, per-page, profile or Shiny Hunter cloud requests are added. A box open can trigger the same lazy refresh when that shared cache is due. Missing quotes stay unknown.

## Validation

Focused checks cover cumulative pages, repeated visits, changing counts, overlapping views, lore/slot validation, native/legacy item identities, page metadata, unknown prices, value sorting, settings persistence and the relocated Phantomleaf control. Verify live Hunting Box lore/navigation, page switching, scrolling, hovers and placement at your GUI scale in game.

## Last updated

2026-11-02 — Added SkyHanni-style Hunting Box valuation with cumulative visited-page totals and the Advanced → SkyBlock Helpers/Endgame tabs.

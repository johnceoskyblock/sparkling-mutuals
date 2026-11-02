# Hunting Box value

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

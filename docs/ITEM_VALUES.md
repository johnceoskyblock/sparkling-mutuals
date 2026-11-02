# Estimated item and chest values

> Shelved as of 2026-11-02. This feature is not included in the mod or config UI. Sources, tests and supporting data are retained under `tools/shelved/item-values` for a future release. The behavior below is archival, not an implementation requirement.

## Controls and display

Advanced → SkyBlock → Helpers contains independent **Estimated item value** and **Estimated chest value** toggles, both on by default. Existing configurations keep all previously saved preferences.

Hover an inventory item in SkyBlock to see its base and installed upgrade costs beside the screen. Scroll over the panel for longer breakdowns. Hover components for their product IDs and price sources. Move the cursor onto the panel to keep the hovered estimate visible.

The chest panel lists stored items by total value and shows a combined total. Identical items with identical upgrades combine into one row; stack counts multiply the unit estimate. Hover a row for its breakdown and to highlight matching slots. Player inventory slots are excluded. Only the currently open storage page is counted, unlike the separate cumulative Hunting Box feature.

Supported storage titles: Chest, Large Chest, Ender Chest and its numbered pages, Backpack Slot menus, Personal Vault, Chest Storage and Wood Chest+. Shop, auction, ticket, recipe and Hunting Box menus do not become chest totals.

## Calculation

Adapted from SkyHanni's EstimatedItemValueCalculator and ChestValue at revision `9cfab4c88`. Base prices use Bazaar buy quotes, then public auction/market quotes. Where available, NPC prices provide an explicitly incomplete fallback.

The breakdown accounts for enchantments, recombination, reforge stones/application fees, potato books, stars and essence/material costs, Crimson prestige, gemstones/unlock costs, skins, runes, ability scrolls, drill and rod parts, pet held items and common permanent book upgrades. Bundled Hypixel, NEU and SkyHanni metadata establishes costs. A skinned pet market quote already includes the skin and does not receive a second skin cost.

These are replacement estimates, not guaranteed resale prices. Unknown component prices show `?`, and affected totals read **Known total**. Exact intermediate pet level premiums, candy/collector discounts and attribute roll premiums are not priced. Soulbound items show a replacement-cost note. Missing auction quotes do not cause recipe searches or per-item API requests.

## Network and cache

- Reuse the existing public Hypixel Bazaar GET, at most once every three hours while needed. Retain every product from that same response; no additional Bazaar requests are added.
- Fetch one public bulk lowest-BIN table from `https://moulberry.codes/lowestbin.json`, at most once every three hours while either value feature is used. If unavailable, try `https://raw.githubusercontent.com/SkyHelperBot/Prices/main/pricesV2.json` once in that window. This fallback is labelled **Market estimate**, rather than Lowest BIN.
- Save successful public market quotes and their timestamp in `sparkling-mutuals-market-prices.json`; restarting retains the cache. Failed refreshes retain earlier quotes and wait for the next three-hour window.
- No profile, player, ShinyHunter cloud, per-hover or per-slot requests. Inventory contents are never uploaded. Item metadata is bundled with the mod.

## Validation

Focused checks cover upgrades/stack totals, books, missing prices, pet skin double counting, Crimson prestige, storage menu filtering, independent toggle persistence and refresh throttling. All supported target versions compile. Live menu placement, item NBT variants and prices should still be checked in game.

Manual checks: hover an upgraded weapon and a stack of Bazaar items; scroll its breakdown; open a chest/backpack and compare totals; change a stack count; hover rows; open a shop and confirm no chest total; disable each toggle separately; test your usual GUI scales.

## Last updated

2026-11-02 — Added SkyHanni-derived item and chest valuation with shared Bazaar prices and a separate three-hour public market cache. See packaged SkyHanni and NEU license notices.

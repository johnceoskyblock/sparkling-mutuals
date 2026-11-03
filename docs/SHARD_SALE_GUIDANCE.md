# Experimental shard sale guidance

## Use and meaning

Local development for **Minecraft 26.1.2 only**. Enable **SkyBlock → Helpers → Shard sale guidance (experimental)** in `/sm`; default off. Owned Hunting Box shards in the current **`/profit` top ten** show green for SELL near/above a recent reference target, red for HOLD below it, and gray when data is unavailable. Other shards receive no range border or added range tooltip. Hover for current competing sell-offer price, reference target, quote age and explanation. Icons, server stacks, clicks and ordinary `/profit` rankings are unchanged. General item/chest valuation stays shelved. The independent cumulative Hunting Box total is described in [Hunting Box value](HUNTING_BOX_VALUE.md).

Selection reuses `ProfitableCritters.cachedSpecies()`, exactly matching the command's cached estimated coins-per-run ranking, chosen ticket and insta-sell/sell-offer preference. It is not a fixed list or highest unit-price ranking. Rockmite remains eligible when listed by the command; entity-ESP exceptions do not apply to the box. No need to run `/profit` manually. New cached quotes/preferences update the selection on the next client tick. If the ranking is unavailable, no fallback highlights appear; opening the enabled box may bootstrap the existing throttled quote refresh. The command remains read-only. HOLD/SELL targets still use experimental history ranges and live prices, independently of the ranking metric.

These are descriptive historical ranges, **not validated forecasts, recovery times, order fills or guaranteed profits**. A below-range price can continue falling. The human approved this experimental meaning after every tested forecast family failed qualification. No three-hour probability or predicted waiting time is implied. No trading actions are automated.

## Saved cross-page list

The Hunting Box also shows an alphabetical list to the right of the inventory: green **SELL**, red **HOLD**, gray **UNAVAILABLE**. Scroll over the panel to see additional rows; inventory scrolling elsewhere is unchanged. The same guidance switch controls the panel, slot borders and tooltips. It lists remembered owned shards currently in the `/profit` top ten (or fewer when the command has fewer valid prices). Ownership for the full 37-species catalog is retained so ranking changes can use previously visited pages without erasing them.

Each visited page updates remembered ownership; repeat visits replace counts rather than adding them. Explicit zero-owned entries remove that product. Missing rows, filters and unopened pages cannot prove zero ownership. Previously observed rows remain between openings; hovering shows the observed count and age. At the human's presentation amendment, visible rows contain only names and HOLD/SELL/UNAVAILABLE states, without ownership asterisks, owned-count subtitles, session/profile hints or experimental footers. Both box overlays use the existing Safari Progress HUD's saved panel styling. Closing the box hides the panel. Market advice is always refreshed and subject to the existing freshness/coverage rules, even when ownership is old.

Verified account/profile lists survive restarts in private local `sparkling-mutuals-box-guidance.json`, with four scopes and 37 entries per scope, <=64 KiB final and <=64 KiB atomic staging file (128 KiB combined). The existing worker and coalescing save queue handle disk work. Restored ownership never restores prices or establishes profile identity. Until a fresh **Profile ID:** line is received, only the current connection's observations are used; `/profileid` enables profile-specific saving. Unverified observations are not written to disk or adopted into a newly verified scope.

Opening the box updates its visible page and requests advice for remembered owned top-ten products within existing request limits. Existing personal sell-offer watches retain their own demand and alert scope, including products outside the box's top ten. Other pages update when visited; no automatic menu navigation is performed. The panel uses actual container bounds, scales down only to 75%, and hides if the remaining margin is too small to remain readable without overlapping inventory slots.

## Data and rule

Live quotes use the public [Hypixel Bazaar endpoint](https://api.hypixel.net/): lowest valid `buy_summary` ask and highest valid `sell_summary` bid, with coherent spreads and server timestamps. Legacy `/profit` weighted `quick_status` prices remain separate.

Anonymous [Skyblock.finance](https://skyblock.finance/) chart requests use `bz!a!SHARD_*` ask symbols. Two days of minute candles supply closed five-minute medians; eight days of fifteen-minute candles supply closed two-hour medians. Each aggregate needs at least 80% actual samples. Null-volume candles mark missing source observations; volume is not executed trades. Missing bins are not interpolated, and highs never establish touches. Historical publication times and exact aggregation provenance remain unknown, honestly recorded.

The descriptive rule blends the 60th percentiles of three hours/day/week with 60%/30%/10% weights, clamped to six-hour 10th–90th percentiles. Three falling closed fifteen-minute medians totaling at least 1% decline enable a falling-range exit interpretation. These are explanatory choices, not successfully validated forecasts. Require 80% three-hour/day/six-hour/week coverage, latest fine bin within fifteen minutes, live quote within three minutes, and newest history reference within 10% of current ask. Failure removes colors and market alerts.

SELL enters at 99.5% of target. Box borders, tooltips and sidebar change a displayed SELL to HOLD on the first valid ask below 98.5%, retaining HOLD until the 99.5% SELL threshold is reached. This display-only state does not wait for alert confirmation. Personal alerts still require two distinct server snapshots at least sixty seconds apart for SELL→HOLD. Repeated reads cannot confirm alerts. UNKNOWN and scope/connection changes reset baselines; recovery cannot invent a drop. Recalculation without an actual ask decline cannot trigger a price-drop alert.

## Personal sell offers

Open **Your Bazaar Orders** or **Co-op Bazaar Orders**. Parsing requires explicit bold SELL, a recognized exact shard name, Offer amount and explicit Price per unit coins. Conflicting metadata, ambiguous fields, buy orders and detail/unrelated screens are ignored. Co-op entries need an explicit owner matching the local username; explicit other owners are excluded. Box slots separately require server shard identity or exact recognized legacy attribute-shard identity and Owned lore, excluding controls/borders/player inventory.

Watches need the local account UUID and a fresh exact **Profile ID:** UUID server line. If missing, check `/profileid`; reconnect/profile/login changes invalidate scope until confirmed again. Box ranges can work while personal scope is unknown. No personal API credentials or order uploads are used.

One initial HOLD notice is labeled separately. Subsequent confirmed SELL→HOLD changes need an actual lower ask, one grouped notice per product, ten-minute cooldown and a confirmed return to SELL before another decline. Warnings say **last observed sell offer**, include age, and ask the player to check orders because it may have filled. UNKNOWN and profile transitions suppress notices. Missing pages, completed rows and detail/cancel controls cannot prove every other offer for that product disappeared.

Fields were researched in [SkyHanni BazaarOrderApi at 07716a4](https://github.com/SkyHanniStudios/SkyHanni/blob/07716a4366d94b20eb9e1de9f7b853fd9384f220/src/main/java/at/hannibal2/skyhanni/features/inventory/bazaar/BazaarOrderApi.kt) and associated BazaarApi. Fixtures combine supported fields; they are **not captured live 26.1.2 sell tooltips**. Sell-detail cancel/claim wording is unsupported. Profile UUID grammar is deliberately stricter than public parsers; unmatched evidence remains unknown.

Live Hunting Box inspection on 2026-11-03 showed the short `Hideonwall` label, `Owned: 243 Shards` and `RARE FOREST SHARD (ID R79)` footer. Menu icons may use that short roster label rather than the carried item's `Hideonwall Shard` name. When the custom ID is absent or generic `ATTRIBUTE_SHARD`, short labels require exactly one shard footer plus the existing exact box, inner-slot and positive owned-count checks. Conflicting product IDs are rejected. The live custom ID itself was not captured; this fallback does not assume one. Missing history produces UNKNOWN guidance for recognized slots.

## Lifecycle and bounds

One integration and one lazy daemon worker. Rendering only reads cached slots, guarded by menu identity, revision and item reference. Parsing, HTTP and disk work stay outside drawing/tooltips. Disable and cached reads do not wait for background processing.

Fresh shared quotes are published for all demanded products with usable cached history before pending history responses are parsed/written, then refreshed again after accepted history. Cold missing history still loads serially within the existing limits. The faster display removes the additional alert-confirmation wait; it does not accelerate the provider's own updates or change the one-minute quote-fetch minimum.

The container tooltip hook shadows the hovered-slot field directly with `@JvmField`. It must not generate Kotlin `getHoveredSlot`/`setHoveredSlot` methods: those collide with NoFrills' public slot accessor and crash container tooltip rendering. A compiled-bytecode regression guards this compatibility boundary.

Demand is limited to visible box products plus revalidated personal watches, at most 37 Safari shards. Shared live refresh is at most once/minute; ordinary consumers keep three-hour cadence. History requests are serialized at least one elapsed second apart: recent data every ten minutes, week context hourly. At most 74 queued jobs, one active request, 2 MiB response, twenty-second timeout, capped fifteen-second→fifteen-minute backoff and a provider-wide elapsed Retry-After cooldown across products, views and sessions. Disable, leaving SkyBlock or disconnect invalidates demand/UI and cancels unnecessary history work. Late generations cannot publish. No collector runs after Minecraft closes.

`sparkling-mutuals-shard-history.json` stores public prices only: eight days, 2,304 fine/96 coarse points per product, 8 MiB each final/staging file (16 MiB combined). Invalid/oversized caches are rejected; replacement is atomic. A zero-byte per-cache OS lock coordinates writers and permits recovery of bounded orphan staging files after a crash. Failed private saves retain the latest snapshot, acknowledge only successful writes and retry with bounded backoff. Private local `sparkling-mutuals-sale-watches.json` holds four recent account/profile scopes, 37 product watches each, thirty-day retention, 512 KiB each final/staging file (1 MiB combined). Restored watches always need live revalidation. Remembered profiles never establish current identity.

## Research and remaining validation

Research used the preceding fourteen days: twenty ask/bid series, 403,200 minute candles for ten shards. A common 622-minute null-volume/unchanged-price interval was excluded. Original and 24 variant rules, 36 movement candidates and twelve predeclared modest-move candidates produced no fully qualified forecast. The latter maximum training touch rate was 4/9 (44.4%); a Chuckwalla training candidate had only two validation HOLDs. Strict research replay retains its original provenance/availability/performance gates. Experimental mode does not rewrite those reports; touches would not establish fills even for a passing model.

The separate five-minute official collector is **stopped**, its six accepted snapshots preserved. Do not restart without a new instruction. Research chats are grouped under Sparkling Mutuals research. The Google Doc and its paused checks are unchanged; no Doc fallback is active.

Detailed unit/native receipts remain in the local execution ledger. Live Hypixel menu/profile delivery, order lifecycle and trading usefulness still require gameplay confirmation. Other Minecraft targets, every provider product and every market regime are unverified. Source/build changes stay local; no push, release or user-instance installation is implied.

## Last updated

2026-11-03 — Made box decisions update on the first valid below-range quote, kept personal alerts confirmed, and published shared prices before pending history processing. Top-ten selection, saved ownership, target rules and request limits are unchanged.

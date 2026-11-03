# Experimental shard sale guidance

## Use and meaning

Local development for **Minecraft 26.1.2 only**. Enable **SkyBlock → Helpers → Shard sale guidance (experimental)** in `/sm`; default off. Hunting Box shard slots show green for SELL near/above a recent reference target, red for HOLD below it, and gray when data is unavailable. Hover for current competing sell-offer price, reference target, quote age and explanation. Icons, server stacks, clicks and ordinary `/profit` rankings are unchanged. Shelved valuation stays shelved.

These are descriptive historical ranges, **not validated forecasts, recovery times, order fills or guaranteed profits**. A below-range price can continue falling. The human approved this experimental meaning after every tested forecast family failed qualification. No three-hour probability or predicted waiting time is implied. No trading actions are automated.

## Data and rule

Live quotes use the public [Hypixel Bazaar endpoint](https://api.hypixel.net/): lowest valid `buy_summary` ask and highest valid `sell_summary` bid, with coherent spreads and server timestamps. Legacy `/profit` weighted `quick_status` prices remain separate.

Anonymous [Skyblock.finance](https://skyblock.finance/) chart requests use `bz!a!SHARD_*` ask symbols. Two days of minute candles supply closed five-minute medians; eight days of fifteen-minute candles supply closed two-hour medians. Each aggregate needs at least 80% actual samples. Null-volume candles mark missing source observations; volume is not executed trades. Missing bins are not interpolated, and highs never establish touches. Historical publication times and exact aggregation provenance remain unknown, honestly recorded.

The descriptive rule blends the 60th percentiles of three hours/day/week with 60%/30%/10% weights, clamped to six-hour 10th–90th percentiles. Three falling closed fifteen-minute medians totaling at least 1% decline enable a falling-range exit interpretation. These are explanatory choices, not successfully validated forecasts. Require 80% three-hour/day/six-hour/week coverage, latest fine bin within fifteen minutes, live quote within three minutes, and newest history reference within 10% of current ask. Failure removes colors and market alerts.

SELL enters at 99.5% of target. SELL→HOLD needs ask below 98.5%, confirmed by two distinct server snapshots at least sixty seconds apart. Repeated reads cannot confirm. UNKNOWN resets the baseline; recovery cannot invent a drop. Recalculation without an actual ask decline cannot trigger a price-drop alert.

## Personal sell offers

Open **Your Bazaar Orders** or **Co-op Bazaar Orders**. Parsing requires explicit bold SELL, a recognized exact shard name, Offer amount and explicit Price per unit coins. Conflicting metadata, ambiguous fields, buy orders and detail/unrelated screens are ignored. Co-op entries need an explicit owner matching the local username; explicit other owners are excluded. Box slots separately require server shard identity or exact recognized legacy attribute-shard identity and Owned lore, excluding controls/borders/player inventory.

Watches need the local account UUID and a fresh exact **Profile ID:** UUID server line. If missing, check `/profileid`; reconnect/profile/login changes invalidate scope until confirmed again. Box ranges can work while personal scope is unknown. No personal API credentials or order uploads are used.

One initial HOLD notice is labeled separately. Subsequent confirmed SELL→HOLD changes need an actual lower ask, one grouped notice per product, ten-minute cooldown and a confirmed return to SELL before another decline. Warnings say **last observed sell offer**, include age, and ask the player to check orders because it may have filled. UNKNOWN and profile transitions suppress notices. Missing pages, completed rows and detail/cancel controls cannot prove every other offer for that product disappeared.

Fields were researched in [SkyHanni BazaarOrderApi at 07716a4](https://github.com/SkyHanniStudios/SkyHanni/blob/07716a4366d94b20eb9e1de9f7b853fd9384f220/src/main/java/at/hannibal2/skyhanni/features/inventory/bazaar/BazaarOrderApi.kt) and associated BazaarApi. Fixtures combine supported fields; they are **not captured live 26.1.2 sell tooltips**. Sell-detail cancel/claim wording is unsupported. Profile UUID grammar is deliberately stricter than public parsers; unmatched evidence remains unknown.

## Lifecycle and bounds

One integration and one lazy daemon worker. Rendering only reads cached slots, guarded by menu identity, revision and item reference. Parsing, HTTP and disk work stay outside drawing/tooltips. Disable and cached reads do not wait for background processing.

Demand is limited to visible box products plus revalidated personal watches, at most 37 Safari shards. Shared live refresh is at most once/minute; ordinary consumers keep three-hour cadence. History requests are serialized at least one elapsed second apart: recent data every ten minutes, week context hourly. At most 74 queued jobs, one active request, 2 MiB response, twenty-second timeout, capped fifteen-second→fifteen-minute backoff and Retry-After. Disable, leaving SkyBlock or disconnect invalidates demand/UI and cancels unnecessary history work. Late generations cannot publish. No collector runs after Minecraft closes.

`sparkling-mutuals-shard-history.json` stores public prices only: eight days, 2,304 fine/96 coarse points per product, 8 MiB each final/staging file (16 MiB combined). Invalid/oversized caches are rejected; replacement is atomic. Private local `sparkling-mutuals-sale-watches.json` holds four recent account/profile scopes, 37 product watches each, thirty-day retention, 512 KiB each final/staging file (1 MiB combined). Restored watches always need live revalidation. Remembered profiles never establish current identity.

## Research and remaining validation

Research used the preceding fourteen days: twenty ask/bid series, 403,200 minute candles for ten shards. A common 622-minute null-volume/unchanged-price interval was excluded. Original and 24 variant rules, 36 movement candidates and twelve predeclared modest-move candidates produced no fully qualified forecast. The latter maximum training touch rate was 4/9 (44.4%); a Chuckwalla training candidate had only two validation HOLDs. Strict research replay retains its original provenance/availability/performance gates. Experimental mode does not rewrite those reports; touches would not establish fills even for a passing model.

The separate five-minute official collector is **stopped**, its six accepted snapshots preserved. Do not restart without a new instruction. Research chats are grouped under Sparkling Mutuals research. The Google Doc and its paused checks are unchanged; no Doc fallback is active.

Detailed unit/native receipts remain in the local execution ledger. Live Hypixel menu/profile delivery, order lifecycle and trading usefulness still require gameplay confirmation. Other Minecraft targets, every provider product and every market regime are unverified. Source/build changes stay local; no push, release or user-instance installation is implied.

## Last updated

2026-11-03 (America/New_York) — Approved descriptive-range meaning, local integration, source bounds and live-evidence limitations.

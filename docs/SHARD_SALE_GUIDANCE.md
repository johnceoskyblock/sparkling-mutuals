# Shard sale guidance

Default on in SkyBlock → Helpers. A one-time `migration.shardSaleGuidanceV2` upgrade enables the redesigned feature even if the old toggle was disabled. The migration is immediately persisted; subsequent saved on/off choices are respected. Other settings are preserved. Guidance covers all 37 Safari shards independently of `/profits` rankings. It recommends an action; it never sells, salvages or changes an order automatically.

## Hunting Box

One guidance panel lists every Safari shard. It expands into two columns when the screen margin permits, displays as many entries as fit, and scrolls for the remainder. Rows use three separated lines: shard/action, explicitly labelled coin value, and seven-day shard volume. Observed holdings rank by estimated recommended proceeds, highest first; per-shard estimates for unobserved holdings follow. The shared seven-day Safari average appears once in the header. Hover for quantities, alternatives and the reason. Observed holdings use estimated stack totals; unobserved holdings show `/ea` per-shard estimates. Visit box pages to refresh quantities; unopened pages cannot establish zero holdings.

Colors: green SELL OFFER, cyan SALVAGE, amber INSTA-SELL, gray while prices are unavailable. A fixed-position volume meter sits beside Low/Typical/High, separately from coin values. A tick marks 100% of the Safari average, and a full meter represents 150% or more. The header is emphasized and neutral dividers separate shard rows. Low means below 50% of the Safari average, Typical means 50–149%, and High means at least 150%. The meter fills at 150%; hover uses the same ordered sections for every shard: recommendation, separate colored selling/salvaging values, and seven-day volume compared with the average Safari shard. Volume estimates shards bought through sell offers. Missing activity is unknown, not zero. The average is omitted unless all 37 activity observations are available and fresh.

The existing Hunting Box value HUD remains separate. Both HUDs appear in `/sm gui` for five minutes after actually rendering. Drag, resize, snap or right-click for their settings. Saved positions persist after the editor entries expire. Unsaved positions fit the container's available margins automatically.

## Comparison

Use one fresh public Hypixel Bazaar snapshot (at most three minutes old). Sell-offer prices are the lowest `buy_summary` ask; instant-sale estimates use the highest `sell_summary` bid. Values are indicative before taxes and do not guarantee depth or fills.

Sell-offer buyer activity is estimated as `max(0, buyMovingWeek - buyVolume)`: the API's moving-week figure includes live orders. Compare each shard with the arithmetic average across all Safari shards. This is estimated activity, not a live fill-rate forecast.

Salvage yields per shard: Common 1, Uncommon 2, Rare 4, Epic 8, Legendary 16 Safari Essence. Value that essence at its current sell-offer price.

Recommendation rules:

- Salvage if its estimated proceeds meet or exceed both selling alternatives.
- Also favour salvage when activity is below half the Safari average and sell offers add at most 25%, provided salvage beats instant selling.
- For holdings exceeding one day's estimated activity, tolerate up to a 50% sell-offer premium before recommending salvage, again only if salvage beats instant selling.
- Prefer instant selling when activity is weak or holdings exceed one day's activity, instant selling beats salvage, and the sell-offer premium is at most 10%.
- Otherwise recommend a sell offer. A large premium can justify selling even with weak activity.

These thresholds are convenience heuristics. Quantity, relative demand and the actual premium matter together; low activity alone never forces salvage.

## Existing price history and personal listings

Skyblock.finance history remains optional reference context. Its descriptive target blends three-hour/day/week 60th percentiles with 60/30/10 weights, with existing coverage, freshness and falling-price safeguards. Missing history does not block the new live-price/salvage comparison.

Personal listing watches retain their existing profile-verified, observed-order eligibility and confirmed price-change alerts. They do not automatically take down listings. Box ownership is scoped to confirmed account/profile; unverified observations stay connection-local. Stored quantities never restore market prices.

No extra player/shared-cloud API calls are added. Demand and prices use the existing public Bazaar snapshot and its one-minute active-guidance refresh throttle. Optional history retains its existing ten-minute recent/one-hour weekly refresh and bounded worker/backoff.

## Last updated

2026-11-12 — Separated coin value from shard volume, added clearer volume labels and row spacing, and ranked observed holdings by estimated proceeds.

2026-11-12 — Enabled guidance by default, added all-Safari demand/salvage comparisons, simplified the panel and added recently visible Hunting Box HUDs to the editor.

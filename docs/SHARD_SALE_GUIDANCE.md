# Shard sale guidance

## Status and scope

In local development for Minecraft **26.1.2 only**, under the approved design and plan in `docs/superpowers/`. No release, GitHub publication or installation has been requested. The intended feature is an opt-in three-hour advisory for Safari shards in the Hunting Box and passive, local warnings about previously observed personal sell offers. This does not restore the shelved valuation feature.

## Verified data contract

Current prices use the public [Hypixel Bazaar endpoint](https://api.hypixel.net/): the lowest valid `buy_summary` price is the competing sell-offer ask; the highest valid `sell_summary` price is the bid. Existing `/profit` weighted `quick_status` prices and ranking retain their existing meaning.

Historical context provisionally uses [SkyCofl's documented public history API](https://sky.coflnet.com/wiki/api), with [data attribution](https://sky.coflnet.com/data). Its [legal page](https://sky.coflnet.com/about) asks API users to link that data page. Only documented anonymous GET endpoints are used; authenticated exports and personal-order APIs are excluded. Source inspection is evidence of field semantics, not copied implementation code.

The public SkyApi `Controllers/BazaarController.cs` passes UTC ranges to SkyBazaar. SkyBazaar `Services/BazaarService.cs` replaces snapshot `BuyPrice` with the first sell-offer ask before aggregation where the book is present. `Controllers/BazaarController.cs` exposes that representative price as `buy`, and timestamps are UTC (including timezone-less serialized UTC values). A timestamped Hideonwall snapshot cross-check matched `buy` to its lowest `buyOrders` offer, 155341.8 coins. Historical graphs may fall back to quick-status prices when order books are absent; that limitation must remain documented, and incoherent/missing records must not qualify recommendations.

The history resolution is chosen by **range length**: less than one hour uses detailed snapshots; less than 24 hours uses minute aggregates; a 24-hour or seven-day request uses coarser aggregates. Therefore replay collection uses bounded public ranges shorter than 24 hours, with overlap removed. A tested 23-hour range two days in the past returned 471 samples; an exactly 24-hour range returned 12. Weekly data is context, never a fabricated fine-grained replay. Normalize actual observations into closed five-minute bins, making them available only at bin closure; never use intrabin maximum/minimum to assert an earlier target touch.

The catalogue survey returned HTTP 200 and 81–82 weekly samples for all 37 Safari shard IDs. This establishes availability, not model accuracy, freshness or eligibility. The sample day-history response was approximately 26 minutes behind collection time, exceeding the proposed 15-minute history freshness bound; such data must remain unknown until a sufficiently fresh provider/local observation exists. Do not silently weaken that bound.

Replay needs earlier context before its evaluation window. Fetch and process older data chronologically in bounded chunks, keeping only the rolling cache plus a small margin in memory. The full downloaded research evidence stays outside the product/repository. Do not retain an unbounded historical collection in the mod.

## Validation

Unmodified 26.1.2 baseline: 1,139 tests passed with an external Gradle init script pointing Java's temporary directory to the feature's isolated local test directory. Initial normal-temp runs had transient JUnit directory-cleanup failures (three, then one different test); behavioral assertions were not disabled, and affected tests also passed unchanged on a focused retry. Keep those failures in the local validation record.

Model acceptance, provider coverage, actual Hunting Box and Manage Orders behavior remain pending. Target touches do not establish order fills. No automatic buying, selling, cancellation or repricing is permitted.

## Last updated

2026-11-03 (America/New_York) — Recorded approved local, 26.1.2-only scope and verified public provider contract; implementation and gameplay validation pending.

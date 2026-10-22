# Profitable critters command

`/profitablecritters` is a local-only command. It lists up to five Safari critters ranked by the cached price of **one shard**, descending. Each critter gets one local chat message containing its name, shard price, estimated average shards per run and estimated coins per run. Ties use alphabetical species order.

## Inputs and calculation

Spawn averages come from the user-supplied `Expected Runs for Diamond Emblem.xlsx` and `Expected Runs for Diamond Emblem (1).xlsx`, sheet **Critters Per Run**, cells **A3:B42**. Both workbooks have identical Full Party averages for all 37 roster species. Column C's Lazy Party averages are not used. Values are bundled in [average_critter_spawns.json](../src/main/resources/assets/sparkling-mutuals/data/average_critter_spawns.json); the mod does not open or depend on the original spreadsheet at runtime. These estimates do not modify actual spawn detection, completion quotas or capture requirements.

Use the requested fixed average of **1.5025 shards per critter**, corresponding to the user's 50.25 average Hunting Fortune assumption across four equally likely hotspot biomes. This is a full-party estimate, not a measurement of the user's equipment, actual hotspot distribution or live captures.

```
average shards per run = full-party average spawns × 1.5025
estimated coins per run = average shards per run × cached price of one shard
```

For example, Wumpa's average is 1 spawn per run. At an illustrative 1,000 coins per shard, the estimate is 1.5025 shards and 1,502.5 coins per run. These are gross shard proceeds before selling fees or other costs. Keep fractional estimates during calculation; display shards to two decimal places and coins rounded to whole coins. Ranking uses the unrounded shard price, not total per-run proceeds.

## Price source and network behavior

Use the existing Bazaar cache and the existing **insta-sell / sell-offer** run-profit preference. The command heading identifies the selected price convention and the full-party multiplier. Missing, zero, negative or non-finite shard quotes are excluded. If fewer than five valid quotes exist, show only those available; if none exist, show a local unavailable-prices message.

The command calls the read-only `BazaarPrices.cachedShardPrice` accessor. It never calls `ensureFresh` or the lazy-refreshing `value` accessor. It does not make shared-cloud, Hypixel, Bazaar or Mojang requests, even if prices are absent or stale. Existing normal three-hour Bazaar updates continue independently. No party discovery data is needed for this ranking, and no party chat is sent.

## Verification

Automated checks verify all 37 source species, fractional arithmetic, invalid/missing quote exclusion, the top-five cutoff and a case where per-run revenue would produce a different ranking than shard price. Live checks: run the command after prices load, verify five local critter messages, change the existing insta-sell preference and verify the quoted prices, and run before prices are available to confirm the local fallback without a new fetch.

## Last updated

2026-10-21 (America/New_York) — Added cached-price local ranking using supplied Full Party averages and the requested 1.5025 shard multiplier.

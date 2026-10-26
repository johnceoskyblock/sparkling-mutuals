# Profitable critters command

`/profitablecritters` is a local-only command. It lists up to ten Safari critters ranked by **estimated total coins per run**, descending. Each critter gets one local chat message containing its name, shard price, estimated average shards per run and estimated coins per run. Ties use alphabetical species order.

## Inputs and calculation

Scrappy, Rockmite and Troodon are excluded from this command because of the time needed to obtain them. This filter applies before ranking and the ten-result limit; it does not change spawn averages, mode requirements or other commands.

Spawn averages come from the user-supplied `Expected Runs for Diamond Emblem.xlsx` and `Expected Runs for Diamond Emblem (1).xlsx`, sheet **Critters Per Run**, cells **A3:B42**. Both workbooks have identical Full Party averages for all 37 roster species. Column C's Lazy Party averages are not used. Values are bundled in [average_critter_spawns.json](../src/main/resources/assets/sparkling-mutuals/data/average_critter_spawns.json); the mod does not open or depend on the original spreadsheet at runtime. These estimates do not modify actual spawn detection, completion quotas or capture requirements.

Use the requested fixed average of **1.5025 shards per critter**, corresponding to the user's 50.25 average Hunting Fortune assumption across four equally likely hotspot biomes. This is a full-party estimate, not a measurement of the user's equipment, actual hotspot distribution or live captures.

```
average shards per run = full-party average spawns × 1.5025
estimated coins per run = average shards per run × cached price of one shard
```

For example, Wumpa's average is 1 spawn per run. At an illustrative 1,000 coins per shard, the estimate is 1.5025 shards and 1,502.5 coins per run. These are gross shard proceeds before selling fees or other costs. Keep fractional estimates during calculation; display shards to two decimal places and coins rounded to whole coins. Ranking uses unrounded estimated coins per run, including spawn frequency and the shard multiplier.

## Price source and network behavior

Use the existing Bazaar cache and the existing **insta-sell / sell-offer** run-profit preference. The command heading identifies the selected price convention and the full-party multiplier. Missing, zero, negative or non-finite shard quotes are excluded. If fewer than ten valid quotes exist, show only those available; if none exist, show a local unavailable-prices message.

The command and profitable-shard ESP share a ranking computed from the read-only `BazaarPrices.cachedPrices` snapshot. The ranking is recomputed only when that snapshot changes or the insta-sell preference changes. Neither consumer calls `ensureFresh` or the lazy-refreshing `value` accessor. They do not make shared-cloud, Hypixel, Bazaar or Mojang requests, even if prices are absent or stale. Existing normal three-hour Bazaar updates continue independently. No party discovery data is needed for this ranking, and no party chat is sent.

## Profitable-shard ESP

In Unique and Sparkle modes, **Critter ESP for profitable shards** retains exactly the eligible species in this command's cached top ten, including after their ordinary capture/check requirement has completed. Each mode retains its own toggle. Scrappy, Rockmite and Troodon are excluded from this extra highlight list; their normal mode requirements still apply. Full Clear is unchanged. Loaded-entity and existing biome/group visibility gates still apply.

Before any valid quotes have loaded, the extra profitable list is empty; ordinary mode and sparkling-detection highlights remain available. A partial price cache yields fewer than ten species, matching the command. Updated quotes and changes to the price preference update both consumers together without repeated sorting for each rendered entity.

In Sparkle mode, a top-ten Gimmiegold also enables remaining Shining Coin floor-drop ESP while the profitable-shard toggle is enabled, even if everyone already owns shiny Gimmiegold. Floor-group and biome visibility restrictions still apply. This does not enable incense, add required shiny checks, change completion, or send unused-resource reminders for already shared discoveries.

## Verification

Automated checks verify all 37 source species, fractional arithmetic, invalid/missing quote exclusion, the top-ten cutoff and a case where per-run revenue would produce a different ranking than shard price. Live checks: run the command after prices load, verify ten local critter messages, change the existing insta-sell preference and verify the quoted prices, and run before prices are available to confirm the local fallback without a new fetch.

## Last updated

2026-10-25 — Extended profitable Sparkle Gimmiegold highlights to their Shining Coin sources; the same cached ranking and mode toggle control both.

2026-10-25 — Unique/Sparkle profitable-shard ESP now shares the command's cached top-ten ranking and exclusions, replacing the fixed five-species list with no new fetches.

2026-10-25 — Excluded Scrappy, Rockmite and Troodon from the profitability ranking.

2026-10-25 — Expanded to ten results and ranked by estimated total coins per run rather than coins per shard.

2026-10-21 (America/New_York) — Added cached-price local ranking using supplied Full Party averages and the requested 1.5025 shard multiplier.

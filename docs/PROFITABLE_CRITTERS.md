# Profitable critters command

`/profits <ticket>` and `/profit <ticket>` are local-only commands. Ticket literals autocomplete as `basic`, `economy`, `premium`, and `first-class`. The old `/profitablecritters` command is removed. With no argument, use the last selected ticket (Basic initially). The selection is saved and also controls profitable-critter ESP. Results rank up to ten Safari critters by **estimated shard plus Safari Essence coins per run**, descending, with alphabetical ties. Each compact row shows rank, rarity-colored species and gold estimated profit. Dark dividers separate entries. The heading names the ticket and coins/run unit; hover a row for shard and essence quantities, prices and contributions. Native chat component styles supply colors, rather than relying on formatting codes embedded in plain text.

## Inputs and calculation

All 37 critter types are eligible, including Scrappy, Rockmite and Troodon. There are no species exclusions. Ranking does not change spawn averages or mode completion requirements.

Spawn averages come from the user-supplied `Expected Runs for Diamond Emblem.xlsx` and `Expected Runs for Diamond Emblem (1).xlsx`, sheet **Critters Per Run**, cells **A3:B42**. Both workbooks have identical Full Party averages for all 37 roster species. Column C's Lazy Party averages are not used. Values are bundled in [average_critter_spawns.json](../src/main/resources/assets/sparkling-mutuals/data/average_critter_spawns.json); the mod does not open or depend on the original spreadsheet at runtime. These estimates do not modify actual spawn detection, completion quotas or capture requirements.

Use the requested fixed average of **1.5025 shards per critter**, corresponding to the user's 50.25 average Hunting Fortune assumption across four equally likely hotspot biomes. This is a full-party estimate, not a measurement of the user's equipment, actual hotspot distribution or live captures.

```
average shards per run = full-party average spawns × 1.5025
average essence per run = full-party average spawns × rarity essence × ticket multiplier
estimated coins per run = average shards per run × shard price + average essence per run × essence price
```

Base essence follows the supplied rarity table: Common 2, Uncommon 4, Rare 8, Epic 16, Legendary 32, using the roster rarity colors. Ticket multipliers are Basic 1.00, Economy 1.10, Premium 1.25 and First-Class 1.50. Essence uses spawns, never the 1.5025 shard yield. For example, one average Legendary spawn yields 32 essence with Basic or 48 with First-Class. These are gross proceeds before fees or ticket costs. Keep fractional estimates through ranking; display yields to two decimals and coins rounded to whole coins.

## Price source and network behavior

Use the existing Bazaar cache and the existing **insta-sell / sell-offer** run-profit preference. The command heading identifies the selected price convention and the full-party multiplier. Missing, zero, negative or non-finite shard quotes are excluded. If fewer than ten valid quotes exist, show only those available; if none exist, show a local unavailable-prices message.

The command and profitable-critter ESP share a ranking computed from the read-only `BazaarPrices.cachedPrices` snapshot. Recompute when that snapshot, the selected ticket or the insta-sell preference changes. The existing three-hour Bazaar response now retains `ESSENCE_SAFARI` as well as shard and feather quotes. Neither consumer starts a request. Until an essence quote is available, the command explicitly reports shard-only totals. No party discovery data is required and no party chat is sent.

## Profitable-shard ESP

In Unique and Sparkle modes, **Critter ESP for profitable shards** highlights the combined-revenue cached top ten, except Rockmite. Rockmite remains in the command ranking but receives no extra profitable highlight; no lower-ranked replacements are added. Ordinary mode-required entity/source highlights still apply. Snoozle is eligible for profitable ESP when ranked in the top ten. Each mode retains its own toggle. Full Clear is unchanged. Loaded-entity and existing biome/group visibility gates still apply.

Before any valid quotes have loaded, the extra profitable list is empty; ordinary mode and sparkling-detection highlights remain available. A partial price cache yields fewer than ten species, matching the command. Updated quotes and changes to the price preference update both consumers together without repeated sorting for each rendered entity.

In Sparkle mode, a top-ten Gimmiegold also enables remaining Shining Coin floor-drop ESP while the profitable-shard toggle is enabled, even if everyone already owns shiny Gimmiegold. Floor-group and biome visibility restrictions still apply. This does not enable incense, add required shiny checks, change completion, or send unused-resource reminders for already shared discoveries.

## Verification

Automated checks verify all 37 source species, fractional arithmetic, invalid/missing quote exclusion, the top-ten cutoff and a case where per-run revenue would produce a different ranking than shard price. Live checks: run the command after prices load, verify ten local critter messages, change the existing insta-sell preference and verify the quoted prices, and run before prices are available to confirm the local fallback without a new fetch.

## Last updated

2026-10-28 - Restored Snoozle profitable ESP eligibility; Rockmite is the only exclusion.


2026-10-27 — Excluded Rockmite and Snoozle only from extra profitable-critter ESP, retaining both in the command list and preserving normal mode-required highlights.

2026-10-27 — Removed `/profitablecritters`. `/profit` and `/profits` now return a compact colored list with per-entry dividers and hover-only reward breakdowns; calculation and ESP ranking are unchanged.

2026-10-27 — Added `/profit` and `/profits`, four ticket multipliers, rarity-based Safari Essence revenue and ticket-aware ESP ranking. Removed all species exclusions. Added essence to the existing Bazaar cache without new requests.

2026-10-25 — Extended profitable Sparkle Gimmiegold highlights to their Shining Coin sources; the same cached ranking and mode toggle control both.

2026-10-25 — Unique/Sparkle profitable-shard ESP now shares the command's cached top-ten ranking and exclusions, replacing the fixed five-species list with no new fetches.

2026-10-25 — Excluded Scrappy, Rockmite and Troodon from the profitability ranking.

2026-10-25 — Expanded to ten results and ranked by estimated total coins per run rather than coins per shard.

2026-10-21 (America/New_York) — Added cached-price local ranking using supplied Full Party averages and the requested 1.5025 shard multiplier.

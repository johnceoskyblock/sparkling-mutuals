# Sparkling rate estimate

`/rates` estimates the party's average Sparkling-mode check opportunities per run using cached discoveries. It works as a local command in any mode without switching modes or changing completion requirements.

Example when every species needs checking:

```
[SM] 144.99 critters checked per run. Your party should find a sparkling every 14.12 runs.
```

Both numbers display to two decimal places, but the calculation uses unrounded averages. Hover over the local message for the number and names of biomes to enter, species detected from the boat, additional passive detections, and manual checks/spawns. Each category includes its expected check total.

## Source and baseline

Use **Critters Per Run!A3:A42 and C3:C42** from the user-supplied `Expected Runs for Diamond Emblem.xlsx` and `Expected Runs for Diamond Emblem (1).xlsx`. Both workbooks have identical **Lazy Party** averages. Their sum is 144.994135774 expected critters per run, reproducing the supplied 14.12 baseline. The Full Party column totals 149.435288032 and gives 13.70 runs; it remains the input for `/profitablecritters`, not this command.

Both average sets are bundled in [average_critter_spawns.json](../src/main/resources/assets/sparkling-mutuals/data/average_critter_spawns.json). The spreadsheets are not needed at runtime. These are expected check counts, not actual current entities, guaranteed spawns or capture quotas. Do not multiply this count by the 1.5025 shard-yield assumption used for profitable critters.

```
expected critters checked = boat detections + passive detections in visited biomes + needed manual checks/spawns
average runs per Sparkling = 2048 / expected critters checked
```

This follows the user's requested 1/2048-per-critter estimate and assumes the planned checks are completed.

## Detection categories

The user identified six species as detectable immediately after leaving the boat, without entering their biome: **Flitter, Shyworm, Driftling, Chuckwalla, Mantis Shrimp and Troodon**. Always count their averages, even when already shared by the party or their biome is skipped. They are excluded from the other categories to prevent double counting. This is a rate-model assumption based on the user's observed detection behavior; the command does not extend detection range or make the server send unloaded entities.

For the remaining species, manual-check membership comes from the same timesave filter used by discovery commands. Birds use its existing grouped rule.

| Biome | Additional passive detections when entered | Manual checks / spawns |
| --- | --- | --- |
| Forest | Foxtrot, Treefrog, Woodchucker, Fluffling, Hideonfloor | Honeybug; Bluebird, Parakeet and Macaw as one spawning group |
| Cavern | Cavernfish, Scrappy | Rockmite, Snoozle, Gemzie |
| Icy | Strongarm, Tepid, Polaris, Shuddersquid, Billygoat, Nozzlenose | Wumpa |
| Haunted | Areita, Bloodbat, Duplico, Litterbug, Solsnatcher, Hideonwall, Hideyho | Gazer, Gimmiegold, Doomspiral |

## Party filtering

1. A species is needed when at least one current party member lacks its discovery. Shared means every current party member has it.
2. A biome needs entering when it has a needed species outside the six boat detections. If only boat-detectable species are missing, entering the biome is unnecessary for this estimate.
3. In skipped biomes, remove all species except boat detections.
4. In visited biomes, include passive species even when already shared, because loading the biome can reveal an incidental Sparkling.
5. Include ordinary manual-check species only when needed by the party.
6. If all three birds are shared, count no birds. If any bird is needed, include **all three** average spawn counts, including already shared birds that can spawn while feeding for the missing one.

With all discoveries shared, no biome requires entry, but the six boat detections still contribute **27.50 checks/run**, giving **74.47 runs per Sparkling**. A party needing only Doomspiral enters Haunted and counts 55.50 checks/run, including incidental passive species and boat detections.

## Cache and network behavior

Read only the currently verified party roster and retained per-member discovery cache. Do not call `withPartyInfo`, a profile loader, a cache initializer, Bazaar refresh, or any HTTP method. If the roster is unknown or any member lacks a baseline, show a local unavailable-data message instead of guessing or starting requests. Accepted no-discovery profiles are valid zero-discovery baselines, as with mode requirements.

Adding/removing members and locally credited catches affect the next estimate through existing cache updates. There is no stored or periodically polled rate result. No party message is sent.

## Verification

Automated coverage checks the full 14.12 baseline, category separation, the six boat exceptions, omission of completed biomes and shared manual sources, grouped bird spawning, unavailable party data and two-decimal formatting. Live checks: run in a party with loaded baselines, hover for the breakdown, change party membership or catch a needed Sparkling and run again, and confirm the command itself adds no fetches.

## Last updated

2026-10-25 — Renamed the local rate command to `/rates`; no new API requests. `/profitablecritters` now ranks ten results by estimated coins per run.

2026-10-21 (America/New_York) — Added cached-party rate estimation, grouped bird rules and six user-reported boat detections, including Shyworm.

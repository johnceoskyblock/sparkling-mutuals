# Achievement calibration and reference audit

The pure `AchievementCalibration` component reproduces the approved workbook baseline. It does not establish a player's probability of receiving a capture, and it does not award achievements. The immutable input is `docs/issues/assets/achievement-rarity/expected-runs-diamond-emblem.xlsx`, SHA-256 `f111ebfb4e88d4f488728b4ea6c6ca4c22d2b7cded357f6f790fb0a7e2863e36`. Source data and cached formulas are recorded in `workbook-extract.json`; no workbook cells were edited or recalculated in Excel.

Reproduce the independent read-only audit from the repository root with:

```text
python docs/issues/assets/achievement-rarity/reproduce-calibration.py
```

It verifies the workbook hash, every extracted exposure against its source cell, and the mapping CSV against the existing 59 achievement definitions, including old goals and tiers. It checks 78 distinct destination IDs: 59 existing rows plus 19 additions. Blank old goal/tier fields are valid only on `NEW` rows. This checks mapping completeness and baseline compatibility; the CSV is the approved editorial decision table, not a claim that every family has an exact probability equivalence. Later definition changes will intentionally require updating that baseline audit.

## Equations and finite horizon

For sparkling chance 1/2048, q=2047/2048. With effective exposure e, hazard is `-e*ln(q)` and the dry tail is `exp(-hazard)`. Zero exposure has zero hazard and a dry probability of one; negative and nonfinite exposures are invalid. For species exposures m_i and nonnegative whole runs n:

- Expected distinct count: `sum_i -expm1(n*m_i*ln(q))`.
- Completion CDF: `product_i (1-q^(n*m_i))`, evaluated by summing logarithms.
- Survival: `-expm1(log(CDF))`, avoiding cancellation in the far tail.
- Mean through horizon H: `sum(n=0..H) survival(n)`.
- Second moment through H: `sum(n=0..H) (2*n+1)*survival(n)`; reported SD is the square root of second moment minus squared finite sum mean.

The horizon is **inclusive**. At H=20,000, mean=4365.831526956 and SD=2152.067616569. These are finite survival-sum statistics, not exact infinite-horizon moments or moments conditional on completion by H. Survival at 20,000 is still 0.000175004650897; the omitted tail is positive. Omitting n=20,000 instead gives mean=4365.831351951 and SD=2152.066345168, which fails the required SD tolerance. Source rate literals retain the extraction's rounded precision; tests allow 1e-6 total exposure and .001 runs for moments.

The first whole-run completion quantiles are:

| Probability | 50% | 75% | 90% | 95% | 99% | 99.9% |
|---|---:|---:|---:|---:|---:|---:|
| Baseline runs | 3839 | 5304 | 7164 | 8559 | 11798 | 16457 |

Expected-count crossings of 20 and 30 occur at 441 and 1056 runs. They are expectations at fixed times, not first-passage means. The total 144.994135774 exposures/run implies 14.124709176 runs per expected sparkling, whereas the geometric mean wait for a run with any sparkling is 14.627161254. Ten baseline runs dry has probability 0.492553555895. One missing species with one opportunity per run remains dry after 250 runs with probability 0.885059747877; a near-complete dex drought is therefore not interchangeable with an ordinary all-species drought.

## Source cells and canonical species

Each rate below is the Sparkling Model row's effective exposure. The actual mixed references are preserved: mostly Lazy Party column C, but B35:B42 in Icy. Those B and C Icy values happen to be equal; changing the references would still erase provenance. Strongarm is six. `Mantis Shrimp` explicitly uses the canonical roster display name (not `MantisShrimp` or the API bestiary key `mantis_shrimp_1`). The Kotlin test compares exactly all 37 roster names.

| Canonical species / source name | Effective exposure | Critters Per Run source |
|---|---:|---|
| Cavernfish | 6.0 | 'Critters Per Run'!C3 |
| Flitter | 7.0 | 'Critters Per Run'!C4 |
| Shyworm | 6.0 | 'Critters Per Run'!C5 |
| Driftling | 4.0 | 'Critters Per Run'!C6 |
| Chuckwalla | 3.0 | 'Critters Per Run'!C7 |
| Rockmite | 4.125 | 'Critters Per Run'!C8 |
| Scrappy | 2.5 | 'Critters Per Run'!C9 |
| Snoozle | 2.5 | 'Critters Per Run'!C10 |
| Gemzie | 3.0 | 'Critters Per Run'!C11 |
| Foxtrot | 7.0 | 'Critters Per Run'!C13 |
| Bluebird | 6.199671201 | 'Critters Per Run'!C14 |
| Honeybug | 4.5 | 'Critters Per Run'!C15 |
| Treefrog | 4.5 | 'Critters Per Run'!C16 |
| Woodchucker | 4.5 | 'Critters Per Run'!C17 |
| Fluffling | 2.0 | 'Critters Per Run'!C18 |
| Hideonfloor | 2.0 | 'Critters Per Run'!C19 |
| Parakeet | 3.153415248 | 'Critters Per Run'!C20 |
| Macaw | 1.293827103 | 'Critters Per Run'!C21 |
| Areita | 4.5 | 'Critters Per Run'!C23 |
| Bloodbat | 4.5 | 'Critters Per Run'!C24 |
| Duplico | 3.0 | 'Critters Per Run'!C25 |
| Gazer | 4.0 | 'Critters Per Run'!C26 |
| Litterbug | 5.0 | 'Critters Per Run'!C27 |
| Solsnatcher | 6.0 | 'Critters Per Run'!C28 |
| Gimmiegold | 4.722222222 | 'Critters Per Run'!C29 |
| Hideonwall | 3.0 | 'Critters Per Run'!C30 |
| Hideyho | 1.0 | 'Critters Per Run'!C31 |
| Doomspiral | 1.0 | 'Critters Per Run'!C32 |
| Strongarm | 6.0 | 'Critters Per Run'!C34 |
| Tepid | 7.0 | 'Critters Per Run'!B35 |
| Polaris | 3.0 | 'Critters Per Run'!B36 |
| Shuddersquid | 4.5 | 'Critters Per Run'!B37 |
| Billygoat | 3.0 | 'Critters Per Run'!B38 |
| Mantis Shrimp | 4.5 | 'Critters Per Run'!B39 |
| Nozzlenose | 3.0 | 'Critters Per Run'!B40 |
| Troodon | 3.0 | 'Critters Per Run'!B41 |
| Wumpa | 1.0 | 'Critters Per Run'!B42 |

Bird rates trace to `CalculationsNotesTests!J21/L21/N21`, averages of three food-specific probabilities in rows 18:20. The Macaw weights include `1.1^2` tracking (M18/M19=1.21, M20=1.815); the workbook notes that tracking is believed to count twice because two Macaws spawn. The effective Macaw rate is twice the expected successful feeds: Lazy `10*N21*2`, Full `L45*N21*2`. Bluebird/Parakeet use `10*J21/L21` in Lazy and `L45*J21/L21` in Full. L45=11.66666667 is nine guaranteed foods plus expected 8/3 random-item foods (Head Start + First Class Safari). These are workbook perk/food assumptions, not measured personal capture rights. Pair arrivals and food variability are collapsed to fractional mean exposure for the journey model; do not use this as an exact within-run Macaw-count distribution.

Rockmite traces through `Critters Per Run!B8/C8` to `CalculationsNotesTests!F14=F13*F11`: assumed 15 mounds times theoretical .275 chance =4.125, rather than replacing it with the observed 4.139130435 over 115 tracking-perk trials. Gimmiegold B29/C29 uses `4.5 + (8 or 2)*L48`; L48=1/9 is the random Shining Coin chance. Its baseline 4.722222222 and Full 5.388888889 reflect different random-item party assumptions.

## Sensitivity and entitlement limits

| Scenario | Exposures/run | Dry after 10 runs | Inclusive mean H=20,000 | SD | Median | 95% |
|---|---:|---:|---:|---:|---:|---:|
| Approved mixed baseline | 144.994135774 | .492553555895 | 4365.831526956 | 2152.067616569 | 3839 | 8559 |
| All Lazy C cells | 144.994135774 | .492553555895 | 4365.831526956 | 2152.067616569 | 3839 | 8559 |
| All Full B cells | 149.435288032 | .481984804627 | 4279.721962056 | 2139.020287398 | 3750 | 8453 |

Fractional spawn means are treated as fixed effective independent opportunities. In reality `E[q^N]` generally differs from `q^E[N]`; spawn variability, shared pairs, missed objectives, perks and correlated events can change tails. Missing objectives contribute zero observed personal exposure. Party spawns, loot shares and a party completion word do not establish any individual player's opportunities. No imported unknown state is treated as zero evidence. These conditional streak-start tails are not lifetime p-values, guarantees or empirical entitlement rates. Exact probabilities are unsupported for speed, profit, clock-time novelty, personal multi-sparkling feats, and mechanical skill comparisons; tiers there remain justified editorial comparisons with scope limitations.

## Reference and mapping audit

SBO reference commit is `218ee282dce677e004bf2e45022af1fa62db5efc`. Droughts use only Inquisitor IDs31/32/33: 250/500/1000 mobs dry at chance 1/50, tiers Common/Rare/Legendary. Their thresholds are `-k*ln(.98)` =5.050676829380, 10.101353658760, 20.202707317519. First whole baseline crossings are 72/143/286 runs. Compare accumulated hazards directly, with the previous run below and the crossing run at or above target. Chimera drought IDs34–37 are a separate family and supply no extra Divine tier here.

Workload anchors IDs57–61 are 10k/25k/50k/100k/150k lifetime Diana kills, Rare/Epic/Legendary/Mythic/Divine. IDs18–22 count burrows within one event and IDs23–27 count event playtime; neither is lifetime work. First Chimera ID12 and first Wool ID83 include own plus loot-share in their trigger, so they cannot support a strictly personal probability analogy. SBO #6 and #7 multi-Inquisitor feats have different event windows and are editorial anchors rather than equal personal capture probabilities.

The maintainer's ticket/contest clarification is comment6195351477 by johnceoskyblock. Both leader/member Manager acknowledgement variants represent one personal admission. Uncommon-or-higher contest rewards have the same base; Two-for-one's assumed 25% bonus gives expected 1.25 tickets per claimed qualifying contest. This is an assumption rather than measured rewards. Mapping ticket goals 10/100/500/1500/4366/8559 correspond to contest goals `ceil(ticketGoal/1.25)` =8/80/400/1200/3493/6848. Spending tickets does not prove complete modeled work or a full dex. The journey-scale goal rounds the finite mean to 4366; 8559 is the baseline 95% quantile, not a second mean.

The CSV retains all existing IDs when the criterion stays the same and marks changed/retired criteria with explicit legacy destinations; added model droughts have new versioned IDs. This numerical component does not migrate history or modify unlock timestamps. The CSV's rationale and scope columns carry the uncertainty; they must remain visible to later implementation and review. The standalone audit validates old-goal/tier correspondence and destination uniqueness without changing the CSV.


## Runtime evidence and preservation

The shipped integration uses the audited definitions, prospective profile snapshots and capped personal observations. Historical source definitions are frozen in `achievement-definitions-v1.kt.txt`; the audit independently validates current Kotlin destinations against the CSV. See [migration, activity coverage and settings](ACHIEVEMENT_REBALANCE.md). Source workbook bytes remain unchanged.

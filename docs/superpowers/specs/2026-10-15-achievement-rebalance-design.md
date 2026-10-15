# Achievement rarity rebalance — design for issue #52

Status: direction approved; written specification awaiting review. No achievement implementation is included in this commit.

## Purpose and boundaries

Make achievement prestige reflect modeled rarity, documented effort and evidence quality. Preserve existing history and distinguish personal captures from party outcomes. Implement on `feature/achievement-rarity-rebalance`, based on verified multi-version commit `eaf6e17807d620af2cb305bcd97f32d991bac6f9`. Keep #54 and #55 on their independent branches. No capture-shot changes, PR, merge, master change or release.

The deliverables are Kotlin implementation, complete per-ID calibration/migration tables, reproducible calculations, regression tests on 26.1.2/26.2/26.3, concise feature documentation and VERSION_NOTES. Existing features stay available. This specification defines evidence and migration contracts; the implementation plan must enumerate the final per-ID mapping before changing definitions.

## Verified inputs

- Immutable workbook: `docs/issues/assets/achievement-rarity/expected-runs-diamond-emblem.xlsx`, SHA-256 `f111ebfb4e88d4f488728b4ea6c6ca4c22d2b7cded357f6f790fb0a7e2863e36`.
- Use all 37 effective-exposure rows in Sparkling Model, including its mixed Lazy/Full Party references. Strongarm is 6. Canonicalize Mantis Shrimp explicitly; retain the source names/cells in calibration data. Trace birds, Rockmite and Gimmiegold through CalculationsNotesTests. Never edit the source workbook.
- Effective total 144.994135774 critters/run; sparkling chance 1/2048. Expected-count reciprocal 14.12470918 differs from mean wait for a run with any sparkling, 14.62716125.
- With q=2047/2048, 10 full modeled runs dry has probability 0.49255356. One missing species spawning once/run stays dry after 250 runs with probability 0.88505975.
- Reproduced inclusive n=0..20,000 survival sums give completion mean 4365.831526956 and SD 2152.067616569. The endpoint matters. Required tolerance: 0.001 run for mean/SD; source-rate rounding is documented. Completion quantiles: 3839/5304/7164/8559/11798/16457 for 50/75/90/95/99/99.9 percent. Expected-count crossings are not first-passage means.
- SBO exact commit `218ee282dce677e004bf2e45022af1fa62db5efc`; inspect definitions AND event/lifetime/own/loot-share triggers. IDs31/32/33: 250/500/1000 mobs dry for Inquisitor, Common/Rare/Legendary. IDs34–37 are a different Chimera family and are not mixed opportunistically into these drought tiers.
- SBO workload IDs57–61: 10k/25k/50k/100k/150k lifetime Diana kills, Rare/Epic/Legendary/Mythic/Divine. IDs18–22 are single-event burrow totals, not lifetime kills. IDs23–27 are event playtime, not lifetime playtime. First Chimera ID12 includes own+loot-share in its actual total; first Wool ID83 likewise includes both, while explicit loot-share awards have separate triggers. Do not label these strictly personal anchors.
- Maintainer clarification: [issue comment 6195351477](https://github.com/johnceoskyblock/sparkling-mutuals/issues/52#issuecomment-6195351477), actual login `johnceoskyblock`. Both Manager acknowledgement variants mark one personal ticket (leader/member variants). Uncommon-or-higher contests have the same base reward. Assume Two-for-one: 25% extra ticket, expected 1.25 tickets/claimed qualifying contest. This is a maintainer assumption, not measured rewards or a guarantee.

## Alternatives and selected approach

1. Retier the existing raw run counters: smaller change, but cannot correct missing-species difficulty or repair unknown historical state. Rejected.
2. Version the evidence and criteria while retaining raw records and legacy awards. Selected: supports conservative modeling, trustworthy units and idempotent migration.
3. Collect a new empirical dataset before any change: improves estimates but unnecessarily postpones corrections already justified by the workbook. Retain explicit limitations and support local evidence export rather than blocking the whole rebalance.

## Calibration rules

Maintain a machine-readable per-achievement audit with ID, old/new goal and tier, condition version, personal/party/event/lifetime scope, exact SBO ID/condition/tier, normalized workload or streak-start dry-tail, rationale and uncertainty. Include every existing ID, removed/replaced IDs and additions. No undocumented numeric weight or points system is introduced.

Use different comparisons for different families. Chance, cumulative work, mechanical skill and novelty are not interchangeable. Preserve a justified exception when there is no defensible exact analog; do not claim universal 1:1 statistical equivalence.

### Droughts

Use SBO Inquisitor IDs31/32/33 consistently. Compare exact dry-tail probabilities, not rounded expected waits:

`targetHazard = -k * ln(1 - 1/50)` for k=250,500,1000.

`baselineRunHazard = -144.994135774 * ln(q)`.

The first whole full-exposure run meeting each threshold is 72,143,286, with Common/Rare/Legendary tiers. Boundaries use accumulated hazard directly, avoiding rounding premature awards. Do not retain the old 10/20/30/40/50 criteria as newly prestigious awards, or extrapolate new Divine droughts from another family.

Ordinary and next-unique droughts remain separate. Both accumulate evidence only for eligible observed personal opportunities. For a run with confirmed personal capture counts c_i, use conservative model exposure `e_i=min(c_i,m_i)` per species; no party loot-share counts, party completion word, bare biome visit or absent source assumption earns personal exposure. Counts must come from complete live capture observations, not estimated imports. This deliberately understates unknown missed opportunities. Display modeled exposure/equivalent runs separately from raw run streak length.

For next-unique, snapshot the known personal missing-species identities before the run/eligible event. Add `-sum(e_i for i in missingAtAttempt)*ln(q)` after a dry eligible run. A skipped biome/species contributes zero. Unknown dex identity or missing event provenance contributes no severity. A newly confirmed own unique resets the unique streak; an API refresh revealing additional discoveries invalidates the unverified continuation and starts a new baseline. Never apply today's dex retroactively. At 37/37, missing-set exposure and next-unique progress stop. Ordinary own-sparkling success resets ordinary hazard; party success does not reset it.

These are conditional modeled streak-start tails, not lifetime p-values or measured personal entitlement rates. Preserve the workbook full-exposure journey as calibration and show sensitivity to Full/Lazy scenarios, partial objectives and personal-vs-party entitlement. Do not silently equate the party spawn model with any one player's capture probability.

### Other families and tier policy

- First/cumulative sparkling, species, uniques and biome sets use their actual ownership and goal semantics. Compare expected exposure/work with the relevant SBO collection/workload anchors, showing limits. Every unchanged condition keeps its ID even when its tier/description changes.
- Multiple sparklings in a party run use the workbook effective-exposure approximation as a stated model, not a measured party rate. Own triples remain a separate personal feat; do not infer a 1/party-size entitlement or advertise a fabricated personal probability.
- Macaw numbers use feeding/pair assumptions from the workbook, not a sparkling probability. Different count and drought families need their own comparisons. Mechanical catching and spawn frequency are distinguished.
- Workload achievements (runs, hours, captures, coins) retain honest lifetime/event scopes. An easier lifetime condition must not be advertised as equivalent to SBO's single-event achievement. Profit remains the mod's existing estimated shard/feather value, not an economy-independent probability.
- Speed and novelty/secrets receive explicit editorial exceptions when no measured analog exists. No probability is invented for Emerald Dye or unsupported skill feats.
- Keep existing rarity enum names for serialization. SPECIAL is a novelty category, not a numeric tier above Divine. Use explicit display rank/title/fanfare properties, replacing ordinal-based prestige behavior. Additional upper tiers are not required merely because SBO has them; introduce none without an independently justified condition. The complete mapping must demonstrate coherent progression within each family.

## Personal ticket activity

Introduce durable, versioned evidence for confirmed personal admissions. Both acknowledged messages are equivalent alternatives, not extra tickets to sum. Collect a pending admission, bind it to a confirmed new Safari run/entry identity and emit exactly one ticket event to RunHistory, SparklingLog and Achievements. Duplicate lines, leader/member variants, listener ordering and ordinary biome changes cannot duplicate it. A genuinely new run with a new acknowledgement may count again, including identical text. An acknowledgement without confirmed entry remains pending/unverified, not an invented completed run. Document this conservative recorded lower bound if failed admission cannot be observed.

Separate ticket types, actual held inventory, tickets earned, actual spent confirmations and run count. Persist observed ticket progress with coverage metadata; missing old history is unknown rather than zero. Historical raw ticket counts remain intact but are not blindly summed into verified spending. Do not combine overlapping imports, API aggregates and live events. A ticket-since-sparkling secret cannot be granted when the relevant interval is unknown.

Add early, intermediate, journey-scale and extended ticket milestones. Calibrate them to the 4365.8315 modeled-run journey and quantiles, stating the confirmed-entry conversion and its coverage. Spending that amount does not establish a full dex. Final thresholds and tiers belong in the complete per-ID audit and must be derived reproducibly before the implementation plan is approved.

## Miria contests

Persist completed qualifying windows once per player/account scope, including across restarts, reconnections, sidebar repeats and imports; revisiting an older window must not increment again. Collection must work with achievement notifications disabled. A tier upgrade in one window is one completion, not another event. Preserve contest completion as its own activity.

Expected ticket comparison is `completed qualifying contests * 1.25`, conditional on claiming and the maintainer's perk assumption. Actual observed earned/spent tickets remain separate. The journey mean is approximately 3492.6652 such contest rewards; a milestone covering that expectation uses ceil=3493, with interpretation clearly stated. Do not claim this many contests guarantees enough tickets or a full dex; other sources, existing tickets and stochastic rewards remain relevant. Do not convert elapsed wall-clock contest availability into active playtime without measurements.

## Data, imports and migration

Version achievement criteria/evidence separately from ordinary history. Before migration, save a recoverable snapshot of the achievements file. Keep raw runs, entries, aggregate counters, unknown IDs and original unlock timestamps. Remove the current `retainAll(current IDs)` deletion behavior.

Unchanged semantic IDs retain their award timestamps. Materially changed drought/ticket conditions receive new versioned IDs; old awards move/copy to a clearly labeled legacy ledger with their original criterion, tier and time. Never silently promote an old low-threshold award into a new prestigious one. Legacy awards are visible separately and excluded from current-criteria totals, progress sorting and current unlock fanfare.

A deterministic migration marker and evidence-event IDs make migration/import replay idempotent. Legacy aggregate drought maxima cannot reconstruct missing-set exposure. Imported historical records without a contemporaneous dex snapshot/verified admission provenance remain useful raw history but earn no fabricated statistical severity or ticket spending. Revalidate only sufficiently evidenced current criteria, quietly, preserving original awards and avoiding duplicate notifications.

Unknown coverage must be displayed as unknown/recorded since the available baseline, not 0 lifetime. Pause or reset a modeled streak when player identity or selected profile scope becomes uncertain; do not combine observations from different profiles. Reuse existing API profile data and identifier plumbing, adding no new network requests solely for achievements.

## Components and UI contracts

- A pure calibration/model component owns rates, hazards, quantiles and deterministic numerical reproduction. No Minecraft or network dependency.
- A pure progression/evidence ledger owns missing-set snapshots, eligible personal exposure, deduplicated admissions/windows and coverage metadata.
- Migration code reads old data and writes versioned current/legacy records through existing atomic HistoryStore saves.
- Achievements remains the event/UI facade; definition scope, typed progress and tier presentation are explicit. Live tracking persists evidence even when announcement settings are off; unlock notifications honor settings.
- History UI shows current criteria, modeled progress units and legacy awards distinctly. Unknown progress has no misleading full/zero bar. Existing search/sort/secret behavior remains coherent; tier ordering and sounds use explicit policy.
- Update existing history/import callbacks to carry evidence while preserving their current raw logs and counters. No automatic upload or remote telemetry. A local export may contain model inputs/outcomes only, if needed for user-directed validation; no credentials.

## Verification and delivery gates

Regression coverage must demonstrate: exact workbook baseline (declared precision and inclusive horizon), quantile/mean distinction, both drought boundaries, 250 runs with one missing single-spawn species remaining ordinary, different missing identities at equal dex counts, changing dex and full dex, skipped objectives, incomplete imports, personal/party separation, admission duplicates/reentry/new runs, listener ordering, persisted contest deduplication, repeated migration/import with timestamp preservation, notification settings, legacy UI and tier policy.

Run the full test suite, build and JAR metadata validation on 26.1.2/26.2/26.3. Review the whole implementation, fix important findings with regression tests, push only the issue branch and verify Actions on its final SHA. Download all three successful Actions artifacts and reply to the still-open source issue/comment with commit, artifacts, concise version notes and live-testing limits. Never claim unperformed Hypixel reward/entitlement tests passed.

## Last updated

2026-10-15 — Drafted after design-direction approval and maintainer ticket/contest clarification. Awaiting written-spec approval before implementation planning.

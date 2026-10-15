# Achievement evidence and migration

The current list has 64 awards. Changed drought/ticket criteria use new versioned IDs; 14 replaced original criteria are archived. Existing unchanged IDs retain their original timestamps. Legacy awards appear under **Show: Legacy**, with their original criterion, rarity and date; they do not count toward current totals or trigger current fanfare. Unknown historical IDs remain recoverable.

## Personal modeled droughts

A known API dex snapshot belongs to the player's selected profile and is attached before a live run. For each species, effective exposure is the smaller of observed personal captures and the workbook baseline. Loot shares, party completion, biome visits and imported estimates add no personal exposure. Partial runs can contribute only captures actually observed. Unique droughts use the species missing before that run; a late snapshot never reconstructs that set.

Own sparkling success resets ordinary drought; a new own unique resets unique drought. Additional API discoveries invalidate the unverified unique continuation. Profile/provider changes, missing identity and disconnects invalidate continuity; the best previously covered interval remains preserved. A full dex has no remaining unique exposure. Displays distinguish modeled full-run equivalents from raw recorded runs and show unknown coverage without a progress bar.

The model uses conditional streak-start dry tails, not lifetime probabilities, guarantees, or measured personal capture rights. The workbook mixes Lazy/Full references and makes perk, bird-pair and effective-exposure assumptions. See [calibration and sensitivity](ACHIEVEMENT_CALIBRATION.md) and the [complete per-ID audit](issues/assets/achievement-rarity/achievement-mapping-v2.csv).

## Tickets, contests and settings

Both acknowledged Safari Manager variants describe one personal ticket. An acknowledgement must be paired with a confirmed run identity; duplicates, listener ordering, biome revisits and mode changes do not multiply it. Unmatched admissions stay unverified. This is a recorded lower bound, separate from held inventory, tickets earned, raw history and completed runs.

A currently observed Uncommon-or-higher Miria tier records one absolute contest window per account, including tier upgrades and reloads. Expected rewards use the maintainer's assumed 25% Two-for-one bonus: **1.25 expected tickets per claimed qualifying contest**. This does not measure actual rewards, claim status, time played or guarantee a complete dex.

Disabling achievements suppresses unlocks and announcements while collecting evidence and event eligibility. Re-enabling catches eligible event awards up quietly; repeated catchup does not change existing timestamps. Turning off raw run history does not turn off modeled evidence collection. Offline/unknown personal-ticket intervals cannot earn the short-interval secret.

## Preservation and imports

Before the first versioned migration, the original achievements file is copied to `sparkling-mutuals-achievements.json.pre-v2.bak`. The original JSON payload, unknown IDs, counters and timestamps remain preserved. Failed parsing/migration protects the source against later empty saves. Normal writes use atomic replacement where supported.

Historical log imports retain their raw history. Without contemporaneous personal dex and provenance, they cannot fabricate drought exposure or verified ticket spending. Replayed imports, run evidence and contest windows are deduplicated. Existing capture totals remain a mixed lower bound: maximum of shared logged captures and own API aggregate, never their sum.

## Sources and validation

- Immutable [workbook](issues/assets/achievement-rarity/expected-runs-diamond-emblem.xlsx): SHA-256 `f111ebfb4e88d4f488728b4ea6c6ca4c22d2b7cded357f6f790fb0a7e2863e36`.
- [Maintainer ticket/contest clarification](https://github.com/johnceoskyblock/sparkling-mutuals/issues/52#issuecomment-6195351477), author `johnceoskyblock`.
- SBO reference commit `218ee282dce677e004bf2e45022af1fa62db5efc`; the calibration document distinguishes lifetime, single-event, own and loot-share triggers.
- Run `python docs/issues/assets/achievement-rarity/reproduce-calibration.py` to verify workbook digest, historical frozen definitions, the complete destination mapping and shipped current definitions. The audit works without Git and does not modify its source workbook.
- Run `./gradlew.bat buildAllTargets testBuildLogic verifyDistributions` for all three supported versions, helper tests and JAR validation.

Local regression/client checks do not test live Hypixel entitlement, actual contest rewards, reward claims or server-side capture outcomes. No telemetry or achievement-specific network requests are added.

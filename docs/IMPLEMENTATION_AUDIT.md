# Implementation documentation audit

## Scope and reviewed revision

Documentation-only audit for [issue #65](https://github.com/johnceoskyblock/sparkling-mutuals/issues/65). The reviewed runtime, tests, build logic and scripts are from **`93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`**. There are **no local runtime/source differences** in this change. Documentation describes the implementation, including its limitations; it does not implement proposed gameplay corrections.

The audit follows owners through event registration/callers, mode/configuration gates, state transitions and display/report consumers. Existing tests are supporting evidence. No new Minecraft/Hypixel tests were performed. Source review establishes what predicates do, not server guarantees. Historical validation is not presented as a new pass.

## Confirmed documentation mismatches

### d1

**Detected Sparkling identities are not completion gates.** Old TRACKING, MISSING_PANEL, species examples and checklist text still required capturing every detected identity. Current `sparklingComplete`, `uniqueComplete` and `completed` in [SafariRun](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt) compose ordinary checks/captures, bird/source exceptions and boss capture requirements without that blanket pending-identity gate. Detection/notification/capture memory still have their own purposes. A new sighting can affect normal Full Clear remainder evidence; identification alone does not impose an extra Sparkling requirement. Current references now distinguish these effects.

### d2

**Implemented counts were sometimes presented as server guarantees.** [SafariRoster](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRoster.kt), [SafariFullClear](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClear.kt) and per-source ledgers encode minima/targets, not proof that every server opportunity was observed. Species pages now label numeric scope and evidence status. The supplied fixed check totals remain distinguishable from this audit's static verification; no new live confirmation is claimed.

### d4

**Shyworm activity and rest countdown are different concepts.** Supplied “eight-cycle” movement notes do not define the implemented rest timer. [ShywormProjection](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/esp/ShywormProjection.kt) uses an 8,000 ms timer for its resting helper. The species reference identifies this as a client helper, not a measured server timing law or a capture hitbox guarantee.

### d5

**Saved settings and presets are not universally independent.** [ConfigManager](../src/main/kotlin/net/johnceo/sparklingmutuals/config/ConfigManager.kt) loads persisted state and applies an ESP preset; [SafariFullClear](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariFullClear.kt) owns mode/panel preset transitions. Old blanket wording that every saved GUI choice remains unchanged was too broad. Configuration documentation now separates independent flags, mode-owned panels and startup ESP enable presets. This is a documentation ambiguity, not a new code fix.

### d6

**Estimated import values can be explicitly repriced.** Old history prose said imported estimates were never repriced. Automatic missing-value filling excludes estimates, but the explicit price action in [HistoryScreen](../src/main/kotlin/net/johnceo/sparklingmutuals/history/ui/HistoryScreen.kt) can reprice them. The history guide separates automatic filling from this explicit UI operation; it does not promise immutable estimates.

### d13

**The older Billygoat two-check Unique proposal is not implemented.** Current [SafariRun.uniqueComplete](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt) follows ordinary first-shared-catch behavior for Billygoat. Its supplied population notes and Sparkling check minimum do not add a second Unique progress gate. The species page and panel guide now describe this distinction; the old proposed requirement is preserved here rather than silently implemented.

### d14

**Party Unique PB proof differs from progress.** [SafariPersonalBests.recordPartyUnique](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariPersonalBests.kt) uses shared catches or `uniqueUnavailable`, rather than `uniqueComplete`. Resolved unseen Rockmite/Snoozle can satisfy that PB path without cd, and a caught source species does not receive progress's extra source gate there. Personal Unique PB instead requires every species' personal capture, with no unseen-species waiver. The mode guide documents these separate consumers without claiming a unified completion rule.

### d17

**Raised population targets have consumer-specific timing.** [SafariRun.observePopulation / updateCaptureEvidence / fullClearComplete / captureCompleteWithZones](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/SafariRun.kt) do not use one universal population target. Local capture/absence proof and personal PB paths retain `knownPopulation`. Report-based progress retains an extra observed target only when that biome has already been reported or the species has a fixed capture target. For a variable species, seven Foxtrots seen before fd can raise local proof to seven, while six subsequent catches plus fd after the remaining entities disappear can satisfy remote progress. This replaces overbroad prose that every positive sighting raises every consumer's target; whether that asymmetry should change is separate implementation work.

## Suspected implementation defects retained without fixes

### d3

**A larger repeated resource notice can affect consumers differently.** In [SafariResources.notice](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariResources.kt), reported coin/food maxima update before the early return for an already resolved/outstanding sender/type. Its outstanding allocation and required population are not increased on that path. A notice of `1x` followed by `2x` from the same sender can therefore increase contributions consumed by the bird/Gimmiegold checking ledgers while leaving this resource obligation at its earlier quantity. Other capture targets may mask the difference. Reproduction should compare resource obligations and species checking separately; this audit changed neither ledger.

**Issue #68 follow-up (runtime `76dd746`):** early/legacy/local quantities now share absolute sender/type maxima, new quantities invalidate dedicated bird/coin proof, and fresh qualified complete F+S coin proof reconciles legacy outstanding records against the verified variable demand. The former fixed-three veto on this Sparkling path is corrected. Generic outstanding allocations for an already outstanding/resolved sender/type still keep their earlier record; do not infer that every separate Full Clear recovery path was rewritten.

### d7

**Unknown imported duration may satisfy speed achievements.** [HistoryBackfill](../src/main/kotlin/net/johnceo/sparklingmutuals/history/importing/HistoryBackfill.kt) stores zero length when an end is unavailable. [Achievements.checkRun](../src/main/kotlin/net/johnceo/sparklingmutuals/history/Achievements.kt) grants speed awards when the party count is sufficient and length is below 180,000/150,000 ms, without a positive-known-duration guard. Rechecking imported records can therefore treat an unknown duration as fast. This is a static risk finding; no new live reproduction or behavior change is claimed.

### d10

**External-prediction final-error naming can overstate coverage.** [analyze.py](../tools/capture_shots/analyze.py) passes only time-matched errors to [models.residual_stats](../tools/capture_shots/models.py), which labels the last element `last_observed_error`. If the final observation has no prediction, that value belongs to the last matched observation, not necessarily the trace's final observation. Read `uncovered_observations` alongside it. This previously deferred minor remains unfixed; it is not endpoint error.

### d11

**Recorder inspection budgeting is not a strict whole-tick 256 bound.** [ShotObservationAdapter.tick](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/ShotObservationAdapter.kt) computes bootstrap allowance as `256 - cache.entries().size`, then samples the post-discovery cache. Newly discovered entries can be inspected again in that tick; additional range-refresh work also exists. The historical claim of at most 256 total inspections is stronger than this composition guarantees (bootstrap plus a newly populated 80-entry cache can reach 336). This previously deferred minor remains recorded without changing code or increasing export caps. Contact-scene budgets are separate.

### d15

**Sparkling boss reward text can bypass boss-PB matchers.** The boss timing feedback/record patterns in [SafariPersonalBests](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SafariPersonalBests.kt) expect `You caught a Doomspiral/Wumpa` or `catching a Doomspiral/Wumpa` immediately. An intervening `SPARKLING` token accepted by the run's general catch parser does not match those paths. A valid run capture can therefore exist without that boss-PB feedback. This is a source-level discrepancy to reproduce separately; the audit does not alter the parser or award records.

## Server and coverage uncertainties

### d8

**Replacement UUIDs are not universally reconciled to one underlying critter.** [SparklingChecks](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/completion/SparklingChecks.kt) retains qualified identities; species/model capture memory and resource/bird ledgers have separate semantics. A server replacing an entity UUID after escape does not prove a larger population, but a broad claim that every retained-check path merges all replacements is unsupported. Current references explain the actual identity mechanisms and retain the need for controlled escape/replacement observations.

### d9

**Full Clear bird capture completion tests known blockers, not complete source knowledge.** [SafariBirdLedger.capturesComplete](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/resources/SafariBirdLedger.kt) can pass from its capture target and absence of known remaining food/held resources. It does not itself require the positive feeding-resolution proof used by other bird consumers. Unknown inventory/floor observations are therefore weaker than confirmed empty observations. The bird guide documents this actual distinction; whether the server supplied every opportunity remains unverified.

**Issue #68 follow-up (runtime `76dd746`):** `SafariRun.birdComplete` now requires complete current starting reports and either known initial floor coverage or a received fd. Wholly unknown coverage with no report no longer passes at the run boundary. The pure capture formula still uses known current blockers and actual captures, rather than requiring the stronger local feeding-resolution/positive-current-empty survey. This finding is narrowed, not a claim that every bird mode now uses the same predicate.

### d12

**Capture projections remain conditional geometry.** The fixed ordinary-capsule spatial curve is not a server-time simulation. Masterful/movement/water previews, launch extrapolation, current-position critter proxies, Shyworm multipart association, mound display surfaces and first-contact ordering are implementation hypotheses. Native rendering checks and narrow historical position residuals do not establish target selection, server hitboxes or guaranteed capture. Optional Iris compatibility was exercised on 26.1.2 with specific shader packs; it does not certify every pack. [The capture guide](CAPTURE_SHOT_RECORDING.md) owns the contracts and limits.

### d16

**The historical custom-data allocation concern remains deferred.** The completed preview plan retained a nonblocking concern about copying item custom data. [CapsulePreview.tick](../src/main/kotlin/net/johnceo/sparklingmutuals/safari/shots/preview/CapsulePreview.kt) still calls `mainHandItem` custom data's `copyTag()` when classifying the held item. This preserves the review item from [the historical preview plan](https://github.com/johnceoskyblock/sparkling-mutuals/blob/93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee/docs/superpowers/plans/2026-10-16-capture-shot-preview.md) and [#51](https://github.com/johnceoskyblock/sparkling-mutuals/issues/51). It is an allocation concern to profile, not a measured gameplay performance defect or authorization to rewrite the item path.

## Consolidation and outstanding work

The eight completed/superseded multi-version and capture-shot plan/specification pages are removed. Their current behavior lives in [multi-version builds](MULTI_VERSION_BUILDS.md), [capture recording/preview](CAPTURE_SHOT_RECORDING.md), [configuration](CONFIG_DEFAULTS.md), the [manual scenarios](IMPLEMENTATION_TEST_CHECKLIST.md), checked schemas and existing source/tests. Historical approvals, execution restrictions and superseded designs remain recoverable in Git history at the reviewed revision; they are not current instructions. The duplicated AI handoff is consolidated into [contributing](../CONTRIBUTING.md) and [source synchronization](SOURCE_SYNC.md).

[Gemzie auto-selection (#64)](https://github.com/johnceoskyblock/sparkling-mutuals/issues/64) is a pending proposal, not implemented by this documentation audit. Its separate design approval is not inferred from #65. Unresolved defects above require their own authorized follow-up; this cleanup does not create runtime fixes or a release. Durable template automation, if desired, is separate work; only temporary read-only checks are used here.

## Validation and maintenance

The PR records the final inventory/disposition map, representative before/after cases, local file/heading checks, the 37-page schema check and documentation-only diff inspection. See [source synchronization](SOURCE_SYNC.md) for the distinction between dates, hashes, reviewed commits and validation. The all-document hash manifest is deliberately not re-recorded to imply the suspected defects were implemented or tested.

## Last updated

2026-10-19 (America/New_York) — Consolidated static documentation discrepancies and retained implementation/server uncertainties at the reviewed revision above. No live validation or gameplay fixes in the original audit. Issue #68 follow-up annotations scope d3/d9 to runtime `76dd746` without redispatching unrelated findings.

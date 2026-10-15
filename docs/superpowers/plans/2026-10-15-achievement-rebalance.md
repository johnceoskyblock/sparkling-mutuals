# Achievement Rarity Rebalance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver issue #52 with calibrated criteria, honest personal activity units and lossless versioned awards.

**Architecture:** Pure calibration and evidence components feed the existing Achievements facade. Dedicated admission/contest evidence is durable and deduplicated; current awards and legacy awards are separated in storage and UI. Existing network calls supply profile scope; no additional calls or telemetry are introduced.

**Tech Stack:** Kotlin, Gson/HistoryStore, JUnit5, Fabric, existing Stonecutter/Gradle three-target build.

**Spec:** `docs/superpowers/specs/2026-10-15-achievement-rebalance-design.md` (user approved 2026-10-15).

## Global Constraints

- Branch `feature/achievement-rarity-rebalance`; base `eaf6e17807d620af2cb305bcd97f32d991bac6f9`. Preserve #54/#55 independent branches; no implicit merge.
- Support exactly 26.1.2,26.2,26.3, Java25 and existing compatibility adapters/dependencies.
- Do not read `.devAuth`, alter master, create a PR/release or implement capture-shot projection.
- Preserve raw history, unknown IDs and original unlock timestamps. Unknown historical state is not zero or modeled evidence.
- Workbook SHA-256 `f111ebfb4e88d4f488728b4ea6c6ca4c22d2b7cded357f6f790fb0a7e2863e36`; no source workbook edits.
- SBO commit `218ee282dce677e004bf2e45022af1fa62db5efc`; ticket/contest clarification source comment6195351477 by johnceoskyblock.
- Notifications obey settings; activity/evidence collection continues with notifications disabled.
- Every code task follows RED→GREEN→whole-suite checks; do not declare Actions success before checking final uploaded SHA.

## Review Focus

- Profile/API refresh races: an old response must not attach the wrong personal dex to a new player/provider/profile (Task2 test).
- Manager acknowledgement before/after entry and duplicate listener order: one confirmed admission, no lost or double ticket (Task3 test).
- Partial biome/source work or no log provenance: contribute only verified personal opportunity counts; no full-party exposure inflation (Task2 test).
- Unknown JSON fields, removed IDs and repeat import/migration: preserve original history/timestamps and avoid notification spam (Task4 test).
- Legacy/unknown/current UI states and settings-off operation: honest labels, sorting and progress without silent data loss (Task5 test).

## File map

Paths below are under `src/main/kotlin/net/johnceo/sparklingmutuals/` unless stated. Tests mirror the package under `src/test/kotlin/`.

- New `history/achievements/AchievementCalibration.kt`: immutable rates, numerical model and tail thresholds.
- New `history/achievements/AchievementEvidence.kt`: scope/run inputs and pure drought/activity state.
- New `history/achievements/AdmissionEvidence.kt`: pending admission/new-entry deduplication.
- New `history/achievements/AchievementMigration.kt`: old/current/legacy conversion only.
- New `history/achievements/AchievementDefinitions.kt`: definitions driven by the reviewed mapping; shrink the existing facade accordingly.
- Modify `history/Achievements.kt`: event integration, persistence, explicit typed progress and tier presentation.
- Modify `history/TicketCounter.kt`, `history/RunHistory.kt`, `history/SparklingLog.kt`, `history/RunRecord.kt`, `safari/SafariRun.kt`, `safari/SafariTracking.kt`: authoritative event identity and conservative live evidence.
- Modify `api/SafariProfile.kt`, `api/HypixelApi.kt`, `history/OwnStats.kt`: carry already-selected profile ID/owner/provider with the existing response.
- Modify `contest/ContestTracker.kt` only if needed to expose the existing completed-window signal independently of announcements. Reuse ContestTicketState semantics; do not count tier upgrades.
- Modify `history/ui/HistoryScreen.kt`: current/legacy view and unknown/model units.
- Calibration/migration documentation: `docs/issues/assets/achievement-rarity/achievement-mapping-v2.csv`, `docs/ACHIEVEMENT_CALIBRATION.md`, concise README and VERSION_NOTES.

## Reviewed definition mapping

The companion CSV `docs/issues/assets/achievement-rarity/achievement-mapping-v2.csv` is normative for every current ID:59 existing IDs plus19 new rows. It records old goal/tier, new ID/goal/tier, scope, SBO anchor and rationale/uncertainty. Validate uniqueness and exact definition coverage with a parser-based test; never a source-text assertion.

Materially replaced families: archive5 old ordinary drought IDs,5 unique drought IDs,3 contest IDs and `instant`. New model families each have three hazard thresholds `-k*ln(.98)`, k250/500/1000, tiers Common/Rare/Legendary. New ticket milestones10/100/500/1500/4366/8559 have Common/Uncommon/Epic/Legendary/Mythic/Divine tiers. Contest goals8/80/400/1200/3493/6848 use the same tiers, as ceil(ticket goal/1.25). `instant_v2` remains at<=5 verified tickets with Common rarity and known interval required. Unchanged conditions retain original IDs and timestamps.

SPECIAL stays a novelty category with explicit display rank80 and normal chime, no prestige title. Common..Divine ranks10..70; Mythic/Divine have fanfare, Legendary/Mythic/Divine prestige title. No additional tiers or numeric points. Within-family ranks must not decrease at harder goals; equal ranks are allowed at the ceiling or where two Macaw capture counts require the same pair spawns.

### Task 1: Reproduce calibration and reference audit

**Files:** Create calibration component and `history/achievements/AchievementCalibrationTest.kt`; complete `docs/ACHIEVEMENT_CALIBRATION.md` and validate mapping CSV.

**Interfaces:** Produces `AchievementCalibration.rates: Map<String,Double>`, `q:Double`, `hazard(exposure:Double):Double`, `dryProbability(exposure:Double):Double`, `expectedDistinct(runs:Int):Double`, `completionProbability(runs:Int):Double`, `completionMoments(horizon:Int):Pair<Double,Double>`, `firstCompletionQuantile(probability:Double):Int`, `droughtThreshold(k:Int):Double`. `droughtThreshold(k)=-k*ln(.98)`; `hazard(e)=-e*ln(q)`.

- [ ] Write numerical tests with independent literals: total144.994135774±1e-6; probability dry10 runs0.4925535559±1e-9; last single-spawn250 dry0.8850597479±1e-9; mean/SD inclusive horizon20000 equal4365.831526956/2152.067616569±.001; all six quantiles exact; E-distinct20 crossing441 and30 crossing1056; 72/143/286 first whole-run drought crossings. Assert rates cover exactly the37 canonical roster species; zero exposure returns zero hazard, and negative/nonfinite exposure is rejected.
- [ ] Run `./gradlew.bat :26.1.2:test --tests '*AchievementCalibrationTest'`; expect failures demonstrating absent numerical implementation, not a broken fixture.
- [ ] Implement the exact signatures with source-cell annotations in rates. Use log/expm1 for stable tails. Keep finite-horizon survival convention inclusive. Document Full/Lazy sensitivity, omitted tail, fractional-exposure assumptions, Macaw pair/feed model and party-vs-personal limits; declare no exact probability for unsupported feats.
- [ ] Run focused test then `./gradlew.bat :26.1.2:test`; expect all tests pass. Confirm workbook SHA unchanged. Commit component/tests/audit/docs.

### Task 2: Conservative scoped drought evidence

**Files:** New evidence component/test; modify SafariProfile/HypixelApi/OwnStats and RunRecord/SafariRun integration fields.

**Interfaces:** Consumes calibration rates/hazard. Produces `AchievementScope(playerUuid:String,profileId:String)` with `key:String` equal normalized playerUUID+":"+profileId; `DexSnapshot(scope:AchievementScope,discoveries:Set<String>,observedAt:Long)`; `AchievementRunEvidence(id:String,scope:AchievementScope,missingBefore:Set<String>?,personalCounts:Map<String,Int>,ownSparklingSpecies:Set<String>,provenanceComplete:Boolean)`; `DroughtEvidence` with current/best ordinary and unique hazards, coverage start and processed run IDs. Methods `recordRun(event:AchievementRunEvidence):Boolean`, `refreshDex(snapshot:DexSnapshot):Unit`, `invalidateScope():Unit`. Return false for duplicate evidence, true for an accepted event. RunRecord adds nullable evidence for backward compatibility, never synthesizes it from old aggregates.

- [ ] Write tests: one missing Wumpa250 fully eligible personal attempts yields hazard250*-ln(q), below Common threshold; one missing Gazer250 attempts with4 personal opportunities/run gives4x severity; a skipped species/biome has no exposure; full dex adds no unique hazard. Unknown missingBefore/incomplete import cannot accrue severity. Own new unique resets unique state, party success does not; same run replay no-ops. API adds a previously unknown discovery -> current unique streak invalidated, current dex used only prospectively. Provider/player/scope response race cannot mutate a different scope.
- [ ] Run `./gradlew.bat :26.1.2:test --tests '*AchievementEvidenceTest'`; expect the specified behavior missing.
- [ ] Implement exposure e_i=min(max(personalCounts_i,0),m_i), excluding unrecognized species and absent provenance. Success runs reset the corresponding current hazard and contribute no ambiguous partial interval. Best hazard records only evidence-backed intervals; do not reconstruct from legacy maxima. Attach a before-run snapshot; a midrun API change invalidates that run's unique input. Carry profileId via existing `getSafariProfile` response; reject stale response with owner/provider request-token checks, no added requests.
- [ ] Run focused and whole first-target suite; expect green, then commit.

### Task 3: Deduplicated admissions and qualifying contest windows

**Files:** New AdmissionEvidence/test; modify TicketCounter, live SafariTracking/RunHistory/SparklingLog/RunRecord event plumbing and evidence ledger.

**Interfaces:** `AdmissionEvidence.acknowledge(text:String,at:Long):Unit`; `confirmEntry(runId:String,scope:AchievementScope,at:Long):Boolean`; durable `ActivityEvidence.recordAdmission(scope:AchievementScope,runId:String,at:Long):Boolean` and `recordContest(playerUuid:String,window:Long,scope:AchievementScope?,at:Long):Boolean`. Stored coverage metadata distinguishes a recorded lower bound from unknown pre-install history. Admission expires after120000ms without confirmation. New runId is a run-owned UUID persisted with its evidence; old records have null.

- [ ] Write tests for each exact Manager variant, repeated/both variants before entry, callback order with acknowledgement just after new entry, same acknowledgement on two distinct new runs, biome revisit/mode change, stale/failed pending admission, repeated entry banner and reconnect/replay. Both downstream histories see one event, never one event each from duplicate callbacks. No banner-only ticket. Old totals unchanged and excluded from verified spending. Known/unknown interval gates `instant_v2`.
- [ ] Write activity tests: one Uncommon→Divine sidebar progression in a window increments once; repeated/older window after save/reload does not increment; a new window does. Achievements disabled still collects; same player switching profile within one window does not double count a personal reward. Expected rewards of4 completions=5 tickets under stated1.25 assumption, never increment actual spent count.
- [ ] Run focused `*AdmissionEvidenceTest` and `*AchievementActivityTest`; expect failures from absent deduplication. Implement pending/bound event state with one fan-out owner, keep TicketCounter.isTicket matcher usable by the existing entry parser. Unknown profile admissions retain raw unscoped evidence and do not award fabricated scoped progress; explicitly show coverage limitations. Contest dedup key is player UUID+absolute window, with per-scope ownership metadata and no retroactive guessed scope.
- [ ] Run focused and whole first-target suite, then commit.

### Task 4: Lossless versioned award migration

**Files:** New migration component/test; modify Achievements.Data/register/afterImport, HistoryStore backup usage; new legacy presentation record.

**Interfaces:** `LegacyAward(id:String,unlockedAt:Long,originalGoal:String,originalTier:String,reason:String)`; `AchievementStateV2(schemaVersion:Int=2,currentUnlocks:MutableMap<String,Long>,legacyAwards:MutableMap<String,LegacyAward>,droughtByScope:MutableMap<String,DroughtEvidence>,activity:ActivityEvidence,originalPayload:com.google.gson.JsonObject)`; `AchievementMigration.migrate(old:Achievements.Data):AchievementStateV2`. Store old fields/raw data intact or as an explicit preserved original snapshot. Unsupported IDs get a legacy entry with recorded original time and unknown original criterion, never deletion.

- [ ] Write tests with old low-threshold droughts, retained unchanged ID, removed emblem ID, unknown custom field and legacy counters. Assert exact timestamps and raw history remain; old droughts do not unlock replacement IDs; applying migration/import twice leaves bytes-equivalent logical state and emits zero unlock notifications. Unknown historical ticket/dex evidence remains unknown. Save/load preserves names of all existing enum tiers.
- [ ] Run `*AchievementMigrationTest` and watch intended failure. Implement one recoverable pre-migration backup before first write, idempotent schema transition, removal of retainAll deletion, and quiet revalidation only for sufficient evidence. Preserve unknown original JSON fields using a retained original payload rather than silently stripping them through Gson round-trip.
- [ ] Run focused and whole first-target suite; expect green. Commit.

### Task 5: Definition rebalance, typed progress and clear UI

**Files:** New definitions component/tests; modify AchievementTier/Measure/Achievements and HistoryScreen; old AchievementApiTest expectations.

**Interfaces:** `AchievementProgress(value:Double?,goal:Double,unit:String,coverage:String,modeled:Boolean)` with null meaning unknown; `Achievements.progressState(achievement:Achievement):AchievementProgress`; compatibility `progress(...)` retained only for known integer historical callers, not used to render unknown/model bars. `Achievements.legacyAwards():List<LegacyAward>`. Explicit tier properties displayRank:Int, fanfare:Boolean, title:Boolean. Definitions follow every normative CSV row and retain secret visibility policy. Extend `Achievement` with optional `modeledHazardGoal:Double?=null`, preserving existing integer `goal:Long` for workload/event callers; modeled progress uses this explicit hazard goal.

- [ ] Write tests for exact mapping coverage/unique IDs, all current definitions represented, archived IDs excluded from new totals, monotone family ranks, no enum ordinal prestige dependency, all modeled threshold boundaries, unknown unit/coverage representation and legacy timestamps. Assert disabled notifications do not stop recording. Historical API totals and shared logs combine by max where they overlap, never sum.
- [ ] Write pure presentation tests for unknown, modeled, current earned and legacy rows: unknown has no misleading zero/full bar; legacy clearly says Legacy and original criteria, separate totals/filter; tier sort uses explicit rank. UI screen uses these tested row models rather than duplicating policy.
- [ ] Run relevant tests RED. Implement definitions from CSV, human-readable descriptions for modeled exposure, verified tickets/recorded lower bounds, expected contest rewards and observed Macaw absence; update affected criteria names coherently. Keep existing first/capture/mechanical rules where mapping preserves semantics. Reuse adapters for all targets.
- [ ] Run focused tests and entire first-target suite, then commit.

### Task 6: End-to-end evidence, documentation and delivery

**Files:** Add integration regressions, concise README/VERSION_NOTES and calibration/migration/source docs. No shared SourceSync baseline recording while unrelated/deferred revisions remain.

**Interfaces:** Consumes complete model/evidence/migration/definition/UI contracts. No new public component.

- [ ] Add a real-component integration sequence: old persisted awards -> migration -> known own dex snapshot -> personal partial run -> duplicate admission -> new own unique -> imported historical data -> API additional discovery -> repeated contest window -> save/reload. Assert all accepted event counts, current/legacy timestamps, resets, no party inflation and no duplicate announcements at every boundary. Include current/new scope races and settings off/on.
- [ ] Run new test RED, implement only missing integration wiring, then run `./gradlew.bat buildAllTargets testBuildLogic verifyDistributions`; expect all targets' suites/build/JAR checks green. Independently inspect suite totals and0errors/failures.
- [ ] Verify source workbook digest unchanged and numerical audit outputs/mapping match shipped definitions. Perform client UI smoke checks when available. Explicitly record unavailable live Hypixel/entitlement/reward checks.
- [ ] Commit final code/docs, request one whole-branch review with exact base/head/spec/plan and Review Focus. Fix important findings with RED→GREEN regressions and the whole suite. Preserve raw source fingerprints and reply IDs in external watch state; monitoring remains paused.
- [ ] Recheck issue52 open and source comment6195351477 author/body. Post implementation-stage reply stating code complete but upload/checks pending. Push only feature/achievement-rarity-rebalance; no PR/merge/release. Check final commit Actions until all required jobs succeed, then download/validate3JARs and post final verified source-linked confirmation on still-open issue. Provide artifact links and concise notes in this chat.

## Plan self-review

All approved spec sections map to Tasks1–6. Shared signatures are listed once and consumed by later tasks; Task2 owns scope/dex/run types, Task3 owns admissions/windows, Task4 owns legacy migration, Task5 owns typed presentation. Five Review Focus cases have explicit tests. Exact per-ID choices are in the78-row companion audit. No architecture/dependency/network expansion or deferred capture-shot work is included.

## Execution handoff

Await user review of this plan and companion mapping. Recommend native execution with one final independent review: six tasks share evidence/persistence interfaces and the plan pins their contracts. The subagent-driven alternative buys a fresh implementer/reviewer for each task, at more context cost.

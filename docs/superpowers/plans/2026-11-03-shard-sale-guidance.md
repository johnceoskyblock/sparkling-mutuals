# Shard Sale Guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Add opt-in, three-hour Safari shard sell/hold guidance to the Hunting Box and local advisories for observed sell offers.

**Architecture:** Pure market parsing, eligibility/replay, target calculation and alert state machines sit under skyblock/market. A single cancellable service owns bounded HTTP/cache work and publishes immutable client snapshots. Native settings, a narrow container-render adapter and passive menu observation integrate with the existing client.

**Tech Stack:** Existing Kotlin 2.4.20 / Java 25 / Fabric APIs, Gson and java.net.http; existing Gradle/Stonecutter three-target build. No new runtime dependencies or personal API credentials.

**Spec:** ./design.txt (direct human approval: Approved, after presentation of the design).

## Global Constraints

- Keep all work local: no pushes, GitHub writes, PRs, master modifications, releases, game-session closures or instance installations.
- Base isolated implementation on verified master 9581012a5b91b92828a1ff7810f9276f2d5bfdf7; recheck current GitHub context and reconcile changes before implementation/final delivery. Preserve the historical revert/restore-78885e0 checkout and all unrelated work. Never read .devAuth.
- Minecraft 26.1.2, 26.2 and 26.3; target-native input/rendering APIs; preserve saved settings and /profit cached-read behavior.
- Default-off Shard sale guidance setting; 37 recognized Safari shards, no Essence/unrelated items, no revival of shelved valuation, no automatic trading or remote personal-data uploads.
- Three-hour advisory horizon; current quotes at most once/minute, stale after three minutes; fine history refresh ten minutes, weekly history hourly, targets every five minutes at most.
- Week context is coarse; never manufacture fine observations. Data/model failure means UNKNOWN/gray and no market-change alerts.
- Model/replay acceptance is exactly the approved design: 20 non-overlapping three-hour windows across four days; most recent full day held out; at least five HOLD decisions in training and holdout separately; >=60% target touches, <=90-minute median successful touch time, <=10% ending >5% below starting ask, on both sets. Report target touches, not fills.
- Do not run DocumentationSync.ps1 -Record to erase unrelated manifest differences. Notes stay local; no release publication.

## Review Focus

1. Historical ask semantics differ from current order-book ask: reject incompatible comparison instead of giving precise-looking wrong advice (Tasks 1–3).
2. Delayed HTTP responses arrive after disable/account change: invalidate their generation before any UI state, file or alert updates (Tasks 4 and 7).
3. Profile changes without a full process restart: invalidate personal-order scope until newly confirmed identity, never use a remembered profile as proof (Tasks 5 and 7).
4. Partially loaded/paginated menus reuse slots: invalidate old per-slot advice immediately and never infer cancellation from an absent order (Tasks 5–6).
5. Historical provider outage or insufficient holdout decisions: gray guidance, explicit reason, no fallback colors or false drop warnings (Tasks 3–4 and 7).

## File Map

Let K=src/main/kotlin/net/johnceo/sparklingmutuals and T=src/test/kotlin/net/johnceo/sparklingmutuals below. Every relative filename is under the isolated repository root, not the old checkout.

Create K/skyblock/market/MarketTypes.kt (immutable shared DTOs), BazaarQuoteParsing.kt (official quote validation), ShardHistoryParsing.kt (provider schema/time bins), ShardTargetModel.kt (pure target/transition), ShardReplay.kt (chronological acceptance), MarketHistoryStore.kt (bounded local cache), ShardMarketService.kt (single HTTP coordinator), HuntingBoxShardParsing.kt (pure menu identity), SellOfferParsing.kt (passive order evidence), SellOfferWatch.kt (scope and notification state), ShardSaleGuidance.kt (client lifecycle/menus/tooltips).
Create K/compat/ContainerOverlayCompat.kt and, only if needed by mapped target APIs, K/mixin/ShardSaleContainerMixin.kt. Keep any mixin narrowly restricted to drawing/slot geometry; it must never consume clicks or modify item stacks.
Modify K/history/BazaarPrices.kt for shared current-price transport; K/config/ConfigManager.kt and SafariSettings.kt for the opt-in setting; K/SparklingMutuals.kt for registration/reset; src/main/resources/sparkling-mutuals.mixins.json only if a mixin is needed. Use existing native string/translation conventions rather than introducing an independent UI framework.
Tests mirror skyblock/market under T; extend existing BazaarPriceCache tests at their actual discovered owner and native settings tests. Add sanitized JSON/text fixtures under src/test/resources/market. Create docs/SHARD_SALE_GUIDANCE.md and update docs/CONFIG_DEFAULTS.md, docs/API_FETCH_AUDIT.md, docs/README.md and docs/IMPLEMENTATION_TEST_CHECKLIST.md. Leave HUNTING_BOX_VALUE.md/ITEM_VALUES.md shelved; add a cross-link only if needed to distinguish this narrow feature.

## Task 1: Establish isolated baseline and verified data contracts

**Files:** local approved spec/plan in docs/superpowers/specs and docs/superpowers/plans after isolation; docs/SHARD_SALE_GUIDANCE.md initial data contract; fixtures src/test/resources/market/provider-contract.json; local evidence outputs outside Git.
**Interfaces:** Define and document MarketScope(accountId: String, profileId: String), MarketQuote(productId: String, ask: Double, bid: Double, serverAt: Long), PriceSample(at: Long, ask: Double, resolutionMillis: Long), AdviceState={SELL,HOLD,UNKNOWN}, UnknownReason and Recommendation(productId,state,target,quoteAt,reason). No user inventory/identifiers enter HTTP requests.

- [ ] Read using-git-worktrees at execution time, use native managed isolation if available, obtain only any consent actually required, verify clean starting status and retain the old checkout. Read applicable instructions and run the read-only documentation check on the isolated base. Current planning inventory has 80 maintained Markdown files and no repository AGENTS; the manifest has historical differences and remains untouched.
- [ ] Read provider API documentation, linked terms/legal and implementation/schema where needed. Verify historical buy versus sell meaning and timezone with concrete source evidence and aligned samples, not field-name guesses. Check custom-range resolution/coverage permits the approved holdout replay. Save only public response fixtures and evidence URLs/times; do not copy personal docs into runtime assets.
- [ ] Inspect representative history availability across 37 IDs with serialized GETs (<=1 request/second, Retry-After honored). Record unsupported IDs and sampling gaps, do not claim whole-catalog support from Hideonwall alone. Determine whether fine-grained seven-day evaluation data can be obtained legitimately; otherwise report this as an unmet validation dependency, not a passing model.
- [ ] Pin the verified provider contract and limits in documentation/fixtures. If terms, timestamps or comparable prices cannot be established, stop dependent automatic advice work and explain the evidence gap. No scraping, paid export bypass or credential use.
- [ ] Commit local baseline/spec/data-contract documentation only when evidence is accurate; no push.

## Task 2: Parse and share fresh current quotes without changing /profit

**Files:** MarketTypes.kt, BazaarQuoteParsing.kt, K/history/BazaarPrices.kt; T/skyblock/market/BazaarQuoteParsingTest.kt; T/history/ProfitTest.kt; T/commands/ProfitableCrittersTest.kt.
**Interfaces:** BazaarQuoteParsing.parse(json: String, receivedAt: Long): QuoteSnapshot(serverAt: Long, quotes: Map<String,MarketQuote>); a shared cancellable Bazaar transport returns both the unchanged quick_status map and validated quote snapshot from one GET. Existing ensureFresh(), cachedPrices, cachedShardPrice() and value() contracts remain compatible. Fast consumer demand is explicit, not triggered by cached access.

- [ ] Write failing tests: unordered buy_summary yields the minimum positive ask; sell_summary yields maximum positive bid; NaN/Infinity/negative/zero, crossed spreads, future timestamps (>60 seconds), success=false and missing schema are rejected; one malformed product does not validate another invalid record.
- [ ] Add cache tests proving concurrent ordinary/fast demand produces one request; /profit alone starts zero requests; ordinary consumer retains three-hour throttle; enabled fast consumer requests no more than once/minute; failed HTTP retains prior numeric cache but marks guidance stale; turning fast demand off restores ordinary behavior.
- [ ] Run ./gradlew.bat :26.1.2:test --tests '*BazaarQuoteParsingTest' --tests '*ProfitTest' --tests '*ProfitableCrittersTest'; confirm expected new assertions fail before implementing. Existing cache tests live in T/history/ProfitTest.kt.
- [ ] Implement parser/shared transport with 20-second request timeout, bounded body (8 MiB maximum) and distinct server timestamps; no request from tooltip/rendering callbacks. Preserve run-history fill and legacy weighted quick_status values for existing consumers.
- [ ] Run the same focused suites and confirm passing; local commit.

## Task 3: Build the pure history/model and chronological eligibility check

**Files:** ShardHistoryParsing.kt, ShardTargetModel.kt, ShardReplay.kt and matching Test.kt files; public sanitized fixtures; tools/shard-market-replay entry point using the same production algorithm (Gradle JVM entry point preferred, no duplicate Python algorithm).
**Interfaces:** ShardHistoryParsing.parse(raw: String, now: Long): List<PriceSample>; normalize(samples, now): HistorySnapshot; ShardTargetModel.target(history: HistorySnapshot, now: Long): TargetResult; ShardTargetModel.observe(previous: AdviceLatch?, quote: MarketQuote?, target: TargetResult, now: Long): AdviceLatch; ShardReplay.evaluate(samples: List<PriceSample>, splitAt: Long): EligibilityReport. EligibilityReport records input identity, model version, training/holdout metrics and pass/failure reasons; Recommendation requires passing current-version eligibility.

- [ ] Write failing history tests for provider timezone contract, newest-first data, duplicates, missing intervals, future values and coarse-versus-fine bins. Fine history coverage: >=80% closed five-minute bins in each recent three-hour and 24-hour window, latest required sample <=15 minutes old; week coverage >=80% of expected two-hour bins across 168 hours. Missing bins are not forward-filled. Validate evidence-based thresholds before relaxing anything; report if the provider cannot support them.
- [ ] Write failing model tests pinning 60/30/10 weighting of three-hour/day/week 60th percentiles; clamp to six-hour 10th–90th percentile; three descending closed 15-minute medians totaling >=1% activate falling-exit guidance toward the latest 30-minute median; a sustained new level survives outlier handling. Quantiles use time bins at their real resolutions. Fixed constants travel in a versioned ModelParameters value.
- [ ] Add transition tests: enter SELL at >=99.5% target; leave only below98.5% with two distinct server snapshots >=60 seconds apart; repeated snapshots never confirm; UNKNOWN immediately clears pending confirmation; data recovery establishes baseline without a fabricated SELL→HOLD event.
- [ ] Add replay tests proving no future sample affects a prior decision, coarse candles cannot establish fine touches, windows do not overlap, target is frozen per replay decision, last full UTC day is held out, fewer than20 windows/four days or fewer thanfive HOLD decisions in either set is inconclusive. Boundary tests pin 60%,90 minutes,10%,5%; poor or inconclusive => no colors and no alerts. Compare against list-now and recent-only baselines; do not call ask touches fills or report trade volume not present in the source.
- [ ] Run ./gradlew.bat :26.1.2:test --tests '*ShardHistoryParsingTest' --tests '*ShardTargetModelTest' --tests '*ShardReplayTest'; verify failures, implement the pure functions, rerun to pass.
- [ ] Run the shared replay entry point on evidence-qualified historical products and save a local result report with coverage/failures. Select parameters using training only; retain the approved acceptance criteria. If none qualify, record that the requested automatic guidance is not yet deliverable; do not call a gray-only implementation complete. Local commit after accurately documenting results.

## Task 4: Implement bounded history storage and one asynchronous service

**Files:** MarketHistoryStore.kt, ShardMarketService.kt and matching tests; shared transport from Task2.
**Interfaces:** ShardMarketService.updateDemand(enabled: Boolean, inSkyblock: Boolean, products: Set<String>, generation: Long); snapshot(now: Long): Map<String,Recommendation>; stop(generation: Long). MarketHistoryStore.read/write accepts a schema/model version and verified product IDs, never account/order content in historical-price files. Inject clock, HTTP transport, executor and store for deterministic tests.

- [ ] Write failing tests for one outstanding historical request; queue deduplication, serial <=1 request/second, 429 Retry-After, bounded exponential retries (15 seconds to15 minutes), ten-minute/day and hourly/week refresh, stop/cancel, reconnect and late-generation callbacks. Include oversized/truncated responses, malformed disk data and concurrent refresh publication.
- [ ] Pin limits: 37 products; cache <=2,304 five-minute bins/product (eight days, seven active plus validation margin) and <=96 coarse bins/product; queued jobs <=74; historical response <=2 MiB; persistent history <=16 MiB; atomic replace and reject oversized files before parsing. Evict/prune on read/write. Warm recent-bin collection is explicitly incomplete until required coverage exists.
- [ ] Verify failing tests with ./gradlew.bat :26.1.2:test --tests '*MarketHistoryStoreTest' --tests '*ShardMarketServiceTest'; implement immutable snapshots and cancellation/generation checks, not new threads per screen. Enforce three-minute quote staleness and coverage/eligibility gates on each snapshot read.
- [ ] Rerun focused tests, including disable+late-response producing zero publication/writes/alerts; local commit.

## Task 5: Identify genuine Hunting Box shards and personal order evidence

**Files:** HuntingBoxShardParsing.kt, SellOfferParsing.kt, SellOfferWatch.kt and matching tests; extend public/sanitized menu fixtures with provenance.
**Interfaces:** HuntingBoxShardParsing.parse(title: String, slots: List<MenuSlot>): List<BoxShard>; SellOfferParsing.parse(title: String, slots: List<MenuSlot>): List<OfferEvidence>; SellOfferWatch.selectScope(scope: MarketScope?); observe(evidence: List<OfferEvidence>, now: Long); transition(productId: String, from: AdviceState, to: AdviceState, now: Long): List<SaleAlert>. MenuSlot includes slot index, server item ID, display name and stripped lore. OfferEvidence distinguishes observed active sell offer, detail/update and explicit completion/cancellation; unknown is not cancellation.

- [ ] Read shelved HuntingBoxLedger parser and current active item-ID utilities; use the narrow validated identity rules, not archived valuation. Add failing tests for all accepted title variants, border/controls/player inventory exclusions, exactly recognized SHARD IDs/legacy attribute shards, Owned lore and loading/page changes. Unknown/custom-name-only items never become Safari products.
- [ ] Establish Manage Orders/detail parser from concrete current public source examples or a user-supplied sanitized in-game sample; do not invent server wording. Fixtures must distinguish sell offers from buy orders, unit price from totals and partial fills from completion. If sufficient evidence is unavailable, keep that parser unknown and report the live evidence needed; implement the pure tracker independently.
- [ ] Add tests for partial pagination, duplicate same-product offers, same-price different orders, missing IDs, absent/unloaded slots, partial fills and explicit cancellation. Aggregate product watches when unique order identity is unavailable; never remove all offers based on one ambiguous product event.
- [ ] Add profile tests: only confirmed exact Profile ID server evidence and local player UUID establish scope; user/party quoted lines do not; restart, account change, profile switch and uncertain scope invalidate current validation. Never borrow HoneyHives' permissive remembered scope.
- [ ] Add alert tests: one grouped SELL→confirmed HOLD advisory; initial HOLD separately labeled once; UNKNOWN/recovery no synthetic transition; persisted watches require revalidation; offscreen text says last observed and includes age. Default per-product cooldown ten minutes; after a confirmed SELL return, the next HOLD may alert only after cooldown. Scope and re-alert latches persist locally, capped to four most-recent account/profile scopes,37 products each,1 MiB/30days and validated before use.
- [ ] Run ./gradlew.bat :26.1.2:test --tests '*HuntingBoxShardParsingTest' --tests '*SellOfferParsingTest' --tests '*SellOfferWatchTest'; verify initial failures, implement state/parsers and persistence with injectedclock/store, rerun to pass; local commit.

## Task 6: Add native settings and menu-local visual guidance

**Files:** K/config/ConfigManager.kt, SafariSettings.kt; ShardSaleGuidance.kt; ContainerOverlayCompat.kt; narrow optional mixin/resource registration; native settings tests and T/skyblock/market/ShardSalePresentationTest.kt.
**Interfaces:** one persisted boolean shardSaleGuidance=false under SkyBlock / Helpers. Immutable SaleMenuSnapshot(menuIdentity, generation, slots: Map<Int,Recommendation>) owned by client integration. ContainerOverlayCompat draws eligible slot outlines from an immutable plan using the correct target API. Tooltip projection consumes the same per-slot validated menu snapshot; it must not decorate an unrelated identical stack outside the eligible container slot.

- [ ] Write failing presentation/settings tests: saved false/true roundtrip, default false, searchable native catalog entry, apply/reload behavior, old hidden valuation settings remain hidden; green/red/gray map to text states; pending new page invalidates prior slot colors immediately.
- [ ] Use current mappings/Fabric screen events to choose the minimal draw adapter. If a mixin is required, inspect each target's mapped signature before registration and test generated target hooks. Do not hardcode GLFW input values, consume events or mutate stacks. Outline/overlay must preserve item readability and tooltip z-order.
- [ ] Implement once-registered callbacks, client-tick parsing of changed menu fingerprints and tooltip-local text: Sell offer / Hold / No reliable advice, current ask, target, three-hour horizon, reason/source age. Include unverified eligibility/data reason and no new standalone HUD. Render callbacks only read immutable plans; no scanning, parsing, HTTP or file I/O there.
- [ ] Run ./gradlew.bat :26.1.2:test --tests '*ShardSalePresentationTest' --tests '*NativeSettingsTest' --tests '*ConfigTest'; verify passing after initial red tests; compile each target's adapter, local commit.

## Task 7: Wire lifecycle and local notifications end to end

**Files:** ShardSaleGuidance.kt; K/SparklingMutuals.kt registration/reset; integration tests T/skyblock/market/ShardSaleGuidanceIntegrationTest.kt.
**Interfaces:** ShardSaleGuidance.register(), tick(client), resetSession(). Client snapshots go to the market service; confirmed product transitions go to SellOfferWatch; eligible SaleAlerts go to existing local chat formatting on the client thread only.

- [ ] Write failing integration tests using fake clock/service/menu/identity/chat sinks: open Hunting Box => visible IDs demand; observe Manage Orders => tracked demand; render/hover => zero HTTP; close menu => bounded demand retained only for known watches; disable/disconnect => no background refresh, clear menus/advice and pending confirmations.
- [ ] Cover account/profile change during in-flight response, world hop missing player, server profile confirmation arriving late, local cached order after restart and stale current price while history succeeds. No accidental message, file update or scope migration is allowed.
- [ ] Confirm one registration across multiple menu openings, no growth of callbacks/watch lists, generated lifecycle cleanup on all targets, and /profit ranking/cache regression unchanged. Wire it to appropriate existing client tick and disconnect paths; do not make HoneyHives setting a prerequisite.
- [ ] Run ./gradlew.bat :26.1.2:test --tests '*ShardSaleGuidanceIntegrationTest' --tests '*ProfitableCrittersTest'; pass focused suite and local commit.

## Task 8: Document, build, independently review and deliver locally

**Files:** docs listed in FileMap; local replay/data-contract/build/native-test receipts; source fixes only if found in verification/review.
**Interfaces:** local final report distinguishes source correctness, model eligibility, cross-version build, offline native checks and live Hypixel evidence. No GitHub Actions run is created because pushing is forbidden.

- [ ] Update feature docs/defaults/API audit/manual checklist and Last updated using America/New_York date. Include opt-in use, public sources, request cadence, limits, historical cache, passive orders revalidation, target-touch caveat and unsupported/unvalidated cases. Recheck latest GitHub/master/maintained docs without altering master or unrelated work.
- [ ] Run ./gradlew.bat clean buildAllTargets with Java25, inspect actual per-target and helper test reports, verify exactly three JARs and build-manifest sizes/SHA256 using existing verification tasks. No version bump/release requested.
- [ ] Run applicable native offline menus on each available target, with controlled eligible and UNKNOWN fixtures, loading/page changes, tooltip and disconnect/disable transitions. Use smoke mode without DevAuth; no interaction with live user game or trading. Record exact untested Hypixel menu/lifecycle and provider-coverage paths.
- [ ] Invoke requesting-code-review for one fresh whole-branch review using the chosen Native workflow, or follow task-by-task review if Subagent-driven was selected. Correct material findings with regression tests and repeat only affected checks plus required final build if source changes.
- [ ] Re-run read-only DocumentationSync.ps1 and reconcile new docs; do not Record while unrelated changes remain. Confirm old checkout preserved, local branch/status sensible, no secret/raw gameplay files in patch and no pushes made.
- [ ] Deliver concise local source/build links, tested versions/counts, replay eligibility by product and limitations. If none of the automatic models qualify, clearly say the requested feature is not yet fully delivered and identify the remaining evidence/model constraint rather than claiming completion. Installing is a separate user action.

## Self-review receipt

Reviewed all approved design sections against these eight tasks. Provider comparability/legitimacy and sufficient fine history are explicit prerequisites, not assumptions. Types are introduced before consumers. Every Review Focus item has owning tests. Passive order evidence, profile scope, rendering locality, quotas, stale handling, model gate, all three builds and documentation each have a task. No product code is included in this plan. Routine implementation choices are left to the builder; changing the approved acceptance contract comes back for review.

## Execution handoff

Recommended Native: implement the tasks sequentially in this session, with one fresh whole-branch reviewer at the end. Shared transport/model/menu interfaces make sequential execution straightforward and avoid repeated contexts. Alternative Subagent-driven: a fresh implementer/reviewer per task, followed by whole-branch review. Human review of this plan and execution choice are pending; no implementation has started.

AMENDMENT APPROVED 2026-11-03: Direct human instruction: Do this on 26.1.2 only and the plan is approved. Supersedes all three-target build/test/delivery requirements for this increment. Use only target-qualified 26.1.2 test, jar and verifyTargetJar tasks. Do not generate 26.2 or 26.3 artifacts or change the repository target matrix. Native execution selected as recommended. All changes remain local.

AMENDMENT APPROVED 2026-11-03: Human chose Use three validation days (Recommended), answering call_c693f07caeba4fd29c7b5ef74b1fee1c. Final chronological validation covers the most recent THREE complete UTC days instead of one. The 3h advice horizon, minimum5HOLD decisions in each training/validation set,60%touch,90minmedianwait,10%>5%end-drop limit and source/evidence qualification remain unchanged. Do not treat an incomplete three-day period as complete. This supersedes all prior one-day holdout wording. User separately chose continued automatic model research; no GoogleDoc-target fallback is authorized.

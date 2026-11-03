# Hunting Box total Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans for the preserved Native execution method. Track the following steps with the per-plan ledger.

**Goal:** Show one cumulative sell-offer valuation above the Hunting Box, without per-shard valuation rows.

**Architecture:** Keep a separate bounded ownership ledger for all recognized shards; Safari advice retains its narrow allowlist. Reuse the existing shared Bazaar snapshot, lifecycle/profile evidence, worker and safe persistence primitives. Render a cached total through the existing tooltip-layer boundary using uniquely named native screen accessors.

**Tech Stack:** Kotlin, Fabric, JUnit 5, Gradle, Minecraft 26.1.2.

**Spec:** docs/superpowers/specs/2026-11-03-hunting-box-total-design.md (approved).

## Global Constraints

- Minecraft 26.1.2 only; local worktree/build/commits. No GitHub writes, instance replacement or automatic menu navigation.
- Sell-offer basis: lowest valid competing ask in the shared book, estimate before fees.
- One total above inventory, no shard-value list or generic hovered-item valuation.
- No per-shard price calls or history downloads for totals; retain shared one-minute minimum and three-minute quote freshness.
- All recognized Hunting Box shards; do not expand Safari guidance or change top-ten advice/alerts.
- Memory/save limits: four verified account/profile scopes,512 products per scope, product IDs at most96 characters,512 KiB final plus512 KiB atomic stage. No saved prices or saved identity claims.
- Unknown profile observations remain connection-local. Disable/profile/connection changes clear visible state and current scan coverage.
- Coverage describes the current opening/view only; remembered ownership may remain. Missing/filtered/unvisited entries never establish zero. Counts reset only by explicit zero or verified profile separation.

## Review Focus

- Generic shard names must not resolve unrelated menu controls or conflicting identities.
- Page/filter transitions cannot double-count shards or imply complete coverage.
- Late disk restore must not overwrite newer live quantities or explicit deletions.
- Shared prices must not be fetched from rendering or restored as fresh from disk.
- Header must coexist with NoFrills, top-ten panel, tooltip layering, resize and narrow GUI scales.

### Task 1: General shard observations, bounded ownership and valuation

**Files:** create market/WholeBoxParsing.kt, market/WholeBoxLedger.kt, market/WholeBoxTotals.kt and corresponding tests. All paths under src/main/kotlin/net/johnceo/sparklingmutuals/skyblock/ and src/test/kotlin/net/johnceo/sparklingmutuals/skyblock/.

**Interfaces:**
- WholeBoxParsing.observe(title:String,slots:List<MenuSlot>,catalog:Set<String>): WholeBoxPage? returns distinct validated product/count observations, page index/count when reliable, view fingerprint/filter state and unsupported-shard count.
- WholeBoxLedger.select(scope:MarketScope?,connection:Any?), beginOpening(), observe(page:WholeBoxPage,now:Long), rows(), coverage(), export():String and restore(raw:String,now:Long). Quantities replace, never add; touched scopes defeat late restore.
- WholeBoxTotals.calculate(rows:List<WholeBoxOwned>,quotes:QuoteSnapshot?,now:Long): WholeBoxValue returns known-value sum, missing-price count and quote timestamp. Unknown or stale prices produce unavailable/partial states.

- [ ] Write parser tests for native IDs, short/long labels backed by the shared all-product catalog, non-Safari shards, exact Owned lore/footer, invalid/duplicate slots, controls and conflicting identity. Add ledger tests for repeated/overlapping pages, zeros, unknown/filtered coverage, profile/connection changes and bounded/late restore. Add totals tests for sell-offer asks, mixed prices, no usable prices, stale quotes and unsafe arithmetic.
- [ ] Run the focused new tests; expected RED due to missing implementation. Make the APIs compile minimally if needed, then observe behavior assertions fail before implementing their logic.
- [ ] Implement exact box/inner-slot validation. Native SHARD_ IDs use strict grammar; generic labels require shard-footer evidence and an unambiguous exact match against the existing all-product catalogue, with no invented aliases. Include unpriced recognized products as missing. Track unresolved shard evidence as incomplete coverage.
- [ ] Implement the bounded per-profile ledger, opening/view coverage and replacement semantics; pure valuation reads valid shared asks only. Reuse proven profile validation. Keep overflow/nonfinite data out of a complete-looking total.
- [ ] Run all 26.1.2 tests; expected GREEN. Commit locally with the task's source/tests.

### Task 2: Independent lifecycle, persistence and compact native total

**Files:** create market/WholeBoxTotal.kt, market/WholeBoxTotalStore.kt and tests; modify market/ShardSaleGuidance.kt, mixin/ShardSaleScreenBounds.kt, mixin/ShardSaleContainerMixin.kt, config/SafariSettings.kt; update docs/HUNTING_BOX_VALUE.md, docs/NATIVE_UI.md and docs/README.md where needed.

**Interfaces:**
- WholeBoxTotal wraps Task1's ledger and exposes tick(enabled,inSkyblock,scope,connection,title,slots,quotes,now), restore(raw,now), export() and cached summary/render inputs.
- WholeBoxTotalStore uses BoundedAtomicCacheFile plus bounded reads, sharing ShardSaleGuidance's worker and WatchSaveQueue.
- Existing ConfigManager.huntingBoxValue saved choice controls valuation. Annotate the existing hidden SafariSettings field as Hunting Box total value; retain its default true and saved preference.
- Add uniquely named top-position accessor to ShardSaleScreenBounds. Keep hoveredSlot field-only shadow and existing tooltip hook compatibility.

- [ ] Add failing tests for independent guidance/total toggles, cold/late loading, profile uncertainty, stale read-time removal, no total-only history demand, store cap/write recovery and header bounds/formatting. Expected RED assertions before production integration.
- [ ] Integrate a separate total ledger into the existing identity/lifecycle owner. Parse box slots when either feature needs them; total-only demand may call the shared quote refresh but never market-history demand. Load/save bounded totals on the existing worker, preserving latest-live-over-restore semantics. Hide immediately on disable or leaving the box.
- [ ] Render a compact estimated sell-offer total and small coverage/older/missing note above actual inventory bounds, before vanilla tooltips. Use known subtotal wording when prices are missing. Unknown prices or an incomplete scan must never be labeled as a complete box value. If there is insufficient readable space, hide the header rather than covering slots. No per-shard valuation lines or tooltip additions.
- [ ] Update current feature documentation, keeping general item/chest valuation shelved. Run read-only documentation synchronization; do not Record over unrelated baseline differences.
- [ ] Run ./gradlew.bat --offline -I ../../outputs/shard-sale-guidance-20261103/local-test-temp.gradle :26.1.2:test :26.1.2:verifyTargetJar with the existing JDK25. Expected: full suite and JAR verification GREEN.
- [ ] Extend the isolated native fixture with a supported non-Safari shard and two-page counts; verify one combined total above the inventory, independent toggle, stale/partial notes, resize/space constraints, unchanged top-ten guidance, NoFrills/owo compatibility and tooltip layering. Inspect screenshots and report assertions.
- [ ] Commit locally; complete the execution ledger and obtain one fresh whole-increment review. Fix Critical/Important findings in one TDD pass. Verify packaged JAR metadata, all compiled classes, hashes and test/native receipts. Deliver only the local26.1.2 build; no live-instance change.

## Research and self-review

GitHub context checked: current master docs/HUNTING_BOX_VALUE.md and tools/shelved/item-values/README.md; local current runtime/lifecycle/settings/cache code reconciled. SkyHanni primary sources inspected at a168f0089f0e8aec70539683f897744a22fef716: features/inventory/attribute/HuntingBoxValue.kt and AttributeShardsData.kt. Their value display resets totals per processed inventory; separate ownership storage tracks seen pages. This plan implements the user's single cumulative total independently.

Spec coverage: all behavior maps to Task1 model/persistence or Task2 lifecycle/UI. The two tasks share only the declared ledger/parser/value interfaces. Existing Safari advice APIs, alert state and price transport remain unchanged. Native execution stays selected; plan confirmation precedes implementation.

# Faster shard guidance display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Update box decisions on the first valid changed quote while retaining confirmed personal alerts.

**Architecture:** Add a small display-only reducer to SaleGuidanceSession; leave ShardTargetModel and SellOfferWatch confirmation contracts unchanged. Publish shared quotes before pending history parsing.

**Tech Stack:** Kotlin, JUnit 5, Fabric, Gradle.

**Spec:** docs/superpowers/specs/2026-11-03-shard-fast-display-design.md

## Global Constraints

- Minecraft 26.1.2 only; Native execution; local commits/builds only.
- No GitHub writes, instance replacement, polling/rate-limit or target-math changes.
- Confirmed alerts retain distinct quotes separated by sixty seconds.
- Freshness and unknown data suppress display colors; profile/connection/disable resets erase display state.

## Review Focus

- Display/alert divergence must not create early or duplicate chat alerts.
- Hysteresis after immediate HOLD, same-quote reads and older quotes must remain stable.
- Unknown data and changed models must not carry display baselines across contexts.
- Interim publication must not revive retired demand or use stale history.
- Cold-history loading remains serialized and honestly unavailable.

### Task 1: Faster display with confirmed alerts and quote-first publication

**Files:** market/ShardDisplayAdvice.kt (new), market/SaleGuidanceSession.kt, market/ShardMarketService.kt; associated tests; docs/SHARD_SALE_GUIDANCE.md.

**Interfaces:** session receives confirmed Recommendation maps; only its UI outputs are transformed. Service snapshot remains confirmed.

- [ ] Add session regressions for immediate HOLD with delayed actual-model warning, hysteresis, resets, unknown/model changes and old snapshots; add a ten-product blocked-history publication test.
- [ ] Run focused tests and observe failures caused by current delayed display and history-first processing.
  Expected: intended assertion failures, no compilation errors.
- [ ] Implement the bounded display-only state and publish-before-pending-history ordering; update concise feature documentation.
- [ ] Run full 26.1.2 tests and verifyTargetJar.
  Expected: all tests and artifact checks pass.
- [ ] Run isolated native client with actual NoFrills/owo fixtures, verify display divergence and UI rendering. Package and verify the JAR.
- [ ] Commit locally, complete ledger and request one fresh whole-increment review. Address Important findings in one TDD pass; deliver verified local build.

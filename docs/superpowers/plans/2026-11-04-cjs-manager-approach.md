# CJ Manager Approach Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Correct the .3 arrival classification and deliver a bounded, opt-in one-block Manager approach on 26.1.2.

**Architecture:** Separate CJ personal-admission state from broad Safari presence. A pure one-use approach policy consumes current evidence and geometry; a native adapter owns/relinquishes normal walking keys at START_CLIENT_TICK. The existing session retains protocol/capture freshness authority.

**Tech Stack:** Kotlin, Fabric 26.1.2, JUnit5, local Gradle/JDK25.

**Spec:** `docs/superpowers/specs/2026-11-04-cjs-manager-approach-design.md`

## Global Constraints

- Local only; 26.1.2 only; keep normal instances separate.
- No live Manager/ticket clicks in this increment; no auto-install or Discord resend.
- One block total approach budget; ordinary forward+sneak, no jump/sprint/teleport.
- Preserve fresh-report and cancellation contracts and warp configuration.

## Review Focus

- Broad island labels before admission must allow a query; exact current-world personal evidence ends it.
- Key ownership must release on every exit, including errors/world/disconnect/focus/menu changes.
- A context/target/input change must never replenish an approach budget.
- Swept path/floor, physical bindings, camera takeover and unusual speed must conservatively cancel.
- Freshness and capture withdrawal must be rechecked before each input tick.

### Task 1: CJ classification and fresh approach evidence

**Files:** Add `CjsAdmissionState.kt`/test; modify `CjsSafariEntry.kt`, `EntryDryRunSession.kt`/test.

**Interfaces:** `CjsAdmissionState.world(Any?)`, `chat(String)`, `admitted`; `EntryDryRunSession.fresh(frame, now): EntrySighting?` returns only a current noncancelled at-entry observation. Native frame uses mapped0 plus lack of personal admission, independent of generic inside label.

- [x] Write/run RED tests for current-world admission and a real session query/reply before the Manager acceptance.
- [x] Implement those contracts, retaining all one-query/freshness rules.
- [x] Run targeted tests; inspect outputs and commit.

### Task 2: One-use bounded approach and native ownership

**Files:** Add `CjsApproach.kt`, `CjsApproachInput.kt`/tests; modify startup/reset/config/settings; update concise guides and version .4.

**Interfaces:** Pure `CjsApproach.tick(frame, now): CjsApproachAction`; immutable context, current sighting/target, position, safety/user input flags. Actions contain only walk/stop and yaw/pitch. Native adapter finds current manager, computes swept safety, applies keys at start tick and releases on all exits.

- [x] Write/run RED tests for movement toward target, 0.85-stop/one-block budget, stale/captured/context/target loss, timeout/stall/rollback, manual/screen/focus interruption and no rearm.
- [x] Implement pure/native policy and config persistence, and verify native API signatures by compiling.
- [x] Run full `:26.1.2:test :26.1.2:build` with the existing external temp-root init script.
- [x] Obtain one fresh whole-change review, fix Important findings with RED/GREEN regressions, rerun full suite, commit and package .4 locally.

## Execution record

Base: 5f7e47688f193d69b0a9f7bc90ccd52b89dd140d. Direct movement approval in this chat applies; no repeat approval needed for its bounded implementation. Earlier Native execution is retained. The off-by-default approach toggle is an implementer choice to keep diagnostic opt-in distinct from gameplay input.

Task 2 ruling: require an anchored server party-warp notice, the next world and the uniquely mapped current leader profile before native movement. Cost: missing evidence conservatively prevents movement. Full pre-review suite: 1412/1412, build succeeded. Independent review found four Important issues; all corrected with RED/GREEN regressions and final 1419/1419 suite. Native ToggleKeyMapping and ordinary-ground recurrence are exercised; live movement remains unverified.

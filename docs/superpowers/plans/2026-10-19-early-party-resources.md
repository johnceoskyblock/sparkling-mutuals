# Early party resources implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Native execution, with one whole-branch review at the end. The issue authorizes the complete workflow; no routine approval/handoff stop is required.

**Goal:** Prevent premature Forest/Haunted checks by sharing original starting resources at HOTSPOT+2s and keeping later reminders local.

**Architecture:** A common run-owned absolute contribution ledger feeds existing bird and Gimmiegold checkers. A verified party inbox and retained source catalog provide sender/run/coverage context; presentation never creates demand.

**Tech Stack:** Kotlin/JVM 25, Fabric/HMAPI, JUnit 5, existing Gradle target matrix.

**Spec:** `docs/superpowers/specs/2026-10-19-early-party-resources-design.md`

## Global Constraints

- Branch `fix/early-party-resource-accounting` from bb32138738b80706f5ed47448358a2289da096a9; preserve independent branches.
- HOTSPOT +2,000 ms, once per run. No arrival/area/HUD fallback.
- No release, instance installation, game termination or automatic monitor resumption.
- Actual roster UUIDs; current world/run/marker context; unknown is not zero.
- F is observed distinct coin groups; C+U uses real catches/current uncaptured models. Preserve Macaw/report/mode rules.
- All three supported targets; fresh final review and passing uploaded Actions before authorized protected merge.

## Review Focus

- A late snapshot must not turn a floor pickup into a silent starting grant (Task 1).
- An asynchronous sender lookup completing after run replacement cannot mutate the new run (Task 2).
- A partial initial phase must not supply a complete source catalog (Task 3).
- Old hd plus new demand must reopen completion even when an announcement was already sent (Task 4).
- A title delivery/retry must emit no party message and create no new obligation (Task 5).

### Task 1: Absolute quantities, early grant evidence and scheduler

Files: `safari/resources/StartingItemReport.kt`, `StartingResourceLedger.kt`, `StartingResourceAnnouncement.kt`, `SafariResources.kt`, `SafariRun.kt`; tests in `safari/resources/EarlyStartingResourcesTest.kt`.

Interfaces: report `parse(String)` / `text`; ledger `report(sender, quantities, complete)`, `roster(Set<String>?)`, `total(type)`, `complete`, `revision(biome)`; announcement `trigger(text, now)`, `dispatch(now, send)`; immutable observed starting quantities in resources.

- [ ] Add a compiling regression proving local title marking creates no contribution; observe failure.
- [ ] Add timing/parser/ledger tests: 6,999 fails, 7,000 sends once for trigger5,000; duplicate HOTSPOT, both identical/mixed grants, use/pickup before send, explicit none, repeats/legacy/self overlap and missing roster report.
- [ ] Implement the pure models and evidence separation; run focused tests to green; commit.

### Task 2: Tracking and verified resource inbox

Files: `SafariTracking.kt`, `SafariMessages.kt`, new `resources/StartingResourcePartyInput.kt`, `SafariRun.kt`; focused inbox/lifecycle tests.

Interfaces: inbox accepts only anchored party payloads and verified current UUID/owner; run snapshots original grant inventory before starting dispatch; scope marker admission is provided by Task3.

- [ ] Add rejection tests for nonparty/Guild/quoted/stale scope and pending callback crossing owner/roster replacement.
- [ ] Hook GAME+CHAT independently of display/downtime settings, bound pending work and caches, initialize local identity before ingestion, and drive schedule on every tracking tick.
- [ ] Remove unverified player-chat mutation from generic server-message parser; verify ordinary server entry/capture parsing and normal completion words; commit green.

### Task 3: Initial reward-source catalog and scope

Files: new `resources/StartingFloorCatalog.kt`, existing `esp/SafariEsp.kt`, `FloorDropPredictor.kt` callers; tests `StartingFloorCatalogTest.kt`.

Interfaces: catalog `observe(groups, activeMembers, markerUUIDs)`, `complete`, `coinIds`, `foodIds`, `scope`, `acceptsScope(token)`. Keep raw presence classifier independent of highlight selections.

- [ ] Add complete/missing prefix, partial triples, incense/generic/decorative exclusions, cross-block positions, newly discovered slots, marker scope old/new run and unload tests.
- [ ] Extend existing Safari-wide scan; retain IDs/UUIDs and uncertainty; do not call render disappearance collection.
- [ ] Wire scope to Task2 and source coverage to Task4; run source/integration tests and commit green.

### Task 4: Demand-aware bird/Gimmiegold local and remote proof

Files: `SafariBirdLedger.kt`, `completion/GimmiegoldSparklingCheck.kt`, `SafariRun.kt`, `SafariBiomeClearTracking.kt`; existing completion tests plus focused regressions.

Interfaces: existing checkers consume common starting totals, known report/catalog coverage and raw remaining blockers. `partyFullClear` stamps current demand; source/contribution changes reopen existing completion state.

- [ ] Add failing cases F3+S1 C1+U2, captured aliases, unknown report/catalog, stale hd/new slot, inside authoritative vs outside remote, retained bird proof/new food, pickup not extra and mode captures.
- [ ] Implement F+S/C+U and evidence gates without parallel counters; canonicalize aliases, preserve real-capture rules and current fd eligibility; update intentional obsolete expectations with explicit source/roster fixtures.
- [ ] Run completion/integration regressions and commit green.

### Task 5: Local titles, docs and complete delivery

Files: `StartingResourceDelivery.kt`, `SafariCompletion.kt`, owning docs in research update map.

- [ ] Prove title-only dispatch and retries cannot mutate contribution demand; remove unused-resource outbound chat at its actual caller, retain combined title and normal completion words.
- [ ] Update owners/manual guidance/revision metadata, including mixed-client/late-snapshot/structural catalog and legacy run-provenance limits.
- [ ] Run documented read-only sync reconciliation, full clean all-target tests/builds, helper/Python checks and relevant native integration where available.
- [ ] Request one fresh whole-branch review; fix Critical/Important with red/green regression coverage, ledger remaining Minor issues.
- [ ] Recheck issue/comments and current master; commit/push branch, open/attach PR; verify final uploaded Actions and download/verify artifacts.
- [ ] Merge only after required protection/review/checks permit; verify final master result, archive execution ledger and deliver concise chat receipt. No release or installation.

## Last updated

2026-10-19 — Issue68 native execution plan.

# CJ's Revenge dry-run implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement task-by-task. Native implementation continues in this session, with independent research and final review in the established research project.

**Goal:** Wire doom-check observation and fresh leader exchange into a strictly non-interacting 26.1.2 dry run.

**Architecture:** A pure environment tracker scopes world/location/party evidence. A pure dry-run session composes the existing sightings/requester/responder and emits only bounded party-protocol messages and diagnostic events. A thin client-thread adapter connects server GAME/location packets, positive detection/capture callbacks, configuration and debug output.

**Tech Stack:** Kotlin, Fabric/Hypixel Mod API, existing JUnit 5 and Gradle Java 25 toolchain.

**Spec:** `docs/superpowers/specs/2026-11-04-safari-entry-dryrun-design.md`

## Global Constraints

- Minecraft 26.1.2 only for generation, testing, builds and delivery.
- Local `experiment/cjs-revenge`; no GitHub writes, install or game operation.
- No warp-delay/config behavior changes, NPC actions, movement, menu actions, ticket permits or claims of admission.
- Off-by-default `Auto Safari Entry (dry run)` setting; both participating clients opt in.
- Existing 12-second arrival preparation, 3-second reply, 5-second sighting freshness and bounded paced response contracts apply.

## Review Focus

1. Location update before world replacement cannot carry a Safari claim into a new world (task 1).
2. Unknown roster followed by equal roster cannot resurrect in-flight authorization (tasks 1/2).
3. Late toggling/roster readiness cannot restart arrival budget (task 2).
4. Positive detection must be refreshed by a live scan, never retained HUD or old party hints; captures/unloading expire (tasks 2/3).
5. Verified sender names/UUIDs, delayed effects and reply pacing must be checked at the actual party-command boundary (tasks 1/2/3).

### Task 1: Environment and sender evidence

**Files:** Create `safari/automation/entry/EntryDryRunEnvironment.kt` and matching `EntryDryRunEnvironmentTest.kt` under the main/test Kotlin package roots.

**Interfaces:** `EntryDryRunEnvironment.world(connection: Any?, world: Any?, now: Long)`; `.location(mode: String?, server: String?, realm: String)` binds to observed world; `.context(account: UUID, name: String, party: EntryParty?): EntryContext?`; `.arrivedAt: Long`. Pure `EntryPartySender.resolve(text: String, profiles: List<EntryPeer>, members: Set<UUID>): Pair<EntryPeer, EntryMessage>?` accepts one exact current roster/profile match and the existing Party envelope/protocol.

- [x] Write tests: packet-before-world rejects, same-world duplicate metadata preserves origin, server/realm change invalidates, unknown/equal roster continuity changes, public/quoted/ambiguous/wrong UUID names reject.
- [x] Run target-qualified test, observe RED for missing production types.
- [x] Implement context tracker with no inherited packet/world claims and bounded immutable party evidence.
- [x] Run tests GREEN and commit focused source/tests.

### Task 2: Fresh-report dry-run session

**Files:** Create `EntryDryRunSession.kt`, `EntryDryRunSessionTest.kt`.

**Interfaces:** `EntryDryRunFrame(context: EntryContext?, enabled: Boolean, atEntry: Boolean, activeRun: Boolean, arrivedAt: Long)`; session `tick(frame, now): List<EntryDryRunOutput>`, `receive(frame, peer, message, now)`, `scan(frame, observed: List<EntryDryRunObservation>, now)`, `retainLoaded(frame, ids: Set<UUID>, now)`, `captured(frame, species, now)`, `debug()`. Reset replaces the session object. Output algebra has only `Party(context, message, proposedAt)` and `Notice(text, elapsedMs)`, with no action executor interface. Complete scans and send-time withdrawal do not refresh retained observation age. Extend `EntrySightings` only with visibility withdrawal; keep capture retirement and requester/responder contracts.

- [x] Write tests: two real sessions query/reply end-to-end; wrong sender/server; one query; no packet replay after disable/context gap; arrival-anchored expiry; capture before queued send; local observation freshness; third peer pacing expiry; no hint authorization; no fresh observation wording; monotonic rollback.
- [x] Run tests RED, implement session, run GREEN.
- [x] Commit session and regression tests.

### Task 3: Client wiring, setting, diagnostics and 26.1.2 delivery

**Files:** Create `CjsSafariEntry.kt`; modify initializer, `SparklingEncounters.kt`, configuration `ConfigManager.kt`/`SafariSettings.kt`, `/sm debug` command, version and affected feature docs. Extend `ConfigTest.kt`.

**Interfaces:** `CjsSafariEntry.register/tick/reset/scanned/debug`. Native adapter supplies current immutable frames, serializes callbacks on client thread, uses fresh location context, and checks the originating context immediately before each `pc` send through `EntryDryRunTransport.send` and `EntryDryRunDispatch.command`. Transport preserves the verified recipient and rechecks its unique current profile mapping, and enforces 1500ms pacing from completed/uncertain sends rather than proposal time. It drops blocked outputs with no queue/retry. Current sender identity derives only from connection profiles and current roster. No standard CHAT handler or automatic name lookup. Complete positive scans come from SafariAssist; capture notices use the anchored existing global capture grammar, including at the entrance. Restrict registration to 26.1.2.

- [x] Write settings default/persistence regression, observe RED, implement setting and apply binding; GREEN.
- [x] Connect native event boundary and diagnostics; use actual loader version check and compile current 26.1.2 APIs.
- [x] Run complete `:26.1.2:test :26.1.2:build` with strict temp harness and 21 build-helper checks; review actual XML counts/failures.
- [x] Independent whole-change review; fix Important findings in one RED/GREEN pass, rerun suite/build.
- [x] Reconcile docs; package only `4.0.2-cjs-revenge.2+mc26.1.2` with precise dry-run instructions and verified SHA256/metadata. Archive execution receipts; retain local branch/worktree. No install or third-party send.

## Execution commands

Set `JAVA_HOME` to the existing Java25 JDK and `GRADLE_USER_HOME` to the workspace cache. Commands use `./gradlew.bat :26.1.2:test --tests '*EntryDryRun*' --offline --no-parallel --max-workers=1 -Pkotlin.daemon.jvmargs=-Xmx3g --init-script ../../outputs/safari-entry-research-20261104/isolated-test-temp.init.gradle` for focused tasks; final removes test filters and adds `:26.1.2:build`. Expected RED: missing feature types or assertions reproducing an incorrect gate; expected GREEN: zero failures/errors/skips. Never invoke `buildAllTargets` for this increment.

# CJ’s Revenge new SM integration implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Execute locally with one fresh whole-branch review.

**Goal:** Deliver CJ .5 for Minecraft 26.1.2 on the supplied unpublished SM source, preserving both upstream behavior and approved CJ safeguards.

**Architecture:** Import the companion archive as a traceable upstream baseline, then reapply the CJ entry package and narrow shared-owner hooks. Keep visit-retained backend discoveries separate from fresh movement authorization.

**Tech Stack:** Kotlin/JVM 25, Fabric, Stonecutter, Gradle, JUnit 5; existing local Python verification tools.

**Spec:** `docs/superpowers/specs/2026-11-04-cjs-new-sm-integration-design.md` (approved in this chat).

## Global Constraints

- Local only; no push, PR, merge, release, install, game launch/closure or Discord send.
- Build, test and package **26.1.2 only**; retain the original inputs and CJ .4 source/build.
- Never read `.devAuth`.
- Version `4.0.2-cjs-revenge.5`, display name `CJ’s Revenge`, mod ID `sparkling-mutuals`, developer flag `false`.
- Preserve fresh status exchange, world-specific admission, party continuity and once-per-warp bounded movement; Manager/ticket clicks remain manual in the test flow.
- Preserve upstream 28-second fresh warp default, configuration migration, synchronization and other changes; explicit saved choices win.

## Review Focus

- Reconnected room snapshots with retained discovery bits must not replenish CJ observation age or authorize movement.
- A backend capture between a positive scan and the next START tick must retire that local CJ evidence.
- Equal party refreshes retain continuity; lost verification or changed membership/leader cancels old effects even if the same roster later returns.
- New contest/downtime holds must also prevent approach without triggering Shift bypass or NPC/ticket side effects.
- Existing migrated saved choices must survive, including deliberately enabled ticket selection; fresh CJ installations keep ticket selection off and approach off.

### Task 1: Import and establish the incoming-source baseline

**Files:** Incoming `net/**/*.kt`, `net/**/*.java`, resources and licenses mapped to their existing owners; `build.gradle.kts` metadata expansion; external provenance records in `outputs/cjs-new-sm-integration-20261104`.

**Interfaces:** Supplies the complete incoming source set and a buildable 26.1.2 baseline. CJ files are temporarily absent from the baseline commit and recoverable from `73432ef`.

- [x] Verify clean isolated branch and input digests; import exact source/resources/notices using the recorded mapping. Do not replace build scripts with absent archive content.
- [x] Temporarily remove only the known CJ entry package from the baseline; retain its tests for restoration in Task 2. Expand incoming `${developer}` as `false` in the reused harness.
- [x] Run `:26.1.2:compileKotlin :26.1.2:processResources --offline --no-parallel --max-workers=1`; expect successful native compilation and resolved metadata. Diagnose actual failures.
- [x] Commit the imported baseline locally and record every imported/intentionally modified file, source archive identity, omitted metadata and validation result.

### Task 2: Port the CJ integration with meaningful regressions

**Files:** Restore `safari/automation/entry/*.kt`; adapt `SparklingMutuals.kt`, `SafariAssist.kt`, `PartyRosterState.kt`, `PartyManager.kt`, `SafariManagerGuard.kt`, `ConfigManager.kt`, `SafariSettings.kt`, `SparklingCommand.kt`, defaults and package metadata. Add focused integration tests; reconcile older tests only against demonstrated upstream contracts.

**Interfaces:** `CjsSafariEntry.register/startTick/tick/reset/scanned`; `SafariAssist.entryVisibleIdentities(): Set<UUID>`; `PartyManager.continuity: Long`; `SafariManagerGuard.allowsCjApproach(client): Boolean`. Upstream synchronization remains owned by `SafariPartySync`; its captured-state read may invalidate CJ evidence but its discoveries never create a CJ sighting.

- [x] Restore the previously verified entry package. Write/run regressions for config migration/defaults and guard-hold composition before implementing those changes; expect assertion failures for the missing integration.
- [x] Port the narrow lifecycle, detector identity/scans, continuity, settings and debug hooks without replacing new shared-owner files wholesale. Use current backend capture state to withdraw observations, with no discovery-to-freshness bridge.
- [x] Run tests proving retained room discoveries alone yield no fresh CJ sighting, backend capture invalidates a prior positive, party loss invalidates old effects, each current entry hold prevents approach, and migrated explicit choices/fresh defaults are preserved. Expect all focused tests passing.
- [x] Run the full `:26.1.2:test` suite. For upstream changes that invalidate old source/API assumptions, record the exact mismatch, preserve the intended new contract and add equivalent behavioral coverage. Do not suppress failures or drop tests to get green.
- [x] Commit the integration and document the observed regression results and any necessary rulings.

### Task 3: Build, independent review and local delivery

**Files:** Affected CJ/config/entry/detection docs, final JAR/ZIP/README/validation/SHA256 manifest and provenance receipts under the output directory.

**Interfaces:** Consumes the integrated source and test results; produces a distinct installable CJ .5 JAR and clear testing instructions without changing an installed instance.

- [x] Update feature docs and check them against the actual lifecycle, preserved upstream default and saved configuration behavior. Run the read-only documentation sync check without recording a false all-document review.
- [x] Run `:26.1.2:test :26.1.2:build --offline --no-parallel --max-workers=1` with the existing isolated test-temp harness; expect zero failing tests and a 26.1.2-only artifact.
- [x] Obtain a fresh whole-branch review against the approved spec and this plan. Correct material findings using failing regression tests and a green full suite; record deferred minors and rulings.
- [x] Verify final metadata, classes/resources, nested dependency, source import/overlay manifest and SHA-256; package README and honest automated/live validation limits. Confirm no input or installed JAR was modified.
- [x] Commit final local source/docs and deliver links to JAR/ZIP and concise notes in this chat.

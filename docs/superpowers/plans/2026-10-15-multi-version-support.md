# Multi-version Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans for native execution, or superpowers:subagent-driven-development only if the user selects delegated execution. Complete the tasks in order.

**Goal:** Build, test and validate three installable Sparkling Mutuals JARs for Minecraft 26.1.2, 26.2 and 26.3, with safe future tagged-release publication.

**Architecture:** Stonecutter creates three target projects from shared root Kotlin sources, resources and tests. A Kotlin build helper verifies distributables and models release decisions; CI builds every target before the publishing job can stage and publish a complete release.

**Tech Stack:** Gradle 9.7.1, Java 25, Kotlin 2.4.20, Stonecutter 0.9.8, Loom 1.17.21, JUnit 5.11.4, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-10-15-multi-version-support-design.md` (user-approved, including exact 26.1.2 patch support).

## Global constraints

- Work on `feature/multi-version-support`, based on `5a4a3e7caedb1e2b865f598940ca70883a8858fc`, in the existing checkout. Preserve historical v4, logs and authentication files; never read `.devAuth`.
- Minecraft targets and initial loader metadata are exactly `26.1.2`, `26.2`, `26.3`; do not advertise other patches. Java target is 25.
- Fabric API: `0.155.3+26.1.2`, `0.161.0+26.2`, `0.163.0+26.3`. HM API: `1.0.3+26.1`, `1.0.4+26.2`, `1.0.5+26.3`. MoulConfig: corresponding `modern-26.1`, `modern-26.2`, `modern-26.3`, all version `4.8.0`.
- All targets use Loader 0.19.5 and Fabric Language Kotlin `1.14.1+kotlin.2.4.20`. DevAuth 1.2.2 stays development-only.
- Keep internal mod version `3.3.0` during migration. Final files are `sparkling-mutuals-<version>+mc<target>.jar`.
- Preserve gameplay, configuration/history compatibility, existing tests, required mixin injections and licensing. No implementation of #51/#52.
- No PR, merge, master push, tag push or actual release publication. Automatic monitoring remains paused.
- Do not run documentation sync with `-Record` while unrelated pending briefs remain unresolved. Record actual tests/live checks separately.

## Review focus

1. A correctly named JAR can contain wrong Minecraft or nested-library metadata: reject it before collection/publication (Task 2).
2. A previously successful target can leave stale output after another target fails: collect only this invocation's declared outputs and require all three (Tasks 2/4).
3. A target can compile while discovering no tests or retaining an invalid mixin signature: check test discovery and perform per-target client startup checks (Tasks 1/3/5).
4. Repeated tag jobs can race or replace a published release: serialize by tag, reconcile drafts only, and reject published mismatches without mutation (Task 4).
5. A maintainer can supply release notes or an unexpected asset: preserve written notes, reject ambiguous/unexpected JAR assets, and do not delete unrelated assets (Task 4).

---

## Task 1: establish the current-source baseline and three target projects

**Files:** replace `settings.gradle`/`build.gradle` with `settings.gradle.kts`/`build.gradle.kts`; create `stonecutter.gradle.kts`, `versions/26.1.2/gradle.properties`, `versions/26.2/gradle.properties`, `versions/26.3/gradle.properties`; modify `gradle.properties`, `.gitignore`, `src/main/resources/fabric.mod.json`.

**Interfaces:** exact target IDs above; each target exposes Gradle `test`, `build`, `jar` and `runClient`; the active development version is `26.1.2`. Shared internal mod version remains independent of target filenames.

- [x] Recheck issue #50's state, actual author, body/comments and current remote/document revisions. Record any revisions since the approved spec before changing build logic.
- [x] Run the unchanged baseline with Java 25: `./gradlew.bat --no-daemon test build --console=plain`. Record executed test counts and suite IDs from XML, plus build result. Stop and diagnose any baseline failure before attributing it to migration.
- [x] Configure the pinned Stonecutter settings/controller and shared Kotlin DSL build. Use the spec's dependency matrix, stable Loom and existing Gradle wrapper. Keep root source/test/resource locations and licenses; separate all target outputs. Preserve current repository exclusions and ignore only new generated outputs.
- [x] Process exact per-target Minecraft requirements into `fabric.mod.json`; keep mod ID, authors, entrypoint, resource paths and configuration locations stable. Bundle matching HM API/MoulConfig and exclude DevAuth from distributables.
- [x] Run `./gradlew.bat projects tasks --all --console=plain`; verify exactly three target projects and their test/client/package tasks. Configure each target's `test` to fail if no tests are discovered.
- [x] Compile/test/build `26.1.2` first, then configure `26.2` and `26.3`; record real compatibility errors for Task 3 rather than suppressing dependencies or mixins. If target errors remain, defer the migration commit until Task 3 resolves them; do not present the migration as complete.

## Task 2: verify and collect only correct installable JARs

**Files:** create `buildSrc/build.gradle.kts`, `buildSrc/src/main/kotlin/DistributionVerifier.kt`, `buildSrc/src/test/kotlin/DistributionVerifierTest.kt`; modify `build.gradle.kts` and `stonecutter.gradle.kts` as the target/controller owners require.

**Interfaces:** `TargetSpec(minecraft: String, fabricApi: String, hmApi: String, moulConfig: String)`; `VerifiedJar(path: Path, minecraft: String, modVersion: String, size: Long, sha256: String)`; `verifyJar(path: Path, target: TargetSpec, modVersion: String): VerifiedJar`; `verifyDistribution(paths: List<Path>, targets: List<TargetSpec>, modVersion: String): List<VerifiedJar>`. Invalid archives throw `IllegalArgumentException` with a diagnostic and cannot be collected. Expose root `buildAllTargets` and `verifyDistributions` tasks.

- [x] Write fixture-based JUnit tests for a correct three-target set and for missing/duplicate targets, wrong names, wrong internal mod/Minecraft versions, missing entrypoint/mixins/icon/licenses, incompatible nested dependency metadata, embedded DevAuth, and source/development JAR contamination. Test a correctly named but wrongly populated JAR explicitly.
  - `completeDistributionHasExactlyThreeVerifiedTargets`: result size is 3 and Minecraft set equals `{26.1.2,26.2,26.3}`, each internal version is `3.3.0`, byte size is positive and digest matches the fixture bytes.
  - `wrongInternalMinecraftFailsDespiteCorrectFilename`: a file named for `mc26.3` containing Minecraft `26.2` throws `IllegalArgumentException`. Apply the same exception assertion to each invalid fixture listed above.
  - `incompleteCollectionCannotUsePreviousOutputs`: two newly produced targets plus an old third JAR outside declared task outputs cannot form a verified distribution or replace the current distribution directory.
- [x] Run `./gradlew.bat -p buildSrc test --console=plain`; confirm the contract tests fail before implementing the verifier.
- [x] Implement archive validation using `JarFile`/nested ZIP metadata and Gradle's available JSON support. Treat metadata and declared task provenance as evidence; do not treat different file hashes as sufficient proof of target correctness. Read only supplied JARs, with bounded nested-archive inspection.
- [x] Wire final target `jar` task outputs, not broad `build/libs` globs, into collection. Stage into a fresh directory, validate all three, then replace only the intended `build/distributions/<version>` output. A missing/failed target cannot reuse an older JAR.
- [x] Run the verifier tests and the working `26.1.2` target's packaging validation. Defer the real all-target collection check until Task 3 resolves compatibility errors; fixtures already require all three target identities. Confirm sources remain outside the installable directory. Commit the migration/packaging milestone only after Task 3 makes every target pass.

## Task 3: adapt only demonstrated Minecraft/Fabric compatibility differences

**Files:** existing affected owners under `safari/esp`, `hud`, `config`, `mixin` and feature integration packages; `src/main/resources/sparkling-mutuals.mixins.json` only for actual target-specific registrations; matching feature tests. Do not recreate pre-cleanup source paths.

**Interfaces:** preserve existing feature APIs, saved keys and event ordering. A small conditional or adapter selects only the Minecraft API difference; shared gameplay decisions remain shared.

- [x] For each actual compiler or mixin error from Task 1, inspect the target's mapped signatures and official Fabric API before selecting a conditional. Keep a short target/error/fix list in the issue validation record.
- [x] Where a changed runtime decision is testable, add the failing regression at its feature owner before implementing the adapter. For signature-only differences, require compilation and client/mixin startup evidence rather than a string assertion mirroring the code.
- [x] Apply the smallest target-specific adapter/Stonecutter conditional. Keep required injection failures visible, preserve callbacks and rendering semantics, and do not change the documented Gemzie/Honeybug policies.
- [x] Run relevant focused suites and then `./gradlew.bat clean buildAllTargets --console=plain` plus buildSrc tests. Inspect all three real JAR metadata/nested libraries and compare suite discovery with the baseline; investigate missing suites or zero-test targets. If unavailable dependencies or APIs block a target, report the exact evidence instead of widening compatibility metadata.
- [x] Launch each target client if available and inspect mixin application plus settings, HUD/ESP, commands and Safari lifecycle. Record any unavailable live Hypixel checks explicitly. Commit compatibility fixes after their checks pass.

## Task 4: gate CI and future publication on a complete verified target set

**Files:** modify `.github/workflows/build.yml`; create `.github/workflows/release.yml`, `buildSrc/src/main/kotlin/ReleasePlanner.kt`, `buildSrc/src/main/kotlin/PublishReleaseTask.kt`, `buildSrc/src/test/kotlin/ReleasePlannerTest.kt`, `buildSrc/src/test/kotlin/ReleasePublishingTest.kt`; register release tasks in the controller build.

**Interfaces:** `ReleaseAsset(id: Long, name: String, size: Long, sha256: String?)`; `ExistingRelease(draft: Boolean, body: String?, assets: List<ReleaseAsset>)`; `ReleaseDecision { CREATE_DRAFT, UPDATE_DRAFT, PUBLISHED_NOOP }`; `ReleasePlan(decision: ReleaseDecision, preserveBody: Boolean)`; `planRelease(tag: String, modVersion: String, files: List<VerifiedJar>, existing: ExistingRelease?): ReleasePlan`. Reject invalid inputs/mismatches with `IllegalArgumentException`. The publishing boundary accepts a fake GitHub client in tests and reads a token from the environment only during explicit publication. No credentials are logged or written into artifacts. Unknown published asset digests cannot establish an identical/no-op result without verified content.

- [x] Write failing tests: tag/version mismatch; missing target; failed build means no publishing call; duplicate/unexpected JAR assets; draft rerun with matching assets; draft replacement limited to intended owned assets; preserved maintainer body; published identical no-op; published mismatch rejected without writes. Simulate HTTP failures midway through upload and require the release to remain a draft.
  - `tagMustMatchInternalVersion`: `v3.3.1` with version `3.3.0` throws; `v3.3.0` with a complete verified set and no existing release yields `CREATE_DRAFT`.
  - `maintainerNotesSurviveDraftRerun`: an existing nonblank body yields `preserveBody=true`, and the fake client's final body equals the original text.
  - `publishedAssetsAreImmutable`: matching verified assets yield `PUBLISHED_NOOP` and zero writes; a changed/missing/unknown digest rejects publication without writes.
  - `partialUploadNeverPublishes`: fail the fake second upload and assert zero publish calls and a draft still present; rerunning with three verified uploads may publish exactly once.
- [x] Implement pure release planning and the small HTTP boundary. Limit writes to a same-tag draft until all three uploaded assets are verified; keep non-JAR assets untouched and reject unexpected JARs for review. Never overwrite a published mismatch. Never forward a GitHub token to a redirected storage host.
- [x] Change push/PR CI to build/test each exact target, upload only its verified installable output, and require an overall all-target validation job. Keep contents permission read-only. Avoid hard-coded counts that can conceal skipped tests.
- [x] Add the tag workflow: check out the immutable tagged commit, validate `v<modVersion>`, build/test all targets, validate exactly three outputs, then run a separate contents-write publishing job with tag-scoped concurrency. Preserve existing draft notes, generating notes only when absent. PR or branch builds cannot enter this job.
- [x] Run buildSrc publishing/planner tests, workflow syntax validation and nonpublishing success/failure simulations. Inspect job dependencies and permissions. Do not push a tag or create a real release to test this task. Commit verified CI/release configuration.

## Task 5: documentation, final verification and delivery

**Files:** modify `README.md`, `CONTRIBUTING.md`, `docs/README.md`; create `docs/MULTI_VERSION_BUILDS.md`; update the issue brief to distinguish implemented build behavior from pending runtime checks. Update this plan's checkboxes and keep the approved spec with it.

- [x] Document the exact target/dependency matrix, install-one-matching-JAR rule, `buildAllTargets`, confirmed target-qualified test/build/client commands, active-version switching, output locations, adding a target and tag/draft release behavior. State that the 26.1 line targets 26.1.2 only. Keep release notes in GitHub Releases.
- [x] Update Last updated sections with an America/New_York date and a concrete explanation. Rerun documentation sync and compare all changed/new document hashes with the start record; read/reconcile unrelated revisions without marking them implemented. Do not use `-Record` while #51/#52 briefs remain unresolved.
- [x] Run final clean all-target tests/build/verification and buildSrc tests. Record per-target test counts, artifact names/digests/metadata, workflow checks, and actual client/live-test results or limitations. Inspect the final diff for unrelated changes and preserved logs/authentication data.
- [ ] Post the authorized implementation-stage reply on still-open issue #50 once code is finished. Commit and push only `feature/multi-version-support`; do not create a PR or release.
- [ ] Verify GitHub Actions on the final uploaded SHA, download all three matching installable JAR artifacts, and validate their identity. If a check fails, investigate before claiming success.
- [ ] Recheck issue #50 open state and source metadata, then post the final confirmation with issue/commit/artifact links, supported versions, commands and validation limits. Record comment IDs/fingerprints and handled history; keep automatic checks paused. Deliver the three JAR links in chat.

## Plan self-review

Coverage: all approved spec sections map to Tasks 1–5. The five review-focus failure classes have concrete checks in Tasks 1–4. All downstream artifact/release inputs use `VerifiedJar`; exact dependency values and naming stay in the shared constraints. No release operation is authorized during implementation, and no gameplay changes are included.

## Last updated

2026-10-15 - Prepared the implementation sequence for the user-approved three-target build design. Native implementation and local verification complete; branch review and uploaded-commit verification remain before delivery.

# Multi-version builds

One shared Kotlin source tree builds three Fabric JARs. Install **one** JAR matching your Minecraft version; remove previous Sparkling Mutuals JARs first. The internal mod version stays the same across the three builds.

## Versions and installation

| Exact Minecraft requirement | Filename for mod version V | Fabric API | Bundled HM API | Bundled MoulConfig |
| --- | --- | --- | --- | --- |
| 26.1.2 | `sparkling-mutuals-V+mc26.1.2.jar` | 0.155.3+26.1.2 | 1.0.3+26.1 | modern-26.1:4.8.0 |
| 26.2 | `sparkling-mutuals-V+mc26.2.jar` | 0.161.0+26.2 | 1.0.4+26.2 | modern-26.2:4.8.0 |
| 26.3 | `sparkling-mutuals-V+mc26.3.jar` | 0.163.0+26.3 | 1.0.5+26.3 | modern-26.3:4.8.0 |

All targets require **Java 25**, **Fabric Loader 0.19.5+**, matching **Fabric API**, and **Fabric Language Kotlin 1.14.1+kotlin.2.4.20**. HM API and MoulConfig are bundled; DevAuth 1.2.2 is development-only. Do not install source, test or development JARs.

The 26.1 line targets **26.1.2 only**. Other patches are not advertised. The migration retains internal version **3.3.0**; it does not publish or replace the existing single-target v3.3.0 release.

Upstream `modern-26.2:4.8.0` retains `Fabric-Minecraft-Version: 26.2-pre-2` in its build manifest. This exact pinned artifact passed 26.2 client startup and settings rendering. Packaging accepts that specific upstream label, rejects other target mismatches, and still requires the mod's Minecraft metadata to be exactly `26.2`.

## Build and test

Use the repository wrapper with Java 25. These examples use Windows; replace `./gradlew.bat` with `./gradlew` on Linux/macOS.

```powershell
./gradlew.bat clean buildAllTargets
./gradlew.bat :26.2:test
./gradlew.bat :26.3:verifyTargetJar
./gradlew.bat :26.1.2:test --tests '*BeeNestSurveyTest'
./gradlew.bat testBuildLogic
./gradlew.bat verifyDistributions
```

`buildAllTargets` runs the shared feature tests for all three targets and the packaging/release-helper tests. It validates each declared final `jar` task output, stages and validates copied bytes, then replaces `build/distributions/<mod-version>/` with exactly three installable JARs plus `build-manifest.json` (names, targets, sizes and SHA-256 digests). An incomplete or invalid set cannot replace a previous complete distribution.

`verifyDistributions -PdistributionDir=incoming` validates downloaded artifacts without rebuilding them. It rejects wrong names, internal versions, Minecraft requirements, missing runtime resources, incompatible nested libraries and development dependencies. Per-target intermediates, source JARs and test reports remain under `versions/<target>/build/`; helper tests report under `buildSrc/build/reports/tests/test/`.

## Develop and launch

26.1.2 is the active editor version. Shared sources/resources/tests stay under root `src/`; generated target sources are ignored. Use small Stonecutter conditionals or `compat` adapters for demonstrated API changes. Preserve required mixins, gameplay rules and saved configuration/history keys.

```powershell
./gradlew.bat :26.1.2:runClient
./gradlew.bat :26.2:runClient
./gradlew.bat :26.3:runClient
./gradlew.bat "Set active project to 26.2"
./gradlew.bat "Reset active project"
```

IntelliJ also exposes the Stonecutter switching and Loom client configurations. Reset the active project to 26.1.2 before committing. Client profiles are separate under `run/<target>/`. DevAuth uses your local setup; keep credentials and authentication files out of Git. `-PclientSmoke` disables DevAuth and enables verbose mixin checks for local startup testing.

Build tools are pinned to Gradle 9.7.1, Stonecutter 0.9.8, Loom 1.17.21 and Kotlin 2.4.20. Shared properties live in `gradle.properties`; each `versions/<target>/gradle.properties` supplies its exact dependency matrix.

## CI and releases

Branch pushes and pull requests run read-only CI: helper tests, three target test/build/packaging jobs, and an aggregate verification job. Download the matching `jar-mc<target>` Actions artifact. No PR or branch job publishes a release.

The required `build` status check succeeds only when build-helper tests, all target jobs and distribution verification succeed. Failed or skipped dependencies keep that gate from passing.

Temporary development exception: pushes or PRs from `feature/capture-shot-projection` build/test/package **26.1.2 only** while capture-shot work is in progress. The complete three-JAR aggregate is skipped for that branch. Other branches and tag releases retain the full matrix. Restore the full branch matrix when the feature work is finished.

For a future release, choose a new `mod_version` and matching **`v<mod_version>`** tag. The tag workflow checks out the event's immutable commit, validates the tag, tests/builds all three targets and verifies the complete artifact set. Only the final publishing job has release write permission. It checks the tag still points at the tested commit and stages one draft before publishing.

Draft reruns retain maintainer-written release notes, generate notes only when blank, retain unrelated non-JAR assets, reuse matching uploads and replace only stale expected JAR assets. Unexpected or duplicate JAR assets stop publication. A failed upload leaves a draft; publication requires all three matching sizes/digests. Rerunning an identical published release does nothing; a different or unverifiable published asset set fails for manual attention. Tokens stay in the publishing environment and are never forwarded through redirects.

Put version notes in the GitHub Release description. This migration does not create a real release or push a tag.

## Validation limits and adding a target

The migration preserved all 115 existing suites (652 tests per target). Local startup checks loaded required mixin owners and rendered settings, history and capture screens on all three clients. These checks do **not** establish live Hypixel Safari tracking, world HUD/ESP appearance or server commands; those still need a suitable in-game session.

Adding a target is a deliberate change: verify official Minecraft/Fabric/library availability, add its Stonecutter settings and properties, inspect actual mapped APIs/mixin signatures, update the verifier/release target set and both CI matrices, then run clean tests, packaging and client/in-game checks. Update this table and installation guidance only to the versions demonstrated by that evidence.

## Last updated

2026-10-16 - Preserved the required build gate across all target/helper/distribution jobs while rebasing the three-target implementation onto master.

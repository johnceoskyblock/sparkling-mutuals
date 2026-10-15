# Support Minecraft 26.1, 26.2, and 26.3 with Stonecutter and three JARs per release

## Implementation status

Implemented on `feature/multi-version-support` for exact targets **26.1.2**, **26.2** and **26.3**, as approved by the maintainer. The [maintained build guide](../MULTI_VERSION_BUILDS.md) contains commands, dependencies, packaging/release rules and validation limits. The original request below is retained for review; its pre-migration file paths and example `mc26.1` label are historical.

Local checks retain all 652 feature tests and 115 suites per target, validate three distinct JARs, and start each client with required mixins and settings/history/capture screen rendering. Packaging and publication-helper tests cover wrong/missing targets, staged collection and interrupted draft uploads. Live Hypixel world HUD/ESP, commands and Safari behavior remain untested in this migration. No tag or release was created; final uploaded-commit Actions verification is recorded in the issue confirmation.

## Goal

Let users run Sparkling Mutuals on Minecraft 26.1, 26.2, or 26.3 instead of relying on a single supported version. Maintain one shared codebase using Stonecutter and publish one installable Fabric mod JAR for each supported version in every GitHub release.

Each release must contain exactly three downloadable mod JAR assets, with one shared mod release version and explicit Minecraft version labels. For example:

```text
sparkling-mutuals-<mod-version>+mc26.1.jar
sparkling-mutuals-<mod-version>+mc26.2.jar
sparkling-mutuals-<mod-version>+mc26.3.jar
```

These are three distinct builds, not three renamed copies of the same binary. GitHub's automatically generated source archives are separate and do not count toward these three JARs.

## Reference and current repository

Use [Gaeritag/Nebulune](https://github.com/Gaeritag/Nebulune) as an architectural reference. Its [settings](https://github.com/Gaeritag/Nebulune/blob/master/settings.gradle.kts) configure Stonecutter targets `26.1`, `26.2`, and `26.3`; its [Stonecutter configuration](https://github.com/Gaeritag/Nebulune/blob/master/stonecutter.gradle.kts) and [build script](https://github.com/Gaeritag/Nebulune/blob/master/build.gradle.kts) demonstrate version-specific preprocessing, dependencies, artifact versioning, and collection. Adapt the approach to this mod; do not import Nebulune's unrelated features, dependencies, or assumptions.

At prompt preparation, this repository has:

- `settings.gradle` and `build.gradle`: one Fabric Loom/Kotlin project.
- `gradle.properties`: Minecraft `26.1.2`, Fabric API `0.155.3+26.1.2`, Java target 25, and mod version `3.3.0`.
- `build.gradle`: bundled dependencies `net.azureaaron:hm-api:1.0.3+26.1` and `org.notenoughupdates.moulconfig:modern-26.1:4.7.2`; DevAuth is development runtime only.
- `src/main/resources/fabric.mod.json`: Minecraft dependency `~26.1.2` and only the mod version currently expanded from Gradle.
- `src/main/resources/sparkling-mutuals.mixins.json`, Java mixins, Kotlin game integrations, and rendering code that may need adaptation between versions.
- `.github/workflows/build.yml`: push/PR build using Java 25, uploading `build/libs/` as an Actions artifact. No release-publishing workflow is currently present.

Recheck these facts on the implementation branch. Read applicable repository instructions and preserve unrelated changes.

## Implementation requirements

### Version support and shared sources

1. Introduce Stonecutter with a compatible, explicitly configured Gradle/Loom/Kotlin/plugin setup. Keep shared Java/Kotlin sources, resources, and tests wherever practical; use small conditional sections or adapters for actual Minecraft API differences. Avoid maintaining three copied source trees.
2. Define the exact Minecraft build and supported patch range for each release line. The existing project targets `26.1.2`, so do not silently change its compatibility to base `26.1` or claim every 26.1 patch works without validation. Document the chosen patch policy and make artifact labels and loader metadata consistent with it.
3. Resolve and verify compatible Fabric API, loader, Fabric Language Kotlin, MoulConfig, Hypixel Mod API, and development dependencies per target. Use published compatible artifacts or justified compatibility work. Do not suppress dependency checks or widen metadata to make an incompatible binary appear supported.
4. Adapt changed Minecraft/Fabric APIs, rendering hooks, and mixin target names/signatures. Preserve existing features and saved config/data compatibility across supported versions. Compile success alone does not establish mixin or runtime compatibility.
5. Parameterize processed mod metadata per target. Keep the mod ID stable and use a consistent release version scheme; declare accurate Minecraft, Java, loader, and dependency requirements. Retain required licenses and bundled dependencies, and exclude development-only dependencies from release packaging.

### Local builds and continuous integration

6. Provide one documented command that tests and builds all three targets from a clean checkout and collects their installable JARs into a predictable output directory. Also document single-target builds, active-version switching, and client run configurations.
7. Ensure builds do not overwrite another target's outputs or package stale files. Preserve discovery and execution of the existing tests under the new layout. Update push/PR CI to validate every target, including compilation, tests, and packaging; a failure on any target must fail the overall checks.

### GitHub release assets

8. Add a tag-triggered release workflow following the repository's version/tag convention, or establish and document a convention if none exists. Build all three targets from the tagged commit, verify success and asset identity, and attach all three installable JARs to the same GitHub release.
9. Prepare and validate all three JARs before publishing the release. A missing or failed target must prevent publication of a partial release. Use a draft/staging approach where needed, and make reruns handle existing draft assets without duplicate or stale files.
10. Select final distributable tasks explicitly, accounting for the Loom version's packaging behavior. Do not upload source, development, test, or intermediate JARs as user-installable release assets. Verify there are exactly three intended mod JAR assets and no output collisions. Keep release write permissions limited to the publishing workflow/job; PR builds must not publish releases.
11. Add concise installation guidance listing supported versions and required dependencies. Explain that users install only the JAR matching their Minecraft version, and show a release asset table mapping each filename to its actual supported version/range.

If a required target or dependency is unavailable, report the exact blocker and evidence. Do not silently drop a version, substitute a mislabeled artifact, or mark the three-version task complete. Do not publish an actual release as part of implementing this issue; deliver the working configuration and workflows for review.

## Acceptance criteria

- A clean checkout can build and test all three targets with the documented command, without manually editing version properties between builds.
- Each target produces a distinct installable JAR with the expected name, matching processed metadata, required resources/mixins, and correct bundled dependencies.
- Every supported target starts successfully with its declared dependencies, without mixin application errors. Smoke-test config UI, HUD, ESP/rendering, commands, and Safari tracking where a test session is available. Report any runtime checks that could not be performed.
- Existing automated tests remain active and pass; push/PR CI checks all three targets.
- The release workflow stages exactly three correct mod JARs for one tag and publishes only after all targets succeed. Validate its asset-selection and missing-target failure behavior without publishing a real release.
- Documentation covers the version/dependency matrix, installation choice, local development commands, adding a future target, and the release process.

## Delivery

Implement the migration and compatibility fixes. Summarize changed files, exact supported versions and dependencies, commands and validation results, artifact locations, and any remaining blockers or unverified runtime behavior. Keep the work focused on multi-version support and release packaging.

## Last updated

2026-10-15 - Linked the implemented three-target design and maintained rules; distinguished startup/screen evidence from unperformed live server checks.

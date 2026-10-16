# Issue #50: three-target Stonecutter build design

Status: approved by the user on 2026-10-15, including the exact 26.1.2 patch target. Implementation plan review is next.

## Outcome and scope

Build Sparkling Mutuals from one shared codebase for the 26.1, 26.2 and 26.3 release lines, with exactly three distinct installable JARs. Test every target and stage all three before a future tagged release can publish. Preserve gameplay, saved data, mod identity and licensing. Do not implement issues #51/#52, create a PR, merge master or publish a release during this task.

Start `feature/multi-version-support` from reviewed commit `5a4a3e7caedb1e2b865f598940ca70883a8858fc`. Use the existing checkout as requested; preserve its untracked logs and local authentication files. Never read `.devAuth`. The historical local v4 commit remains available as its branch; do not recreate remote v4.

## Verified starting point

- Full onboarding reviewed 56 Markdown documents, including all 37 critter pages. The shared source baseline reports the three pending implementation briefs as New; do not record them as implemented.
- Current released mod version is 3.3.0. Keep that value during this build migration; the user chooses the next release version separately.
- Existing build: Gradle 9.7.1, Java 25, Loom 1.17-SNAPSHOT, Kotlin 2.4.20, one Minecraft 26.1.2 target, one build workflow, no publishing workflow.
- Official Mojang metadata lists release builds 26.1.2, 26.2 and 26.3; each requires Java 25. Official dependency repositories contain the target-specific artifacts below. Artifact existence is not runtime compatibility certification.
- Architectural reference: Gaeritag/Nebulune at `6c49e9bf6267beb28781a981addc8a9779fa9e93`; inspected settings, Stonecutter controller, build and version catalogs. Adapt its shared-source/target structure without its unrelated libraries or broad Minecraft metadata ranges.

## Target and dependency policy

| Release line | Exact build/loader Minecraft requirement | Fabric API | Bundled HM API | Bundled MoulConfig |
| --- | --- | --- | --- | --- |
| 26.1 | 26.1.2 | 0.155.3+26.1.2 | 1.0.3+26.1 | modern-26.1:4.8.0 |
| 26.2 | 26.2 | 0.161.0+26.2 | 1.0.4+26.2 | modern-26.2:4.8.0 |
| 26.3 | 26.3 | 0.163.0+26.3 | 1.0.5+26.3 | modern-26.3:4.8.0 |

Use Fabric Loader 0.19.5, Fabric Language Kotlin 1.14.1+kotlin.2.4.20 and Java 25 for all three. Retain JUnit 5.11.4 and DevAuth 1.2.2 as development runtime only. Verify transitive/embedded metadata during dependency resolution and packaging; a target fails rather than suppressing an incompatible dependency check.

The 26.1 line deliberately retains 26.1.2, matching the existing supported patch. Do not advertise base 26.1, 26.1.1 or untested future patches. Use exact Minecraft requirements initially, widening them only after separate compatibility evidence. Corresponding filenames explicitly include 26.1.2, 26.2 and 26.3.

Pin Stonecutter 0.9.8 and Loom 1.17.21 instead of a mutable Loom snapshot. Keep Gradle 9.7.1 and Kotlin 2.4.20. Published version availability was verified; initialization/compilation must validate the combination before it is called supported.

## Build structure

Recommended: Stonecutter settings/controller plus one shared Kotlin DSL build, with small per-target property files. Root `src/main`, resources and `src/test` remain the source of truth. Keep 26.1.2 as the active development version. Convert the existing short Groovy build/settings to Kotlin DSL without importing Nebulune's feature plugins.

Alternatives considered: separate copied version projects would duplicate code and violate the issue's shared-source goal; broad loader ranges around one binary would not produce three real builds. Retaining the Groovy build is possible, but matching the verified Kotlin DSL integration keeps the controller/build interfaces easier to inspect.

Version-specific rendering/API/mixin differences use minimal Stonecutter conditionals or focused adapters at existing feature boundaries. Do not reorganize gameplay owners or relax required mixin injections to hide failures. Keep test discovery for all matching feature packages and `safari/integration` on every target.

Expose `buildAllTargets` as the single clean-checkout verification/collection command and normal version-qualified `test`, `build` and `runClient` tasks. `buildAllTargets` builds/tests all targets, verifies their final JARs and copies only installable outputs into `build/distributions/<mod-version>/`. Document actual generated task names after configuration is verified.

## Packaging contract

For mod version V, the distribution directory contains exactly:

```text
sparkling-mutuals-V+mc26.1.2.jar
sparkling-mutuals-V+mc26.2.jar
sparkling-mutuals-V+mc26.3.jar
```

Each JAR is built against its own Minecraft/Fabric/dependency set. The mod's internal release version remains V; its Minecraft requirement identifies the target. Validate archive name, internal version, exact Minecraft requirement, Java/loader requirements, entrypoint, mixin registrations, icon, license notices and bundled dependency metadata. DevAuth, source, test and development JARs are excluded from distributions. Fail on missing/duplicate/unexpected targets or stale/mislabeled output; do not merely rename one binary three times.

Keep per-target intermediate directories separate. Collection starts from a clean destination and consumes declared final task outputs, not an unbounded glob of old build files. Do not require different binary digests as the sole proof of target correctness; verify target metadata and build provenance directly.

## CI and future release behavior

Push/PR CI builds/tests all three targets, validates the wrapper and packaging, and uploads labeled installable JAR artifacts. Any target failure fails the overall checks. PR workflows keep contents permission read-only and never publish.

A separate `v*` tag workflow verifies that the tag equals the shared mod version, builds/tests all targets from that immutable tagged commit, and validates the complete three-JAR set before granting a publishing job contents-write permission. Stage files in one draft release; publish only after all three assets are uploaded and verified. Preserve a maintainer-written draft description; generate release-description notes only when absent. Do not restore cumulative VERSION_NOTES.md.

Reruns can reconcile same-tag draft assets idempotently. A published release with a different asset set must fail for manual attention rather than overwrite user assets or republish silently. Missing targets and validation failures leave the release unpublished. Test staging/failure logic without pushing a tag or creating/publishing a real release in this task.

## Validation and documentation

1. Run the untouched current-source baseline tests/build after establishing the feature branch. Record exact counts and failures.
2. Configure Stonecutter and compile/test each target independently; resolve real API differences with focused regression coverage. Run the all-target command from clean outputs.
3. Exercise packaging validation against correct archives, wrong versions, duplicate/missing targets, stale output and development/source JAR contamination. Verify all existing suites remain discoverable on each target.
4. Test tag/version mismatch and partial-target failure paths locally or in nonpublishing CI. Verify upload selection cannot include source/development JARs.
5. Launch each client when an appropriate session is available and check required mixin application, settings, HUD/ESP rendering, commands and Safari tracking. Record any unavailable live Hypixel checks explicitly; passing compilation alone does not prove them.
6. Update README, CONTRIBUTING, repository/build guidance, the supported version/dependency matrix and the issue brief's lasting rules. Add America/New_York Last updated dates to changed maintained docs. Recheck all document hashes before finalizing.
7. Push only the selected feature branch, verify final-SHA Actions checks and download all three corresponding installable JARs. Post implementation-stage and verified issue replies only after their respective evidence exists and issue #50 remains open.

## Remaining decisions and limitations

- The user approved this design and the exact 26.1.2 patch policy on 2026-10-15.
- Dependency resolution, per-version compilation and the 652 existing tests per target have run successfully. All three clients started and rendered settings/history/capture screens; live Hypixel world/HUD/ESP and server behavior remain unverified.
- No gameplay-rule changes are planned, so the documented Gemzie/Honeybug wording inconsistencies remain recorded for the relevant future issue rather than being silently changed here.
- No shared documentation -Record operation while unrelated pending briefs remain unresolved. The reading record is separate from implementation certification.

## Primary references inspected

- Issue: https://github.com/johnceoskyblock/sparkling-mutuals/issues/50
- Reference build: https://github.com/Gaeritag/Nebulune/tree/6c49e9bf6267beb28781a981addc8a9779fa9e93
- Minecraft releases: https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
- Fabric API versions: https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
- Loom versions: https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml
- HM API versions: https://maven.azureaaron.net/releases/net/azureaaron/hm-api/maven-metadata.xml
- MoulConfig repositories: https://maven.notenoughupdates.org/releases/org/notenoughupdates/moulconfig/
- Stonecutter marker: https://maven.kikugie.dev/releases/dev/kikugie/stonecutter/dev.kikugie.stonecutter.gradle.plugin/maven-metadata.xml

## Last updated

2026-10-15 - Recorded the approved exact-target design and subsequent local compilation, test and client-screen validation; live server checks remain explicit limitations.

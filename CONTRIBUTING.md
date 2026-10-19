# Contributing to Sparkling Mutuals

## Purpose

Contribute focused bug fixes, features, documentation, screenshots, testing or useful reports. This guide takes inspiration from [SkyHanni's contributor guide](https://github.com/hannibal002/SkyHanni/blob/beta/CONTRIBUTING.md). Contributors remain responsible for understanding and verifying AI-assisted work.

## Prerequisites

Use Git, a Java **25** JDK, an editor and the included Gradle wrapper. IntelliJ IDEA is recommended; set both project SDK and Gradle JVM to Java 25. Clone/fork the repository and create a branch describing the change. See [multi-version dependencies and setup](docs/MULTI_VERSION_BUILDS.md).

Read applicable repository instructions and the current issue/comments. Initial implementation onboarding reads the maintained Markdown set, including all 37 species and linked evidence. For later issues, inventory actual root/docs files, run the read-only [documentation-sync check](docs/SOURCE_SYNC.md), and read changed/new documents plus the relevant dependencies. Record the actual branch/source revision and preserve unrelated work.

## Procedure

### Report or choose work

Check [existing issues](https://github.com/johnceoskyblock/sparkling-mutuals/issues). Describe expected/actual behavior, reproduction steps, mod/client version and relevant mods. Safari reports benefit from mode, biome, discovery needs, starting resources, late entry and relevant messages or `/sm debug` excerpts. Share useful evidence rather than entire unrelated sessions; remove secrets and personal data.

### Make a focused change

Locate current owners through the [source/test map](docs/README.md#source-and-test-map), not an old issue path. Trace event registration, callers, configuration gates, modes, state transitions and display/report consumers. Existing tests are evidence, not the only description of a live path.

Keep personal/shared captures, sightings, checks, resources and source resolution distinct. [Safari logic](SAFARI_LOGIC.md) owns mode composition; [tracking](docs/critters/TRACKING.md) and species pages supply the exceptions. A new approved requirement can deliberately change that baseline. Explain conflicts; do not silently preserve a bug because an old test expects it, or treat an unverified numeric assumption as a server guarantee.

Follow nearby Kotlin style and package layout. Isolate pure decisions where useful, preserve saved keys, and avoid unrelated formatting/dependency/version changes. Behavior-changing code changes update their owning docs and species exceptions with concise **Last updated** metadata. Keep current requests in GitHub rather than duplicate issue briefs.

### Validate

| Scope | Validation |
| --- | --- |
| Code/build changes | `./gradlew.bat clean buildAllTargets` (Windows) or `./gradlew clean buildAllTargets`; meaningful relevant regression tests |
| Focused investigation | Target-qualified suites, e.g. `:26.1.2:test --tests '*BeeNestSurveyTest'` |
| HUD/rendering/server behavior | Applicable [manual scenarios](docs/IMPLEMENTATION_TEST_CHECKLIST.md); state any live checks not performed |
| Documentation-only change | Source/owner review, links, inventory, consistency and scope checks; a build is not proof of documentation accuracy |

The shared source is under `src/`; per-target output and reports are under `versions/<target>/build/`; verified installable JARs are collected under `build/distributions/<mod-version>/`. Use the wrapper from the repository root; on Unix make it executable if necessary. Development clients use `:<target>:runClient`. Keep your local authentication setup private.

For relevant changes, cover party loot shares, partial entry, starting resources, unloading/reentry, mode/new-run transitions and settings reload. Do not claim a live test from unit-test or native offline evidence. Reconcile current source/docs/issue revisions again before delivery. [Source synchronization](docs/SOURCE_SYNC.md) explains when recording the all-document manifest is justified.

### Submit

Push your branch and open a focused pull request when authorized by your working arrangement. Describe the problem, resulting behavior, issue/evidence links, checks actually performed and remaining limits. Drafts are welcome; no arbitrary prefix or length is required. Respond constructively to review.

Do not commit generated build output, local IDE files, authentication data or raw personal recordings/logs. Preserve [LICENSE](LICENSE), [third-party notices](licenses) and attribution. Version notes belong in the matching [GitHub Release](https://github.com/johnceoskyblock/sparkling-mutuals/releases), including upgrade guidance and validation limits; feature pages describe current behavior rather than accumulating patch histories. Publishing or merging is a separate maintainer action.

## Expected results

A reviewer can trace the requested change to its implementation, current documentation and actual validation. Source review, static checks, automated builds and live Minecraft observations are reported separately. An unresolved proposed correction remains a proposal.

## Troubleshooting

Use Java 25 for the terminal and Gradle. Read [multi-version troubleshooting](docs/MULTI_VERSION_BUILDS.md#troubleshooting) for distribution/CI problems. Read [revision guidance](docs/SOURCE_SYNC.md#troubleshooting) for changed documents. Do not run `DocumentationSync.ps1 -Record` merely to hide warnings, or let a timestamp stand in for source review.

## Last updated

2026-10-19 (America/New_York) — Reorganized the current implementation reference. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; documentation-only changes, no local runtime differences or new live validation.

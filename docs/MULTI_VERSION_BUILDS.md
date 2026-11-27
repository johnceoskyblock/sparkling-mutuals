# Multi-version builds

## Prerequisites

Use Java 25 and the repository Gradle wrapper. Sparkling Mutuals targets these exact Minecraft versions. Install matching Fabric API and Fabric Language Kotlin in the Minecraft instance. HM API is bundled. [Shared versions](../gradle.properties) · [Target properties](../versions)

| Minecraft | Fabric API | Bundled HM API |
| --- | --- | --- |
| 26.1.2 | 0.155.3+26.1.2 | 1.0.3+26.1 |
| 26.2 | 0.161.0+26.2 | 1.0.4+26.2 |
| 26.3 | 0.163.0+26.3 | 1.0.5+26.3 |

## Build and test

| Edition | Gradle value | Filename suffix before `+mc` |
| --- | --- | --- |
| Standard | `standard` (default) | None |
| Full | `full` | `-full` |
| Full No Auto Update | `full-no-auto-update` | `-full-no-auto-update` |

These examples use Windows PowerShell. Use `./gradlew` on Linux or macOS.

```powershell
# Focused checks for one edition and Minecraft version.
.\gradlew.bat :26.3:verifyTargetJar -Pedition=standard
.\gradlew.bat :26.1.2:test -Pedition=full --tests '*BeeNestSurveyTest'

# Build each edition across the three Minecraft versions.
.\gradlew.bat buildAllTargets -Pedition=standard
.\gradlew.bat buildAllTargets -Pedition=full
.\gradlew.bat buildAllTargets -Pedition=full-no-auto-update
.\gradlew.bat verifyReleaseMatrix
```

`buildAllTargets` verifies the selected edition's three final JARs and stages them under `build/distributions/<version>/<edition>`. `verifyReleaseMatrix` verifies all nine JARs and collects them with `build-manifest.json` under `build/release-assets`. Use `verifyDistributions -Pedition=<edition> -PdistributionDir=<folder>` for one collected edition or `verifyReleaseMatrix -PdistributionDir=<folder>` for all nine.

Individual target outputs go under `versions/<target>/build/` for Full and `versions/<target>/build/<edition>/` for the other editions. Install the matching JAR from the collected distribution.

## Develop and launch

Keep shared sources under `src/main` and edition-specific replacements in the edition source sets. Use small Stonecutter conditionals or the `compat` package for demonstrated API differences. Generated source directories belong to the build.

```powershell
.\gradlew.bat :26.3:runClient -Pedition=standard
.\gradlew.bat :26.1.2:runClient -Pedition=full -PclientSmoke
.\gradlew.bat "Set active project to 26.2"
.\gradlew.bat "Reset active project"
```

The repository editor target is 26.1.2. Reset it after changing the IDE target. `-PclientSmoke` disables DevAuth and enables verbose mixin checks. Keep local authentication files out of Git.

## CI and releases

The PR build verifies all nine edition and Minecraft combinations. The downloadable Actions artifacts are named `jar-<edition>-mc<minecraft>`. The aggregate `build` check also covers the shipping script, backend tests, build logic, and Windows updater. [Build workflow](../.github/workflows/build.yml)

Use `Ship.ps1` to publish an authorized stable release. A manual run of the `release` workflow verifies the matrix and produces test assets. The tag-triggered workflow publishes the nine JARs and manifest to GitHub, then publishes the three Standard JARs to Modrinth. [Shipping guide](SHIPPING.md) · [Release workflow](../.github/workflows/release.yml)

## Last updated

2026-11-23 (America/New_York) — Updated build commands, paths, and CI verification for the nine-artifact release matrix.

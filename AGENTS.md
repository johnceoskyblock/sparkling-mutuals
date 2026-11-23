# Sparkling Mutuals

Use `johnceoskyblock/sparkling-mutuals` for every edition. Read the relevant feature docs and check the working tree before editing. Preserve unrelated changes, including untracked files. [Documentation index](docs/README.md) · [Shipping guide](docs/SHIPPING.md)

## Edition boundaries

| Edition | Gradle property | JAR suffix before `+mc` | Behavior |
| --- | --- | --- | --- |
| Standard | `-Pedition=standard` (default) | None | Line-of-sight highlights and local sparkling alerts. Manual updates. |
| Full | `-Pedition=full` | `-full` | Full gameplay features with verified update downloads. Automatic downloads default off. |
| Full No Auto Update | `-Pedition=full-no-auto-update` | `-full-no-auto-update` | Full gameplay features with manual updates. |

- Put tracking, modes, party sync, HUDs and history in `src/main` and shared tests in `src/test`. Use `src/standard` and `src/standardTest` for Standard replacements and exclusions. `src/noUpdates` and `src/noUpdatesTest` cover both editions without the updater. Keep the installer and its tests in `src/updater` and `src/updaterTest`.
- Keep gameplay automation, Sparkle Warping, hidden sparkling alerts, and through-terrain highlights of dynamic targets out of Standard, including their hooks, mixins, commands, settings, and packaged classes. Apply local visibility to sync-driven visuals and critter contacts in capsule previews. Capsule paths, fixed Snoozle wall waypoints, and Torrhus Honeyhive markers based on known locations and refill timers can show through terrain. Check uncertain behavior against Hypixel's guidance. [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications)
- Check both gameplay editions when changing shared settings, commands, rendering, or party sync. For replacements or mixins, check registration, resources, exclusions, and JAR verification as well as the settings UI. Provide Full and Full No Auto Update the same gameplay behavior.
- All editions share `config/sparkling-mutuals`. Keep settings keys stable when changing labels, preserve inactive keys on ordinary saves, and use `standard.` for Standard-only settings. Preserve Standard-only keys when resetting Full, and register new Full-only keys in Standard's reset preservation rules. Keep history, personal bests, profits, and sparkling records out of settings resets.
- Publish all three editions on GitHub. Standard is the only edition intended for Modrinth, where publication is pending. Use the former legit fork as a migration reference. [Edition handoff](docs/EDITION_CONSOLIDATION_HANDOFF.md)

## What’s New and release notes

- Keep current player-facing changes in `docs/releases/NEXT.md` for the next tag. Review the diff from the last release before preparing the notes. Put shared and edition-specific changes under clear headings, use actual settings names, and remove entries that already shipped or were dropped before release. The file can be passed directly to `Ship.ps1 -NotesFile`.
- Add flagship features to `src/main/resources/releases/flagship-highlights.json` under the version that will ship them, using `X.Y.Z` without `v` or a Minecraft suffix. Give each entry an `audience`, `title`, and `details`. Keep older entries for players skipping releases and split mixed entries when wording or availability differs by edition.

| Audience | Standard | Full | Full No Auto Update |
| --- | --- | --- | --- |
| `shared` | Yes | Yes | Yes |
| `standard` | Yes | No | No |
| `full` | No | Yes | Yes |
| `updater` | No | Yes | No |

- `ReleaseHighlights` filters the bundled catalog. `InstalledVersionHistory` saves the installed version, previous version, and acknowledgement for each edition separately under `updates.<edition>.*`. Preserve these values on saves and resets. When changing these paths, check first install, skipped releases, same-version relaunch, and edition switching. Check both the first `/sm` prompt after an upgrade and the manual **What’s New** button. Keep the welcome chat separate from feature highlights.
- When a feature or its name changes, update the relevant player docs and release notes. Use `pwsh -NoProfile -File ./scripts/DocumentationSync.ps1` to audit Markdown changes. Its `-Record` option updates the baseline for the entire documentation set. Use it only after a complete source review. [Source synchronization](docs/SOURCE_SYNC.md)

## Build and ship

Build with Java 25 and keep the Stonecutter targets for Minecraft 26.1.2, 26.2, and 26.3. Run a focused build or the tests for the affected editions while developing. Before tagging, verify the full nine-JAR matrix and smoke-test affected game behavior when possible. Report build and in-game verification separately. [Multi-version builds](docs/MULTI_VERSION_BUILDS.md)

```powershell
.\gradlew.bat buildAllTargets -Pedition=standard
.\gradlew.bat buildAllTargets -Pedition=full
.\gradlew.bat buildAllTargets -Pedition=full-no-auto-update
.\gradlew.bat verifyReleaseMatrix
pwsh -NoProfile -File ./scripts/tests/Ship.Tests.ps1
```

`buildAllTargets` writes each edition to `build/distributions/<version>/<edition>`. `verifyReleaseMatrix` gathers the nine verified JARs and `build-manifest.json` in `build/release-assets`. The GitHub `release` workflow can rehearse the matrix from Actions. Publication runs from a matching version tag in the main repository. [GitHub workflows](.github/workflows)

Use PowerShell 7 and authenticated GitHub CLI for `scripts/Ship.ps1`. Preview with `-WhatIf` and review all staged, unstaged, and untracked files plus unpushed commits. The script commits all non-ignored local changes, rebases on the fetched branch and `origin/master`, opens or updates one PR, waits for matching CI and repository checks, and squash-merges. Use the versioned form only when asked to publish. If asked to prepare a release, update the notes and provide the commands.

```powershell
# Preview a change without creating a release.
pwsh -NoProfile -File ./scripts/Ship.ps1 -Message "Fix Safari highlights" -WhatIf

# Ship the reviewed change through a PR.
pwsh -NoProfile -File ./scripts/Ship.ps1 -Message "Fix Safari highlights"

# Preview and then publish a prepared release. Substitute the intended unused version.
pwsh -NoProfile -File ./scripts/Ship.ps1 -Message "Release v4.4.4" -Version 4.4.4 -NotesFile ./docs/releases/NEXT.md -WhatIf
pwsh -NoProfile -File ./scripts/Ship.ps1 -Message "Release v4.4.4" -Version 4.4.4 -NotesFile ./docs/releases/NEXT.md
```

- In release mode, let `Ship.ps1` bump `mod_version`, create the draft and tag the verified merged commit. The script checks release metadata, downloads, and the manifest after the tag workflow publishes. For a failed run, inspect the reported PR, workflow, tag, and draft before retrying. A partial release with an existing version needs the recovery steps in the shipping guide. [Recovery steps](docs/SHIPPING.md#when-it-stops)

# Pushes and releases without an AI session

Run `scripts/Ship.ps1` in PowerShell 7 to handle the mechanical Git/GitHub steps. It reuses this repository's build and release Actions; no AI is involved in a normal run. Review and test the mod yourself first. The script cannot judge whether a feature works in-game or write accurate feature notes from code.

## One repository for all editions

Ship from `johnceoskyblock/sparkling-mutuals`. Use one PR into protected `master` and one version tag per release. The former Standard fork is a migration reference. [Edition layout](EDITION_CONSOLIDATION_HANDOFF.md)

This repository uses `release_artifact_layout=editions`. The release workflow builds and verifies all nine combinations. GitHub receives all nine Standard, Full, and Full No Auto Update JARs and `build-manifest.json`. Standard uses the plain filename; Full uses `-full` and Full No Auto Update uses `-full-no-auto-update`. Modrinth publication remains pending review.

The plain JAR filename identifies Standard. GitHub releases use `-full` and `-full-no-auto-update` before the `+mc` suffix. Use the first explicitly named Full JAR for manual upgrades from builds whose update checker expects the old filename.

Full keeps update checks and adds a **Download Update** button plus an **Auto Update** toggle (off by default) in the update screen. Downloads must match the release manifest's SHA-256 and the JAR's mod ID, Full edition, version, and exact Minecraft target. Installation waits for the game to exit, preserves the last working JAR outside the mods folder, and supports retry after a failed installation. Standard and Full No Auto Update exclude the downloader and installer classes and controls.

Windows CI also tests restart installation while the running JAR is locked. A live Minecraft smoke test is still recommended before release. Standard publication on Modrinth remains pending review; no Modrinth upload job is enabled.

## Rehearse a release

Run the **release** workflow from the Actions tab using **Run workflow** after the workflow is available on `master`. The manual run uses the selected branch, verifies all nine JARs, and produces the `verified-release-assets` workflow artifact. The publication job requires a matching version tag pushed to the main repository.

To build one edition locally, run one of these commands in PowerShell with Java 25. Omit `-Pedition` for Standard.

```powershell
.\gradlew.bat buildAllTargets -Pedition=standard
.\gradlew.bat buildAllTargets -Pedition=full
.\gradlew.bat buildAllTargets -Pedition=full-no-auto-update
.\gradlew.bat verifyReleaseMatrix
```

Each build verifies all three Minecraft targets for the selected edition and writes them under `build/distributions/<version>/<edition>`. The last command verifies the nine collected JARs and prepares `build/release-assets` with the assets intended for GitHub. Use `-PdistributionDir=<folder>` to verify nine JARs collected elsewhere.

## One-time setup

Install PowerShell 7 and [GitHub CLI](https://cli.github.com/), then authenticate with the account that can push and merge this repository. These installation commands also work in Command Prompt:

```powershell
winget install --id Microsoft.PowerShell --exact --source winget
winget install --id GitHub.cli --exact
# Reopen your terminal after installation.
gh auth login --hostname github.com
gh auth setup-git --hostname github.com
```

Verify PowerShell 7 is available with `pwsh --version` in a newly opened terminal. Codex's bundled PowerShell is private to its environment and does not make `pwsh` available in a regular Command Prompt. Git must also be available. The script uses GitHub CLI's saved authentication, never a token pasted into the script. Normal repository rules, reviews, and required checks apply; it never uses administrator bypasses.

## Push and merge without a release

From the repository folder:

```powershell
# Preview only: no commit, fetch, push, PR, merge, or release.
pwsh -File ./scripts/Ship.ps1 -Message "Fix Safari highlights" -WhatIf

# Run when the listed changes are ready.
pwsh -File ./scripts/Ship.ps1 -Message "Fix Safari highlights"
```

This commits **all tracked and untracked local changes**, including staged changes, into one commit using your message. Files ignored by Git stay ignored. If you are on `master`, it creates a `codex/ship-...` branch. Otherwise it uses your current branch. Existing unpushed branch commits are included too.

It fetches `origin`, rebases onto the equivalent remote branch when present and then `origin/master`, and pushes with a precise lease when rebasing an existing remote branch. It opens or reuses a PR into `master`, waits for the matching commit's build workflow, and squash-merges with an expected-head check. Finally it fast-forwards local `master` when possible. No tag or release is created unless you supply `-Version`.

## Publish a new version

Append flagship features to the in-game highlights catalog under the version that ships them, and keep older entries for players skipping releases. Use a short feature name and a sentence or two on what changed or where to find it. Set `audience` to `shared` for all editions, `standard` for Standard only, `full` for both Full editions, or `updater` for the Full edition with update downloads. Split entries when editions need different wording. Patch-only releases can omit an entry. [In-game highlights catalog](../src/main/resources/releases/flagship-highlights.json)

Review [NEXT.md](releases/NEXT.md) against the last released tag. It is the repository's release-notes draft and can be passed directly to the script. The version below is the prepared release. Substitute the intended unused version when shipping a later release.

```powershell
pwsh -NoProfile -File ./scripts/Ship.ps1 -Message "Release v4.4.4" -Version 4.4.4 -NotesFile ./docs/releases/NEXT.md -WhatIf
pwsh -NoProfile -File ./scripts/Ship.ps1 -Message "Release v4.4.4" -Version 4.4.4 -NotesFile ./docs/releases/NEXT.md
```

Choose an unused version newer than the local and remote `mod_version`. Use `-ReleaseTitle` to set a custom GitHub display title. The default waiting limit is 45 minutes per stage and can be adjusted with `-TimeoutMinutes`.

Release mode also:

1. Bumps `mod_version` before committing.
2. Waits for the PR to merge, then verifies the merged tree exactly matches the tested branch and belongs to remote master.
3. Creates a draft with your notes and tags that verified merged commit.
4. Lets the tag-triggered Actions build, test, and verify Minecraft **26.1.2, 26.2, and 26.3**, then publish the verified JARs.
5. Checks the published release identity, latest-release metadata, tag, notes, and title. Downloads the expected GitHub assets and verifies sizes and SHA-256 hashes. For the consolidated layout, it also checks the manifest's version, Minecraft targets, and JAR sizes and hashes against the verified assets.

Stable releases only: `X.Y.Z`. Same-version re-releases, beta releases, deleting old versions, README edits, and conflict resolution remain manual tasks. A new stable version with the expected assets is discoverable by the mod's existing updater. This script does not run a live Minecraft GUI test.

## When it stops

Conflicts, a moved PR head, new local edits during CI, failed checks, existing tags/releases, or unexpected release assets stop the script. Work, commits, branches, and drafts are preserved; it does not reset or delete them. Temporary verification files are retained under your system temp folder.

- **Rebase conflict:** resolve the reported files and use `git rebase --continue`, or return to the pre-rebase commit with `git rebase --abort`.
- **Checks fail:** use the printed Actions link to inspect logs. Fix the cause or rerun a failed download job in GitHub. Failed checks are never bypassed.
- **Stopped after pushing:** the PR and branch may already exist. Inspect them before rerunning.
- **Stopped during release:** the tag or draft may already exist. Finish that release manually from the printed workflow link; rerunning release mode deliberately refuses the same version. It does not automatically resume a partially published release.
- **Local master advanced while CI ran:** rebase onto the new `origin/master`, push, and rerun checks before merging.

For code review, behavior changes, conflicts, or troubleshooting, an agent can still help. Ordinary commit/PR/merge/release runs only need the script.

## Tests

```powershell
pwsh -NoProfile -File ./scripts/tests/Ship.Tests.ps1
```

The tests use disposable local Git repositories and mocked GitHub/HTTP responses. They exercise preview, rebase, merge, conflict/failure preservation, version/tag guards, release notes, and asset verification without GitHub writes. They also run in the PR build workflow.

CLI references: [PR merge and expected head](https://cli.github.com/manual/gh_pr_merge), [GitHub API requests](https://cli.github.com/manual/gh_api).

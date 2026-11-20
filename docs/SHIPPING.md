# Pushes and releases without an AI session

Run `scripts/Ship.ps1` in PowerShell 7 to handle the mechanical Git/GitHub steps. It reuses this repository's build and release Actions; no AI is involved in a normal run. Review and test the mod yourself first. The script cannot judge whether a feature works in-game or write accurate feature notes from code.

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

Write a short notes file describing the final features and fixes. You can keep it outside the repository so it is not included in the commit. For example, create `$HOME/Desktop/sm-release-notes.txt` in a text editor:

```text
### Features
- Describe the new behavior users can use.

### Fixes
- Describe the bug and the corrected behavior.
```

Then run:

```powershell
pwsh -File ./scripts/Ship.ps1 -Message "Release v4.3.10" -Version 4.3.10 -NotesFile "$HOME/Desktop/sm-release-notes.txt" -WhatIf
pwsh -File ./scripts/Ship.ps1 -Message "Release v4.3.10" -Version 4.3.10 -NotesFile "$HOME/Desktop/sm-release-notes.txt"
```

Choose an unused version newer than the current `mod_version` and remote master. The number above is an example. Optional `-ReleaseTitle "My release v4.3.10"` changes the GitHub display title. `-TimeoutMinutes 60` changes the per-stage waiting limit; the default is 45 minutes.

Release mode also:

1. Bumps `mod_version` before committing.
2. Waits for the PR to merge, then verifies the merged tree exactly matches the tested branch and belongs to remote master.
3. Creates a draft with your notes and tags that verified merged commit.
4. Lets the tag-triggered Actions build, test, and verify Minecraft **26.1.2, 26.2, and 26.3**, then publish the verified JARs.
5. Checks the published release identity, latest-release metadata, tag, notes, and title; downloads all three public JARs and verifies their sizes and SHA-256 hashes.

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

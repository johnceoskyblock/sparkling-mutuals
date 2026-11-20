#requires -Version 7.0
<#
.SYNOPSIS
Commit local changes, rebase, push a PR, and merge after CI. Optionally publish a new stable release.
.EXAMPLE
./scripts/Ship.ps1 -Message 'Fix Safari highlights' -WhatIf
.EXAMPLE
./scripts/Ship.ps1 -Message 'Fix Safari highlights'
.EXAMPLE
./scripts/Ship.ps1 -Message 'Improve Safari highlights' -Version 4.3.10 -NotesFile ./release-notes.txt
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string]$Message,
    [ValidatePattern('^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$')][string]$Version,
    [string]$NotesFile,
    [string]$ReleaseTitle,
    [ValidateRange(1,180)][int]$TimeoutMinutes = 45,
    [string]$RepositoryPath = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
. (Join-Path $PSScriptRoot 'ShipTools.ps1')

$repository = 'johnceoskyblock/sparkling-mutuals'
$api = "repos/$repository"
$temporary = $null
$notes = $null
# Resolve an explicitly supplied notes path relative to the caller, before changing directories.
if ($NotesFile) {
    $notes = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $NotesFile).Path)
    if ([string]::IsNullOrWhiteSpace($notes)) { throw 'The release notes file is empty.' }
}
if ($Version -and !$notes) { throw 'A release requires -NotesFile with readable feature/fix notes.' }
if (!$Version -and ($NotesFile -or $ReleaseTitle)) { throw '-NotesFile and -ReleaseTitle require -Version.' }

Push-Location -LiteralPath $RepositoryPath
try {
    $root = Invoke-ShipNative git @('rev-parse','--show-toplevel')
    Set-Location -LiteralPath $root
    $remote = Invoke-ShipNative git @('config','--get','remote.origin.url')
    if ($remote -notin @("https://github.com/$repository.git", "https://github.com/$repository", "git@github.com:$repository.git")) {
        throw 'origin must be the Sparkling Mutuals GitHub repository.'
    }
    $branch = Invoke-ShipNative git @('symbolic-ref','--short','HEAD')
    foreach ($state in @('rebase-merge','rebase-apply','MERGE_HEAD','CHERRY_PICK_HEAD','REVERT_HEAD')) {
        $statePath = Invoke-ShipNative git @('rev-parse','--git-path',$state)
        if (Test-Path -LiteralPath $statePath) { throw "Finish or abort the existing Git operation ($state) first." }
    }
    Invoke-ShipNative git @('diff','--check') | Out-Host
    Invoke-ShipNative git @('diff','--cached','--check') | Out-Host
    $properties = Join-Path $root 'gradle.properties'
    $originalProperties = [IO.File]::ReadAllText($properties)
    if ($Version) { $null = Set-ShipVersion $originalProperties $Version }
    Write-Host "Repository: $repository"
    Invoke-ShipNative git @('status','--short') | Out-Host
    Write-Host 'Plan: commit ALL tracked/untracked local changes, rebase on the remote branch and master, push, create/update PR, wait for CI, merge.'
    if ($Version) { Write-Host "Then publish v$Version with three verified Minecraft JARs and your notes." }
    else { Write-Host 'No release or tag will be created.' }
    if (!$PSCmdlet.ShouldProcess($repository, $(if ($Version) { "Ship and release v$Version" } else { 'Ship and merge local changes' }))) { return }

    if (!(Get-Command gh -ErrorAction SilentlyContinue)) { throw 'Install GitHub CLI first: winget install --id GitHub.cli --exact; then run gh auth login.' }
    Invoke-ShipNative gh @('auth','status','--hostname','github.com') | Out-Host
    Invoke-ShipNative git @('fetch','origin','--tags') | Out-Host
    $tag = if ($Version) { "v$Version" } else { $null }
    if ($Version) {
        Assert-ShipTagUnused $tag
        $existing = @(Get-ShipReleases | Where-Object tag_name -eq $tag)
        if ($existing.Count) { throw "Release $tag already exists, including drafts. Review it manually; this script never re-releases." }
        # Compare against remote master too, so stale local metadata cannot cause a downgrade.
        $null = Set-ShipVersion (Invoke-ShipNative git @('show','origin/master:gradle.properties')) $Version
    }
    if ($branch -eq 'master') {
        $branch = 'codex/ship-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,6)
        Invoke-ShipNative git @('switch','-c',$branch) | Out-Host
    }
    if ($Version) {
        [IO.File]::WriteAllText($properties,(Set-ShipVersion $originalProperties $Version),[Text.UTF8Encoding]::new($false))
    }
    Invoke-ShipNative git @('add','--all') | Out-Host
    if (Invoke-ShipNative git @('diff','--cached','--name-only')) {
        Invoke-ShipNative git @('commit','-m',$Message) | Out-Host
    }
    # Lease the fetched revision, not a newer remote revision we have not rebased onto.
    $lease = Invoke-ShipNative git @('for-each-ref','--format=%(objectname)',"refs/remotes/origin/$branch")
    if ($lease) { Invoke-ShipNative git @('rebase',"origin/$branch") | Out-Host }
    Invoke-ShipNative git @('rebase','origin/master') | Out-Host
    if ($Version -and (Get-ShipVersion ([IO.File]::ReadAllText($properties))) -ne $Version) {
        throw 'The version changed during rebase. Review gradle.properties before continuing.'
    }
    if (!(Invoke-ShipNative git @('log','--oneline','origin/master..HEAD'))) { throw 'There are no commits to merge.' }
    $tested = Invoke-ShipNative git @('rev-parse','HEAD')
    $push = @('push','-u','origin',"HEAD:refs/heads/$branch")
    if ($lease) { $push += "--force-with-lease=refs/heads/${branch}:$lease" }
    Invoke-ShipNative git $push | Out-Host

    $temporary = Join-Path ([IO.Path]::GetTempPath()) ('sparkling-ship-' + [guid]::NewGuid().ToString('N'))
    $null = New-Item -ItemType Directory -Path $temporary
    $bodyFile = Join-Path $temporary 'pr.md'
    $body = if ($Version) { "$Message`n`n$notes" } else { $Message }
    [IO.File]::WriteAllText($bodyFile,$body)
    $prs = @(Invoke-ShipJson @('pr','list','-R',"github.com/$repository",'--head',$branch,'--base','master','--state','open','--json','number,url'))
    if ($prs.Count -gt 1) { throw 'More than one matching PR exists. Review them manually.' }
    if (!$prs.Count) {
        Invoke-ShipNative gh @('pr','create','-R',"github.com/$repository",'--base','master','--head',$branch,'--title',$Message,'--body-file',$bodyFile) | Out-Host
    }
    $pr = Invoke-ShipJson @('pr','view',$branch,'-R',"github.com/$repository",'--json','number,url,headRefOid')
    if ($pr.headRefOid -ne $tested) { throw 'PR head differs from the pushed commit.' }
    Write-Host "PR: $($pr.url)"
    Wait-ShipWorkflow 'build.yml' $tested $branch 'pull_request' | Out-Host
    Assert-ShipUnchanged $tested
    Invoke-ShipNative git @('fetch','origin') | Out-Host
    Invoke-ShipNative git @('merge-base','--is-ancestor','origin/master',$tested) | Out-Null
    # No --admin: branch rules, reviews, and any additional required checks remain enforced.
    Invoke-ShipNative gh @('pr','merge',"$($pr.number)",'-R',"github.com/$repository",'--squash','--match-head-commit',$tested) | Out-Host
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    do {
        $mergedPr = Invoke-ShipJson @('pr','view',"$($pr.number)",'-R',"github.com/$repository",'--json','state,mergeCommit,headRefOid')
        if ($mergedPr.headRefOid -ne $tested) { throw 'The PR changed while waiting for merge.' }
        if ($mergedPr.state -eq 'MERGED') { break }
        if ($mergedPr.state -eq 'CLOSED') { throw 'PR was closed without merging.' }
        if ((Get-Date) -gt $deadline) { throw "Merge is still pending: $($pr.url). No release tag was created." }
        Start-Sleep -Seconds 15
    } while ($true)
    $merged = $mergedPr.mergeCommit.oid
    Assert-ShipUnchanged $tested
    Invoke-ShipNative git @('fetch','origin') | Out-Host
    Invoke-ShipNative git @('merge-base','--is-ancestor',$merged,'origin/master') | Out-Null
    Invoke-ShipNative git @('diff','--exit-code',$tested,$merged,'--') | Out-Null
    Write-Host "Merged verified commit: $merged"

    if ($Version) {
        Assert-ShipTagUnused $tag
        $payload = Join-Path $temporary 'release.json'
        $title = if ($ReleaseTitle) { $ReleaseTitle } else { $tag }
        @{tag_name=$tag; target_commitish=$merged; name=$title; body=$notes; draft=$true; prerelease=$false} |
            ConvertTo-Json | Set-Content -LiteralPath $payload -Encoding utf8NoBOM
        $draft = Invoke-ShipJson @('api','--hostname','github.com',"$api/releases",'--method','POST','--input',$payload)
        if (!$draft.draft -or $draft.tag_name -ne $tag) { throw 'Expected a same-tag draft release.' }
        Invoke-ShipNative git @('tag',$tag,$merged) | Out-Host
        Invoke-ShipNative git @('push','origin',"refs/tags/$tag") | Out-Host
        Wait-ShipWorkflow 'release.yml' $merged $tag 'push' | Out-Host
        $release = Invoke-ShipJson @('api','--hostname','github.com',"$api/releases/tags/$tag")
        if ($release.draft -or $release.prerelease -or $release.id -ne $draft.id) { throw 'Release identity/publication differs from the staged draft. Review it manually.' }
        Assert-ShipAssets $release.assets $Version
        foreach ($asset in $release.assets) {
            $destination = Join-Path $temporary $asset.name
            Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $destination -MaximumRetryCount 3 -RetryIntervalSec 2
            if ((Get-Item -LiteralPath $destination).Length -ne $asset.size -or
                'sha256:' + (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() -ne $asset.digest) {
                throw "Public JAR failed digest verification: $($asset.name)"
            }
        }
        if ($release.body -ne $notes -or $release.name -ne $title) {
            $notesSnapshot = Join-Path $temporary 'notes.md'
            [IO.File]::WriteAllText($notesSnapshot,$notes)
            Invoke-ShipNative gh @('release','edit',$tag,'-R',"github.com/$repository",'--title',$title,'--notes-file',$notesSnapshot) | Out-Host
            $release = Invoke-ShipJson @('api','--hostname','github.com',"$api/releases/tags/$tag")
            if ($release.body -ne $notes -or $release.name -ne $title) { throw 'Release notes/title did not persist.' }
        }
        $latest = Invoke-ShipJson @('api','--hostname','github.com',"$api/releases/latest")
        if ($latest.id -ne $release.id) { throw 'This release is not the latest stable release. Review it manually.' }
        $remoteTag = Invoke-ShipNative git @('ls-remote','--tags','origin',"refs/tags/$tag")
        if (($remoteTag -split '\s+')[0] -ne $merged) { throw 'The release tag moved during publication.' }
        Write-Host "Release: $($release.html_url)"
        Write-Host 'All three public JAR sizes and SHA-256 hashes verified.'
    }
    Assert-ShipUnchanged $tested
    # Update local master only if it can be fast-forwarded without losing local commits.
    & git merge-base --is-ancestor master origin/master
    if ($LASTEXITCODE -eq 0) {
        Invoke-ShipNative git @('switch','master') | Out-Host
        Invoke-ShipNative git @('merge','--ff-only','origin/master') | Out-Host
    } else { Write-Warning "Local master has separate commits; leaving $branch checked out." }
    Write-Host "Complete: $($pr.url)"
} catch {
    Write-Warning 'Stopped. Local commits, PRs, drafts, and tags are preserved. Review the reported failure before retrying. Rebase conflicts require manual resolution or git rebase --abort. Nothing is reset or deleted.'
    if ($temporary) { Write-Warning "Verification files: $temporary" }
    throw
} finally {
    Pop-Location
}

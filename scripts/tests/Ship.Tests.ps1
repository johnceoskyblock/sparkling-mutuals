#requires -Version 7.0
# No Pester, GitHub credentials, network, or Gradle required. Real Git uses disposable local repositories.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$ship = Join-Path (Split-Path -Parent $PSScriptRoot) 'Ship.ps1'
. (Join-Path (Split-Path -Parent $PSScriptRoot) 'ShipTools.ps1')
$fixtures = Join-Path ([IO.Path]::GetTempPath()) ('ship-tests-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $fixtures
$passed = 0
function Assert($Condition,[string]$Message) { if (!$Condition) { throw $Message } }
function Expect-Failure([scriptblock]$Action,[string]$Pattern) {
    try { & $Action } catch { if ($_.Exception.Message -notmatch $Pattern) { throw }; return }
    throw "Expected failure matching: $Pattern"
}
function Test([string]$Name,[scriptblock]$Action) {
    & $Action
    $script:passed++
    Write-Host "PASS: $Name"
}
function New-Fixture {
    $dir = Join-Path $fixtures ([guid]::NewGuid().ToString('N'))
    $work = Join-Path $dir 'work'
    $bare = Join-Path $dir 'remote.git'
    $null = New-Item -ItemType Directory -Path $work
    Invoke-ShipNative git @('init','--bare','--initial-branch=master',$bare) | Out-Null
    Invoke-ShipNative git @('init','--initial-branch=master',$work) | Out-Null
    Push-Location $work
    try {
        Invoke-ShipNative git @('config','user.email','ship-test@example.invalid') | Out-Null
        Invoke-ShipNative git @('config','user.name','Ship Test') | Out-Null
        [IO.File]::WriteAllText((Join-Path $work 'gradle.properties'),"mod_version=4.3.9`n")
        [IO.File]::WriteAllText((Join-Path $work 'change.txt'),'before')
        Invoke-ShipNative git @('add','--all') | Out-Null
        Invoke-ShipNative git @('commit','-m','Initial') | Out-Null
        Invoke-ShipNative git @('remote','add','origin','https://github.com/johnceoskyblock/sparkling-mutuals.git') | Out-Null
        # Repo-local rewrite routes every git fetch/push to the disposable bare repository.
        $fileUrl = [uri]::new($bare + [IO.Path]::DirectorySeparatorChar).AbsoluteUri.TrimEnd('/')
        Invoke-ShipNative git @('config',"url.$fileUrl.insteadOf",'https://github.com/johnceoskyblock/sparkling-mutuals.git') | Out-Null
        Invoke-ShipNative git @('push','-u','origin','master') | Out-Null
        $head = Invoke-ShipNative git @('rev-parse','HEAD')
    } finally { Pop-Location }
    $global:ShipMock = @{ Work=$work; Bare=$bare; Initial=$head; Calls=0; Merges=0; FailBuild=$false;
        Merged=$null; Release=$null; ReleaseTag=$null; BadAssets=$false; DirtyDuringBuild=$false;
        StaleMetadataReads=0; MetadataReads=0; Editions=$false; Manifest=$null; BadManifest=$false }
    return $work
}

# These mocks shadow executable gh and the HTTP cmdlet only within this test process.
function global:gh {
    $a = @($args)
    $global:LASTEXITCODE = 0
    $s = $global:ShipMock
    $s.Calls++
    if ($a[0] -eq 'auth') { return }
    if ($a[0] -eq 'pr') {
        switch ($a[1]) {
            'list' { return '[]' }
            'create' { return 'https://github.com/example/test/pull/1' }
            'view' {
                $head = Invoke-ShipNative git @('rev-parse','HEAD')
                return @{ number=1; url='https://github.com/example/test/pull/1'; headRefOid=$head;
                    state=$(if ($s.Merged) {'MERGED'} else {'OPEN'}); mergeCommit=@{oid=$s.Merged} } | ConvertTo-Json -Compress
            }
            'merge' {
                $s.Merges++
                $tree = Invoke-ShipNative git @('rev-parse','HEAD^{tree}')
                $parent = Invoke-ShipNative git @('--git-dir',$s.Bare,'rev-parse','master')
                $s.Merged = Invoke-ShipNative git @('--git-dir',$s.Bare,'-c','user.name=Ship Test','-c','user.email=test@example.invalid',
                    'commit-tree',$tree,'-p',$parent,'-m','Simulated squash merge')
                Invoke-ShipNative git @('--git-dir',$s.Bare,'update-ref','refs/heads/master',$s.Merged) | Out-Null
                return
            }
        }
    }
    if ($a[0] -eq 'api') {
        $route = $a[3]
        if ($route -match '/actions/workflows/(build|release)\.yml/runs\?head_sha=([a-f0-9]+)&event=([^&]+)') {
            $workflow=$Matches[1]; $sha=$Matches[2]; $event=$Matches[3]
            if ($workflow -eq 'build' -and $s.DirtyDuringBuild) {
                [IO.File]::WriteAllText((Join-Path $s.Work 'change.txt'),'changed while CI ran')
            }
            $branch = if ($workflow -eq 'release') { $s.ReleaseTag } else { Invoke-ShipNative git @('symbolic-ref','--short','HEAD') }
            return @{workflow_runs=@(@{id=1;head_sha=$sha;head_branch=$branch;event=$event;status='completed';
                conclusion=$(if ($s.FailBuild) {'failure'} else {'success'});html_url='https://github.com/example/test/actions/runs/1'})} | ConvertTo-Json -Depth 8 -Compress
        }
        if ($route -match '/releases\?') { return '[[]]' }
        if ($route -match '/releases$' -and $a -contains 'POST') {
            $inputIndex = [array]::IndexOf($a,'--input') + 1
            $r = Get-Content -LiteralPath $a[$inputIndex] -Raw | ConvertFrom-Json
            $s.ReleaseTag = $r.tag_name
            $version = $r.tag_name.Substring(1)
            $baseUrl = "https://github.com/johnceoskyblock/sparkling-mutuals/releases/download/$($r.tag_name)"
            $hash = 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad'
            $jars = @(foreach ($minecraft in @('26.1.2','26.2','26.3')) {
                @{name="sparkling-mutuals-$version+mc$minecraft.jar"; minecraft=$minecraft; size=3; sha256=$hash}
                if ($s.Editions) {
                    @{name="sparkling-mutuals-$version-full-no-auto-update+mc$minecraft.jar"; minecraft=$minecraft; size=3; sha256=$hash}
                }
            })
            $assets = @($jars | ForEach-Object {
                @{name=$_.name; size=$_.size; digest="sha256:$($_.sha256)"; browser_download_url="$baseUrl/$([uri]::EscapeDataString($_.name))"}
            })
            if ($s.Editions) {
                if ($s.BadManifest) { $jars[0].sha256 = '0' * 64 }
                $s.Manifest = @{version=$version; files=$jars} | ConvertTo-Json -Depth 8 -Compress
                $bytes = [Text.Encoding]::UTF8.GetBytes($s.Manifest)
                $sha256 = [Security.Cryptography.SHA256]::Create()
                try { $manifestHash = [BitConverter]::ToString($sha256.ComputeHash($bytes)).Replace('-','').ToLowerInvariant() }
                finally { $sha256.Dispose() }
                $assets += @{name='build-manifest.json'; size=$bytes.Length; digest="sha256:$manifestHash";
                    browser_download_url="$baseUrl/build-manifest.json"}
            }
            if ($s.BadAssets) { $assets = @($assets[0],$assets[1]) }
            $s.Release = @{id=10;draft=$false;prerelease=$false;body=$r.body;name=$r.name;tag_name=$r.tag_name;
                html_url="https://github.com/example/test/releases/tag/$($r.tag_name)";assets=$assets}
            return @{id=10;draft=$true;tag_name=$r.tag_name} | ConvertTo-Json -Compress
        }
        if ($route -match '/releases/10$') {
            $s.MetadataReads++
            if ($s.StaleMetadataReads -gt 0) {
                $s.StaleMetadataReads--
                $stale = $s.Release.Clone()
                $stale.body = 'Stale release notes'
                return $stale | ConvertTo-Json -Depth 8 -Compress
            }
            return $s.Release | ConvertTo-Json -Depth 8 -Compress
        }
        if ($route -match '/releases/(tags/[^/]+|latest)$') { return $s.Release | ConvertTo-Json -Depth 8 -Compress }
    }
    throw "Unexpected mock gh command: $($a -join ' ')"
}
function global:Invoke-WebRequest {
    param($Uri,$OutFile,$MaximumRetryCount,$RetryIntervalSec)
    if (!$Uri.StartsWith('https://github.com/johnceoskyblock/sparkling-mutuals/releases/download/')) { throw 'Unexpected test HTTP URL' }
    $content = if ($Uri.EndsWith('/build-manifest.json')) { $global:ShipMock.Manifest } else { 'abc' }
    [IO.File]::WriteAllText($OutFile,$content)
}

try {
    Test 'Version bump preserves CRLF and rejects equal/older releases' {
        Assert ((Set-ShipVersion "x=1`r`nmod_version=4.3.9`r`n" '4.3.10') -ceq "x=1`r`nmod_version=4.3.10`r`n") 'Line endings changed'
        Expect-Failure { Set-ShipVersion 'mod_version=4.3.9' '4.3.9' } 'must be newer'
        Expect-Failure { Set-ShipVersion 'mod_version=4.3.9' '4.2.9' } 'must be newer'
        Assert ((Set-ShipVersion 'mod_version=4.4.0-beta.1' '4.4.0') -eq 'mod_version=4.4.0') 'Beta promotion failed'
    }
    Test 'Notes comparison tolerates line endings but preserves Markdown content and case' {
        Assert (Test-ShipNotes "## Features`r`n- Fix`r`n" "## Features`n- Fix") 'Line endings were treated as a content change'
        Assert (!(Test-ShipNotes 'Fix A' 'Fix B')) 'Different notes were accepted'
        Assert (!(Test-ShipNotes 'FIX' 'Fix')) 'Changed case was accepted'
        Assert (!(Test-ShipNotes "Line  `nNext" "Line`nNext")) 'Meaningful Markdown spaces were ignored'
    }
    Test 'Metadata verification waits for stale reads without republishing' {
        $null = New-Fixture
        $api = 'repos/johnceoskyblock/sparkling-mutuals'
        $global:ShipMock.Release = @{id=10;tag_name='v4.3.10';name='v4.3.10';draft=$false;prerelease=$false;body="Feature notes`r`n"}
        $global:ShipMock.StaleMetadataReads = 2
        $release = Wait-ShipReleaseMetadata 'v4.3.10' 10 'v4.3.10' "Feature notes`n" -DelaySeconds 0
        Assert ($release.id -eq 10 -and $global:ShipMock.MetadataReads -eq 3) 'Stale reads were not retried correctly'
    }
    Test 'Metadata verification fails after its bounded retries for genuinely different notes' {
        $null = New-Fixture
        $api = 'repos/johnceoskyblock/sparkling-mutuals'
        $global:ShipMock.Release = @{id=10;tag_name='v4.3.10';name='v4.3.10';draft=$false;prerelease=$false;body='Wrong notes'}
        Expect-Failure { Wait-ShipReleaseMetadata 'v4.3.10' 10 'v4.3.10' 'Expected notes' -DelaySeconds 0 } 'after 5 reads'
        Assert ($global:ShipMock.MetadataReads -eq 5) 'Metadata read retries were unbounded'
    }
    Test 'WhatIf leaves HEAD, branch, files, and GitHub untouched' {
        $work = New-Fixture
        [IO.File]::WriteAllText((Join-Path $work 'change.txt'),'local')
        $before = Invoke-ShipNative git @('-C',$work,'status','--porcelain')
        & $ship -RepositoryPath $work -Message 'Preview' -WhatIf
        Assert ((Invoke-ShipNative git @('-C',$work,'rev-parse','HEAD')) -eq $global:ShipMock.Initial) 'Preview committed'
        Assert ((Invoke-ShipNative git @('-C',$work,'status','--porcelain')) -eq $before) 'Preview changed files'
        Assert ($global:ShipMock.Calls -eq 0) 'Preview called GitHub'
    }
    Test 'Push-only ships tracked/untracked changes through a tested PR without tags' {
        $work = New-Fixture
        [IO.File]::WriteAllText((Join-Path $work 'change.txt'),'changed')
        [IO.File]::WriteAllText((Join-Path $work 'new.txt'),'new')
        & $ship -RepositoryPath $work -Message 'Ship fixture'
        Assert ($global:ShipMock.Merges -eq 1) 'PR not merged'
        Assert ((Invoke-ShipNative git @('-C',$work,'rev-parse','HEAD')) -eq $global:ShipMock.Merged) 'Master not synced'
        Assert (!(Invoke-ShipNative git @('-C',$work,'status','--porcelain'))) 'Working tree not clean'
        Assert (!(Invoke-ShipNative git @('--git-dir',$global:ShipMock.Bare,'tag','--list'))) 'Push-only created a tag'
        Assert (!$global:ShipMock.Release) 'Push-only created a release'
    }
    Test 'Failed CI preserves committed work and never merges or tags' {
        $work = New-Fixture
        $global:ShipMock.FailBuild = $true
        [IO.File]::WriteAllText((Join-Path $work 'change.txt'),'kept')
        Expect-Failure { & $ship -RepositoryPath $work -Message 'Fail fixture' } 'Workflow build.yml failed'
        Assert ($global:ShipMock.Merges -eq 0) 'Merged failed CI'
        Assert ((Invoke-ShipNative git @('--git-dir',$global:ShipMock.Bare,'rev-parse','master')) -eq $global:ShipMock.Initial) 'Remote master changed'
        Assert (([IO.File]::ReadAllText((Join-Path $work 'change.txt'))) -eq 'kept') 'Work lost'
    }
    Test 'Existing remote feature branch uses a lease when pushed after rebase' {
        $work = New-Fixture
        Invoke-ShipNative git @('-C',$work,'switch','-c','codex/existing') | Out-Null
        Invoke-ShipNative git @('-C',$work,'push','-u','origin','codex/existing') | Out-Null
        [IO.File]::WriteAllText((Join-Path $work 'change.txt'),'feature edit')
        & $ship -RepositoryPath $work -Message 'Existing branch fixture'
        Assert ($global:ShipMock.Merges -eq 1) 'Existing feature branch was not merged'
    }
    Test 'Remote master changes are rebased before pushing' {
        $work = New-Fixture
        $bare = $global:ShipMock.Bare
        $tree = Invoke-ShipNative git @('--git-dir',$bare,'rev-parse','master^{tree}')
        $advanced = Invoke-ShipNative git @('--git-dir',$bare,'-c','user.name=Ship Test','-c','user.email=test@example.invalid',
            'commit-tree',$tree,'-p',$global:ShipMock.Initial,'-m','Remote advanced')
        Invoke-ShipNative git @('--git-dir',$bare,'update-ref','refs/heads/master',$advanced) | Out-Null
        [IO.File]::WriteAllText((Join-Path $work 'change.txt'),'local work')
        & $ship -RepositoryPath $work -Message 'Rebase fixture'
        Invoke-ShipNative git @('-C',$work,'merge-base','--is-ancestor',$advanced,'HEAD') | Out-Null
        Assert ($global:ShipMock.Merges -eq 1) 'Rebased PR was not merged'
    }
    Test 'Rebase conflict stops before push or merge and keeps the local commit' {
        $work = New-Fixture
        $other = Join-Path (Split-Path -Parent $work) 'other'
        Invoke-ShipNative git @('clone',$global:ShipMock.Bare,$other) | Out-Null
        [IO.File]::WriteAllText((Join-Path $other 'change.txt'),'remote conflict')
        Invoke-ShipNative git @('-C',$other,'add','--all') | Out-Null
        Invoke-ShipNative git @('-C',$other,'-c','user.name=Ship Test','-c','user.email=test@example.invalid','commit','-m','Remote edit') | Out-Null
        Invoke-ShipNative git @('-C',$other,'push','origin','master') | Out-Null
        [IO.File]::WriteAllText((Join-Path $work 'change.txt'),'local conflict')
        Expect-Failure { & $ship -RepositoryPath $work -Message 'Conflict fixture' } 'git failed.*rebase'
        Assert ($global:ShipMock.Merges -eq 0) 'Merged a conflict'
        Assert ((Invoke-ShipNative git @('-C',$work,'show','ORIG_HEAD:change.txt')) -eq 'local conflict') 'Local commit was lost'
        Assert ((Invoke-ShipNative git @('--git-dir',$global:ShipMock.Bare,'for-each-ref','--format=%(refname)','refs/heads/codex/')) -eq '') 'Pushed a conflicted branch'
    }
    Test 'New edits during CI stop the merge and remain on disk' {
        $work = New-Fixture
        $global:ShipMock.DirtyDuringBuild = $true
        [IO.File]::WriteAllText((Join-Path $work 'change.txt'),'initial edit')
        Expect-Failure { & $ship -RepositoryPath $work -Message 'Race fixture' } 'Local files or HEAD changed'
        Assert ($global:ShipMock.Merges -eq 0) 'Merged while local work changed'
        Assert (([IO.File]::ReadAllText((Join-Path $work 'change.txt'))) -eq 'changed while CI ran') 'Concurrent edits were lost'
    }
    Test 'Release requires notes and rejects an existing remote tag before committing' {
        $work = New-Fixture
        Expect-Failure { & $ship -RepositoryPath $work -Message 'Release' -Version 4.3.10 } 'requires -NotesFile'
        $notes = Join-Path $fixtures 'notes.txt'
        [IO.File]::WriteAllText($notes,'Feature notes')
        Invoke-ShipNative git @('-C',$work,'tag','v4.3.10') | Out-Null
        Invoke-ShipNative git @('-C',$work,'push','origin','refs/tags/v4.3.10') | Out-Null
        Invoke-ShipNative git @('-C',$work,'tag','-d','v4.3.10') | Out-Null
        Expect-Failure { & $ship -RepositoryPath $work -Message 'Release' -Version 4.3.10 -NotesFile $notes } 'Tag v4.3.10 already exists'
        Assert ((Invoke-ShipNative git @('-C',$work,'rev-parse','HEAD')) -eq $global:ShipMock.Initial) 'Committed despite existing tag'
    }
    Test 'Release tags the verified merged commit and checks all three public JAR digests' {
        $work = New-Fixture
        $notes = Join-Path $fixtures 'notes.txt'
        [IO.File]::WriteAllText($notes,"Feature one`nFeature two")
        & $ship -RepositoryPath $work -Message 'Release fixture' -Version 4.3.10 -NotesFile $notes
        Assert ((Invoke-ShipNative git @('--git-dir',$global:ShipMock.Bare,'rev-parse','v4.3.10')) -eq $global:ShipMock.Merged) 'Tagged unmerged commit'
        Assert ($global:ShipMock.Release.body -eq "Feature one`nFeature two") 'Notes lost newlines'
        Assert ((Get-ShipVersion ([IO.File]::ReadAllText((Join-Path $work 'gradle.properties')))) -eq '4.3.10') 'Version not bumped'
    }
    Test 'Incomplete release assets fail verification' {
        $work = New-Fixture
        $global:ShipMock.BadAssets = $true
        $notes = Join-Path $fixtures 'notes.txt'
        Expect-Failure { & $ship -RepositoryPath $work -Message 'Bad release' -Version 4.3.10 -NotesFile $notes } 'exactly the three expected'
    }
    Test 'Consolidated release verifies six JARs and manifest at the merged tag' {
        $work = New-Fixture
        $global:ShipMock.Editions = $true
        Add-Content -LiteralPath (Join-Path $work 'gradle.properties') -Value 'release_artifact_layout=editions'
        $notes = Join-Path $fixtures 'notes.txt'
        & $ship -RepositoryPath $work -Message 'Consolidated release' -Version 4.3.10 -NotesFile $notes
        Assert ($global:ShipMock.Release.assets.Count -eq 7) 'Wrong number of public assets'
        Assert ((Invoke-ShipNative git @('--git-dir',$global:ShipMock.Bare,'rev-parse','v4.3.10')) -eq $global:ShipMock.Merged) 'Tagged unmerged commit'
    }
    Test 'Consolidated release rejects a published manifest with the wrong JAR hash' {
        $work = New-Fixture
        $global:ShipMock.Editions = $true
        $global:ShipMock.BadManifest = $true
        Add-Content -LiteralPath (Join-Path $work 'gradle.properties') -Value 'release_artifact_layout=editions'
        $notes = Join-Path $fixtures 'notes.txt'
        Expect-Failure { & $ship -RepositoryPath $work -Message 'Bad manifest' -Version 4.3.10 -NotesFile $notes } 'public manifest disagrees'
    }
    Write-Host "$passed shipping script tests passed. No real GitHub or HTTP requests were made."
} finally {
    Remove-Item Function:\gh,Function:\Invoke-WebRequest -ErrorAction SilentlyContinue
    Remove-Variable ShipMock -Scope Global -ErrorAction SilentlyContinue
    # Keep disposable fixture paths for failure diagnosis; they contain no credentials.
    Write-Host "Test fixtures: $fixtures"
}

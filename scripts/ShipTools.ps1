# Internal helpers for Ship.ps1; dot-sourcing this file performs no operations.
function Invoke-ShipNative([string]$Program, [string[]]$Arguments) {
    $output = & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed (exit $LASTEXITCODE): $($Arguments -join ' ')" }
    return ($output -join "`n").TrimEnd()
}
function Invoke-ShipJson([string[]]$Arguments) {
    Invoke-ShipNative gh $Arguments | ConvertFrom-Json -Depth 100
}
function Test-ShipNotes([string]$Actual,[string]$Expected) {
    # Preserve Markdown spaces/content; normalize only line endings and final newlines.
    $actualText = $Actual.Replace("`r`n","`n").Replace("`r","`n").TrimEnd([char[]]"`n")
    $expectedText = $Expected.Replace("`r`n","`n").Replace("`r","`n").TrimEnd([char[]]"`n")
    return $actualText -ceq $expectedText
}
function Wait-ShipReleaseMetadata(
    [string]$Tag,[long]$ReleaseId,[string]$Title,[string]$Notes,
    [ValidateRange(1,10)][int]$Attempts = 5,
    [ValidateRange(0,30)][int]$DelaySeconds = 2
) {
    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        $current = Invoke-ShipJson @('api','--hostname','github.com',"$api/releases/$ReleaseId",'--header','Cache-Control: no-cache')
        if ($current.id -ne $ReleaseId -or $current.tag_name -cne $Tag -or $current.draft -or $current.prerelease) {
            throw 'Release identity/publication changed during metadata verification. Review it manually.'
        }
        if ($current.name -ceq $Title -and (Test-ShipNotes $current.body $Notes)) { return $current }
        if ($attempt -lt $Attempts) { Start-Sleep -Seconds $DelaySeconds }
    }
    throw "Release $Tag is published, but notes/title could not be confirmed after $Attempts reads. Inspect its page; do not rerun release mode."
}
function Get-ShipVersion([string]$Properties) {
    $matches = [regex]::Matches($Properties,'(?m)^mod_version=([^\r\n]+)\r?$')
    if ($matches.Count -ne 1) { throw 'Expected exactly one mod_version in gradle.properties.' }
    return $matches[0].Groups[1].Value
}
function Set-ShipVersion([string]$Properties,[string]$Requested) {
    $current = Get-ShipVersion $Properties
    $base = ($current -split '-')[0]
    if ($base -notmatch '^\d+\.\d+\.\d+$' -or [version]$Requested -lt [version]$base -or
        ([version]$Requested -eq [version]$base -and $current -notmatch '-beta\.\d+$')) {
        throw "Release version $Requested must be newer than $current. Same-version re-releases are manual."
    }
    return [regex]::Replace($Properties,'(?m)^mod_version=[^\r\n]+',"mod_version=$Requested")
}
function Assert-ShipTagUnused([string]$Tag) {
    if ((Invoke-ShipNative git @('tag','--list',$Tag)) -or
        (Invoke-ShipNative git @('ls-remote','--tags','origin',"refs/tags/$Tag"))) {
        throw "Tag $Tag already exists. It will not be moved or overwritten."
    }
}
function Get-ShipReleases {
    $pages = Invoke-ShipJson @('api','--hostname','github.com',"$api/releases?per_page=100",'--paginate','--slurp')
    foreach ($page in $pages) { foreach ($release in $page) { $release } }
}
function Assert-ShipUnchanged([string]$Expected) {
    if ((Invoke-ShipNative git @('rev-parse','HEAD')) -ne $Expected -or
        (Invoke-ShipNative git @('status','--porcelain','--untracked-files=all'))) {
        throw 'Local files or HEAD changed during the operation. No further steps will run.'
    }
}
function Wait-ShipWorkflow([string]$Workflow,[string]$Sha,[string]$Branch,[string]$Event) {
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    $runId = $null
    do {
        $runs = Invoke-ShipJson @('api','--hostname','github.com',"$api/actions/workflows/$Workflow/runs?head_sha=$Sha&event=$Event&per_page=100")
        $run = @($runs.workflow_runs | Where-Object { $_.head_sha -eq $Sha -and $_.head_branch -eq $Branch -and $_.event -eq $Event } |
            Sort-Object id -Descending | Select-Object -First 1)
        if ($run.Count) {
            $current = $run[0]
            if ($runId -ne $current.id) { $runId = $current.id; Write-Host "Waiting for $Workflow : $($current.html_url)" }
            if ($current.status -eq 'completed') {
                if ($current.conclusion -ne 'success') { throw "Workflow $Workflow failed ($($current.conclusion)): $($current.html_url). Fix/retry it in GitHub; no automatic failure bypass." }
                return $current.html_url
            }
        }
        if ((Get-Date) -gt $deadline) { throw "Timed out waiting for $Workflow at $Sha. Inspect GitHub Actions before retrying." }
        Start-Sleep -Seconds 15
    } while ($true)
}
function Get-ShipReleaseLayout([string]$Properties) {
    $entries = [regex]::Matches($Properties,'(?m)^[ \t]*release_artifact_layout[ \t]*=[ \t]*([^\r\n]*)')
    if (!$entries.Count) { return 'legacy' }
    if ($entries.Count -eq 1 -and $entries[0].Groups[1].Value.Trim() -ceq 'editions') { return 'editions' }
    throw 'Expected release_artifact_layout=editions once in gradle.properties, or omit it for the legacy release layout.'
}
function Get-ShipJarNames([string]$Version,[string]$Layout) {
    foreach ($minecraft in @('26.1.2','26.2','26.3')) {
        $suffix = if ($Layout -eq 'editions') { '-full' } else { '' }
        "sparkling-mutuals-$Version$suffix+mc$minecraft.jar"
        if ($Layout -eq 'editions') { "sparkling-mutuals-$Version-full-no-auto-update+mc$minecraft.jar" }
    }
}
function Assert-ShipAssets($Assets,[string]$Version,[string]$Layout) {
    $expected = @(Get-ShipJarNames $Version $Layout)
    if ($Layout -eq 'editions') { $expected += 'build-manifest.json' }
    $names = @($Assets | ForEach-Object { $_.name })
    if ($names.Count -ne $expected.Count -or @(Compare-Object $expected $names -CaseSensitive).Count) {
        if ($Layout -eq 'legacy') { throw 'Release must contain exactly the three expected Minecraft JARs.' }
        throw 'Release must contain exactly the six expected GitHub JARs and build-manifest.json.'
    }
    $downloadBase = "https://github.com/johnceoskyblock/sparkling-mutuals/releases/download/v$Version"
    foreach ($asset in $Assets) {
        $encodedName = [uri]::EscapeDataString($asset.name)
        if ($asset.size -le 0 -or $asset.digest -notmatch '^sha256:[a-f0-9]{64}$' -or
            $asset.browser_download_url -cnotin @("$downloadBase/$($asset.name)", "$downloadBase/$encodedName")) {
            throw "Invalid public asset metadata: $($asset.name)"
        }
    }
}
function Assert-ShipManifest([string]$ManifestPath,$Assets,[string]$Version) {
    $manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json -AsHashtable
    $jars = @($Assets | Where-Object name -cne 'build-manifest.json')
    if ($manifest.version -cne $Version -or $manifest.files -isnot [array] -or $manifest.files.Count -ne $jars.Count) {
        throw 'The public manifest must describe the six GitHub JARs for the tagged version.'
    }
    foreach ($asset in $jars) {
        $rows = @($manifest.files | Where-Object { $_.name -ceq $asset.name })
        $minecraft = ($asset.name -split '\+mc')[1] -replace '\.jar$',''
        if ($rows.Count -ne 1 -or $rows[0].minecraft -cne $minecraft -or $rows[0].size -ne $asset.size -or
            "sha256:$($rows[0].sha256)" -ne $asset.digest) {
            throw "The public manifest disagrees with the verified JAR: $($asset.name)"
        }
    }
}

# Internal helpers for Ship.ps1; dot-sourcing this file performs no operations.
function Invoke-ShipNative([string]$Program, [string[]]$Arguments) {
    $output = & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed (exit $LASTEXITCODE): $($Arguments -join ' ')" }
    return ($output -join "`n").TrimEnd()
}
function Invoke-ShipJson([string[]]$Arguments) {
    Invoke-ShipNative gh $Arguments | ConvertFrom-Json -Depth 100
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
function Assert-ShipAssets($Assets,[string]$Version) {
    $expected = @('26.1.2','26.2','26.3' | ForEach-Object { "sparkling-mutuals-$Version+mc$_.jar" })
    if (@($Assets).Count -ne 3 -or @(Compare-Object ($Assets.name | Sort-Object) ($expected | Sort-Object)).Count) {
        throw 'Release must contain exactly the three expected Minecraft JARs.'
    }
    foreach ($asset in $Assets) {
        if ($asset.size -le 0 -or $asset.digest -notmatch '^sha256:[a-f0-9]{64}$' -or
            !$asset.browser_download_url.StartsWith("https://github.com/johnceoskyblock/sparkling-mutuals/releases/download/v$Version/")) {
            throw "Invalid public asset metadata: $($asset.name)"
        }
    }
}

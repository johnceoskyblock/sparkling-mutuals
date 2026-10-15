param([switch]$Record)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$manifestPath = Join-Path $repoRoot 'docs/source-documentation.json'
$documents = @(
    Get-ChildItem -LiteralPath $repoRoot -File -Filter '*.md'
    Get-ChildItem -LiteralPath (Join-Path $repoRoot 'docs') -File -Filter '*.md' -Recurse
)
$current = [ordered]@{}
foreach ($document in ($documents | Sort-Object FullName)) {
    $relative = [IO.Path]::GetRelativePath($repoRoot, $document.FullName).Replace('\', '/')
    # Git checkouts can change LF to CRLF without changing the document revision.
    $normalizedText = [IO.File]::ReadAllText($document.FullName).Replace("`r`n", "`n")
    $current[$relative] = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($normalizedText)))
}
if ($Record) {
    # Run only after reviewing the changed rules and implementing/testing the corresponding source.
    [ordered]@{
        recordedAt = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow, 'America/New_York').ToString('o')
        documents = $current
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding utf8NoBOM
    Write-Output "Recorded source documentation baseline for $($current.Count) documents."
    return
}
$previous = @{}
if (Test-Path -LiteralPath $manifestPath) {
    $previous = (Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json -AsHashtable).documents
}
$changes = @(
    foreach ($path in $current.Keys) {
        if (!$previous.Contains($path)) { [pscustomobject]@{ Status = 'New'; Document = $path } }
        elseif ($previous[$path] -ne $current[$path]) { [pscustomobject]@{ Status = 'Updated'; Document = $path } }
    }
    foreach ($path in $previous.Keys) {
        if (!$current.Contains($path)) { [pscustomobject]@{ Status = 'Removed'; Document = $path } }
    }
)
if ($changes.Count -eq 0) { Write-Output 'No documentation changed since the last source synchronization.' }
else { $changes | Format-Table -AutoSize }

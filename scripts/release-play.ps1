# Repo-local Play release shortcut. Full docs: RELEASING.md
#   powershell scripts/release-play.ps1 -Action <internal|closed|production|status|verify>
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('internal', 'closed', 'production', 'status', 'verify')]
    [string]$Action,
    [switch]$ConfirmProduction,
    [string]$Notes = ''
)
$root = Split-Path -Parent $PSScriptRoot
$tool = Join-Path $env:USERPROFILE '.eyc\bin\eyc-release.ps1'
if (-not (Test-Path -LiteralPath $tool)) {
    Write-Error 'EYC infrastructure missing. Read %USERPROFILE%\.eyc\docs\AGENT_GUIDE.md'
    exit 1
}
& $tool -Action $Action -ProjectDir $root -ConfirmProduction:$ConfirmProduction -Notes $Notes
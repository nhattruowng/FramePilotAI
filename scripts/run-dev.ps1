param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly
)

. "$PSScriptRoot\lib\framepilot-common.ps1"

$repoRoot = Get-FramePilotRoot
Use-FramePilotJava
Use-FramePilotNode

if (-not $FrontendOnly) {
    Write-Section "Start backend"
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$repoRoot'; .\gradlew.bat :apps:local-server:bootRun"
    ) | Out-Null
}

if (-not $BackendOnly) {
    Write-Section "Start frontend"
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$repoRoot'; corepack pnpm --dir apps/desktop-ui dev"
    ) | Out-Null
}

Write-Host "Development processes started in separate PowerShell windows."

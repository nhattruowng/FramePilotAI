param(
    [switch]$IncludeCargoCheck,
    [switch]$IncludeSmoke
)

. "$PSScriptRoot\lib\framepilot-common.ps1"

$repoRoot = Get-FramePilotRoot
Use-FramePilotJava
Use-FramePilotNode
Set-Location $repoRoot

Write-Section "Backend tests"
.\gradlew.bat :apps:local-server:test

Write-Section "Frontend build verification"
corepack pnpm --dir apps/desktop-ui build

if ($IncludeCargoCheck -and (Get-Command cargo -ErrorAction SilentlyContinue)) {
    Write-Section "Cargo check"
    Set-Location (Join-Path $repoRoot "apps\desktop-ui\src-tauri")
    cargo check
    Set-Location $repoRoot
}

if ($IncludeSmoke) {
    Write-Section "Smoke test"
    & (Join-Path $repoRoot "scripts\smoke-test.ps1")
}

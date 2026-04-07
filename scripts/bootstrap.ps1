param(
    [switch]$SkipFrontendInstall
)

. "$PSScriptRoot\lib\framepilot-common.ps1"

$repoRoot = Get-FramePilotRoot
Write-Section "Bootstrap"
Use-FramePilotJava
Use-FramePilotNode

Write-Section "Prepare directories"
Ensure-Directory -PathValue (Join-Path $repoRoot "storage")
Ensure-Directory -PathValue (Join-Path $repoRoot "storage\exports")
Ensure-Directory -PathValue (Join-Path $repoRoot "storage\projects")
Ensure-Directory -PathValue (Join-Path $repoRoot "tmp")
Ensure-Directory -PathValue (Join-Path $repoRoot "apps\desktop-ui\src-tauri\resources\backend")

Write-Section "Frontend dependencies"
if (-not $SkipFrontendInstall) {
    Set-Location $repoRoot
    corepack pnpm install --dir apps/desktop-ui
}

Write-Section "Environment hints"
Write-Host "Repo root: $repoRoot"
Write-Host "Set FRAMEPILOT_FFMPEG to override FFmpeg path if needed."
Write-Host "Set FRAMEPILOT_SKIP_BUNDLED_BACKEND=1 when running Tauri against a separately started backend."

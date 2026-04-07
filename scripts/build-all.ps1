param(
    [switch]$IncludeTauriBundle
)

. "$PSScriptRoot\lib\framepilot-common.ps1"

$repoRoot = Get-FramePilotRoot
Use-FramePilotJava
Use-FramePilotNode
Set-Location $repoRoot

Write-Section "Build backend jar"
.\gradlew.bat :apps:local-server:bootJar

Write-Section "Stage backend resource for Tauri"
Stage-TauriBackendResource -RepoRoot $repoRoot

Write-Section "Build frontend"
corepack pnpm --dir apps/desktop-ui build

if ($IncludeTauriBundle) {
    Write-Section "Build Tauri bundle"
    if (-not (Test-WindowsPackagingPrerequisites)) {
        throw "Windows packaging prerequisites are incomplete. Install Rust toolchain and Visual Studio Build Tools with Windows SDK."
    }
    corepack pnpm --dir apps/desktop-ui tauri build
} else {
    Write-Host "Skipped Tauri bundle. Pass -IncludeTauriBundle to build the desktop package."
}

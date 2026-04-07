. "$PSScriptRoot\lib\framepilot-common.ps1"

$repoRoot = Get-FramePilotRoot
Write-Section "Package Windows release"

if (-not (Test-WindowsPackagingPrerequisites)) {
    throw "Packaging requires cargo, rustc, cl.exe and rc.exe. Install Rust toolchain and Visual Studio Build Tools with Windows SDK."
}

& (Join-Path $repoRoot "scripts\build-all.ps1") -IncludeTauriBundle

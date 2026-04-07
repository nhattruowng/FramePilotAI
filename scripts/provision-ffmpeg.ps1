$ErrorActionPreference = "Stop"

$toolsRoot = Join-Path $PSScriptRoot "..\\tools"
$ffmpegRoot = Join-Path $toolsRoot "ffmpeg"
$zipPath = Join-Path $toolsRoot "ffmpeg.zip"
$downloadUrl = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"

New-Item -ItemType Directory -Force -Path $toolsRoot | Out-Null

Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath

if (Test-Path $ffmpegRoot) {
    Remove-Item -Recurse -Force $ffmpegRoot
}

Expand-Archive -Path $zipPath -DestinationPath $toolsRoot -Force
$expandedFolder = Get-ChildItem $toolsRoot -Directory | Where-Object { $_.Name -like "ffmpeg-*" } | Select-Object -First 1
Move-Item -Path $expandedFolder.FullName -Destination $ffmpegRoot

Write-Host "FFmpeg provisioned at $ffmpegRoot"

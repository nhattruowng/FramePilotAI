# Operations Runbook

## Windows-First Prerequisites

### Required

- Java 21+
- Node.js 22+ with Corepack

### Recommended

- FFmpeg provisioned locally
- Rust toolchain
- Visual Studio Build Tools with Windows SDK for Tauri packaging

## Bootstrap

```powershell
Set-Location D:\FramePilotAI
.\scripts\bootstrap.ps1
```

The PowerShell helpers auto-detect Java 21 from common Windows install locations and prefer JDK 21 LTS when multiple JDKs are present.

## Development

### Start everything

```powershell
Set-Location D:\FramePilotAI
.\scripts\run-dev.ps1
```

### Start backend manually

```powershell
Set-Location D:\FramePilotAI
.\gradlew.bat :apps:local-server:bootRun
```

### Start frontend manually

```powershell
Set-Location D:\FramePilotAI
corepack pnpm --dir apps/desktop-ui dev
```

## Build

### Build backend and frontend

```powershell
Set-Location D:\FramePilotAI
.\scripts\build-all.ps1
```

### Build Windows package

```powershell
Set-Location D:\FramePilotAI
.\scripts\package-windows.ps1
```

The packaging script stages the backend jar into Tauri resources before attempting the native bundle.

## Test and QA

### Full repository test pass

```powershell
Set-Location D:\FramePilotAI
.\scripts\test-all.ps1
```

### Smoke test

```powershell
Set-Location D:\FramePilotAI
.\scripts\smoke-test.ps1
```

The smoke script reuses an already healthy backend on `localhost:8080` when one is running, so it does not fail on a port collision during repeated local validation.

## Runtime Storage

- SQLite DB: `storage/app.db`
- Project metadata: `storage/projects/<project-id>/metadata`
- Render artifacts: `storage/exports/generated/<project-id>/<job-id>`
- Temporary script output: `tmp/`

## Logs

- Backend console logs in dev terminal
- Bundled desktop mode writes backend log to `framepilot-data/logs/local-server.log`
- FFmpeg logs are written per shot and per final mux inside each render job directory

## FFmpeg

### Provision bundled FFmpeg

```powershell
Set-Location D:\FramePilotAI
.\scripts\provision-ffmpeg.ps1
```

### Override path

```powershell
$env:FRAMEPILOT_FFMPEG = "D:\FramePilotAI\tools\ffmpeg\bin\ffmpeg.exe"
```

## Common Failure Modes

### Tauri build fails with missing `kernel32.lib` or MSVC tooling

- Install Visual Studio Build Tools
- Include Windows SDK and MSVC C++ workload
- Re-run `.\scripts\package-windows.ps1`

### OCR explanation mentions stub runtime

- Expected in current baseline
- Native OCR adapter is future work and tracked in release docs

### Render falls back to lighter pipeline

- Inspect `GET /api/render/jobs/{jobId}` or the UI render console
- Review checkpoint reasons and runtime samples
- Reduce project complexity or effect density if the machine is near threshold

### Desktop UI opens but backend is not reachable

- For dev mode, start backend separately or set `FRAMEPILOT_SKIP_BUNDLED_BACKEND=1`
- For packaged mode, verify `framepilot-ai-local-server.jar` exists in Tauri resources

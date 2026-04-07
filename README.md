# FramePilot AI

FramePilot AI is a local-first desktop platform for turning comic assets into deterministic motion-comic or hybrid video exports. The repository is organized as a production-oriented monorepo with a Spring Boot local server, a React desktop UI, and a Tauri packaging shell for Windows-first delivery.

## Current Scope

- Device scan, benchmark, capability scoring and preset recommendation
- Project intake, asset import, panel parsing baseline, OCR abstraction and project planning
- Scene and shot planning with deterministic pipeline selection
- Async render workflow with queueing, pause/resume/cancel, runtime sampling and fallback
- FFmpeg-based MP4 export with sidecar artifacts, checkpoint metadata and export verification
- Agent advisory layer for device, project, scene and copilot guidance

## Tech Stack

- Desktop UI: Tauri + React + TypeScript
- Local backend: Spring Boot 3 + WebFlux
- Local DB: SQLite
- Device scan and runtime metrics: OSHI
- CV bridge: JavaCV
- Render and mux: FFmpeg
- AI inference bridge: ONNX Runtime Java
- OCR abstraction: local adapter contract with stub baseline
- Logging: SLF4J + Logback
- Config: YAML
- Frontend package manager: pnpm
- Backend build: Gradle Kotlin DSL

## Repository Layout

```text
FramePilotAI/
├─ apps/
│  ├─ desktop-ui/          # React UI + Tauri desktop shell
│  └─ local-server/        # Spring Boot local API and engines
├─ configs/                # Sample config and preset files
├─ docs/                   # Architecture, operations, QA and release docs
├─ scripts/                # Bootstrap, dev, build, test and packaging scripts
├─ storage/                # Local SQLite database, demo assets and export output
└─ tools/                  # Optional bundled FFmpeg and related native tools
```

## Quick Start

### 1. Bootstrap

```powershell
Set-Location D:\FramePilotAI
.\scripts\bootstrap.ps1
```

### 2. Run development mode

```powershell
Set-Location D:\FramePilotAI
.\scripts\run-dev.ps1
```

This opens the local server and the desktop UI in separate PowerShell windows.

### 3. Build everything

```powershell
Set-Location D:\FramePilotAI
.\scripts\build-all.ps1
```

### 4. Run all tests

```powershell
Set-Location D:\FramePilotAI
.\scripts\test-all.ps1
```

### 5. Smoke test the end-to-end demo flow

```powershell
Set-Location D:\FramePilotAI
.\scripts\smoke-test.ps1
```

## Manual Run

### Backend

```powershell
Set-Location D:\FramePilotAI
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :apps:local-server:bootRun
```

### Frontend

```powershell
Set-Location D:\FramePilotAI
corepack pnpm --dir apps/desktop-ui dev
```

### Desktop packaging

```powershell
Set-Location D:\FramePilotAI
.\scripts\package-windows.ps1
```

Packaging requires Rust plus Visual Studio Build Tools with the Windows SDK.

## API Groups

### Device Intelligence

- `POST /api/device/scan`
- `POST /api/device/benchmark`
- `GET /api/device/recommendation`
- `GET /api/device/explanation`

### Project Planning

- `POST /api/projects`
- `POST /api/projects/{projectId}/assets/import`
- `POST /api/projects/import-demo`
- `GET /api/projects/{projectId}/workspace`
- `POST /api/projects/{projectId}/panels/parse`
- `POST /api/projects/{projectId}/ocr/run`
- `POST /api/projects/{projectId}/analysis`
- `POST /api/projects/{projectId}/analysis/summary`
- `POST /api/projects/{projectId}/scene-plan`
- `POST /api/projects/{projectId}/copilot/query`

### Render

- `POST /api/projects/{projectId}/render/start`
- `GET /api/render/jobs/{jobId}`
- `GET /api/render/jobs/{jobId}/stream`
- `POST /api/render/jobs/{jobId}/pause`
- `POST /api/render/jobs/{jobId}/resume`
- `POST /api/render/jobs/{jobId}/cancel`
- `GET /api/render/jobs/{jobId}/runtime`
- `GET /api/render/jobs/{jobId}/artifacts`

### Agent Coordinator

- `GET /api/agents/contracts`
- `POST /api/agents/briefing`

## Scripts

- `scripts/bootstrap.ps1`: validates toolchain, prepares folders and installs frontend dependencies
- `scripts/run-dev.ps1`: starts backend and frontend dev processes
- `scripts/build-all.ps1`: builds backend jar, stages Tauri resources and builds frontend; optional Tauri bundle
- `scripts/test-all.ps1`: runs backend tests, frontend build verification and optional cargo/smoke checks
- `scripts/smoke-test.ps1`: runs the main local demo flow from device scan to render
- `scripts/package-windows.ps1`: Windows-first packaging wrapper for Tauri
- `scripts/provision-ffmpeg.ps1`: provisions FFmpeg into the local `tools` directory

## Config and Environment

- Sample env file: [framepilot.env.example](D:\FramePilotAI\configs\framepilot.env.example)
- Local runtime overrides: [application.local.yml](D:\FramePilotAI\configs\application.local.yml)
- Desktop sample config: [framepilot.desktop.sample.yml](D:\FramePilotAI\apps\desktop-ui\src-tauri\resources\configs\framepilot.desktop.sample.yml)

Primary environment variables:

- `FRAMEPILOT_STORAGE_ROOT`
- `FRAMEPILOT_FFMPEG`
- `FRAMEPILOT_SKIP_BUNDLED_BACKEND`

## Production-Ready Today

- Clean backend layering with interfaces and implementations
- Device-aware deterministic planning and render guardrails
- SQLite persistence for device, project and render workflow state
- Checkpoint-aware render orchestration with verified export artifacts
- Windows-first scripts for bootstrap, build, test and smoke validation
- Agent advisory contracts that do not bypass runtime or rule engines

## Future Work

- Replace stub OCR adapter with PaddleOCR or ONNX OCR local runtime
- Upgrade panel parsing from rule-based baseline to stronger CV segmentation
- Add real local TTS and audio track mixing into render hooks
- Improve Tauri packaging polish with icons, installer metadata and SDK-verified CI packaging
- Expand integration and failure-path coverage around packaging and long-running render sessions

## Release Readiness Checklist

- Backend tests passing
- Frontend build passing
- Smoke test passing
- Packaging scripts present and Windows-first
- Architecture, QA, operations and release docs present
- Future work and TODO items grouped into roadmap docs

Detailed release readiness notes live in [release-plan.md](D:\FramePilotAI\docs\release\release-plan.md).

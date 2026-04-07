# Module Breakdown

## Agent Layer

### Agent Coordinator

- Entry point for UI or backend callers that need a consolidated advisory view
- Aggregates device, project, scene and copilot advisories
- Publishes explicit guardrails and decision authority

### Device Intelligence Agent

- Reads device scan and benchmark outputs
- Explains preset, constraints and operating envelope
- Confidence drops when OCR or inference paths still use stubs

### Project Analysis Agent

- Summarizes panel count, OCR coverage, scene count and planning complexity
- Advises on chapter splitting and readiness gaps

### Scene Planning Agent

- Explains pacing, shot density and camera effect intensity
- Advises on checkpoint density and scene review focus

### User Copilot Agent

- Answers plain-language questions about preset, fallback, complexity and optimization
- Never changes pipeline or runtime state directly

## Deterministic Engines

### Device stack

- `device-scanner`
- `benchmark-runner`
- `capability-engine`
- `preset-engine`

### Project stack

- `project-manager`
- `panel-parser`
- `ocr-engine`
- `scene-planner`
- `shot-planner`

### Render stack

- `pipeline-selector`
- `render-workflow`
- `render-orchestrator`
- `runtime-monitor`
- `fallback-engine`
- `export-service`

## Persistence and Artifact Modules

- SQLite repositories for device, benchmark, project and render state
- Filesystem metadata for panels, OCR, analysis, scene plan and render checkpoints
- Export artifact listing and verification for MP4 and sidecars

## Desktop and Packaging

- React UI for operator workflow
- Tauri shell for Windows-first desktop delivery
- Bundled backend jar staged into Tauri resources during build

## Cross-Cutting Concerns

- Validation and API exception handling
- YAML configuration and sample env files
- Smoke scripts, integration tests and release docs

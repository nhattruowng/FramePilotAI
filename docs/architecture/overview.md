# FramePilot AI Architecture Overview

## Principles

- Local-first execution and storage
- Device-aware planning before heavy processing
- Stability-first render orchestration
- Deterministic engines behind agent-style coordination
- Interfaces first, native adapters second

## Runtime Shape

```text
Desktop UI (Tauri + React)
  -> Spring Boot Local API
      -> Agent Coordinator
          -> Device Intelligence Agent
          -> Project Analysis Agent
          -> Scene Planning Agent
          -> User Copilot Agent
      -> Engine Adapters
          -> OSHI
          -> OCR adapter
          -> ONNX Runtime
          -> JavaCV bridge
          -> FFmpeg adapter
      -> SQLite + filesystem storage
```

## Current Delivery Scope

- Device intelligence with OSHI and a synthetic benchmark
- Capability scoring and preset recommendation
- Project import and asset catalog in SQLite
- Heuristic panel parsing and local OCR abstraction with stub adapter
- Deterministic scene and shot planning
- Runtime snapshot and threshold-based fallback
- Async render workflow with per-shot queueing, checkpoint-at-shot-boundary pause and resume, runtime sampling, FFmpeg shot encoding and final mux

## Planned Extension Path

1. Replace mock OCR with PaddleOCR or ONNX OCR implementation.
2. Introduce JavaCV-backed panel segmentation and optical flow helpers.
3. Add resumable render checkpoints per shot and persistent job history.
4. Enable real desktop shell packaging after Rust toolchain provisioning.

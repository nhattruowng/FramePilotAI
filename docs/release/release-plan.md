# Release Plan

## Release Readiness Checklist

- [x] Device intelligence flow implemented
- [x] Project intake and planning flow implemented
- [x] Async render flow implemented
- [x] Agent contracts and coordinator endpoint implemented
- [x] Bootstrap, dev, build, test and smoke scripts present
- [x] Architecture, operations, QA and release docs present
- [x] Sample assets included
- [x] Integration test coverage added for main workflow and failure path
- [ ] Native OCR runtime integrated
- [ ] Windows packaging validated on a fully provisioned SDK machine

## Packaging Strategy

1. Build backend jar with a stable filename.
2. Stage backend jar into Tauri resource directory.
3. Build React frontend assets.
4. Build native Tauri bundle on Windows with Rust + MSVC + Windows SDK.
5. Validate bundled backend startup, local storage path and log path.

## Production-Ready Areas

- Local backend architecture and API layering
- Deterministic device and render guardrails
- SQLite persistence and filesystem metadata output
- Async render lifecycle with checkpoint and artifact tracking
- Windows-first build/test/run scripts

## Known Gaps and Roadmap

- Replace OCR stub with a native local OCR adapter
- Improve panel parsing accuracy and edit tooling
- Harden Tauri bundle verification on a fully provisioned Windows packaging machine
- Add richer audio and TTS local integration
- Expand integration coverage around long-running render failure recovery

## Handoff Notes

- Agents are advisory only and must remain non-authoritative
- Runtime and rule engines make all final execution decisions
- FFmpeg logs, checkpoints and runtime samples are the primary source for render diagnosis
- Packaging is prepared in repo, but final installer validation still depends on complete Windows SDK availability

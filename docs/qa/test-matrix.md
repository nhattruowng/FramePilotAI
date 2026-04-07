# Test Matrix

## Scope

This matrix covers the main Windows-first local workflows that must stay healthy for technical handoff.

## Automated Coverage

| Area | Test Type | Status |
| --- | --- | --- |
| Capability scoring | Unit tests | Present |
| Preset mapping | Unit tests | Present |
| Pipeline selection | Unit tests | Present |
| Fallback thresholds | Unit tests | Present |
| Scene planning | Unit tests | Present |
| Shot planning | Unit tests | Present |
| Main project workflow | Integration test | Present |
| Failure path for OCR before parsing | Integration test | Present |
| Frontend compile and bundle | Build verification | Present |

## Scripted QA

| Workflow | Script | Notes |
| --- | --- | --- |
| Bootstrap validation | `scripts/bootstrap.ps1` | Checks core dependencies and folder prep |
| Full test pass | `scripts/test-all.ps1` | Runs backend tests and frontend build |
| End-to-end demo smoke | `scripts/smoke-test.ps1` | Covers device, project and render path |
| Windows packaging | `scripts/package-windows.ps1` | Requires Rust + Windows SDK |

## Sample Asset Validation

- Uses bundled assets in `storage/projects/demo-assets`
- Ensures project import, panel parsing, OCR baseline and scene planning remain stable

## Failure Paths Covered

- OCR before panel parsing returns `400`
- Render fallback reasons are persisted into checkpoint metadata
- Render pause/resume at shot boundary is validated through focused render verification and remains part of the manual release checklist

## Manual QA Before Release

- Open desktop UI and verify device, project and render screens respond
- Run a demo import and inspect panel/OCR intermediate data
- Start a render, pause mid-job, resume and confirm MP4 export exists
- Inspect `render-checkpoint.json`, runtime sample list and artifact verification
- If packaging is intended, run Windows bundle script on a machine with full SDK

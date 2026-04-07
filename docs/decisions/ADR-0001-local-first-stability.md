# ADR-0001: Local-First Stability-First Baseline

## Status

Accepted

## Context

FramePilot AI must run on heterogeneous creator machines without depending on cloud services. Native CV, OCR, and render toolchains may be partially missing during onboarding.

## Decision

- Keep orchestration in Spring Boot WebFlux, but isolate all native execution behind ports.
- Use deterministic mock adapters when native dependencies are missing.
- Prefer fallback completion over hard failure during render orchestration.
- Persist project catalog in SQLite and artifacts on the local filesystem.

## Consequences

- MVP flow remains buildable and demonstrable early.
- Native engine upgrades can be introduced without controller or UI churn.
- Quality ceiling is initially limited until adapters are provisioned.

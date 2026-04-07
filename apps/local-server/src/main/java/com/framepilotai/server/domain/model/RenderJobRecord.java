package com.framepilotai.server.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RenderJobRecord(
        UUID id,
        UUID projectId,
        RenderStatus status,
        PipelineType pipelineType,
        FallbackLevel fallbackLevel,
        String artifactPath,
        String artifactType,
        String checkpointPath,
        Instant startedAt,
        Instant finishedAt
) {
}

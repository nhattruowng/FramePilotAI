package com.framepilotai.server.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RenderCheckpoint(
        UUID jobId,
        UUID projectId,
        String currentPhase,
        PipelineType pipelineType,
        FallbackLevel fallbackLevel,
        String currentShotId,
        int totalShots,
        double progressPercent,
        List<String> completedShots,
        List<String> reasons,
        Instant updatedAt
) {
}

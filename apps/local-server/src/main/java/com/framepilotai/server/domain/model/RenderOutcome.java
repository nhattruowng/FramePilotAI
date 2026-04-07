package com.framepilotai.server.domain.model;

import java.util.List;
import java.util.UUID;

public record RenderOutcome(
        UUID jobId,
        UUID projectId,
        RenderStatus status,
        String artifactPath,
        String artifactType,
        PipelinePlan finalPlan,
        List<String> events,
        RuntimeSnapshot runtimeSnapshot
) {
}

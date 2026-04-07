package com.framepilotai.server.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RenderEventRecord(
        UUID jobId,
        String phase,
        String shotId,
        double progressPercent,
        String message,
        Instant createdAt
) {
}

package com.framepilotai.server.domain.model;

import java.time.Instant;
import java.util.UUID;

public record BenchmarkRunRecord(
        UUID id,
        UUID scanId,
        BenchmarkResult result,
        Instant createdAt
) {
}

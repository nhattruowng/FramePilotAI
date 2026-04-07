package com.framepilotai.server.domain.model;

import java.time.Instant;

public record RuntimeSnapshot(
        double cpuLoadPercent,
        long usedMemoryBytes,
        long totalMemoryBytes,
        long availableMemoryBytes,
        long estimatedVramBytes,
        Instant capturedAt
) {
}

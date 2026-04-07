package com.framepilotai.server.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RuntimeSampleRecord(
        UUID jobId,
        String phase,
        String shotId,
        double progressPercent,
        double cpuLoadPercent,
        long usedMemoryBytes,
        long totalMemoryBytes,
        long availableMemoryBytes,
        long estimatedVramBytes,
        Instant capturedAt
) {
    public RuntimeSnapshot snapshot() {
        return new RuntimeSnapshot(
                cpuLoadPercent,
                usedMemoryBytes,
                totalMemoryBytes,
                availableMemoryBytes,
                estimatedVramBytes,
                capturedAt
        );
    }
}

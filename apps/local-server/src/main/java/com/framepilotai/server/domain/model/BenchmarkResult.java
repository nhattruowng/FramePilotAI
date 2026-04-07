package com.framepilotai.server.domain.model;

public record BenchmarkResult(
        double cpuTaskScore,
        double imageProcessingScore,
        double encodeScore,
        double ocrInferenceScore,
        double aggregateScore,
        long benchmarkDurationMillis,
        boolean usedStub,
        java.util.List<String> notes
) {
}

package com.framepilotai.server.domain.model;

public record DeviceIntelligenceSnapshot(
        java.util.UUID scanId,
        java.util.UUID benchmarkId,
        DeviceProfile deviceProfile,
        BenchmarkResult benchmark,
        CapabilityAssessment capability,
        PresetRecommendation preset,
        String summary,
        java.util.List<String> explanation
) {
}

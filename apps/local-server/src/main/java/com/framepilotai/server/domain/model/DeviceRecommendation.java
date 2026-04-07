package com.framepilotai.server.domain.model;

import java.util.List;
import java.util.UUID;

public record DeviceRecommendation(
        UUID scanId,
        UUID benchmarkId,
        DeviceProfile deviceProfile,
        BenchmarkResult benchmark,
        CapabilityAssessment capability,
        PresetRecommendation preset,
        String summary,
        List<String> explanation
) {
}

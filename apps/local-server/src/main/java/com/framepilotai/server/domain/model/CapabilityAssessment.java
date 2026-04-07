package com.framepilotai.server.domain.model;

import java.util.List;

public record CapabilityAssessment(
        CapabilityTier tier,
        double score,
        List<String> constraints
) {
}

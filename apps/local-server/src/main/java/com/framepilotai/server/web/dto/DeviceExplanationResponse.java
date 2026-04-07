package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.DeviceExplanation;

import java.util.List;
import java.util.UUID;

public record DeviceExplanationResponse(
        UUID scanId,
        UUID benchmarkId,
        String headline,
        String summary,
        List<String> reasons
) {
    public static DeviceExplanationResponse from(DeviceExplanation explanation) {
        return new DeviceExplanationResponse(
                explanation.scanId(),
                explanation.benchmarkId(),
                explanation.headline(),
                explanation.summary(),
                explanation.reasons()
        );
    }
}

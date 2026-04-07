package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.RuntimeSampleRecord;

import java.util.List;
import java.util.UUID;

public record RenderRuntimeStatsResponse(
        UUID jobId,
        List<RenderJobStatusResponse.RuntimeDto> samples
) {
    public static RenderRuntimeStatsResponse from(UUID jobId, List<RuntimeSampleRecord> samples) {
        return new RenderRuntimeStatsResponse(jobId, samples.stream().map(RenderJobStatusResponse.RuntimeDto::from).toList());
    }
}

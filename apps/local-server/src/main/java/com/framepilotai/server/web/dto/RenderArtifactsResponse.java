package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.ExportArtifact;

import java.util.List;
import java.util.UUID;

public record RenderArtifactsResponse(
        UUID jobId,
        List<RenderJobStatusResponse.ArtifactDto> artifacts
) {
    public static RenderArtifactsResponse from(UUID jobId, List<ExportArtifact> artifacts) {
        return new RenderArtifactsResponse(jobId, artifacts.stream().map(RenderJobStatusResponse.ArtifactDto::from).toList());
    }
}

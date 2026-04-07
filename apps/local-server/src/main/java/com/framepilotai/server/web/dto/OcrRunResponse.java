package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.OcrBlock;

import java.util.List;
import java.util.UUID;

public record OcrRunResponse(
        UUID projectId,
        int blockCount,
        List<ProjectWorkspaceResponse.OcrDto> ocrBlocks
) {
    public static OcrRunResponse from(UUID projectId, List<OcrBlock> blocks) {
        return new OcrRunResponse(projectId, blocks.size(), blocks.stream().map(ProjectWorkspaceResponse.OcrDto::from).toList());
    }
}

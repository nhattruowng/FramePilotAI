package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.PanelData;

import java.util.List;
import java.util.UUID;

public record PanelParseResponse(
        UUID projectId,
        int panelCount,
        List<ProjectWorkspaceResponse.PanelDto> panels
) {
    public static PanelParseResponse from(UUID projectId, List<PanelData> panels) {
        return new PanelParseResponse(projectId, panels.size(), panels.stream().map(ProjectWorkspaceResponse.PanelDto::from).toList());
    }
}

package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.SceneData;

import java.util.List;
import java.util.UUID;

public record ScenePlanResponse(
        UUID projectId,
        int sceneCount,
        List<ProjectWorkspaceResponse.SceneDto> scenes
) {
    public static ScenePlanResponse from(UUID projectId, List<SceneData> scenes) {
        return new ScenePlanResponse(projectId, scenes.size(), scenes.stream().map(ProjectWorkspaceResponse.SceneDto::from).toList());
    }
}

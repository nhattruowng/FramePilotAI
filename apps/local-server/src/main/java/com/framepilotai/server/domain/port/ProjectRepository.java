package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.AssetRecord;
import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectMetadataRecord;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.ProjectStatus;
import com.framepilotai.server.domain.model.ProjectWorkspace;
import com.framepilotai.server.domain.model.SceneData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {
    ProjectRecord create(String name, String description, String sourceType);

    ProjectRecord create(String name, List<AssetRecord> assets);

    ProjectRecord importAssets(UUID projectId, List<AssetRecord> assets);

    Optional<ProjectRecord> findById(UUID projectId);

    ProjectWorkspace getWorkspace(UUID projectId);

    ProjectRecord updateStatus(UUID projectId, ProjectStatus status);

    ProjectMetadataRecord saveMetadata(UUID projectId, ProjectMetadataRecord metadata);

    List<PanelData> savePanels(UUID projectId, List<PanelData> panels);

    List<PanelData> findPanels(UUID projectId);

    List<OcrBlock> saveOcrBlocks(UUID projectId, List<OcrBlock> blocks);

    List<OcrBlock> findOcrBlocks(UUID projectId);

    List<SceneData> saveScenePlan(UUID projectId, List<SceneData> scenes);

    List<SceneData> findScenePlan(UUID projectId);
}

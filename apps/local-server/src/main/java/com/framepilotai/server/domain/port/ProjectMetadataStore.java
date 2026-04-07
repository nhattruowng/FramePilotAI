package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.SceneData;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface ProjectMetadataStore {
    Path writePanels(UUID projectId, List<PanelData> panels) throws Exception;

    Path writeOcr(UUID projectId, List<OcrBlock> blocks) throws Exception;

    Path writeScenePlan(UUID projectId, List<SceneData> scenes) throws Exception;

    Path writeAnalysis(UUID projectId, ProjectAnalysis analysis) throws Exception;
}

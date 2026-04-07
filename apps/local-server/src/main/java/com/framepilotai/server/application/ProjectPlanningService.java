package com.framepilotai.server.application;

import com.framepilotai.server.common.error.DomainException;
import com.framepilotai.server.domain.model.DeviceIntelligenceSnapshot;
import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.ProjectMetadataRecord;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.ProjectStatus;
import com.framepilotai.server.domain.model.ProjectWorkspace;
import com.framepilotai.server.domain.port.OcrEngine;
import com.framepilotai.server.domain.port.PanelParser;
import com.framepilotai.server.domain.port.PipelineSelector;
import com.framepilotai.server.domain.port.ProjectMetadataStore;
import com.framepilotai.server.domain.port.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectPlanningService {

    private final ProjectRepository projectRepository;
    private final ProjectMetadataStore projectMetadataStore;
    private final PanelParser panelParser;
    private final OcrEngine ocrEngine;
    private final ScenePlanningAgentService scenePlanningAgentService;
    private final PipelineSelector pipelineSelector;
    private final UserCopilotAgentService userCopilotAgentService;

    public ProjectPlanningService(
            ProjectRepository projectRepository,
            ProjectMetadataStore projectMetadataStore,
            PanelParser panelParser,
            OcrEngine ocrEngine,
            ScenePlanningAgentService scenePlanningAgentService,
            PipelineSelector pipelineSelector,
            UserCopilotAgentService userCopilotAgentService
    ) {
        this.projectRepository = projectRepository;
        this.projectMetadataStore = projectMetadataStore;
        this.panelParser = panelParser;
        this.ocrEngine = ocrEngine;
        this.scenePlanningAgentService = scenePlanningAgentService;
        this.pipelineSelector = pipelineSelector;
        this.userCopilotAgentService = userCopilotAgentService;
    }

    public ProjectWorkspace getWorkspace(UUID projectId) {
        return projectRepository.getWorkspace(projectId);
    }

    public List<PanelData> parsePanels(UUID projectId) {
        ProjectRecord project = requireProject(projectId);
        List<PanelData> panels = projectRepository.savePanels(projectId, panelParser.parse(project));
        try {
            projectMetadataStore.writePanels(projectId, panels);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write panel metadata", exception);
        }
        refreshMetadata(projectId, null, null);
        return panels;
    }

    public List<OcrBlock> runOcr(UUID projectId) {
        ProjectRecord project = requireProject(projectId);
        List<PanelData> panels = projectRepository.findPanels(projectId);
        if (panels.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "No parsed panels found. Run panel parsing first.");
        }
        List<OcrBlock> blocks = projectRepository.saveOcrBlocks(projectId, ocrEngine.extract(project, panels));
        try {
            projectMetadataStore.writeOcr(projectId, blocks);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write OCR metadata", exception);
        }
        refreshMetadata(projectId, null, null);
        return blocks;
    }

    public ProjectAnalysis analyzeProject(UUID projectId, DeviceIntelligenceSnapshot deviceSnapshot) {
        ProjectRecord project = requireProject(projectId);
        List<PanelData> panels = projectRepository.findPanels(projectId);
        List<OcrBlock> blocks = projectRepository.findOcrBlocks(projectId);
        if (panels.isEmpty()) {
            panels = parsePanels(projectId);
        }
        if (blocks.isEmpty()) {
            blocks = runOcr(projectId);
        }
        List<com.framepilotai.server.domain.model.SceneData> scenes = scenePlanningAgentService.planScenes(panels, blocks);
        String complexityLevel = classifyComplexity(panels.size(), blocks.size(), scenes.size());
        String summary = buildSummary(project, panels, blocks, scenes, complexityLevel);
        var pipelinePlan = pipelineSelector.select(project, deviceSnapshot.capability(), deviceSnapshot.preset(), scenes);
        var analysis = new ProjectAnalysis(
                project,
                panels,
                blocks,
                scenes,
                summary,
                complexityLevel,
                pipelinePlan,
                List.of()
        );
        try {
            projectMetadataStore.writeAnalysis(projectId, analysis);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write analysis metadata", exception);
        }
        refreshMetadata(projectId, summary, complexityLevel);
        projectRepository.updateStatus(projectId, ProjectStatus.PLANNING);
        return analysis;
    }

    public List<com.framepilotai.server.domain.model.SceneData> generateScenePlan(UUID projectId, DeviceIntelligenceSnapshot deviceSnapshot) {
        ProjectAnalysis analysis = analyzeProject(projectId, deviceSnapshot);
        List<com.framepilotai.server.domain.model.SceneData> scenes = projectRepository.saveScenePlan(projectId, analysis.scenes());
        try {
            projectMetadataStore.writeScenePlan(projectId, scenes);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write scene metadata", exception);
        }
        refreshMetadata(projectId, analysis.projectSummary(), analysis.complexityLevel());
        projectRepository.updateStatus(projectId, ProjectStatus.READY);
        return scenes;
    }

    public String queryCopilot(UUID projectId, String question) {
        ProjectWorkspace workspace = projectRepository.getWorkspace(projectId);
        return userCopilotAgentService.answer(workspace, question);
    }

    private ProjectRecord requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
    }

    private void refreshMetadata(UUID projectId, String summaryOverride, String complexityOverride) {
        ProjectWorkspace workspace = projectRepository.getWorkspace(projectId);
        String summary = summaryOverride != null ? summaryOverride : workspace.metadata().summary();
        String complexity = complexityOverride != null ? complexityOverride : workspace.metadata().complexityLevel();
        projectRepository.saveMetadata(
                projectId,
                new ProjectMetadataRecord(
                        workspace.metadata().sourceType(),
                        workspace.metadata().description(),
                        summary,
                        complexity,
                        workspace.panels().size(),
                        workspace.scenes().size(),
                        workspace.metadata().metadataJson(),
                        Instant.now()
                )
        );
    }

    private String classifyComplexity(int panelCount, int ocrCount, int sceneCount) {
        int score = panelCount * 2 + ocrCount + sceneCount * 3;
        if (score >= 18) {
            return "HIGH";
        }
        if (score >= 9) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildSummary(ProjectRecord project, List<PanelData> panels, List<OcrBlock> blocks, List<com.framepilotai.server.domain.model.SceneData> scenes, String complexityLevel) {
        return "Project "
                + project.name()
                + " has "
                + panels.size()
                + " panels, "
                + blocks.size()
                + " OCR blocks, "
                + scenes.size()
                + " planned scenes, complexity "
                + complexityLevel
                + ".";
    }
}

package com.framepilotai.server.application;

import com.framepilotai.server.common.error.DomainException;
import com.framepilotai.server.domain.model.DeviceIntelligenceSnapshot;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.agent.AgentAdvisory;
import com.framepilotai.server.domain.model.agent.AgentContractSpec;
import com.framepilotai.server.domain.model.agent.AgentType;
import com.framepilotai.server.domain.port.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectAnalysisAgentService {

    private final ProjectRepository projectRepository;
    private final ProjectManagerService projectManagerService;
    private final ProjectPlanningService projectPlanningService;

    public ProjectAnalysisAgentService(
            ProjectRepository projectRepository,
            ProjectManagerService projectManagerService,
            ProjectPlanningService projectPlanningService
    ) {
        this.projectRepository = projectRepository;
        this.projectManagerService = projectManagerService;
        this.projectPlanningService = projectPlanningService;
    }

    public ProjectRecord importProject(String name, List<String> assetPaths) {
        ProjectRecord project = projectManagerService.createProject(name, "Imported project", "image-sequence");
        return projectManagerService.importAssets(project.id(), assetPaths);
    }

    public ProjectRecord getProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
    }

    public ProjectAnalysis analyze(UUID projectId, DeviceIntelligenceSnapshot deviceSnapshot) {
        return projectPlanningService.analyzeProject(projectId, deviceSnapshot);
    }

    public AgentAdvisory advisory(UUID projectId, DeviceIntelligenceSnapshot deviceSnapshot) {
        ProjectAnalysis analysis = analyze(projectId, deviceSnapshot);
        List<String> warnings = new ArrayList<>();
        if (analysis.ocrBlocks().stream().anyMatch(block -> block.adapterName().contains("stub"))) {
            warnings.add("Project OCR is currently backed by a deterministic stub adapter.");
        }
        if (analysis.pipelinePlan().fallbackLevel().name().equals("F1") || analysis.pipelinePlan().fallbackLevel().name().equals("F2")) {
            warnings.add("Project planning already starts from a lighter pipeline because of device or content constraints.");
        }

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Project complexity is " + analysis.complexityLevel() + " with " + analysis.scenes().size() + " scenes.");
        recommendations.add("Pipeline candidate is " + analysis.pipelinePlan().pipelineType() + " under preset " + analysis.pipelinePlan().preset() + ".");
        if (analysis.panels().size() > 12) {
            recommendations.add("Consider splitting the project into smaller chapter slices for smoother review and render batches.");
        }

        return new AgentAdvisory(
                AgentType.PROJECT_ANALYSIS,
                analysis.projectSummary(),
                analysis.ocrBlocks().stream().anyMatch(block -> block.adapterName().contains("stub")) ? 0.74 : 0.88,
                List.of(
                        "Panel count: " + analysis.panels().size(),
                        "OCR block count: " + analysis.ocrBlocks().size(),
                        "Scene count: " + analysis.scenes().size(),
                        "Pipeline baseline: " + analysis.pipelinePlan().pipelineType()
                ),
                List.copyOf(warnings),
                List.copyOf(recommendations)
        );
    }

    public AgentContractSpec contract() {
        return new AgentContractSpec(
                AgentType.PROJECT_ANALYSIS,
                "Explains project complexity, content readiness and planning implications.",
                "{ projectId: uuid, deviceSnapshot?: DeviceIntelligenceSnapshot }",
                "{ summary, confidence, explanations[], warnings[], recommendations[] }",
                "summary -> complexity rationale -> plan-safe recommendations",
                List.of("stub OCR notice", "high complexity", "planning readiness gap"),
                List.of("project split", "pipeline suitability", "content cleanup"),
                List.of(
                        "Must not skip panel parsing or OCR preconditions.",
                        "Must not bypass pipeline selector or render guardrails.",
                        "Advisory output only. Planning and runtime engines remain authoritative."
                )
        );
    }
}

package com.framepilotai.server.application;

import com.framepilotai.server.domain.model.DeviceIntelligenceSnapshot;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.ProjectWorkspace;
import com.framepilotai.server.domain.model.RenderJobSnapshot;
import com.framepilotai.server.domain.model.agent.AgentAdvisory;
import com.framepilotai.server.domain.model.agent.AgentContractSpec;
import com.framepilotai.server.domain.model.agent.AgentCoordinatorBriefing;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgentCoordinatorService {

    private static final List<String> COORDINATOR_GUARDRAILS = List.of(
            "Agents are advisory only. Capability engines, planners, runtime monitors and fallback engines remain authoritative.",
            "No agent may bypass render guardrails, checkpoint policy, or runtime thresholds.",
            "No agent may switch the system to cloud-only dependencies."
    );

    private final DeviceIntelligenceAgentService deviceIntelligenceAgentService;
    private final ProjectAnalysisAgentService projectAnalysisAgentService;
    private final ProjectPlanningService projectPlanningService;
    private final ScenePlanningAgentService scenePlanningAgentService;
    private final UserCopilotAgentService userCopilotAgentService;
    private final RenderWorkflowService renderWorkflowService;

    public AgentCoordinatorService(
            DeviceIntelligenceAgentService deviceIntelligenceAgentService,
            ProjectAnalysisAgentService projectAnalysisAgentService,
            ProjectPlanningService projectPlanningService,
            ScenePlanningAgentService scenePlanningAgentService,
            UserCopilotAgentService userCopilotAgentService,
            RenderWorkflowService renderWorkflowService
    ) {
        this.deviceIntelligenceAgentService = deviceIntelligenceAgentService;
        this.projectAnalysisAgentService = projectAnalysisAgentService;
        this.projectPlanningService = projectPlanningService;
        this.scenePlanningAgentService = scenePlanningAgentService;
        this.userCopilotAgentService = userCopilotAgentService;
        this.renderWorkflowService = renderWorkflowService;
    }

    public DeviceIntelligenceSnapshot runDeviceIntelligence() {
        return deviceIntelligenceAgentService.scanAndAssess();
    }

    public ProjectAnalysis analyzeProject(UUID projectId) {
        DeviceIntelligenceSnapshot snapshot = runDeviceIntelligence();
        ProjectAnalysis analysis = projectAnalysisAgentService.analyze(projectId, snapshot);
        return new ProjectAnalysis(
                analysis.project(),
                analysis.panels(),
                analysis.ocrBlocks(),
                analysis.scenes(),
                analysis.projectSummary(),
                analysis.complexityLevel(),
                analysis.pipelinePlan(),
                userCopilotAgentService.buildNotes(snapshot, analysis)
        );
    }

    public ProjectRecord getProject(UUID projectId) {
        return projectAnalysisAgentService.getProject(projectId);
    }

    public List<AgentContractSpec> contracts() {
        return List.of(
                deviceIntelligenceAgentService.contract(),
                projectAnalysisAgentService.contract(),
                scenePlanningAgentService.contract(),
                userCopilotAgentService.contract()
        );
    }

    public AgentCoordinatorBriefing coordinate(
            UUID projectId,
            UUID scanId,
            UUID benchmarkId,
            UUID renderJobId,
            String question
    ) {
        UUID requestId = UUID.randomUUID();
        List<AgentAdvisory> advisories = new ArrayList<>();
        List<String> warnings = new ArrayList<>(COORDINATOR_GUARDRAILS);
        List<String> recommendations = new ArrayList<>();

        AgentAdvisory deviceAdvisory = deviceIntelligenceAgentService.advisory(scanId, benchmarkId);
        advisories.add(deviceAdvisory);
        warnings.addAll(deviceAdvisory.warnings());
        recommendations.addAll(deviceAdvisory.recommendations());

        if (projectId != null) {
            DeviceIntelligenceSnapshot snapshot = deviceIntelligenceAgentService.snapshot(scanId, benchmarkId);
            AgentAdvisory projectAdvisory = projectAnalysisAgentService.advisory(projectId, snapshot);
            advisories.add(projectAdvisory);
            warnings.addAll(projectAdvisory.warnings());
            recommendations.addAll(projectAdvisory.recommendations());

            ProjectWorkspace workspace = projectPlanningService.getWorkspace(projectId);
            if (!workspace.scenes().isEmpty()) {
                AgentAdvisory sceneAdvisory = scenePlanningAgentService.advisory(workspace.scenes());
                advisories.add(sceneAdvisory);
                warnings.addAll(sceneAdvisory.warnings());
                recommendations.addAll(sceneAdvisory.recommendations());
            }

            RenderJobSnapshot renderSnapshot = renderJobId == null ? null : renderWorkflowService.getJobSnapshot(renderJobId);
            AgentAdvisory copilotAdvisory = userCopilotAgentService.advisory(
                    workspace,
                    snapshot,
                    renderSnapshot,
                    question == null || question.isBlank() ? "Explain the current project and optimization opportunities." : question
            );
            advisories.add(copilotAdvisory);
            warnings.addAll(copilotAdvisory.warnings());
            recommendations.addAll(copilotAdvisory.recommendations());
        }

        return new AgentCoordinatorBriefing(
                requestId,
                Instant.now(),
                "Rule engines and runtime engines decide. Agents explain and recommend only.",
                COORDINATOR_GUARDRAILS,
                List.copyOf(advisories),
                warnings.stream().distinct().toList(),
                recommendations.stream().distinct().toList()
        );
    }
}

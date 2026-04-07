package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.ProjectAnalysis;

import java.util.List;
import java.util.UUID;

public record ProjectAnalysisSummaryResponse(
        UUID projectId,
        String projectSummary,
        String complexityLevel,
        String status,
        String preset,
        String pipelineType,
        List<String> copilotNotes
) {
    public static ProjectAnalysisSummaryResponse from(ProjectAnalysis analysis) {
        return new ProjectAnalysisSummaryResponse(
                analysis.project().id(),
                analysis.projectSummary(),
                analysis.complexityLevel(),
                analysis.project().status().name(),
                analysis.pipelinePlan().preset().name(),
                analysis.pipelinePlan().pipelineType().name(),
                analysis.copilotNotes()
        );
    }
}

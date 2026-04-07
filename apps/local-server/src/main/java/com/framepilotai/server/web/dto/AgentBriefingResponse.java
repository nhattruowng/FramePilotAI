package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.agent.AgentAdvisory;
import com.framepilotai.server.domain.model.agent.AgentCoordinatorBriefing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentBriefingResponse(
        UUID requestId,
        Instant generatedAt,
        String decisionAuthority,
        List<String> guardrails,
        List<AdvisoryDto> advisories,
        List<String> warnings,
        List<String> recommendations
) {
    public static AgentBriefingResponse from(AgentCoordinatorBriefing briefing) {
        return new AgentBriefingResponse(
                briefing.requestId(),
                briefing.generatedAt(),
                briefing.decisionAuthority(),
                briefing.guardrails(),
                briefing.advisories().stream().map(AdvisoryDto::from).toList(),
                briefing.warnings(),
                briefing.recommendations()
        );
    }

    public record AdvisoryDto(
            String agentType,
            String summary,
            double confidence,
            List<String> explanations,
            List<String> warnings,
            List<String> recommendations
    ) {
        static AdvisoryDto from(AgentAdvisory advisory) {
            return new AdvisoryDto(
                    advisory.agentType().name(),
                    advisory.summary(),
                    advisory.confidence(),
                    advisory.explanations(),
                    advisory.warnings(),
                    advisory.recommendations()
            );
        }
    }
}

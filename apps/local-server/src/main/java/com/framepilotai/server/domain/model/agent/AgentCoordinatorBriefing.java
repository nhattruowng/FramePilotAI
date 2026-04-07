package com.framepilotai.server.domain.model.agent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentCoordinatorBriefing(
        UUID requestId,
        Instant generatedAt,
        String decisionAuthority,
        List<String> guardrails,
        List<AgentAdvisory> advisories,
        List<String> warnings,
        List<String> recommendations
) {
}

package com.framepilotai.server.domain.model.agent;

import java.util.List;

public record AgentAdvisory(
        AgentType agentType,
        String summary,
        double confidence,
        List<String> explanations,
        List<String> warnings,
        List<String> recommendations
) {
}

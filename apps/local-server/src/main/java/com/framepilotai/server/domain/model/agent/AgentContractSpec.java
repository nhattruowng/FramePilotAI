package com.framepilotai.server.domain.model.agent;

import java.util.List;

public record AgentContractSpec(
        AgentType agentType,
        String role,
        String inputSchema,
        String outputSchema,
        String explanationFormat,
        List<String> warningsSchema,
        List<String> recommendationSchema,
        List<String> guardrails
) {
}

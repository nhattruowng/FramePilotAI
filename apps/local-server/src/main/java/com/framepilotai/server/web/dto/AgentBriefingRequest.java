package com.framepilotai.server.web.dto;

import java.util.UUID;

public record AgentBriefingRequest(
        UUID projectId,
        UUID scanId,
        UUID benchmarkId,
        UUID renderJobId,
        String question
) {
}

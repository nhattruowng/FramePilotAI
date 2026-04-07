package com.framepilotai.server.web.dto;

import java.util.UUID;

public record CopilotResponse(
        UUID projectId,
        String answer
) {
}

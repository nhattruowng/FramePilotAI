package com.framepilotai.server.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CopilotQueryRequest(
        @NotBlank(message = "Question is required")
        String question
) {
}

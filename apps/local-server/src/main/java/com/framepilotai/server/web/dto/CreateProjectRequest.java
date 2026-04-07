package com.framepilotai.server.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        String name,
        String description,
        String sourceType
) {
}

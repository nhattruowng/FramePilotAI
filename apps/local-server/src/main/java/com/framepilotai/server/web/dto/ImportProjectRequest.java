package com.framepilotai.server.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportProjectRequest(
        @NotBlank(message = "Project name is required")
        String name,
        @NotEmpty(message = "At least one asset path is required")
        List<@NotBlank(message = "Asset path cannot be blank") String> assetPaths
) {
}

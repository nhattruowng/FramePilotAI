package com.framepilotai.server.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ImportAssetsRequest(
        @NotEmpty(message = "At least one asset path is required")
        List<String> assetPaths
) {
}

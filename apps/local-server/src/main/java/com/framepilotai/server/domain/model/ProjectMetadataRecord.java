package com.framepilotai.server.domain.model;

import java.time.Instant;

public record ProjectMetadataRecord(
        String sourceType,
        String description,
        String summary,
        String complexityLevel,
        int panelCount,
        int sceneCount,
        String metadataJson,
        Instant updatedAt
) {
}

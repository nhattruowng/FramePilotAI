package com.framepilotai.server.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectRecord(
        UUID id,
        String name,
        ProjectStatus status,
        List<AssetRecord> assets,
        Instant createdAt,
        Instant updatedAt
) {
}

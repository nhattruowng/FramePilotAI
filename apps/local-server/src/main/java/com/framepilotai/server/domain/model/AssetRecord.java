package com.framepilotai.server.domain.model;

import java.util.UUID;

public record AssetRecord(
        UUID id,
        UUID projectId,
        String sourcePath,
        String mediaType,
        long sizeBytes,
        int assetOrder
) {
}

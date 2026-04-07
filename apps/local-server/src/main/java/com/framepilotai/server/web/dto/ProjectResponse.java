package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.AssetRecord;
import com.framepilotai.server.domain.model.ProjectRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<AssetDto> assets
) {
    public static ProjectResponse from(ProjectRecord project) {
        return new ProjectResponse(
                project.id(),
                project.name(),
                project.status().name(),
                project.createdAt(),
                project.updatedAt(),
                project.assets().stream().map(AssetDto::from).toList()
        );
    }

    public record AssetDto(UUID id, String sourcePath, String mediaType, long sizeBytes, int assetOrder) {
        static AssetDto from(AssetRecord asset) {
            return new AssetDto(asset.id(), asset.sourcePath(), asset.mediaType(), asset.sizeBytes(), asset.assetOrder());
        }
    }
}

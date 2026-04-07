package com.framepilotai.server.domain.model;

public record ExportArtifact(
        String label,
        String path,
        String mediaType,
        long sizeBytes,
        boolean verified
) {
}

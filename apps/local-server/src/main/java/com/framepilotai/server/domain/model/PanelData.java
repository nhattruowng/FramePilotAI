package com.framepilotai.server.domain.model;

public record PanelData(
        String panelId,
        String assetId,
        String assetPath,
        int readingOrder,
        int cropX,
        int cropY,
        int width,
        int height,
        String summary,
        String reviewState
) {
}

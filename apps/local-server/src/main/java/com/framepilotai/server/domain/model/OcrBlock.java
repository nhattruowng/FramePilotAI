package com.framepilotai.server.domain.model;

public record OcrBlock(
        String ocrId,
        String panelId,
        String text,
        double confidence,
        String language,
        String adapterName
) {
}

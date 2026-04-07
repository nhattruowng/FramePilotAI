package com.framepilotai.server.domain.model;

public record RenderProfile(
        int outputWidth,
        int outputHeight,
        int fps,
        double zoomIntensity,
        double shotDurationScale,
        int maxShotDurationMillis,
        boolean subtitleOverlayEnabled
) {
}

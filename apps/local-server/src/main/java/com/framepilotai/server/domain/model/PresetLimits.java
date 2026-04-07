package com.framepilotai.server.domain.model;

public record PresetLimits(
        int outputWidth,
        int outputHeight,
        int fps,
        int maxShotLengthMillis,
        double aiShotRatio,
        int checkpointIntervalShots
) {
}

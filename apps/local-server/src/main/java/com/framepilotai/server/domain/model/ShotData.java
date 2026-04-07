package com.framepilotai.server.domain.model;

public record ShotData(
        String shotId,
        String sceneId,
        String panelId,
        int orderIndex,
        String cameraMove,
        int durationMillis,
        String effectLevel,
        int timelineStartMillis,
        int timelineEndMillis,
        String notes
) {
}

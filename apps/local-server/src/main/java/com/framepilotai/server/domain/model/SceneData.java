package com.framepilotai.server.domain.model;

import java.util.List;

public record SceneData(
        String sceneId,
        int sceneOrder,
        String title,
        String narrative,
        double complexityScore,
        String cameraEffectLevel,
        int totalDurationMillis,
        List<ShotData> shots
) {
}

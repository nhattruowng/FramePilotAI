package com.framepilotai.server.domain.model;

import java.util.List;

public record PresetRecommendation(
        PresetName preset,
        PipelineType defaultPipeline,
        PresetLimits limits,
        String headline,
        List<String> rationale
) {
}

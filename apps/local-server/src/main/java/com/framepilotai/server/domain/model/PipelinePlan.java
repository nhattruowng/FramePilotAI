package com.framepilotai.server.domain.model;

import java.util.List;

public record PipelinePlan(
        PipelineType pipelineType,
        PresetName preset,
        FallbackLevel fallbackLevel,
        List<String> reasons
) {
}

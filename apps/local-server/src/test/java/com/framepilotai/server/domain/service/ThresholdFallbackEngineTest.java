package com.framepilotai.server.domain.service;

import com.framepilotai.server.common.config.FramePilotProperties;
import com.framepilotai.server.domain.model.FallbackLevel;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.PipelineType;
import com.framepilotai.server.domain.model.PresetName;
import com.framepilotai.server.domain.model.RuntimeSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdFallbackEngineTest {

    private final ThresholdFallbackEngine engine = new ThresholdFallbackEngine(new FramePilotProperties());

    @Test
    void shouldForceMotionComicWhenMemoryPressureIsHigh() {
        var plan = new PipelinePlan(PipelineType.ENHANCED_HYBRID, PresetName.QUALITY, FallbackLevel.NONE, List.of("initial"));
        var snapshot = new RuntimeSnapshot(45, 92, 100, 8, 0, Instant.now());

        var adjusted = engine.apply(plan, snapshot);

        assertThat(adjusted.pipelineType()).isEqualTo(PipelineType.MOTION_COMIC);
        assertThat(adjusted.fallbackLevel()).isEqualTo(FallbackLevel.F4);
        assertThat(adjusted.reasons()).anyMatch(reason -> reason.contains("Memory pressure"));
    }
}

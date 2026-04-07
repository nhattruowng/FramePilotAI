package com.framepilotai.server.domain.service;

import com.framepilotai.server.domain.model.CapabilityAssessment;
import com.framepilotai.server.domain.model.CapabilityTier;
import com.framepilotai.server.domain.model.FallbackLevel;
import com.framepilotai.server.domain.model.PipelineType;
import com.framepilotai.server.domain.model.PresetLimits;
import com.framepilotai.server.domain.model.PresetName;
import com.framepilotai.server.domain.model.PresetRecommendation;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.ProjectStatus;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.model.ShotData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPipelineSelectorTest {

    private final DefaultPipelineSelector selector = new DefaultPipelineSelector();

    @Test
    void shouldDownshiftWhenConstraintsAreStacked() {
        ProjectRecord project = new ProjectRecord(UUID.randomUUID(), "demo", ProjectStatus.PLANNING, List.of(), Instant.now(), Instant.now());
        CapabilityAssessment assessment = new CapabilityAssessment(CapabilityTier.MEDIUM, 60, List.of("low ram", "no gpu"));
        PresetRecommendation recommendation = new PresetRecommendation(
                PresetName.BALANCED,
                PipelineType.HYBRID,
                new PresetLimits(1600, 900, 24, 3200, 0.28, 3),
                "Balanced preset recommended",
                List.of("balanced")
        );
        SceneData scene = new SceneData(
                "scene-1",
                1,
                "Scene 1",
                "narrative",
                0.52,
                "balanced",
                2000,
                List.of(new ShotData("shot-1", "scene-1", "panel-1", 1, "push-in", 2000, "medium", 0, 2000, "Test shot"))
        );

        var plan = selector.select(project, assessment, recommendation, List.of(scene));

        assertThat(plan.pipelineType()).isEqualTo(PipelineType.HYBRID_LITE);
        assertThat(plan.fallbackLevel()).isEqualTo(FallbackLevel.F1);
    }

    @Test
    void shouldPromoteToEnhancedHybridForHighComplexityOnStrongMachine() {
        ProjectRecord project = new ProjectRecord(UUID.randomUUID(), "demo", ProjectStatus.READY, List.of(), Instant.now(), Instant.now());
        CapabilityAssessment assessment = new CapabilityAssessment(CapabilityTier.HIGH, 91, List.of());
        PresetRecommendation recommendation = new PresetRecommendation(
                PresetName.QUALITY,
                PipelineType.HYBRID,
                new PresetLimits(1920, 1080, 30, 4200, 0.55, 4),
                "Quality preset recommended",
                List.of("quality")
        );
        SceneData scene = new SceneData(
                "scene-1",
                1,
                "Scene 1",
                "dense narrative",
                0.92,
                "enhanced",
                4800,
                List.of(new ShotData("shot-1", "scene-1", "panel-1", 1, "push-in", 2400, "enhanced", 0, 2400, "Shot A"))
        );

        var plan = selector.select(project, assessment, recommendation, List.of(scene));

        assertThat(plan.pipelineType()).isEqualTo(PipelineType.ENHANCED_HYBRID);
        assertThat(plan.fallbackLevel()).isEqualTo(FallbackLevel.NONE);
    }
}

package com.framepilotai.server.domain.service;

import com.framepilotai.server.domain.model.CapabilityAssessment;
import com.framepilotai.server.domain.model.CapabilityTier;
import com.framepilotai.server.domain.model.PresetName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPresetEngineTest {

    private final DefaultPresetEngine engine = new DefaultPresetEngine();

    @Test
    void shouldMapLowTierToEco() {
        var preset = engine.recommend(new CapabilityAssessment(CapabilityTier.LOW, 41.2, List.of("low ram")));

        assertThat(preset.preset()).isEqualTo(PresetName.ECO);
        assertThat(preset.limits().outputWidth()).isEqualTo(1280);
        assertThat(preset.limits().aiShotRatio()).isLessThanOrEqualTo(0.10);
    }

    @Test
    void shouldMapMediumTierToBalanced() {
        var preset = engine.recommend(new CapabilityAssessment(CapabilityTier.MEDIUM, 66.4, List.of()));

        assertThat(preset.preset()).isEqualTo(PresetName.BALANCED);
        assertThat(preset.limits().fps()).isEqualTo(24);
    }

    @Test
    void shouldMapHighTierToQuality() {
        var preset = engine.recommend(new CapabilityAssessment(CapabilityTier.HIGH, 88.5, List.of()));

        assertThat(preset.preset()).isEqualTo(PresetName.QUALITY);
        assertThat(preset.limits().outputWidth()).isEqualTo(1920);
        assertThat(preset.limits().aiShotRatio()).isGreaterThan(0.5);
    }
}

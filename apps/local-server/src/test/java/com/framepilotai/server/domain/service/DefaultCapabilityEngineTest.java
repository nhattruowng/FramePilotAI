package com.framepilotai.server.domain.service;

import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.CapabilityTier;
import com.framepilotai.server.domain.model.DeviceProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCapabilityEngineTest {

    private final DefaultCapabilityEngine engine = new DefaultCapabilityEngine(new CapabilityScoreCalculator());

    @Test
    void shouldClassifyHighTierWhenGpuAndRamAreStrong() {
        DeviceProfile profile = new DeviceProfile(
                "dev",
                "Windows",
                "11",
                new DeviceProfile.CpuProfile("Intel", 8, 16, 20),
                new DeviceProfile.MemoryProfile(32L * 1_073_741_824L, 24L * 1_073_741_824L),
                List.of(new DeviceProfile.GpuProfile("NVIDIA", "RTX", 8L * 1_073_741_824L, true)),
                List.of(),
                Instant.now()
        );

        var assessment = engine.assess(profile, new BenchmarkResult(88, 90, 91, 80, 89, 1000, false, List.of()));

        assertThat(assessment.tier()).isEqualTo(CapabilityTier.HIGH);
        assertThat(assessment.score()).isGreaterThan(80);
    }

    @Test
    void shouldClassifyLowTierWhenResourcesAreConstrained() {
        DeviceProfile profile = new DeviceProfile(
                "dev",
                "Windows",
                "11",
                new DeviceProfile.CpuProfile("Intel", 2, 4, 80),
                new DeviceProfile.MemoryProfile(8L * 1_073_741_824L, 2L * 1_073_741_824L),
                List.of(),
                List.of(),
                Instant.now()
        );

        var assessment = engine.assess(profile, new BenchmarkResult(30, 25, 22, 40, 28, 1000, true, List.of("stub")));

        assertThat(assessment.tier()).isEqualTo(CapabilityTier.LOW);
        assertThat(assessment.constraints()).isNotEmpty();
    }
}

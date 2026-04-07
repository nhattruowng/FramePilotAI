package com.framepilotai.server.domain.service;

import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.DeviceProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityScoreCalculatorTest {

    private final CapabilityScoreCalculator calculator = new CapabilityScoreCalculator();

    @Test
    void shouldProduceHigherScoreForStrongerHardwareAndBenchmark() {
        DeviceProfile strong = new DeviceProfile(
                "dev-a",
                "Windows",
                "11",
                new DeviceProfile.CpuProfile("Ryzen 9", 12, 24, 14),
                new DeviceProfile.MemoryProfile(32L * 1_073_741_824L, 24L * 1_073_741_824L),
                List.of(new DeviceProfile.GpuProfile("NVIDIA", "RTX 4070", 12L * 1_073_741_824L, true)),
                List.of(),
                Instant.now()
        );

        DeviceProfile constrained = new DeviceProfile(
                "dev-b",
                "Windows",
                "11",
                new DeviceProfile.CpuProfile("Mobile CPU", 2, 4, 70),
                new DeviceProfile.MemoryProfile(8L * 1_073_741_824L, 2L * 1_073_741_824L),
                List.of(new DeviceProfile.GpuProfile("Fallback", "Unavailable", 0L, false)),
                List.of(),
                Instant.now()
        );

        double strongScore = calculator.calculate(strong, new BenchmarkResult(92, 88, 90, 85, 89, 1100, false, List.of()));
        double constrainedScore = calculator.calculate(constrained, new BenchmarkResult(30, 35, 22, 28, 28, 1100, true, List.of("stub")));

        assertThat(strongScore).isGreaterThan(constrainedScore);
        assertThat(strongScore).isGreaterThan(75);
        assertThat(constrainedScore).isLessThan(45);
    }
}

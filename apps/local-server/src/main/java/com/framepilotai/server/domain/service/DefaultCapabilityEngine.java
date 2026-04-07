package com.framepilotai.server.domain.service;

import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.CapabilityAssessment;
import com.framepilotai.server.domain.model.CapabilityTier;
import com.framepilotai.server.domain.model.DeviceProfile;
import com.framepilotai.server.domain.port.CapabilityEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultCapabilityEngine implements CapabilityEngine {

    private final CapabilityScoreCalculator scoreCalculator;

    public DefaultCapabilityEngine(CapabilityScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    @Override
    public CapabilityAssessment assess(DeviceProfile deviceProfile, BenchmarkResult benchmarkResult) {
        double totalRamGb = deviceProfile.memory().totalBytes() / 1_073_741_824d;
        double freeRamGb = deviceProfile.memory().availableBytes() / 1_073_741_824d;
        boolean hasGpu = !deviceProfile.gpus().isEmpty() && deviceProfile.gpus().stream().anyMatch(DeviceProfile.GpuProfile::hardwareAcceleration);
        double gpuVramGb = deviceProfile.gpus().stream().mapToLong(DeviceProfile.GpuProfile::vramBytes).max().orElse(0L) / 1_073_741_824d;

        double score = scoreCalculator.calculate(deviceProfile, benchmarkResult);

        List<String> constraints = new ArrayList<>();
        if (!hasGpu) {
            constraints.add("GPU acceleration is limited or unavailable. Keep AI-heavy shots constrained.");
        }
        if (freeRamGb < 6) {
            constraints.add("Free RAM is below 6 GB. Use shorter shot batches and more frequent checkpoints.");
        }
        if (gpuVramGb > 0 && gpuVramGb < 4) {
            constraints.add("Detected VRAM is under 4 GB. Avoid high AI shot ratios.");
        }
        if (benchmarkResult.encodeScore() < 52) {
            constraints.add("Video encode throughput is below the balanced preset target.");
        }
        if (benchmarkResult.usedStub()) {
            constraints.add("OCR and inference benchmark is currently based on a local stub until the runtime is provisioned.");
        }

        CapabilityTier tier;
        if (score >= 82) {
            tier = CapabilityTier.HIGH;
        } else if (score >= 55) {
            tier = CapabilityTier.MEDIUM;
        } else {
            tier = CapabilityTier.LOW;
        }

        return new CapabilityAssessment(tier, score, List.copyOf(constraints));
    }
}

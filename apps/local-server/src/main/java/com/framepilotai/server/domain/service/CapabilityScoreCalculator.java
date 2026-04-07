package com.framepilotai.server.domain.service;

import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.DeviceProfile;
import org.springframework.stereotype.Component;

@Component
public class CapabilityScoreCalculator {

    public double calculate(DeviceProfile deviceProfile, BenchmarkResult benchmarkResult) {
        double totalRamGb = deviceProfile.memory().totalBytes() / 1_073_741_824d;
        double logicalCores = Math.min(deviceProfile.cpu().logicalCores(), 16);
        double gpuVramGb = deviceProfile.gpus().stream().mapToLong(DeviceProfile.GpuProfile::vramBytes).max().orElse(0L) / 1_073_741_824d;
        boolean hardwareGpu = deviceProfile.gpus().stream().anyMatch(DeviceProfile.GpuProfile::hardwareAcceleration);

        double score = benchmarkResult.aggregateScore() * 0.58;
        score += Math.min(totalRamGb, 32) * 0.9;
        score += logicalCores * 1.35;
        score += hardwareGpu ? 9.5 : 0;
        score += Math.min(gpuVramGb, 12) * 2.2;

        return Math.round(Math.min(score, 100d) * 10d) / 10d;
    }
}

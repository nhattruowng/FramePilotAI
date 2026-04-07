package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.DeviceRecommendation;
import com.framepilotai.server.domain.model.PresetLimits;

import java.util.List;
import java.util.UUID;

public record DeviceRecommendationResponse(
        UUID scanId,
        UUID benchmarkId,
        DeviceScanResponse.DeviceProfileDto device,
        BenchmarkRunResponse.BenchmarkDto benchmark,
        CapabilityDto capability,
        PresetDto preset,
        String summary,
        List<String> explanation
) {
    public static DeviceRecommendationResponse from(DeviceRecommendation recommendation) {
        return new DeviceRecommendationResponse(
                recommendation.scanId(),
                recommendation.benchmarkId(),
                DeviceScanResponse.DeviceProfileDto.from(recommendation.deviceProfile()),
                BenchmarkRunResponse.BenchmarkDto.from(recommendation.benchmark()),
                new CapabilityDto(recommendation.capability().tier().name(), recommendation.capability().score(), recommendation.capability().constraints()),
                PresetDto.from(recommendation.preset()),
                recommendation.summary(),
                recommendation.explanation()
        );
    }

    public record CapabilityDto(String tier, double score, List<String> constraints) {
    }

    public record PresetDto(
            String preset,
            String defaultPipeline,
            PresetLimitsDto limits,
            String headline,
            List<String> rationale
    ) {
        static PresetDto from(com.framepilotai.server.domain.model.PresetRecommendation preset) {
            return new PresetDto(
                    preset.preset().name(),
                    preset.defaultPipeline().name(),
                    PresetLimitsDto.from(preset.limits()),
                    preset.headline(),
                    preset.rationale()
            );
        }
    }

    public record PresetLimitsDto(
            int outputWidth,
            int outputHeight,
            int fps,
            int maxShotLengthMillis,
            double aiShotRatio,
            int checkpointIntervalShots
    ) {
        static PresetLimitsDto from(PresetLimits limits) {
            return new PresetLimitsDto(
                    limits.outputWidth(),
                    limits.outputHeight(),
                    limits.fps(),
                    limits.maxShotLengthMillis(),
                    limits.aiShotRatio(),
                    limits.checkpointIntervalShots()
            );
        }
    }
}

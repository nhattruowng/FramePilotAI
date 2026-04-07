package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.BenchmarkRunRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BenchmarkRunResponse(
        UUID benchmarkId,
        UUID scanId,
        Instant executedAt,
        BenchmarkDto benchmark
) {
    public static BenchmarkRunResponse from(BenchmarkRunRecord record) {
        return new BenchmarkRunResponse(record.id(), record.scanId(), record.createdAt(), BenchmarkDto.from(record.result()));
    }

    public record BenchmarkDto(
            double cpuTaskScore,
            double imageProcessingScore,
            double encodeScore,
            double ocrInferenceScore,
            double aggregateScore,
            long benchmarkDurationMillis,
            boolean usedStub,
            List<String> notes
    ) {
        static BenchmarkDto from(BenchmarkResult result) {
            return new BenchmarkDto(
                    result.cpuTaskScore(),
                    result.imageProcessingScore(),
                    result.encodeScore(),
                    result.ocrInferenceScore(),
                    result.aggregateScore(),
                    result.benchmarkDurationMillis(),
                    result.usedStub(),
                    result.notes()
            );
        }
    }
}

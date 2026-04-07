package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.ExportArtifact;
import com.framepilotai.server.domain.model.RenderCheckpoint;
import com.framepilotai.server.domain.model.RenderEventRecord;
import com.framepilotai.server.domain.model.RenderJobSnapshot;
import com.framepilotai.server.domain.model.RuntimeSampleRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RenderJobStatusResponse(
        UUID jobId,
        UUID projectId,
        String status,
        String pipelineType,
        String fallbackLevel,
        String artifactPath,
        String artifactType,
        Instant startedAt,
        Instant finishedAt,
        CheckpointDto checkpoint,
        List<EventDto> events,
        RuntimeDto latestRuntime,
        List<ArtifactDto> artifacts
) {
    public static RenderJobStatusResponse from(RenderJobSnapshot snapshot) {
        RuntimeSampleRecord latestRuntime = snapshot.runtimeSamples().isEmpty()
                ? null
                : snapshot.runtimeSamples().get(snapshot.runtimeSamples().size() - 1);
        return new RenderJobStatusResponse(
                snapshot.job().id(),
                snapshot.job().projectId(),
                snapshot.job().status().name(),
                snapshot.job().pipelineType().name(),
                snapshot.job().fallbackLevel().name(),
                snapshot.job().artifactPath(),
                snapshot.job().artifactType(),
                snapshot.job().startedAt(),
                snapshot.job().finishedAt(),
                CheckpointDto.from(snapshot.checkpoint()),
                snapshot.events().stream().map(EventDto::from).toList(),
                latestRuntime == null ? null : RuntimeDto.from(latestRuntime),
                snapshot.artifacts().stream().map(ArtifactDto::from).toList()
        );
    }

    public record CheckpointDto(
            String currentPhase,
            String currentShotId,
            int totalShots,
            double progressPercent,
            List<String> completedShots,
            List<String> reasons,
            Instant updatedAt
    ) {
        static CheckpointDto from(RenderCheckpoint checkpoint) {
            return new CheckpointDto(
                    checkpoint.currentPhase(),
                    checkpoint.currentShotId(),
                    checkpoint.totalShots(),
                    checkpoint.progressPercent(),
                    checkpoint.completedShots(),
                    checkpoint.reasons(),
                    checkpoint.updatedAt()
            );
        }
    }

    public record EventDto(
            String phase,
            String shotId,
            double progressPercent,
            String message,
            Instant createdAt
    ) {
        static EventDto from(RenderEventRecord event) {
            return new EventDto(event.phase(), event.shotId(), event.progressPercent(), event.message(), event.createdAt());
        }
    }

    public record RuntimeDto(
            String phase,
            String shotId,
            double progressPercent,
            double cpuLoadPercent,
            long usedMemoryBytes,
            long totalMemoryBytes,
            long availableMemoryBytes,
            long estimatedVramBytes,
            Instant capturedAt
    ) {
        static RuntimeDto from(RuntimeSampleRecord sample) {
            return new RuntimeDto(
                    sample.phase(),
                    sample.shotId(),
                    sample.progressPercent(),
                    sample.cpuLoadPercent(),
                    sample.usedMemoryBytes(),
                    sample.totalMemoryBytes(),
                    sample.availableMemoryBytes(),
                    sample.estimatedVramBytes(),
                    sample.capturedAt()
            );
        }
    }

    public record ArtifactDto(
            String label,
            String path,
            String mediaType,
            long sizeBytes,
            boolean verified
    ) {
        static ArtifactDto from(ExportArtifact artifact) {
            return new ArtifactDto(artifact.label(), artifact.path(), artifact.mediaType(), artifact.sizeBytes(), artifact.verified());
        }
    }
}

package com.framepilotai.server.domain.model;

import java.util.List;

public record RenderJobSnapshot(
        RenderJobRecord job,
        RenderCheckpoint checkpoint,
        List<RenderEventRecord> events,
        List<RuntimeSampleRecord> runtimeSamples,
        List<ExportArtifact> artifacts
) {
}

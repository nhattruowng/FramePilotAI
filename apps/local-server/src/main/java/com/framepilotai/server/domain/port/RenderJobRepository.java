package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.RenderEventRecord;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.RenderJobRecord;
import com.framepilotai.server.domain.model.RuntimeSampleRecord;
import com.framepilotai.server.domain.model.RenderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RenderJobRepository {
    RenderJobRecord create(UUID jobId, UUID projectId, PipelinePlan plan, String checkpointPath);

    Optional<RenderJobRecord> findById(UUID jobId);

    RenderJobRecord updateStatus(UUID jobId, RenderStatus status, String checkpointPath);

    RenderJobRecord complete(UUID jobId, RenderStatus status, String artifactPath, String artifactType, String checkpointPath);

    RenderJobRecord fail(UUID jobId, String checkpointPath);

    void appendEvent(UUID jobId, String phase, String shotId, double progressPercent, String message);

    List<RenderEventRecord> listEvents(UUID jobId);

    void appendRuntimeSample(UUID jobId, String phase, String shotId, double progressPercent, com.framepilotai.server.domain.model.RuntimeSnapshot snapshot);

    List<RuntimeSampleRecord> listRuntimeSamples(UUID jobId);
}

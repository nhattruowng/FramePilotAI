package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.ExportArtifact;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.RenderCheckpoint;
import com.framepilotai.server.domain.model.RenderPreparation;
import com.framepilotai.server.domain.model.RenderProfile;
import com.framepilotai.server.domain.model.RenderedShot;
import com.framepilotai.server.domain.model.SceneData;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExportService {
    Path createJobDirectory(UUID projectId, UUID jobId) throws Exception;

    Path writeStoryPackage(UUID projectId, UUID jobId, ProjectAnalysis analysis) throws Exception;

    Path writeCheckpoint(UUID projectId, UUID jobId, RenderCheckpoint checkpoint) throws Exception;

    Path writeSubtitleFile(UUID projectId, UUID jobId, List<SceneData> scenes) throws Exception;

    Path writeTtsHook(UUID projectId, UUID jobId, ProjectAnalysis analysis) throws Exception;

    Path writeAudioHook(UUID projectId, UUID jobId, ProjectAnalysis analysis) throws Exception;

    Path writeConcatManifest(UUID projectId, UUID jobId, List<RenderedShot> renderedShots) throws Exception;

    Optional<RenderCheckpoint> readCheckpoint(UUID projectId, UUID jobId) throws Exception;

    List<ExportArtifact> listArtifacts(UUID projectId, UUID jobId) throws Exception;

    boolean verifyArtifact(Path artifactPath) throws Exception;

    Optional<Path> resolveFfmpegExecutable();

    String sanitizeText(String value);
}

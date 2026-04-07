package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.RenderOutcome;

import com.framepilotai.server.domain.model.RenderPreparation;
import com.framepilotai.server.domain.model.RenderProfile;
import com.framepilotai.server.domain.model.RenderedShot;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.model.ShotData;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RenderOrchestrator {
    RenderPreparation prepare(UUID jobId, ProjectRecord project, ProjectAnalysis analysis, PipelinePlan plan) throws Exception;

    RenderedShot renderShot(RenderPreparation preparation, SceneData scene, ShotData shot, ProjectAnalysis analysis, PipelinePlan plan, RenderProfile profile) throws Exception;

    Path muxFinal(RenderPreparation preparation, List<RenderedShot> renderedShots, ProjectAnalysis analysis, PipelinePlan plan, RenderProfile profile) throws Exception;

    Optional<RenderOutcome> fallbackPackage(RenderPreparation preparation, ProjectAnalysis analysis, PipelinePlan plan) throws Exception;
}

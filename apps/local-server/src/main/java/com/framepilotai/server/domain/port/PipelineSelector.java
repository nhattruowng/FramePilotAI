package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.CapabilityAssessment;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.PresetRecommendation;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.SceneData;

import java.util.List;

public interface PipelineSelector {
    PipelinePlan select(ProjectRecord project, CapabilityAssessment assessment, PresetRecommendation preset, List<SceneData> scenes);
}

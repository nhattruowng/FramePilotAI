package com.framepilotai.server.domain.service;

import com.framepilotai.server.domain.model.CapabilityAssessment;
import com.framepilotai.server.domain.model.FallbackLevel;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.PipelineType;
import com.framepilotai.server.domain.model.PresetRecommendation;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.port.PipelineSelector;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultPipelineSelector implements PipelineSelector {

    @Override
    public PipelinePlan select(ProjectRecord project, CapabilityAssessment assessment, PresetRecommendation preset, List<SceneData> scenes) {
        int shotCount = scenes.stream().mapToInt(scene -> scene.shots().size()).sum();
        double averageComplexity = scenes.stream().mapToDouble(SceneData::complexityScore).average().orElse(0);
        List<String> reasons = new ArrayList<>(preset.rationale());
        PipelineType pipelineType = preset.defaultPipeline();
        FallbackLevel fallbackLevel = FallbackLevel.NONE;

        if (averageComplexity >= 0.78 && assessment.score() >= 82 && assessment.constraints().isEmpty()) {
            pipelineType = PipelineType.ENHANCED_HYBRID;
            reasons.add("Project complexity and capability score support enhanced hybrid treatment.");
        }

        if (shotCount > 12 && pipelineType == PipelineType.ENHANCED_HYBRID) {
            pipelineType = PipelineType.HYBRID;
            reasons.add("Shot volume is high. Reducing one tier for stability-first rendering.");
        }

        if ((assessment.constraints().size() >= 2 || averageComplexity <= 0.32) && pipelineType != PipelineType.MOTION_COMIC) {
            pipelineType = PipelineType.HYBRID_LITE;
            fallbackLevel = FallbackLevel.F1;
            reasons.add("Hardware constraints or low narrative complexity favor a lighter hybrid baseline.");
        }

        if (assessment.score() < 52) {
            pipelineType = PipelineType.MOTION_COMIC;
            fallbackLevel = FallbackLevel.F2;
            reasons.add("Capability score is below hybrid threshold.");
        }

        reasons.add("Project " + project.name() + " planned with " + shotCount + " shots at average complexity " + Math.round(averageComplexity * 100d) / 100d + ".");
        return new PipelinePlan(pipelineType, preset.preset(), fallbackLevel, List.copyOf(reasons));
    }
}

package com.framepilotai.server.domain.service;

import com.framepilotai.server.domain.model.CapabilityAssessment;
import com.framepilotai.server.domain.model.CapabilityTier;
import com.framepilotai.server.domain.model.PipelineType;
import com.framepilotai.server.domain.model.PresetLimits;
import com.framepilotai.server.domain.model.PresetName;
import com.framepilotai.server.domain.model.PresetRecommendation;
import com.framepilotai.server.domain.port.PresetEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultPresetEngine implements PresetEngine {

    @Override
    public PresetRecommendation recommend(CapabilityAssessment assessment) {
        List<String> rationale = new ArrayList<>(assessment.constraints());
        if (assessment.tier() == CapabilityTier.HIGH) {
            rationale.add("High capability score supports quality-first hybrid rendering with more AI-assisted shots.");
            return new PresetRecommendation(
                    PresetName.QUALITY,
                    PipelineType.ENHANCED_HYBRID,
                    new PresetLimits(1920, 1080, 30, 4200, 0.55, 4),
                    "Quality preset unlocked",
                    List.copyOf(rationale)
            );
        }
        if (assessment.tier() == CapabilityTier.MEDIUM) {
            rationale.add("Medium capability score favors balanced preset with selective animation and tighter shot limits.");
            return new PresetRecommendation(
                    PresetName.BALANCED,
                    PipelineType.HYBRID,
                    new PresetLimits(1600, 900, 24, 3200, 0.28, 3),
                    "Balanced preset recommended",
                    List.copyOf(rationale)
            );
        }
        rationale.add("Low capability score favors deterministic motion-comic export and minimal AI ratio.");
        return new PresetRecommendation(
                PresetName.ECO,
                PipelineType.MOTION_COMIC,
                new PresetLimits(1280, 720, 20, 2400, 0.10, 2),
                "Eco preset recommended",
                List.copyOf(rationale)
        );
    }
}

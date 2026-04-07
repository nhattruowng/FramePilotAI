package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.CapabilityAssessment;
import com.framepilotai.server.domain.model.PresetRecommendation;

public interface PresetEngine {
    PresetRecommendation recommend(CapabilityAssessment assessment);
}

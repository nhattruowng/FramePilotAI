package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.RuntimeSnapshot;

public interface FallbackEngine {
    PipelinePlan apply(PipelinePlan currentPlan, RuntimeSnapshot snapshot);
}

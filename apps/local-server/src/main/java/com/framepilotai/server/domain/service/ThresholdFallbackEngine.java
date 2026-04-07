package com.framepilotai.server.domain.service;

import com.framepilotai.server.common.config.FramePilotProperties;
import com.framepilotai.server.domain.model.FallbackLevel;
import com.framepilotai.server.domain.model.PipelinePlan;
import com.framepilotai.server.domain.model.PipelineType;
import com.framepilotai.server.domain.model.RuntimeSnapshot;
import com.framepilotai.server.domain.port.FallbackEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ThresholdFallbackEngine implements FallbackEngine {

    private final FramePilotProperties properties;

    public ThresholdFallbackEngine(FramePilotProperties properties) {
        this.properties = properties;
    }

    @Override
    public PipelinePlan apply(PipelinePlan currentPlan, RuntimeSnapshot snapshot) {
        double memoryUsagePercent = snapshot.totalMemoryBytes() == 0
                ? 0
                : (double) snapshot.usedMemoryBytes() / snapshot.totalMemoryBytes() * 100;
        double vramUsagePercent = snapshot.estimatedVramBytes() <= 0
                ? 0
                : (double) snapshot.usedMemoryBytes() / Math.max(snapshot.estimatedVramBytes(), 1L) * 100;

        List<String> reasons = new ArrayList<>(currentPlan.reasons());
        PipelineType pipelineType = currentPlan.pipelineType();
        FallbackLevel fallbackLevel = currentPlan.fallbackLevel();

        if (memoryUsagePercent >= properties.getRuntime().getMemoryPressureThresholdPercent()) {
            pipelineType = PipelineType.MOTION_COMIC;
            fallbackLevel = FallbackLevel.F4;
            reasons.add("Memory pressure threshold exceeded. Forcing motion comic pipeline.");
        } else if (vramUsagePercent >= properties.getRuntime().getVramPressureThresholdPercent()) {
            pipelineType = PipelineType.HYBRID_LITE;
            fallbackLevel = fallbackLevel.ordinal() > FallbackLevel.F3.ordinal() ? fallbackLevel : FallbackLevel.F3;
            reasons.add("Estimated VRAM pressure exceeded threshold. Lowering effects and AI intensity.");
        } else if (snapshot.cpuLoadPercent() >= properties.getRuntime().getCpuPressureThresholdPercent()) {
            pipelineType = PipelineType.HYBRID_LITE;
            fallbackLevel = fallbackLevel.ordinal() > FallbackLevel.F2.ordinal() ? fallbackLevel : FallbackLevel.F2;
            reasons.add("CPU pressure exceeded threshold. Dropping to lighter pipeline.");
        }

        return new PipelinePlan(pipelineType, currentPlan.preset(), fallbackLevel, List.copyOf(reasons));
    }
}

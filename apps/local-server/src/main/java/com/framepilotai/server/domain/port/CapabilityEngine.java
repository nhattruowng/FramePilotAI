package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.CapabilityAssessment;
import com.framepilotai.server.domain.model.DeviceProfile;

public interface CapabilityEngine {
    CapabilityAssessment assess(DeviceProfile deviceProfile, BenchmarkResult benchmarkResult);
}

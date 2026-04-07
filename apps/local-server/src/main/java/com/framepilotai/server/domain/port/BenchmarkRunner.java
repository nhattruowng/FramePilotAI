package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.DeviceProfile;

public interface BenchmarkRunner {
    BenchmarkResult run(DeviceProfile deviceProfile);
}

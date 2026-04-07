package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.BenchmarkRunRecord;
import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.DeviceProfile;
import com.framepilotai.server.domain.model.DeviceScanRecord;

import java.util.Optional;
import java.util.UUID;

public interface DeviceIntelligenceRepository {
    DeviceScanRecord saveScan(DeviceProfile deviceProfile);

    Optional<DeviceScanRecord> findScan(UUID scanId);

    Optional<DeviceScanRecord> findLatestScan();

    BenchmarkRunRecord saveBenchmark(UUID scanId, BenchmarkResult benchmarkResult);

    Optional<BenchmarkRunRecord> findBenchmark(UUID benchmarkId);

    Optional<BenchmarkRunRecord> findLatestBenchmark();
}

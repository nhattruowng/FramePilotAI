package com.framepilotai.server.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.framepilotai.server.domain.model.BenchmarkResult;
import com.framepilotai.server.domain.model.BenchmarkRunRecord;
import com.framepilotai.server.domain.model.DeviceProfile;
import com.framepilotai.server.domain.model.DeviceScanRecord;
import com.framepilotai.server.domain.port.DeviceIntelligenceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcDeviceIntelligenceRepository implements DeviceIntelligenceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcDeviceIntelligenceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public DeviceScanRecord saveScan(DeviceProfile deviceProfile) {
        try {
            UUID scanId = UUID.randomUUID();
            Instant now = Instant.now();
            jdbcTemplate.update(
                    "insert into device_scans(id, device_id, os_name, os_version, profile_json, created_at) values (?, ?, ?, ?, ?, ?)",
                    scanId.toString(),
                    deviceProfile.deviceId(),
                    deviceProfile.osName(),
                    deviceProfile.osVersion(),
                    objectMapper.writeValueAsString(deviceProfile),
                    Timestamp.from(now)
            );
            return new DeviceScanRecord(scanId, deviceProfile, now);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist device scan", exception);
        }
    }

    @Override
    public Optional<DeviceScanRecord> findScan(UUID scanId) {
        return jdbcTemplate.query(
                "select id, profile_json, created_at from device_scans where id = ?",
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new DeviceScanRecord(
                            UUID.fromString(rs.getString("id")),
                            deserializeDeviceProfile(rs.getString("profile_json")),
                            rs.getTimestamp("created_at").toInstant()
                    ));
                },
                scanId.toString()
        );
    }

    @Override
    public Optional<DeviceScanRecord> findLatestScan() {
        return jdbcTemplate.query(
                "select id, profile_json, created_at from device_scans order by created_at desc limit 1",
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new DeviceScanRecord(
                            UUID.fromString(rs.getString("id")),
                            deserializeDeviceProfile(rs.getString("profile_json")),
                            rs.getTimestamp("created_at").toInstant()
                    ));
                }
        );
    }

    @Override
    public BenchmarkRunRecord saveBenchmark(UUID scanId, BenchmarkResult benchmarkResult) {
        try {
            UUID benchmarkId = UUID.randomUUID();
            Instant now = Instant.now();
            jdbcTemplate.update(
                    "insert into benchmark_runs(id, scan_id, result_json, aggregate_score, created_at) values (?, ?, ?, ?, ?)",
                    benchmarkId.toString(),
                    scanId.toString(),
                    objectMapper.writeValueAsString(benchmarkResult),
                    benchmarkResult.aggregateScore(),
                    Timestamp.from(now)
            );
            return new BenchmarkRunRecord(benchmarkId, scanId, benchmarkResult, now);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist benchmark run", exception);
        }
    }

    @Override
    public Optional<BenchmarkRunRecord> findBenchmark(UUID benchmarkId) {
        return jdbcTemplate.query(
                "select id, scan_id, result_json, created_at from benchmark_runs where id = ?",
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new BenchmarkRunRecord(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("scan_id")),
                            deserializeBenchmarkResult(rs.getString("result_json")),
                            rs.getTimestamp("created_at").toInstant()
                    ));
                },
                benchmarkId.toString()
        );
    }

    @Override
    public Optional<BenchmarkRunRecord> findLatestBenchmark() {
        return jdbcTemplate.query(
                "select id, scan_id, result_json, created_at from benchmark_runs order by created_at desc limit 1",
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new BenchmarkRunRecord(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("scan_id")),
                            deserializeBenchmarkResult(rs.getString("result_json")),
                            rs.getTimestamp("created_at").toInstant()
                    ));
                }
        );
    }

    private DeviceProfile deserializeDeviceProfile(String json) {
        try {
            return objectMapper.readValue(json, DeviceProfile.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to deserialize device profile", exception);
        }
    }

    private BenchmarkResult deserializeBenchmarkResult(String json) {
        try {
            return objectMapper.readValue(json, BenchmarkResult.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to deserialize benchmark result", exception);
        }
    }
}

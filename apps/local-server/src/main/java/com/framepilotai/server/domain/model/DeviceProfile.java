package com.framepilotai.server.domain.model;

import java.time.Instant;
import java.util.List;

public record DeviceProfile(
        String deviceId,
        String osName,
        String osVersion,
        CpuProfile cpu,
        MemoryProfile memory,
        List<GpuProfile> gpus,
        List<DiskProfile> disks,
        Instant capturedAt
) {
    public record CpuProfile(String model, int physicalCores, int logicalCores, double systemLoadPercent) {}
    public record MemoryProfile(long totalBytes, long availableBytes) {}
    public record GpuProfile(String vendor, String name, long vramBytes, boolean hardwareAcceleration) {}
    public record DiskProfile(String name, long totalBytes, long freeBytes) {}
}

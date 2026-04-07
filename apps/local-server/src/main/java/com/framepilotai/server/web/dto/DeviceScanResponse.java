package com.framepilotai.server.web.dto;

import com.framepilotai.server.domain.model.DeviceProfile;
import com.framepilotai.server.domain.model.DeviceScanRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeviceScanResponse(
        UUID scanId,
        Instant scannedAt,
        DeviceProfileDto device
) {
    public static DeviceScanResponse from(DeviceScanRecord scan) {
        return new DeviceScanResponse(scan.id(), scan.createdAt(), DeviceProfileDto.from(scan.profile()));
    }

    public record DeviceProfileDto(
            String deviceId,
            String osName,
            String osVersion,
            CpuDto cpu,
            MemoryDto memory,
            List<GpuDto> gpus,
            List<DiskDto> disks,
            Instant capturedAt
    ) {
        static DeviceProfileDto from(DeviceProfile profile) {
            return new DeviceProfileDto(
                    profile.deviceId(),
                    profile.osName(),
                    profile.osVersion(),
                    new CpuDto(profile.cpu().model(), profile.cpu().physicalCores(), profile.cpu().logicalCores(), profile.cpu().systemLoadPercent()),
                    new MemoryDto(profile.memory().totalBytes(), profile.memory().availableBytes()),
                    profile.gpus().stream().map(gpu -> new GpuDto(gpu.vendor(), gpu.name(), gpu.vramBytes(), gpu.hardwareAcceleration())).toList(),
                    profile.disks().stream().map(disk -> new DiskDto(disk.name(), disk.totalBytes(), disk.freeBytes())).toList(),
                    profile.capturedAt()
            );
        }
    }

    public record CpuDto(String model, int physicalCores, int logicalCores, double systemLoadPercent) {
    }

    public record MemoryDto(long totalBytes, long availableBytes) {
    }

    public record GpuDto(String vendor, String name, long vramBytes, boolean hardwareAcceleration) {
    }

    public record DiskDto(String name, long totalBytes, long freeBytes) {
    }
}

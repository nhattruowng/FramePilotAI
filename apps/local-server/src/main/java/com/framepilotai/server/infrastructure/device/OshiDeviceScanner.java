package com.framepilotai.server.infrastructure.device;

import com.framepilotai.server.domain.model.DeviceProfile;
import com.framepilotai.server.domain.port.DeviceScanner;
import com.framepilotai.server.domain.port.GpuDetailsProvider;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OSFileStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OshiDeviceScanner implements DeviceScanner {

    private final SystemInfo systemInfo = new SystemInfo();
    private final GpuDetailsProvider gpuDetailsProvider;

    public OshiDeviceScanner(GpuDetailsProvider gpuDetailsProvider) {
        this.gpuDetailsProvider = gpuDetailsProvider;
    }

    @Override
    public DeviceProfile scan() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] ticks = processor.getSystemCpuLoadTicks();
        double systemLoad = processor.getSystemCpuLoadBetweenTicks(ticks) * 100;

        List<DeviceProfile.GpuProfile> gpus = gpuDetailsProvider.detect();

        List<DeviceProfile.DiskProfile> disks = systemInfo.getOperatingSystem().getFileSystem().getFileStores().stream()
                .limit(6)
                .map(this::toDiskProfile)
                .toList();

        return new DeviceProfile(
                systemInfo.getOperatingSystem().getFamily() + "-" + systemInfo.getOperatingSystem().getVersionInfo().getBuildNumber(),
                systemInfo.getOperatingSystem().getFamily(),
                systemInfo.getOperatingSystem().getVersionInfo().toString(),
                new DeviceProfile.CpuProfile(
                        processor.getProcessorIdentifier().getName(),
                        processor.getPhysicalProcessorCount(),
                        processor.getLogicalProcessorCount(),
                        Math.round(systemLoad * 10d) / 10d
                ),
                new DeviceProfile.MemoryProfile(
                        systemInfo.getHardware().getMemory().getTotal(),
                        systemInfo.getHardware().getMemory().getAvailable()
                ),
                gpus,
                disks,
                Instant.now()
        );
    }

    private DeviceProfile.DiskProfile toDiskProfile(OSFileStore store) {
        String name = store.getName() + (store.getMount() == null || store.getMount().isBlank() ? "" : " (" + store.getMount() + ")");
        return new DeviceProfile.DiskProfile(name, Math.max(store.getTotalSpace(), 0L), Math.max(store.getUsableSpace(), 0L));
    }
}

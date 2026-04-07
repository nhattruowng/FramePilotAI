package com.framepilotai.server.infrastructure.device;

import com.framepilotai.server.domain.model.DeviceProfile;
import com.framepilotai.server.domain.port.GpuDetailsProvider;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OshiGpuDetailsProvider implements GpuDetailsProvider {

    private final SystemInfo systemInfo = new SystemInfo();

    @Override
    public List<DeviceProfile.GpuProfile> detect() {
        return systemInfo.getHardware().getGraphicsCards().stream()
                .map(this::toGpuProfile)
                .toList();
    }

    private DeviceProfile.GpuProfile toGpuProfile(GraphicsCard card) {
        long vram = Math.max(card.getVRam(), 0L);
        return new DeviceProfile.GpuProfile(
                card.getVendor() == null || card.getVendor().isBlank() ? "Unknown vendor" : card.getVendor(),
                card.getName() == null || card.getName().isBlank() ? "Unknown GPU" : card.getName(),
                vram,
                vram > 0
        );
    }
}

package com.framepilotai.server.infrastructure.runtime;

import com.framepilotai.server.domain.model.RuntimeSnapshot;
import com.framepilotai.server.domain.port.RuntimeMonitor;
import oshi.SystemInfo;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OshiRuntimeMonitor implements RuntimeMonitor {

    private final SystemInfo systemInfo = new SystemInfo();

    @Override
    public RuntimeSnapshot capture() {
        long totalMemory = systemInfo.getHardware().getMemory().getTotal();
        long availableMemory = systemInfo.getHardware().getMemory().getAvailable();
        long usedMemory = totalMemory - availableMemory;
        long estimatedVram = systemInfo.getHardware().getGraphicsCards().stream()
                .mapToLong(card -> Math.max(card.getVRam(), 0))
                .max()
                .orElse(0L);

        return new RuntimeSnapshot(
                Math.round(systemInfo.getHardware().getProcessor().getSystemCpuLoad(500) * 1000d) / 10d,
                usedMemory,
                totalMemory,
                availableMemory,
                estimatedVram,
                Instant.now()
        );
    }
}

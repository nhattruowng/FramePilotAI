package com.framepilotai.server.infrastructure.device;

import com.framepilotai.server.domain.model.DeviceProfile;
import com.framepilotai.server.domain.port.GpuDetailsProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class FallbackGpuDetailsProvider implements GpuDetailsProvider {

    private final OshiGpuDetailsProvider delegate;

    public FallbackGpuDetailsProvider(OshiGpuDetailsProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<DeviceProfile.GpuProfile> detect() {
        try {
            List<DeviceProfile.GpuProfile> detected = delegate.detect();
            if (!detected.isEmpty()) {
                return detected;
            }
        } catch (Exception ignored) {
            // Fall through to cross-platform-safe placeholder profile.
        }
        return List.of(new DeviceProfile.GpuProfile(
                "Fallback",
                "GPU details unavailable on this runtime",
                0L,
                false
        ));
    }
}

package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.DeviceProfile;

import java.util.List;

public interface GpuDetailsProvider {
    List<DeviceProfile.GpuProfile> detect();
}

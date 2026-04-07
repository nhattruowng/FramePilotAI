package com.framepilotai.server.domain.port;

import com.framepilotai.server.domain.model.DeviceProfile;

public interface DeviceScanner {
    DeviceProfile scan();
}

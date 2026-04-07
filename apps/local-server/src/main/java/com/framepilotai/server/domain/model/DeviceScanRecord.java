package com.framepilotai.server.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DeviceScanRecord(
        UUID id,
        DeviceProfile profile,
        Instant createdAt
) {
}

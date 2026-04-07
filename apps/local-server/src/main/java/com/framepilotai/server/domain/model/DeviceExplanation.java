package com.framepilotai.server.domain.model;

import java.util.List;
import java.util.UUID;

public record DeviceExplanation(
        UUID scanId,
        UUID benchmarkId,
        String headline,
        String summary,
        List<String> reasons
) {
}

package com.framepilotai.server.domain.model;

import java.nio.file.Path;

public record RenderedShot(
        String shotId,
        Path clipPath,
        int durationMillis
) {
}

package com.framepilotai.server.domain.model;

import java.nio.file.Path;
import java.util.UUID;

public record RenderPreparation(
        UUID jobId,
        UUID projectId,
        Path jobDirectory,
        Path storyPackagePath,
        Path subtitleFilePath,
        Path ttsHookPath,
        Path audioHookPath
) {
}

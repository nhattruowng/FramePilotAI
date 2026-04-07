package com.framepilotai.server.infrastructure.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.framepilotai.server.common.config.FramePilotProperties;
import com.framepilotai.server.domain.model.ExportArtifact;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.RenderCheckpoint;
import com.framepilotai.server.domain.model.RenderedShot;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.port.ExportService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class FileSystemExportService implements ExportService {

    private final Path exportsRoot;
    private final Path toolsRoot;
    private final ObjectMapper objectMapper;
    private final String configuredFfmpegExecutable;

    public FileSystemExportService(FramePilotProperties properties, ObjectMapper objectMapper) throws Exception {
        Path storageRoot = Path.of(properties.getStorage().getRoot()).toAbsolutePath().normalize();
        Path workspaceRoot = storageRoot.getParent() == null ? storageRoot : storageRoot.getParent();
        this.exportsRoot = storageRoot.resolve(properties.getStorage().getExportsDir()).normalize();
        this.toolsRoot = workspaceRoot.resolve("tools").normalize();
        this.objectMapper = objectMapper;
        this.configuredFfmpegExecutable = properties.getFfmpeg().getExecutable();
        Files.createDirectories(exportsRoot);
        Files.createDirectories(toolsRoot);
    }

    @Override
    public Path createJobDirectory(UUID projectId, UUID jobId) throws Exception {
        Path jobDirectory = exportsRoot.resolve(projectId.toString()).resolve(jobId.toString());
        Files.createDirectories(jobDirectory);
        return jobDirectory;
    }

    @Override
    public Path writeStoryPackage(UUID projectId, UUID jobId, ProjectAnalysis analysis) throws Exception {
        Path packageFile = createJobDirectory(projectId, jobId).resolve("story-package.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(packageFile.toFile(), analysis);
        return packageFile;
    }

    @Override
    public Path writeCheckpoint(UUID projectId, UUID jobId, RenderCheckpoint checkpoint) throws Exception {
        Path checkpointFile = createJobDirectory(projectId, jobId).resolve("render-checkpoint.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(checkpointFile.toFile(), checkpoint);
        return checkpointFile;
    }

    @Override
    public Path writeSubtitleFile(UUID projectId, UUID jobId, List<SceneData> scenes) throws Exception {
        Path subtitleFile = createJobDirectory(projectId, jobId).resolve("captions.srt");
        List<String> lines = new ArrayList<>();
        double currentSeconds = 0;
        int index = 1;
        for (SceneData scene : scenes) {
            for (var shot : scene.shots()) {
                lines.add(Integer.toString(index++));
                lines.add(formatSrtTime(currentSeconds) + " --> " + formatSrtTime(currentSeconds + (shot.durationMillis() / 1000d)));
                lines.add(scene.title() + " | " + shot.notes());
                lines.add("");
                currentSeconds += shot.durationMillis() / 1000d;
            }
        }
        Files.writeString(subtitleFile, String.join(System.lineSeparator(), lines), StandardCharsets.UTF_8);
        return subtitleFile;
    }

    @Override
    public Path writeTtsHook(UUID projectId, UUID jobId, ProjectAnalysis analysis) throws Exception {
        Path hookFile = createJobDirectory(projectId, jobId).resolve("tts-hook.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(hookFile.toFile(), java.util.Map.of(
                "enabled", false,
                "note", "Hook point for local TTS generation.",
                "shots", analysis.scenes().stream().flatMap(scene -> scene.shots().stream()).map(shot -> java.util.Map.of(
                        "shotId", shot.shotId(),
                        "panelId", shot.panelId(),
                        "durationMillis", shot.durationMillis()
                )).toList()
        ));
        return hookFile;
    }

    @Override
    public Path writeAudioHook(UUID projectId, UUID jobId, ProjectAnalysis analysis) throws Exception {
        Path hookFile = createJobDirectory(projectId, jobId).resolve("audio-hook.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(hookFile.toFile(), java.util.Map.of(
                "enabled", false,
                "note", "Hook point for local background music and sound effects muxing.",
                "sceneCount", analysis.scenes().size(),
                "projectId", analysis.project().id().toString()
        ));
        return hookFile;
    }

    @Override
    public Path writeConcatManifest(UUID projectId, UUID jobId, List<RenderedShot> renderedShots) throws Exception {
        Path manifest = createJobDirectory(projectId, jobId).resolve("shots.concat.txt");
        List<String> lines = renderedShots.stream()
                .sorted(Comparator.comparing(RenderedShot::shotId))
                .map(shot -> "file '" + shot.clipPath().toAbsolutePath().normalize().toString().replace("'", "'\\''") + "'")
                .toList();
        Files.writeString(manifest, String.join(System.lineSeparator(), lines), StandardCharsets.UTF_8);
        return manifest;
    }

    @Override
    public Optional<RenderCheckpoint> readCheckpoint(UUID projectId, UUID jobId) throws Exception {
        Path checkpointFile = createJobDirectory(projectId, jobId).resolve("render-checkpoint.json");
        if (!Files.exists(checkpointFile)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(checkpointFile.toFile(), RenderCheckpoint.class));
    }

    @Override
    public List<ExportArtifact> listArtifacts(UUID projectId, UUID jobId) throws Exception {
        Path jobDirectory = createJobDirectory(projectId, jobId);
        if (!Files.exists(jobDirectory)) {
            return List.of();
        }
        try (var stream = Files.list(jobDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted()
                    .map(path -> {
                        try {
                            return new ExportArtifact(
                                    path.getFileName().toString(),
                                    path.toAbsolutePath().normalize().toString(),
                                    Files.probeContentType(path),
                                    Files.size(path),
                                    verifyArtifact(path)
                            );
                        } catch (Exception exception) {
                            return new ExportArtifact(
                                    path.getFileName().toString(),
                                    path.toAbsolutePath().normalize().toString(),
                                    null,
                                    0,
                                    false
                            );
                        }
                    })
                    .toList();
        }
    }

    @Override
    public boolean verifyArtifact(Path artifactPath) throws Exception {
        return Files.exists(artifactPath) && Files.isRegularFile(artifactPath) && Files.size(artifactPath) > 0;
    }

    @Override
    public Optional<Path> resolveFfmpegExecutable() {
        if (configuredFfmpegExecutable != null && !configuredFfmpegExecutable.isBlank()) {
            Path configured = Path.of(configuredFfmpegExecutable).toAbsolutePath().normalize();
            if (Files.exists(configured)) {
                return Optional.of(configured);
            }
        }

        Path bundled = toolsRoot.resolve("ffmpeg").resolve("bin").resolve("ffmpeg.exe");
        if (Files.exists(bundled)) {
            return Optional.of(bundled);
        }

        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }

        for (String entry : path.split(";")) {
            Path candidate = Path.of(entry.trim(), "ffmpeg.exe");
            if (Files.exists(candidate)) {
                return Optional.of(candidate.toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }

    @Override
    public String sanitizeText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(":", "\\:")
                .replace("'", "\\'")
                .replace(",", "\\,")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("%", "\\%")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private String formatSrtTime(double totalSeconds) {
        long millis = Math.round(totalSeconds * 1000);
        long hours = millis / 3_600_000;
        long minutes = (millis % 3_600_000) / 60_000;
        long seconds = (millis % 60_000) / 1000;
        long remainder = millis % 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", hours, minutes, seconds, remainder);
    }
}

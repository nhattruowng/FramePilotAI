package com.framepilotai.server.infrastructure.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.framepilotai.server.common.config.FramePilotProperties;
import com.framepilotai.server.domain.model.OcrBlock;
import com.framepilotai.server.domain.model.PanelData;
import com.framepilotai.server.domain.model.ProjectAnalysis;
import com.framepilotai.server.domain.model.SceneData;
import com.framepilotai.server.domain.port.ProjectMetadataStore;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class FileSystemProjectMetadataStore implements ProjectMetadataStore {

    private final Path projectsRoot;
    private final ObjectMapper objectMapper;

    public FileSystemProjectMetadataStore(FramePilotProperties properties, ObjectMapper objectMapper) throws Exception {
        this.projectsRoot = Path.of(properties.getStorage().getRoot(), properties.getStorage().getProjectsDir()).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
        Files.createDirectories(projectsRoot);
    }

    @Override
    public Path writePanels(UUID projectId, List<PanelData> panels) throws Exception {
        Path target = metadataDir(projectId).resolve("panels.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), panels);
        return target;
    }

    @Override
    public Path writeOcr(UUID projectId, List<OcrBlock> blocks) throws Exception {
        Path target = metadataDir(projectId).resolve("ocr.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), blocks);
        return target;
    }

    @Override
    public Path writeScenePlan(UUID projectId, List<SceneData> scenes) throws Exception {
        Path target = metadataDir(projectId).resolve("scene-plan.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), scenes);
        return target;
    }

    @Override
    public Path writeAnalysis(UUID projectId, ProjectAnalysis analysis) throws Exception {
        Path target = metadataDir(projectId).resolve("analysis.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), analysis);
        return target;
    }

    private Path metadataDir(UUID projectId) throws Exception {
        Path target = projectsRoot.resolve(projectId.toString()).resolve("metadata");
        Files.createDirectories(target);
        return target;
    }
}

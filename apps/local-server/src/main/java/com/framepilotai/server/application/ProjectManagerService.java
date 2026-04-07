package com.framepilotai.server.application;

import com.framepilotai.server.common.error.DomainException;
import com.framepilotai.server.domain.model.AssetRecord;
import com.framepilotai.server.domain.model.ProjectMetadataRecord;
import com.framepilotai.server.domain.model.ProjectRecord;
import com.framepilotai.server.domain.model.ProjectWorkspace;
import com.framepilotai.server.domain.port.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectManagerService {

    private final ProjectRepository projectRepository;

    public ProjectManagerService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectRecord createProject(String name, String description, String sourceType) {
        return projectRepository.create(name, description, sourceType);
    }

    public ProjectRecord importAssets(UUID projectId, List<String> assetPaths) {
        if (assetPaths == null || assetPaths.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "At least one asset path is required.");
        }
        List<AssetRecord> assets = new java.util.ArrayList<>();
        for (int index = 0; index < assetPaths.size(); index++) {
            String path = assetPaths.get(index);
                    Path assetPath = Path.of(path).toAbsolutePath().normalize();
                    if (!Files.exists(assetPath)) {
                        throw new DomainException(HttpStatus.BAD_REQUEST, "Asset does not exist: " + assetPath);
                    }
                    try {
                        assets.add(new AssetRecord(
                                UUID.randomUUID(),
                                projectId,
                                assetPath.toString(),
                                Files.probeContentType(assetPath),
                                Files.size(assetPath),
                                index
                        ));
                    } catch (Exception exception) {
                        throw new DomainException(HttpStatus.BAD_REQUEST, "Unable to inspect asset: " + assetPath);
                    }
        }
        return projectRepository.importAssets(projectId, assets);
    }

    public ProjectWorkspace getWorkspace(UUID projectId) {
        return projectRepository.getWorkspace(projectId);
    }

    public ProjectRecord importDemoProject(List<String> demoAssets) {
        ProjectRecord project = createProject("Demo Comic Project", "Bundled sample comic project", "image-sequence");
        ProjectRecord updated = importAssets(project.id(), demoAssets);
        projectRepository.saveMetadata(
                project.id(),
                new ProjectMetadataRecord("image-sequence", "Bundled sample comic project", "", "LOW", 0, 0, "{\"demo\":true}", Instant.now())
        );
        return updated;
    }
}
